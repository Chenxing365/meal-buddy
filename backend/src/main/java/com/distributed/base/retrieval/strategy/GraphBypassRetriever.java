package com.distributed.base.retrieval.strategy;

import com.distributed.base.entity.Dish;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.retrieval.RetrievalHit;
import com.distributed.base.retrieval.RetrievalProperties;
import com.distributed.base.retrieval.RetrievalQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GraphBypassRetriever {

    private static final String STRATEGY = "GRAPH_BYPASS";

    private final DishRepository dishRepository;
    private final RetrievalProperties retrievalProperties;

    public List<RetrievalHit> retrieve(RetrievalQuery query, List<RetrievalHit> seeds) {
        if (!retrievalProperties.getGraph().isEnabled() || seeds == null || seeds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> seedDishIds = seeds.stream()
                .filter(h -> "DISH".equals(h.getDocType()))
                .map(RetrievalHit::getDocId)
                .limit(Math.max(1, retrievalProperties.getGraph().getMaxSeeds()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (seedDishIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Dish> seedDishes = dishRepository.findByIdInAndIsDeletedFalse(new ArrayList<>(seedDishIds));
        Set<String> merchantIds = seedDishes.stream()
                .map(Dish::getMerchantId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, RetrievalHit> related = new LinkedHashMap<>();
        for (String merchantId : merchantIds) {
            dishRepository.findByMerchantIdAndStatusAndIsDeletedFalse(merchantId, 1).stream()
                    .filter(d -> !seedDishIds.contains(d.getId()))
                    .filter(d -> !related.containsKey(d.getId()))
                    .limit(Math.max(1, retrievalProperties.getGraph().getTopK()))
                    .forEach(d -> related.put(d.getId(), toHit(d, merchantId)));
        }
        return new ArrayList<>(related.values());
    }

    private RetrievalHit toHit(Dish dish, String merchantId) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("relation", "same_merchant");
        metadata.put("merchantId", merchantId);
        metadata.put("calorie", dish.getCalorie());
        metadata.put("price", dish.getPrice());
        return RetrievalHit.builder()
                .docType("DISH")
                .docId(dish.getId())
                .title(dish.getName())
                .content(dish.getDescription())
                .score(0.35D)
                .strategy(STRATEGY)
                .metadata(metadata)
                .build();
    }
}
