package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.MerchantResponse;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.service.MerchantService;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 平台管理端商家审核 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "平台商家审核接口", description = "管理员查看待审核入驻申请、审批通过与驳回")
public class AdminMerchantController {

    private final MerchantService merchantService;

    @GetMapping("/merchants")
    @Operation(summary = "管理员分页查询商家列表", description = "支持按状态过滤（0-待审核，1-正常营业，2-歇业，3-驳回）")
    public Result<Page<MerchantResponse>> listMerchants(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        checkAdmin();
        Page<MerchantResponse> pageResult = merchantService.adminListMerchants(status, page, size);
        return Result.ok(pageResult);
    }

    @PostMapping("/merchant/{id}/approve")
    @Operation(summary = "审核通过商家入驻", description = "门店状态置为 1-正常营业，关联合同角色升级为商家")
    public Result<Void> approveMerchant(@PathVariable("id") String id) {
        checkAdmin();
        merchantService.adminApproveMerchant(id);
        return Result.ok("审核通过，门店已激活营业", null);
    }

    @PostMapping("/merchant/{id}/reject")
    @Operation(summary = "驳回商家入驻申请", description = "门店状态置为 3-审核驳回，记录驳回理由")
    public Result<Void> rejectMerchant(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, String> body) {
        checkAdmin();
        String reason = body != null ? body.get("reason") : "资质材料审核未达标";
        merchantService.adminRejectMerchant(id, reason);
        return Result.ok("申请已驳回", null);
    }

    private void checkAdmin() {
        Integer role = UserContext.getRole();
        if (role == null || role != 9) {
            throw new BusinessException(403, "无权访问：需要平台管理员权限");
        }
    }
}
