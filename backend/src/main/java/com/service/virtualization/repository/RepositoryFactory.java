package com.service.virtualization.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory for creating appropriate repository implementations based on configuration
 */
@Component
public class RepositoryFactory {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryFactory.class);

    private final RestStubRepository restStubRepository;
    private final RecordingRepository recordingRepository;
    private final RecordingConfigRepository recordingConfigRepository;

    public RepositoryFactory(
            RestStubRepository restStubRepository,
            RecordingRepository recordingRepository,
            RecordingConfigRepository recordingConfigRepository) {
        this.restStubRepository = restStubRepository;
        this.recordingRepository = recordingRepository;
        this.recordingConfigRepository = recordingConfigRepository;
    }

    public RestStubRepository getStubRepository() {
        return restStubRepository;
    }

    public RecordingRepository getRecordingRepository() {
        return recordingRepository;
    }
    
    public RecordingConfigRepository getRecordingConfigRepository() {
        return recordingConfigRepository;
    }
} 