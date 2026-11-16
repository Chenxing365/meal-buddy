package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "retrieval_index_state")
public class RetrievalIndexState implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field("last_indexed_at")
    private LocalDateTime lastIndexedAt;

    @Field("last_full_rebuild_at")
    private LocalDateTime lastFullRebuildAt;

    @Field("corpus_size")
    private Integer corpusSize;

    @Field("last_mode")
    private String lastMode;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
