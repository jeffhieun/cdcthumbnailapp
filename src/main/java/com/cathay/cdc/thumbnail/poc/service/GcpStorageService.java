package com.cathay.cdc.thumbnail.poc.service;

import com.cathay.cdc.thumbnail.poc.dto.FileMetadata;
import com.cathay.cdc.thumbnail.poc.libs.TimeUtil;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class GcpStorageService {

    @Value("${gcp.project-id}")
    private String projectId;
    @Value("${gcp.bucket-name}")
    private String bucketName;
    @Value("${gcp.credentials.path}")
    private Resource credentialsResource;
    private Storage storage;

    @PostConstruct
    private void init() throws IOException {
        log.info("Initializing GCP Storage client for project: {}", projectId);
        this.storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(ServiceAccountCredentials.fromStream(credentialsResource.getInputStream()))
                .build()
                .getService();
        log.info("GCP Storage initialized successfully with bucket: {}", bucketName);
        log.debug("Loaded GCP credentials: {}", storage.getOptions().getCredentials().toString());
    }

    @Cacheable(value = "listFilesCache", key = "#bucketName")
    public List<FileMetadata> listFiles(String bucketName) {
        log.info("Listing files in bucket: {}", bucketName);
        Bucket bucket = storage.get(bucketName);

        if(bucket == null) {
            log.error("Bucket not found: {}", bucketName);
            throw new RuntimeException("Bucket not found: " + bucketName);
        }

        List<FileMetadata> fileMetadataList = StreamSupport
                .stream(bucket.list().iterateAll().spliterator(), false)
                .map(blob -> {
                    FileMetadata metadata = FileMetadata.builder()
                            .name(blob.getName())
                            .url(String.format("https://storage.googleapis.com/%s/%s", bucketName, blob.getName()))
                            .createdAt(TimeUtil.toLocalDateTime(blob.getCreateTime()))
                            .build();
                    log.debug("Found file: {}", metadata);
                    return metadata;
                })
                // sort by createdAt DESC
                .sorted(Comparator.comparing(FileMetadata::getCreatedAt).reversed())
                .collect(Collectors.toList());
        log.info("Total files found in bucket {}: {}", bucketName, fileMetadataList.size());
        return fileMetadataList;
    }

    @CacheEvict(value = "listFilesCache", key = "#bucketName") // clear cache for this bucket
    public List<FileMetadata> uploadMultipartFiles(String bucketName, List<MultipartFile> files) {
        List<FileMetadata> metadataList = new ArrayList<>();
        for (MultipartFile file : files) {
            String extension = getFileExtension(file.getOriginalFilename());
            String objectName = UUID.randomUUID() + extension;

            try {
                BlobId blobId = BlobId.of(bucketName, objectName);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType(file.getContentType())
                        .build();
                storage.create(blobInfo, file.getBytes());

                String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);

                FileMetadata fileMetadata = FileMetadata.builder()
                        .name(file.getName())
                        .url(publicUrl)
                        .createdAt(LocalDateTime.now())
                        .build();
                metadataList.add(fileMetadata);
                log.info("File uploaded: {} -> {}", file.getOriginalFilename(), publicUrl);
            } catch (IOException e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
            }
        }
        return metadataList;
    }

    public byte[] downloadFile(String bucketName, String objectName) {
        log.info("Downloading file: {} from bucket: {}", objectName, bucketName);
        Blob blob = storage.get(bucketName, objectName);
        if (blob == null) {
            log.error("File not found in bucket {}: {}", bucketName, objectName);
            throw new RuntimeException("File not found: " + objectName);
        }
        log.info("File downloaded successfully: {}", objectName);
        return blob.getContent();
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
