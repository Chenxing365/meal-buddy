package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.*;
import com.distributed.base.service.MerchantService;
import com.distributed.base.service.OrderService;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 商家门店业务 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
@Tag(name = "商家门店接口", description = "商家入驻资质申请、门店详情查看、附近门店检索、营业信息变更、到店核销与经营大盘")
public class MerchantController {

    private final MerchantService merchantService;
    private final OrderService orderService;

    @PostMapping("/apply")
    @Operation(summary = "商家提交入驻申请", description = "初始状态置为 0-待审核，等待平台管理员审核")
    public Result<MerchantResponse> apply(@Valid @RequestBody MerchantApplyRequest req) {
        String userId = UserContext.getUserId();
        MerchantResponse response = merchantService.apply(userId, req);
        return Result.ok("入驻资质已提交，等待平台审核", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取门店详情与菜品清单", description = "支持 Redis 缓存 300 秒（cache:merchant:{id}）")
    public Result<MerchantDetailResponse> getMerchantDetail(@PathVariable("id") String id) {
        MerchantDetailResponse response = merchantService.getMerchantDetail(id);
        return Result.ok(response);
    }

    @GetMapping("/list")
    @Operation(summary = "分页/附近/品类查询商家列表", description = "仅展示审核通过且正常营业的门店（status=1）")
    public Result<Page<MerchantResponse>> listMerchants(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "longitude", required = false) Double longitude,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "distanceKm", required = false) Double distanceKm,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Page<MerchantResponse> result = merchantService.listMerchants(category, longitude, latitude, distanceKm, page, size);
        return Result.ok(result);
    }

    @PutMapping("/info/{id}")
    @Operation(summary = "修改门店营业信息", description = "修改门店名称、地址、营业打烊状态等，联动驱逐缓存")
    public Result<MerchantResponse> updateMerchantInfo(
            @PathVariable("id") String id,
            @RequestBody MerchantUpdateRequest req) {
        String userId = UserContext.getUserId();
        MerchantResponse response = merchantService.updateMerchantInfo(id, userId, req);
        return Result.ok("门店信息已更新", response);
    }

    @PostMapping("/verify")
    @Operation(summary = "商家核销自提/堂食订单", description = "校验核销码，流转为已核销，幂等防重")
    public Result<VerifyResponse> verifyOrder(@Valid @RequestBody VerifyRequest req) {
        String userId = UserContext.getUserId();
        VerifyResponse response = orderService.verifyOrder(userId, req);
        return Result.ok("核销成功", response);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "商家经营大盘", description = "查看当日订单量、营业额、已核销数及核销率")
    public Result<MerchantDashboardResponse> getDashboard() {
        String userId = UserContext.getUserId();
        MerchantDashboardResponse response = orderService.getMerchantDashboard(userId);
        return Result.ok(response);
    }

    @GetMapping("/orders")
    @Operation(summary = "商家门店订单管理", description = "分页查看属于当前商家门店的所有订单")
    public Result<Page<OrderResponse>> listMerchantOrders(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        String userId = UserContext.getUserId();
        Page<OrderResponse> pageResult = orderService.listMerchantOrders(userId, page, size);
        return Result.ok(pageResult);
    }
}
