package com.distributed.base.retrieval.index;

import com.distributed.base.entity.Dish;
import com.distributed.base.entity.RetrievalIndexState;
import com.distributed.base.entity.RetrievalVector;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.RetrievalIndexStateRepository;
import com.distributed.base.repository.RetrievalVectorRepository;
import com.distributed.base.retrieval.RetrievalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EraIncrementalIndexer {

    private static final String DOC_TYPE = "dish";
    private static final String STATE_ID = "dish";

    private final DishRepository dishRepository;
    private final RetrievalVectorRepository retrievalVectorRepository;
    private final RetrievalIndexStateRepository retrievalIndexStateRepository;
    private final RetrievalProperties retrievalProperties;

    private final AtomicLong lastRunAt = new AtomicLong(0L);

    public synchronized IndexReport ensureFresh() {
        if (!retrievalProperties.getEra().isEnabled()) {
            return new IndexReport("SKIPPED", 0, 0);
        }
        long now = System.currentTimeMillis();
        if (now - lastRunAt.get() < retrievalProperties.getEra().getRefreshIntervalMs()) {
            return new IndexReport("CACHED", 0, 0);
        }
        lastRunAt.set(now);

        List<Dish> dishes = dishRepository.findByIsDeletedFalse();
        RetrievalIndexState state = retrievalIndexStateRepository.findById(STATE_ID).orElse(null);
        boolean firstRun = state == null || state.getLastIndexedAt() == null;

        List<Dish> changed = firstRun
                ? dishes
                : dishes.stream()
                .filter(d -> d.getUpdatedAt() != null && d.getUpdatedAt().isAfter(state.getLastIndexedAt()))
                .collect(Collectors.toList());

        boolean eraTriggered = dishes.size() >= retrievalProperties.getEra().getMinCorpus()
                && (changed.size() >= retrievalProperties.getEra().getChangeThreshold()
                || changed.size() > dishes.size() * retrievalProperties.getEra().getChangeRatio());
        String mode = firstRun || eraTriggered ? "FULL" : "INCREMENTAL";

        if ("FULL".equals(mode)) {
            retrievalVectorRepository.deleteByDocType(DOC_TYPE);
            retrievalVectorRepository.saveAll(dishes.stream().map(this::toVector).collect(Collectors.toList()));
        } else if (!changed.isEmpty()) {
            retrievalVectorRepository.saveAll(changed.stream().map(this::toVector).collect(Collectors.toList()));
        }

        removeStale(dishes);

        LocalDateTime nowTime = LocalDateTime.now();
        RetrievalIndexState newState = state != null ? state : RetrievalIndexState.builder().id(STATE_ID).build();
        newState.setLastIndexedAt(nowTime);
        if ("FULL".equals(mode)) {
            newState.setLastFullRebuildAt(nowTime);
        }
        newState.setCorpusSize(dishes.size());
        newState.setLastMode(mode);
        newState.setUpdatedAt(nowTime);
        retrievalIndexStateRepository.save(newState);

        log.info("[EraIndex] mode={}, indexed={}, corpus={}", mode, changed.size(), dishes.size());
        return new IndexReport(mode, changed.size(), dishes.size());
    }

    private void removeStale(List<Dish> dishes) {
        Set<String> currentIds = dishes.stream().map(Dish::getId).collect(Collectors.toSet());
        List<String> staleIds = retrievalVectorRepository.findByDocType(DOC_TYPE).stream()
                .map(RetrievalVector::getDocId)
                .filter(id -> !currentIds.contains(id))
                .collect(Collectors.toList());
        if (!staleIds.isEmpty()) {
            retrievalVectorRepository.deleteByDocTypeAndDocIdIn(DOC_TYPE, staleIds);
        }
    }

    private RetrievalVector toVector(Dish dish) {
        return RetrievalVector.builder()
                .id(DOC_TYPE + ":" + dish.getId())
                .docType(DOC_TYPE)
                .docId(dish.getId())
                .dim(HashedEmbedding.DIM)
                .vector(HashedEmbedding.toList(HashedEmbedding.embed(vectorText(dish))))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String vectorText(Dish dish) {
        StringBuilder sb = new StringBuilder();
        sb.append(dish.getName() == null ? "" : dish.getName()).append(' ');
        sb.append(dish.getDescription() == null ? "" : dish.getDescription()).append(' ');
        if (dish.getFlavorTags() != null) {
            sb.append(String.join(" ", dish.getFlavorTags())).append(' ');
        }
        if (dish.getIngredientTags() != null) {
            sb.append(String.join(" ", dish.getIngredientTags()));
        }
        return sb.toString();
    }

    public record IndexReport(String mode, int indexed, int corpusSize) {
    }
}
