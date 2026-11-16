package com.distributed.base.repository;

import com.distributed.base.entity.RetrievalVector;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetrievalVectorRepository extends MongoRepository<RetrievalVector, String> {

    List<RetrievalVector> findByDocType(String docType);

    Optional<RetrievalVector> findByDocTypeAndDocId(String docType, String docId);

    void deleteByDocType(String docType);

    void deleteByDocTypeAndDocIdIn(String docType, Collection<String> docIds);
}
