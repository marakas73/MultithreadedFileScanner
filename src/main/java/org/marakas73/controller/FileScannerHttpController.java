package org.marakas73.controller;

import jakarta.validation.Valid;
import org.marakas73.controller.dto.response.ResponseWrapper;
import org.marakas73.controller.mapper.FileScanRequestMapper;
import org.marakas73.controller.dto.request.FileScanRequestDto;
import org.marakas73.controller.dto.response.ResponseStatus;
import org.marakas73.model.FileScanRequest;
import org.marakas73.model.FileScanResult;
import org.marakas73.service.filescanner.FileScanner;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/file-scanner/scan")
public class FileScannerHttpController {
    private final FileScanner fileScanner;
    private final FileScanRequestMapper fileScanRequestMapper;

    public FileScannerHttpController(FileScanner fileScanner, FileScanRequestMapper fileScanRequestMapper) {
        this.fileScanner = fileScanner;
        this.fileScanRequestMapper = fileScanRequestMapper;
    }

    @PostMapping
    public ResponseEntity<ResponseWrapper<FileScanResult>> startScan(
            @Valid @RequestBody FileScanRequestDto requestDto
    ) {
        try{
            FileScanRequest scanRequest = fileScanRequestMapper.toModel(requestDto);
            FileScanResult scanResponse = fileScanner.startScan(scanRequest);

            return ResponseEntity.ok().body(new ResponseWrapper<>(ResponseStatus.SUCCESS, Map.of(), scanResponse));
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
