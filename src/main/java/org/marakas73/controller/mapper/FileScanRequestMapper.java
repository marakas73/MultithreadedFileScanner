package org.marakas73.controller.mapper;

import org.marakas73.controller.dto.request.FileScanRequestDto;
import org.marakas73.model.FileScanFilter;
import org.marakas73.model.FileScanRequest;
import org.springframework.stereotype.Component;

@Component
public class FileScanRequestMapper {
    private final FileScanFilterMapper filterMapper;

    public FileScanRequestMapper(FileScanFilterMapper filterMapper) {
        this.filterMapper = filterMapper;
    }

    public FileScanRequest toModel(FileScanRequestDto dto) {
        FileScanFilter scanFilterModel = dto.scanFilter() == null ? null : filterMapper.toModel(dto.scanFilter());

        return new FileScanRequest(
                dto.directoryPath(),
                dto.threadsCount(),
                dto.depthLimit(),
                scanFilterModel
        );
    }
}
