package org.marakas73.controller;

import jakarta.validation.Valid;
import org.marakas73.controller.dto.response.FileScanResponseDto;
import org.marakas73.controller.dto.response.ResponseWrapper;
import org.marakas73.controller.mapper.FileScanRequestMapper;
import org.marakas73.controller.dto.request.FileScanRequestDto;
import org.marakas73.controller.dto.response.ResponseStatus;
import org.marakas73.controller.mapper.FileScanResultMapper;
import org.marakas73.model.FileScanRequest;
import org.marakas73.model.FileScanResult;
import org.marakas73.service.filescanner.FileScanner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/file-scanner/scan")
public class FileScannerHttpController {
    private final FileScanner fileScanner;
    private final FileScanRequestMapper fileScanRequestMapper;
    private final FileScanResultMapper fileScanResultMapper;

    public FileScannerHttpController(
            FileScanner fileScanner,
            FileScanRequestMapper fileScanRequestMapper,
            FileScanResultMapper fileScanResultMapper
    ) {
        this.fileScanner = fileScanner;
        this.fileScanRequestMapper = fileScanRequestMapper;
        this.fileScanResultMapper = fileScanResultMapper;
    }

    @PostMapping
    public ResponseEntity<ResponseWrapper<FileScanResponseDto>> startScan(
            @Valid @RequestBody FileScanRequestDto requestDto
    ) {
        try{
            FileScanRequest scanRequest = fileScanRequestMapper.toModel(requestDto);
            FileScanResult scanResponse = fileScanner.startScan(scanRequest);
            FileScanResponseDto responseDto = fileScanResultMapper.toResponseDto(scanResponse);

            return ResponseEntity.ok().body(new ResponseWrapper<>(ResponseStatus.SUCCESS, Map.of(), responseDto));
        } catch (RuntimeException e) {
            Map<String, String> error = Map.of(
                    e.getClass().getSimpleName(),
                    e.getMessage() == null ? "" : e.getMessage()
            );
            return ResponseEntity.badRequest().body(new ResponseWrapper<>(ResponseStatus.ERROR, error, null));
        }
    }

    @GetMapping("/{token}")
    public ResponseEntity<ResponseWrapper<FileScanResult>> getResult(@PathVariable String token) {
    public ResponseEntity<ResponseWrapper<FileScanResponseDto>> getResult(@PathVariable String token) {
        try{
            FileScanResult result = fileScanner.getResult(token);

            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            var resultDto = fileScanResultMapper.toResponseDto(result);

            if (resultDto.completed()) {
                return ResponseEntity.ok(new ResponseWrapper<>(ResponseStatus.SUCCESS, Map.of(), resultDto));
            } else {
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(new ResponseWrapper<>(ResponseStatus.SUCCESS, Map.of(), resultDto));
            }
        } catch (RuntimeException e) {
            Map<String, String> error = Map.of(
                    e.getClass().getSimpleName(),
                    e.getMessage() == null ? "" : e.getMessage()
            );
            return ResponseEntity.badRequest().body(new ResponseWrapper<>(ResponseStatus.ERROR, error, null));
        }
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> killScan(@PathVariable String token) {
        boolean stopped = fileScanner.kill(token);
        return stopped ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
