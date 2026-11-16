package com.distributed.base.retrieval;

import com.distributed.base.entity.ChatNode;
import com.distributed.base.repository.ChatNodeRepository;
import com.distributed.base.retrieval.index.HashedEmbedding;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatTreeIndexer {

    private static final int LEVEL_SESSION = 1;
    private static final int LEVEL_TOPIC = 2;
    private static final int LEVEL_MESSAGE = 3;

    private final ChatNodeRepository chatNodeRepository;
    private final RetrievalProperties retrievalProperties;

    public void appendTurn(String userId, String userMessage, String assistantReply) {
        if (!retrievalProperties.getTree().isEnabled() || userId == null || userMessage == null) {
            return;
        }
        try {
            String sessionId = userId + ":" + LocalDate.now();
            ChatNode session = chatNodeRepository
                    .findFirstBySessionIdAndLevelOrderByCreatedAtAsc(sessionId, LEVEL_SESSION)
                    .orElseGet(() -> createNode(userId, sessionId, null, LEVEL_SESSION, "SESSION",
                            truncate(userMessage, 30), userMessage, null));

            long messageCount = chatNodeRepository.countBySessionIdAndLevel(sessionId, LEVEL_MESSAGE);
            int topicSeq = (int) (messageCount / Math.max(1, retrievalProperties.getTree().getTopicTurnSize()));

            ChatNode topic = chatNodeRepository
                    .findFirstBySessionIdAndLevelAndTopicSeqOrderByCreatedAtAsc(sessionId, LEVEL_TOPIC, topicSeq)
                    .orElseGet(() -> createNode(userId, sessionId, session.getId(), LEVEL_TOPIC, "TOPIC",
                            "话题 " + (topicSeq + 1), truncate(userMessage, 120), topicSeq));

            String turnContent = "用户: " + userMessage + "\n助手: " + assistantReply;
            createNode(userId, sessionId, topic.getId(), LEVEL_MESSAGE, "MESSAGE",
                    truncate(userMessage, 30), turnContent, topicSeq);
            log.debug("[ChatTreeIndexer] 节点写入完成: sessionId={}, topicSeq={}", sessionId, topicSeq);
        } catch (Exception e) {
            log.warn("[ChatTreeIndexer] 会话树写入失败: {}", e.getMessage());
        }
    }

    private ChatNode createNode(String userId, String sessionId, String parentId, int level, String type,
                                String title, String content, Integer topicSeq) {
        return chatNodeRepository.save(ChatNode.builder()
                .userId(userId)
                .sessionId(sessionId)
                .parentId(parentId)
                .level(level)
                .type(type)
                .topicSeq(topicSeq)
                .title(title)
                .content(content)
                .embedding(HashedEmbedding.toList(HashedEmbedding.embed(title + " " + content)))
                .build());
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() <= max ? text : text.substring(0, max);
    }
}
