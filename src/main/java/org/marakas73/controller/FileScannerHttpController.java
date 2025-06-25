package org.marakas73.controller;

import org.marakas73.dto.FileScanResponseDto;
import org.marakas73.dto.ResponseStatus;
import org.marakas73.service.FileScanner;
import org.marakas73.service.validation.FileScannerParamsValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("api")
public class FileScannerHttpController {
    private final FileScanner fileScanner;
    private final FileScannerParamsValidator scannerParamsValidator;

    public FileScannerHttpController(FileScanner fileScanner, FileScannerParamsValidator scannerParamsValidator) {
        this.fileScanner = fileScanner;
        this.scannerParamsValidator = scannerParamsValidator;
    }

    @GetMapping("filescan")
    public ResponseEntity<FileScanResponseDto> fileScan(
            @RequestParam(name = "path") String directoryPath,
            @RequestParam(name = "pattern") String pattern,
            @RequestParam(name = "threads-count", required = false) Integer threadsCount
    ) {
        if(!scannerParamsValidator.isPathValid(directoryPath)) {
            return ResponseEntity.badRequest().body(new FileScanResponseDto(ResponseStatus.ERROR, List.of()));
        }

        try{
            Path path = Paths.get(directoryPath);
            FileScanResponseDto responseBody;

            if(threadsCount == null) { // With default threads count
                responseBody = new FileScanResponseDto(ResponseStatus.SUCCESS, fileScanner.scan(path, pattern));
            } else { // With custom threads count
                responseBody = new FileScanResponseDto(
                        ResponseStatus.SUCCESS,
                        fileScanner.scan(path, pattern, threadsCount)
                );
            }
            return ResponseEntity.ok().body(responseBody);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new FileScanResponseDto(ResponseStatus.ERROR, List.of()));
        }
    }
}
