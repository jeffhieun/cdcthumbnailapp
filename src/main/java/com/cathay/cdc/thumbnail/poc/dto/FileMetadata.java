package com.cathay.cdc.thumbnail.poc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder // Gives you fluent builder API
@NoArgsConstructor // Required by JPA (reflection)
@AllArgsConstructor // Needed by @Builder (to call all-args constructor)
public class FileMetadata {
    private String name;
    private String url;
    private long size;          // in bytes
    private LocalDateTime createdAt;
}
