package com.distributed.base.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalHit implements Serializable {

    private static final long serialVersionUID = 1L;

    private String docType;

    private String docId;

    private String title;

    private String content;

    private double score;

    private String strategy;

    private Map<String, Object> metadata;
}
