package com.service.virtualization.dto;

import com.service.virtualization.model.RestStub;
import com.service.virtualization.model.StubStatus;

public class DtoConverter {
    public static RestStub toRestStub(RestStubDTO restStubDTO) {
        return new RestStub(
                restStubDTO.id(),
                restStubDTO.name(),
                restStubDTO.description(),
                restStubDTO.userId(),
                restStubDTO.behindProxy(),
                restStubDTO.protocol(),
                restStubDTO.tags(),
                StubStatus.valueOf(restStubDTO.status()),
                restStubDTO.createdAt(),
                restStubDTO.updatedAt(),
                restStubDTO.wiremockMappingId(),
                restStubDTO.matchConditions(),
                restStubDTO.response()
        );
    }

    public static RestStubDTO fromRestStub(RestStub restStub) {
        return new RestStubDTO(
                restStub.id(),
                restStub.name(),
                restStub.description(),
                restStub.userId(),
                restStub.behindProxy(),
                restStub.protocol(),
                restStub.tags(),
                restStub.status().name(),
                restStub.createdAt(),
                restStub.updatedAt(),
                restStub.wiremockMappingId(),
                restStub.matchConditions(),
                restStub.response()
        );
    }

}
