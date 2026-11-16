package com.distributed.base.retrieval.strategy;

import com.distributed.base.entity.ChatNode;
import com.distributed.base.repository.ChatNodeRepository;
import com.distributed.base.retrieval.RetrievalHit;
import com.distributed.base.retrieval.RetrievalProperties;
import com.distributed.base.retrieval.RetrievalQuery;
import com.distributed.base.retrieval.index.HashedEmbedding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ChatTreeRetriever {

    private static final String STRATEGY = "TREE_RAG";

    private final ChatNodeRepository chatNodeRepository;
    private final RetrievalProperties retrievalProperties;

    public List<RetrievalHit> retrieve(RetrievalQuery query) {
        if (!retrievalProperties.getTree().isEnabled() || query.getUserId() == null) {
            return Collections.emptyList();
        }

        LocalDateTime threshold = LocalDateTime.now()
                .minusDays(Math.max(1, retrievalProperties.getTree().getSessionWindowDays()));
        List<ChatNode> summaries = chatNodeRepository
                .findByUserIdAndLevelInOrderByCreatedAtDesc(query.getUserId(), List.of(1, 2));

        float[] queryVector = HashedEmbedding.embed(query.getText());
        List<ScoredNode> tops = summaries.stream()
                .filter(n -> n.getCreatedAt() == null || n.getCreatedAt().isAfter(threshold))
                .map(n -> new ScoredNode(n, score(n, queryVector)))
                .sorted(Comparator.comparingDouble(ScoredNode::score).reversed())
                .limit(Math.max(1, retrievalProperties.getTree().getTopNodes()))
                .toList();

        List<RetrievalHit> hits = new ArrayList<>();
        Map<String, ChatNode> leafPool = new LinkedHashMap<>();
        for (ScoredNode top : tops) {
            hits.add(toHit(top.node(), top.score(), "CHAT_SUMMARY"));
            collectLeaves(top.node(), leafPool);
        }

        leafPool.values().stream()
                .map(n -> new ScoredNode(n, score(n, queryVector)))
                .sorted(Comparator.comparingDouble(ScoredNode::score).reversed())
                .limit(Math.max(1, retrievalProperties.getTree().getLeafTopK()))
                .forEach(s -> hits.add(toHit(s.node(), s.score(), "CHAT_MESSAGE")));

        return hits;
    }

    private void collectLeaves(ChatNode node, Map<String, ChatNode> pool) {
        int maxDepth = Math.max(2, retrievalProperties.getTree().getMaxDepth());
        Deque<ChatNode> queue = new ArrayDeque<>();
        queue.add(node);
        while (!queue.isEmpty()) {
            ChatNode current = queue.poll();
            int level = current.getLevel() == null ? 0 : current.getLevel();
            if (level >= maxDepth) {
                pool.putIfAbsent(current.getId(), current);
                continue;
            }
            List<ChatNode> children = chatNodeRepository.findByParentIdOrderByCreatedAtAsc(current.getId());
            if (children.isEmpty()) {
                if (level > 1) {
                    pool.putIfAbsent(current.getId(), current);
                }
            } else {
                queue.addAll(children);
            }
        }
    }

    private double score(ChatNode node, float[] queryVector) {
        double semantic = HashedEmbedding.cosine(queryVector, HashedEmbedding.fromList(node.getEmbedding()));
        double recency = 0D;
        if (node.getCreatedAt() != null) {
            long days = Duration.between(node.getCreatedAt(), LocalDateTime.now()).toDays();
            recency = Math.max(0D, 1D - days / 30D);
        }
        return semantic * 0.85 + recency * 0.15;
    }

    private RetrievalHit toHit(ChatNode node, double score, String docType) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("level", node.getLevel());
        metadata.put("type", node.getType());
        metadata.put("sessionId", node.getSessionId());
        metadata.put("createdAt", node.getCreatedAt() == null ? null : node.getCreatedAt().toString());
        return RetrievalHit.builder()
                .docType(docType)
                .docId(node.getId())
                .title(node.getTitle())
                .content(truncate(node.getContent(), 300))
                .score(score)
                .strategy(STRATEGY)
                .metadata(metadata)
                .build();
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private record ScoredNode(ChatNode node, double score) {
    }
}
