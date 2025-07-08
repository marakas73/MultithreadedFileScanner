package org.marakas73.controller.mapper;

import org.marakas73.controller.dto.response.FileScanResponseDto;
import org.marakas73.model.FileScanResult;
import org.springframework.stereotype.Component;

@Component
public class FileScanResultMapper {
    public FileScanResponseDto toResponseDto(FileScanResult model) {
        return new FileScanResponseDto(
                model.token(),
                model.completed(),
                model.result()
        );
    }
}
