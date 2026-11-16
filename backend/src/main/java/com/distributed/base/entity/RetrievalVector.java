package com.distributed.base.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "retrieval_vectors")
public class RetrievalVector implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Indexed
    @Field("doc_type")
    private String docType;

    @Indexed
    @Field("doc_id")
    private String docId;

    @Field("dim")
    private Integer dim;

    @Field("vector")
    private List<Float> vector;

    @Field("updated_at")
    private LocalDateTime updatedAt;
}
