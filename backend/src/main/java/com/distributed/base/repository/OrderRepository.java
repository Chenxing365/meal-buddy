package com.distributed.base.repository;

import com.distributed.base.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 订单持久层接口
 */
@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Optional<Order> findByOrderNo(String orderNo);

    Optional<Order> findByPickupCodeAndMerchantId(String pickupCode, String merchantId);

    Optional<Order> findByPickupCode(String pickupCode);

    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    Page<Order> findByMerchantIdOrderByCreatedAtDesc(String merchantId, Pageable pageable);

    List<Order> findByMerchantIdAndCreatedAtBetween(String merchantId, LocalDateTime start, LocalDateTime end);

    long countByMerchantId(String merchantId);
}
