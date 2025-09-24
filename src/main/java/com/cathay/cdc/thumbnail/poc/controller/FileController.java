package com.cathay.cdc.thumbnail.poc.controller;

import com.cathay.cdc.thumbnail.poc.controller.dto.ApiResponse;
import com.cathay.cdc.thumbnail.poc.service.GcpStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final GcpStorageService gcpStorageService;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String publicUrl = gcpStorageService.uploadMultipartFile(bucketName, file);
            log.info("File uploaded successfully: {}", file.getOriginalFilename());
            return ResponseEntity.ok(
                    ApiResponse.success(publicUrl, "File uploaded successfully: " + file.getOriginalFilename())
            );
        } catch (Exception e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<ApiResponse<byte[]>> download(@PathVariable("fileName") String fileName) {
        try {
            byte[] fileContent = gcpStorageService.downloadFile(bucketName, fileName);
            return ResponseEntity.ok(
                    ApiResponse.success(fileContent, "File downloaded successfully: " + fileName)
            );
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to download file: " + e.getMessage()));
        }
    }
}
