package com.distributed.base.repository;

import com.distributed.base.entity.VerifyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单核销记录持久层接口
 */
@Repository
public interface VerifyRecordRepository extends MongoRepository<VerifyRecord, String> {

    Optional<VerifyRecord> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);

    List<VerifyRecord> findByMerchantIdAndVerifiedAtBetween(String merchantId, LocalDateTime start, LocalDateTime end);

    long countByMerchantId(String merchantId);
}
