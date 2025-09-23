package com.cathay.cdc.thumbnail.poc.controller;

import com.cathay.cdc.thumbnail.poc.service.GcpStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final String bucketName = "plasma-galaxy-472907-c7-thumbnails"; // or inject via @Value

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String publicUrl = gcpStorageService.uploadFile(bucketName, file);
            log.info("File uploaded successfully: {}", file.getOriginalFilename());
            return ResponseEntity.ok(publicUrl);
        } catch (Exception e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> download(@PathVariable String fileName) {
        try {
            byte[] content = gcpStorageService.downloadFile(bucketName, fileName);
            log.info("File downloaded successfully: {}", fileName);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(content);
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

//    @PostMapping("/upload")
//    public String upload(@RequestParam("file") MultipartFile file) throws Exception {
//        return gcpStorageService.uploadFile("your-bucket-name", file.getOriginalFilename(), file.getBytes());
//    }
//
//    @GetMapping("/download/{fileName}")
//    public byte[] download(@PathVariable String fileName) {
//        return gcpStorageService.downloadFile("your-bucket-name", fileName);
//    }
}
