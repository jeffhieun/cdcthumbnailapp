package com.cathay.cdc.thumbnail.poc.controller;

import com.cathay.cdc.thumbnail.poc.controller.dto.ApiResponse;
import com.cathay.cdc.thumbnail.poc.dto.FileMetadata;
import com.cathay.cdc.thumbnail.poc.service.GcpStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final GcpStorageService gcpStorageService;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileMetadata>>> listImages() {
        log.info("Fetching list of images from bucket: {}", bucketName);
        List<FileMetadata> images = gcpStorageService.listFiles(bucketName);
        return ResponseEntity.ok(ApiResponse.success(images, "Fetched image list successfully"));
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<FileMetadata>>> upload(@RequestParam("files") List<MultipartFile> files) {
        List<FileMetadata>  publicUrl = gcpStorageService.uploadMultipartFiles(bucketName, files);
        log.info("File uploaded: {}", publicUrl.size());
        return ResponseEntity.ok(ApiResponse.success(publicUrl, "Files uploaded successfully"));
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<ApiResponse<byte[]>> download(@PathVariable("fileName") String fileName) {
        byte[] fileContent = gcpStorageService.downloadFile(bucketName, fileName);
        log.info("File downloaded: {}", fileName);
        return ResponseEntity.ok(ApiResponse.success(fileContent, "File downloaded successfully"));
    }
}
