package com.distributed.base.repository;

import com.distributed.base.entity.RetrievalIndexState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetrievalIndexStateRepository extends MongoRepository<RetrievalIndexState, String> {
}
