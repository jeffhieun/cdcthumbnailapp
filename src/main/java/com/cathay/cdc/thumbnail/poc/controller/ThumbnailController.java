package com.cathay.cdc.thumbnail.poc.controller;

import com.cathay.cdc.thumbnail.poc.controller.dto.ApiResponse;
import com.cathay.cdc.thumbnail.poc.dto.FileMetadata;
import com.cathay.cdc.thumbnail.poc.service.ThumbnailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ThumbnailController {

    private final ThumbnailService thumbnailService;

    @GetMapping("/thumbnails/last")
    public ResponseEntity<ApiResponse<List<FileMetadata>>> getLastCreatedThumbnails() {
        log.info("📥 Request received: GET /api/admin/thumbnails/last");

        List<FileMetadata> thumbnails = thumbnailService.getLastCreatedThumbnails();
        log.info("📊 Returning {} thumbnails", thumbnails.size());

        ApiResponse<List<FileMetadata>> response = ApiResponse.<List<FileMetadata>>builder()
                .success(true)
                .message("Thumbnails generated in last job run")
                .data(thumbnails)
                .build();

        log.info("✅ Response ready for /api/admin/thumbnails/last");
        return ResponseEntity.ok(response);
    }
}
