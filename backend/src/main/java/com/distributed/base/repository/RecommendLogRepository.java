package com.distributed.base.repository;

import com.distributed.base.entity.RecommendLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI 推荐日志持久层接口
 */
@Repository
public interface RecommendLogRepository extends MongoRepository<RecommendLog, String> {

    Page<RecommendLog> findByUserId(String userId, Pageable pageable);

    List<RecommendLog> findByUserIdOrderByCreatedAtDesc(String userId);
}
