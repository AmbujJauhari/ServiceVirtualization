package com.service.virtualization.ibmmq.repository;

import com.service.virtualization.ibmmq.model.IBMMQStub;
import com.service.virtualization.model.StubStatus;

import java.util.List;
import java.util.Optional;

public interface IBMMQStubRepository {
    List<IBMMQStub> findByUserId(String userId);
    List<IBMMQStub> findByUserIdAndStatus(String userId, StubStatus status);
    List<IBMMQStub> findByQueueName(String queueName);
    List<IBMMQStub> findByQueueManagerAndQueueName(String queueManager, String queueName);
    List<IBMMQStub> findByQueueManagerAndQueueNameAndStatus(String queueManager, String queueName, StubStatus status);
    Optional<IBMMQStub> findById(String id);
    IBMMQStub save(IBMMQStub stub);
    void delete(IBMMQStub stub);
    List<IBMMQStub> findAll();
} 