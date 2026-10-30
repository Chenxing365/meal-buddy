package com.distributed.base.repository;

import com.distributed.base.entity.Dish;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 菜品持久层接口
 */
@Repository
public interface DishRepository extends MongoRepository<Dish, String> {

    List<Dish> findByMerchantIdAndStatusAndIsDeletedFalse(String merchantId, Integer status);

    Page<Dish> findByMerchantIdAndIsDeletedFalse(String merchantId, Pageable pageable);

    Optional<Dish> findByIdAndIsDeletedFalse(String id);

    List<Dish> findByIdInAndIsDeletedFalse(List<String> ids);

    Page<Dish> findByCalorieLessThanEqualAndStatusAndIsDeletedFalse(Integer maxCalorie, Integer status, Pageable pageable);
}
