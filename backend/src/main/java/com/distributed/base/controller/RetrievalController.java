package com.distributed.base.controller;

import com.distributed.base.common.Result;
import com.distributed.base.dto.RetrievalPreviewRequest;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.retrieval.MealBuddyRetriever;
import com.distributed.base.retrieval.RetrievalQuery;
import com.distributed.base.retrieval.RetrievalResult;
import com.distributed.base.util.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/retrieval")
@RequiredArgsConstructor
@Tag(name = "检索层预览接口", description = "TreeRAG 主链 + 叶子层 Naive 检索 + GraphRAG 旁路的链路预览")
public class RetrievalController {

    private final MealBuddyRetriever mealBuddyRetriever;
    private final UserPreferenceRepository userPreferenceRepository;

    @PostMapping("/preview")
    @Operation(summary = "预览检索链路命中结果", description = "返回命中列表与各策略耗时/状态，用于学习与联调排查")
    public Result<RetrievalResult> preview(@RequestBody RetrievalPreviewRequest req) {
        String userId = UserContext.getUserId();
        UserPreference preference = userPreferenceRepository.findByUserId(userId).orElse(null);

        RetrievalQuery query = RetrievalQuery.builder()
                .userId(userId)
                .text(req.getText())
                .mode(req.getMode())
                .maxCalorie(req.getMaxCalorie() != null ? req.getMaxCalorie()
                        : preference != null ? preference.getCalorieLimit() : null)
                .excludeIngredients(preference != null ? preference.getAllergyTags() : null)
                .limit(req.getLimit())
                .build();

        return Result.ok(mealBuddyRetriever.retrieve(query));
    }
}
