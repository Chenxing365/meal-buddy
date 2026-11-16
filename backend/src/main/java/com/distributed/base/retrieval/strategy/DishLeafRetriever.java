package com.distributed.base.retrieval.strategy;

import com.distributed.base.entity.Dish;
import com.distributed.base.entity.Merchant;
import com.distributed.base.entity.RetrievalVector;
import com.distributed.base.repository.DishRepository;
import com.distributed.base.repository.MerchantRepository;
import com.distributed.base.repository.RetrievalVectorRepository;
import com.distributed.base.retrieval.RetrievalHit;
import com.distributed.base.retrieval.RetrievalProperties;
import com.distributed.base.retrieval.RetrievalQuery;
import com.distributed.base.retrieval.index.EraIncrementalIndexer;
import com.distributed.base.retrieval.index.HashedEmbedding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DishLeafRetriever {

    private static final String STRATEGY = "LEAF_NAIVE";
    private static final String DOC_TYPE = "dish";

    private final DishRepository dishRepository;
    private final MerchantRepository merchantRepository;
    private final RetrievalVectorRepository retrievalVectorRepository;
    private final EraIncrementalIndexer eraIncrementalIndexer;
    private final RetrievalProperties retrievalProperties;

    public List<RetrievalHit> retrieve(RetrievalQuery query) {
        if (!retrievalProperties.getLeaf().isEnabled()) {
            return Collections.emptyList();
        }

        eraIncrementalIndexer.ensureFresh();

        List<Dish> dishes = dishRepository.findByStatusAndIsDeletedFalse(1);
        Map<String, Merchant> merchantIndex = loadMerchantIndex(dishes);
        List<String> allergies = query.getExcludeIngredients() != null
                ? query.getExcludeIngredients() : Collections.emptyList();

        List<Dish> candidates = dishes.stream()
                .filter(d -> isServing(d, merchantIndex))
                .filter(d -> !hasAllergyConflict(d, allergies))
                .filter(d -> query.getMaxCalorie() == null
                        || (d.getCalorie() != null && d.getCalorie() <= query.getMaxCalorie()))
                .collect(Collectors.toList());

        Map<String, List<Float>> vectorIndex = loadVectors();
        float[] queryVector = HashedEmbedding.embed(query.getText());
        int topK = Math.max(1, retrievalProperties.getLeaf().getTopK());

        List<ScoredDish> scored = candidates.stream()
                .map(d -> new ScoredDish(d, scoreDish(d, queryVector, vectorIndex, query.getText())))
                .sorted(Comparator.comparingDouble(ScoredDish::score).reversed())
                .limit(topK)
                .collect(Collectors.toList());

        boolean weak = scored.isEmpty()
                || scored.get(0).score() < retrievalProperties.getLeaf().getMinScore();
        if (weak) {
            scored = candidates.stream()
                    .sorted(Comparator.comparingInt((Dish d) -> d.getCalorie() != null ? d.getCalorie() : Integer.MAX_VALUE))
                    .limit(topK)
                    .map(d -> new ScoredDish(d, 0.01D))
                    .collect(Collectors.toList());
        }

        return scored.stream().map(s -> toHit(s, merchantIndex)).collect(Collectors.toList());
    }

    private Map<String, List<Float>> loadVectors() {
        if (!retrievalProperties.getEra().isEnabled()) {
            return Collections.emptyMap();
        }
        return retrievalVectorRepository.findByDocType(DOC_TYPE).stream()
                .collect(Collectors.toMap(RetrievalVector::getDocId, RetrievalVector::getVector, (a, b) -> a));
    }

    private double scoreDish(Dish dish, float[] queryVector, Map<String, List<Float>> vectorIndex, String text) {
        List<Float> stored = vectorIndex.get(dish.getId());
        float[] dishVector = stored != null
                ? HashedEmbedding.fromList(stored)
                : HashedEmbedding.embed(vectorText(dish));
        double semantic = HashedEmbedding.cosine(queryVector, dishVector);

        double tagScore = 0D;
        if (text != null && !text.isBlank()) {
            List<String> tags = new ArrayList<>();
            if (dish.getFlavorTags() != null) {
                tags.addAll(dish.getFlavorTags());
            }
            if (dish.getIngredientTags() != null) {
                tags.addAll(dish.getIngredientTags());
            }
            long matched = tags.stream().filter(text::contains).count();
            tagScore = Math.min(1D, matched * 0.5D);
        }
        return semantic * 0.75D + tagScore * 0.25D;
    }

    private String vectorText(Dish dish) {
        StringBuilder sb = new StringBuilder();
        sb.append(dish.getName() == null ? "" : dish.getName()).append(' ');
        sb.append(dish.getDescription() == null ? "" : dish.getDescription()).append(' ');
        if (dish.getFlavorTags() != null) {
            sb.append(String.join(" ", dish.getFlavorTags())).append(' ');
        }
        if (dish.getIngredientTags() != null) {
            sb.append(String.join(" ", dish.getIngredientTags()));
        }
        return sb.toString();
    }

    private Map<String, Merchant> loadMerchantIndex(List<Dish> dishes) {
        List<String> merchantIds = dishes.stream()
                .map(Dish::getMerchantId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (merchantIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return merchantRepository.findAllById(merchantIds).stream()
                .collect(Collectors.toMap(Merchant::getId, m -> m, (a, b) -> a));
    }

    private boolean isServing(Dish dish, Map<String, Merchant> merchantIndex) {
        if (dish.getMerchantId() == null || dish.getMerchantId().isBlank()) {
            return true;
        }
        Merchant merchant = merchantIndex.get(dish.getMerchantId());
        return merchant != null && merchant.getStatus() != null && merchant.getStatus() == 1;
    }

    private boolean hasAllergyConflict(Dish dish, List<String> allergyTags) {
        if (allergyTags == null || allergyTags.isEmpty()) {
            return false;
        }
        Set<String> ingredients = new HashSet<>();
        if (dish.getIngredientTags() != null) {
            ingredients.addAll(dish.getIngredientTags());
        }
        if (dish.getFlavorTags() != null) {
            ingredients.addAll(dish.getFlavorTags());
        }
        if (dish.getName() != null) {
            ingredients.add(dish.getName());
        }
        for (String allergy : allergyTags) {
            for (String ingredient : ingredients) {
                if (ingredient.contains(allergy) || allergy.contains(ingredient)) {
                    return true;
                }
            }
        }
        return false;
    }

    private RetrievalHit toHit(ScoredDish scored, Map<String, Merchant> merchantIndex) {
        Dish dish = scored.dish();
        Merchant merchant = dish.getMerchantId() == null ? null : merchantIndex.get(dish.getMerchantId());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("calorie", dish.getCalorie());
        metadata.put("price", dish.getPrice());
        metadata.put("merchantId", dish.getMerchantId());
        metadata.put("merchantName", merchant != null ? merchant.getName() : null);
        metadata.put("flavorTags", dish.getFlavorTags());
        metadata.put("ingredientTags", dish.getIngredientTags());
        return RetrievalHit.builder()
                .docType("DISH")
                .docId(dish.getId())
                .title(dish.getName())
                .content(dish.getDescription())
                .score(scored.score())
                .strategy(STRATEGY)
                .metadata(metadata)
                .build();
    }

    private record ScoredDish(Dish dish, double score) {
    }
}
