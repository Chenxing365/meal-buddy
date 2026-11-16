package com.distributed.base.retrieval;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    private List<RetrievalHit> hits = new ArrayList<>();

    @Builder.Default
    private List<RetrievalTrace> trace = new ArrayList<>();

    private boolean degraded;

    private long elapsedMs;
}
