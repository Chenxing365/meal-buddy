package com.distributed.base.service;

import com.distributed.base.dto.MerchantApplyRequest;
import com.distributed.base.dto.MerchantDetailResponse;
import com.distributed.base.dto.MerchantResponse;
import com.distributed.base.dto.MerchantUpdateRequest;
import org.springframework.data.domain.Page;

/**
 * 商家门店业务服务接口
 */
public interface MerchantService {

    /**
     * 商家申请入驻（初始状态 0-待审核）
     */
    MerchantResponse apply(String userId, MerchantApplyRequest req);

    /**
     * 获取门店详情与菜品清单（支持 Redis 缓存 300s：cache:merchant:{id}）
     */
    MerchantDetailResponse getMerchantDetail(String merchantId);

    /**
     * 前台分页与附近门店检索（仅展示 status == 1 正常营业门店）
     */
    Page<MerchantResponse> listMerchants(String category, Double lng, Double lat, Double distanceKm, int page, int size);

    /**
     * 商家修改门店资料
     */
    MerchantResponse updateMerchantInfo(String merchantId, String userId, MerchantUpdateRequest req);

    /**
     * 管理端查看待审核/各状态商家列表
     */
    Page<MerchantResponse> adminListMerchants(Integer status, int page, int size);

    /**
     * 管理端审核通过
     */
    void adminApproveMerchant(String merchantId);

    /**
     * 管理端驳回入驻申请
     */
    void adminRejectMerchant(String merchantId, String reason);
}
