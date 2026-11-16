package com.distributed.base.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;

    private String text;

    private String mode;

    private Integer maxCalorie;

    private List<String> excludeIngredients;

    private Integer limit;
}
