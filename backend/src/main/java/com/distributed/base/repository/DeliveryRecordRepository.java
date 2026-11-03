package com.distributed.base.repository;

import com.distributed.base.entity.DeliveryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 外卖配送单持久层接口
 */
@Repository
public interface DeliveryRecordRepository extends MongoRepository<DeliveryRecord, String> {

    Optional<DeliveryRecord> findByOrderId(String orderId);

    Page<DeliveryRecord> findByRiderId(String riderId, Pageable pageable);
}
