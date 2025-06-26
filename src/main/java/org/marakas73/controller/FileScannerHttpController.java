package org.marakas73.controller;

import jakarta.validation.Valid;
import org.marakas73.controller.dto.response.ResponseWrapper;
import org.marakas73.controller.mapper.FileScanRequestMapper;
import org.marakas73.controller.dto.request.FileScanRequestDto;
import org.marakas73.controller.dto.response.FileScanResponseDto;
import org.marakas73.controller.dto.response.ResponseStatus;
import org.marakas73.model.FileScanRequest;
import org.marakas73.service.FileScanner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api")
public class FileScannerHttpController {
    private final FileScanner fileScanner;
    private final FileScanRequestMapper fileScanRequestMapper;

    public FileScannerHttpController(FileScanner fileScanner, FileScanRequestMapper fileScanRequestMapper) {
        this.fileScanner = fileScanner;
        this.fileScanRequestMapper = fileScanRequestMapper;
    }

    @PostMapping("filescan")
    public ResponseEntity<ResponseWrapper<FileScanResponseDto>> fileScan(
            @Valid @RequestBody FileScanRequestDto requestDto
    ) {
        try{
            FileScanRequest scanRequest = fileScanRequestMapper.toModel(requestDto);
            FileScanResponseDto scanResponse = new FileScanResponseDto(fileScanner.scan(scanRequest));

            return ResponseEntity.ok().body(new ResponseWrapper<>(ResponseStatus.SUCCESS, Map.of(), scanResponse));
        } catch (Exception e) {
            Map<String, String> error = Map.of(e.getClass().getName(), e.getMessage());
            return ResponseEntity.badRequest().body(new ResponseWrapper<>(ResponseStatus.ERROR, error, null));
        }
    }
}
