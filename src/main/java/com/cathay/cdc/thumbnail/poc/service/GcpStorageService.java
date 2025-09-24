package com.cathay.cdc.thumbnail.poc.service;

import com.cathay.cdc.thumbnail.poc.dto.FileMetadata;
import com.cathay.cdc.thumbnail.poc.libs.TimeUtil;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class GcpStorageService {

    private static final Logger log = LoggerFactory.getLogger(GcpStorageService.class);
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

        List<FileMetadata> fileMetadataList = new ArrayList<>();
        for(Blob blob: bucket.list().iterateAll()) {
            FileMetadata fileMetadata = FileMetadata.builder()
                    .name(blob.getName())
                    .url(String.format("https://storage.googleapis.com/%s/%s", bucketName, blob.getName()))
                    .createdAt(TimeUtil.toLocalDateTime(blob.getCreateTime()))
                    .build();
            fileMetadataList.add(fileMetadata);
            log.debug("Found file: {}", fileMetadataList);
        }
        log.info("Total files found in bucket {}: {}", bucketName, fileMetadataList.size());
        return fileMetadataList;
    }

    public String uploadFile(String bucketName, MultipartFile file) throws IOException {
        String objectName = file.getOriginalFilename();
        log.info("Uploading file: {} to bucket: {}", objectName, bucketName);

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        storage.create(blobInfo, file.getBytes());

        String gsUrl = String.format("gs://%s/%s", bucketName, objectName);
        log.info("File uploaded successfully: {}", gsUrl);
        return gsUrl;
    }

    @CacheEvict(value = "listFilesCache", allEntries = true)
    public String uploadMultipartFile(String bucketName, MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename());
        String objectName = UUID.randomUUID() + extension;
        log.info("Uploading multipart file: {} as {}", file.getOriginalFilename(), objectName);
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();
            storage.create(blobInfo, file.getBytes());

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
            log.info("File uploaded successfully: {} -> {}", file.getOriginalFilename(), publicUrl);
            return publicUrl;
        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
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
