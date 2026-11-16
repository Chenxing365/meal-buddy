package com.distributed.base.repository;

import com.distributed.base.entity.ChatNode;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatNodeRepository extends MongoRepository<ChatNode, String> {

    Optional<ChatNode> findFirstBySessionIdAndLevelOrderByCreatedAtAsc(String sessionId, Integer level);

    Optional<ChatNode> findFirstBySessionIdAndLevelAndTopicSeqOrderByCreatedAtAsc(String sessionId, Integer level, Integer topicSeq);

    long countBySessionIdAndLevel(String sessionId, Integer level);

    List<ChatNode> findByParentIdOrderByCreatedAtAsc(String parentId);

    List<ChatNode> findByUserIdAndLevelInOrderByCreatedAtDesc(String userId, Collection<Integer> levels);

    List<ChatNode> findBySessionIdOrderByLevelAscCreatedAtAsc(String sessionId);
}
