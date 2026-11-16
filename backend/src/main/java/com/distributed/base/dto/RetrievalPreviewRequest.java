package com.distributed.base.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalPreviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String text;

    private String mode;

    private Integer maxCalorie;

    private Integer limit;
}
