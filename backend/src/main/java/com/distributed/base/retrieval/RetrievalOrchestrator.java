package com.distributed.base.retrieval;

import com.distributed.base.retrieval.strategy.ChatTreeRetriever;
import com.distributed.base.retrieval.strategy.DishLeafRetriever;
import com.distributed.base.retrieval.strategy.GraphBypassRetriever;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalOrchestrator implements MealBuddyRetriever {

    private final ChatTreeRetriever chatTreeRetriever;
    private final DishLeafRetriever dishLeafRetriever;
    private final GraphBypassRetriever graphBypassRetriever;
    private final RetrievalProperties retrievalProperties;

    @Override
    public RetrievalResult retrieve(RetrievalQuery query) {
        long start = System.currentTimeMillis();
        List<RetrievalHit> hits = new ArrayList<>();
        List<RetrievalTrace> traces = new ArrayList<>();

        hits.addAll(run("TREE_RAG", traces, () -> chatTreeRetriever.retrieve(query)));
        List<RetrievalHit> leafHits = run("LEAF_NAIVE", traces, () -> dishLeafRetriever.retrieve(query));
        hits.addAll(leafHits);

        if (retrievalProperties.getGraph().isEnabled()) {
            hits.addAll(run("GRAPH_BYPASS", traces, () -> graphBypassRetriever.retrieve(query, leafHits)));
        }

        int limit = query.getLimit() != null ? Math.max(1, query.getLimit()) : 10;
        List<RetrievalHit> merged = merge(hits, limit);
        boolean degraded = traces.stream().anyMatch(t -> "DEGRADED".equals(t.getStatus()));

        return RetrievalResult.builder()
                .hits(merged)
                .trace(traces)
                .degraded(degraded)
                .elapsedMs(System.currentTimeMillis() - start)
                .build();
    }

    private List<RetrievalHit> run(String strategy, List<RetrievalTrace> traces, Supplier<List<RetrievalHit>> supplier) {
        long start = System.currentTimeMillis();
        try {
            List<RetrievalHit> hits = supplier.get();
            traces.add(RetrievalTrace.builder()
                    .strategy(strategy)
                    .status("OK")
                    .hits(hits.size())
                    .elapsedMs(System.currentTimeMillis() - start)
                    .build());
            return hits;
        } catch (Exception e) {
            log.warn("[Retrieval] 策略 {} 执行失败，链路降级: {}", strategy, e.getMessage());
            traces.add(RetrievalTrace.builder()
                    .strategy(strategy)
                    .status("DEGRADED")
                    .hits(0)
                    .elapsedMs(System.currentTimeMillis() - start)
                    .detail(e.getMessage())
                    .build());
            return List.of();
        }
    }

    private List<RetrievalHit> merge(List<RetrievalHit> hits, int limit) {
        Map<String, RetrievalHit> merged = new LinkedHashMap<>();
        for (RetrievalHit hit : hits) {
            String key = hit.getDocType() + ":" + hit.getDocId();
            RetrievalHit existing = merged.get(key);
            if (existing == null || hit.getScore() > existing.getScore()) {
                merged.put(key, hit);
            }
        }
        return merged.values().stream()
                .sorted(Comparator.comparingDouble(RetrievalHit::getScore).reversed())
                .limit(limit)
                .toList();
    }
}
