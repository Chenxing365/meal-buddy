package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.DishCreateRequest;
import com.distributed.base.dto.DishResponse;
import com.distributed.base.dto.DishUpdateRequest;
import com.distributed.base.service.DishService;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 菜品供给管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/dish")
@RequiredArgsConstructor
@Tag(name = "菜品供给接口", description = "菜品录入（热量/风味）、修改、上下架、逻辑删除、详情查看")
public class DishController {

    private final DishService dishService;

    @PostMapping
    @Operation(summary = "录入新菜品", description = "必须包含标称热量与口味标签，联动驱逐门店缓存")
    public Result<DishResponse> createDish(@Valid @RequestBody DishCreateRequest req) {
        String userId = UserContext.getUserId();
        DishResponse response = dishService.createDish(userId, req);
        return Result.ok("菜品添加成功", response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改菜品信息", description = "更新价格、热量、食材等，联动驱逐菜品与门店缓存")
    public Result<DishResponse> updateDish(
            @PathVariable("id") String id,
            @RequestBody DishUpdateRequest req) {
        String userId = UserContext.getUserId();
        DishResponse response = dishService.updateDish(id, userId, req);
        return Result.ok("菜品信息已修改", response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "菜品上下架切换", description = "0-下架售罄，1-正常上架")
    public Result<Void> toggleDishStatus(
            @PathVariable("id") String id,
            @RequestBody Map<String, Integer> body) {
        String userId = UserContext.getUserId();
        Integer status = body.get("status");
        dishService.toggleDishStatus(id, userId, status);
        return Result.ok(status == 1 ? "菜品已上架" : "菜品已下架", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "逻辑删除菜品", description = "软删除标记 isDeleted=true")
    public Result<Void> deleteDish(@PathVariable("id") String id) {
        String userId = UserContext.getUserId();
        dishService.deleteDish(id, userId);
        return Result.ok("菜品已删除", null);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取菜品详情", description = "支持 Redis 缓存 300 秒（cache:dish:{id}）")
    public Result<DishResponse> getDishDetail(@PathVariable("id") String id) {
        DishResponse response = dishService.getDishDetail(id);
        return Result.ok(response);
    }

    @GetMapping("/merchant/{merchantId}")
    @Operation(summary = "分页查询门店菜品列表", description = "供门店管理端与顾客点餐浏览")
    public Result<Page<DishResponse>> listDishesByMerchant(
            @PathVariable("merchantId") String merchantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Page<DishResponse> dishPage = dishService.listDishesByMerchant(merchantId, page, size);
        return Result.ok(dishPage);
    }
}
