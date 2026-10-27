package com.distributed.base.service;

import com.distributed.base.dto.DishCreateRequest;
import com.distributed.base.dto.DishResponse;
import com.distributed.base.dto.DishUpdateRequest;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 菜品供给管理服务接口
 */
public interface DishService {

    /**
     * 录入菜品（必须包含热量 calorie 与口味标签）
     */
    DishResponse createDish(String userId, DishCreateRequest req);

    /**
     * 修改菜品信息（联动驱逐 Redis 对应缓存）
     */
    DishResponse updateDish(String dishId, String userId, DishUpdateRequest req);

    /**
     * 菜品上下架切换
     */
    void toggleDishStatus(String dishId, String userId, Integer status);

    /**
     * 逻辑删除菜品
     */
    void deleteDish(String dishId, String userId);

    /**
     * 查询菜品详情（支持 Redis 缓存 300s：cache:dish:{dishId}）
     */
    DishResponse getDishDetail(String dishId);

    /**
     * 分页查询指定商家的菜品列表
     */
    Page<DishResponse> listDishesByMerchant(String merchantId, int page, int size);
}
