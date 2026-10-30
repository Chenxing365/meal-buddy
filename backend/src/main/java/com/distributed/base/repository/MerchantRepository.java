package com.distributed.base.repository;

import com.distributed.base.entity.Merchant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 商家持久层接口
 */
@Repository
public interface MerchantRepository extends MongoRepository<Merchant, String> {

    Optional<Merchant> findByUserId(String userId);

    boolean existsByUserId(String userId);

    Page<Merchant> findByStatus(Integer status, Pageable pageable);

    Page<Merchant> findByCategoryAndStatus(String category, Integer status, Pageable pageable);

    Page<Merchant> findByLocationNearAndStatus(Point point, Distance distance, Integer status, Pageable pageable);
}
