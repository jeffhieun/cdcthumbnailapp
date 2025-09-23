package com.cathay.cdc.thumbnail.poc.service;

import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class GcpStorageService {

    private final Storage storage ;
    public GcpStorageService() {
        // Automatically uses GOOGLE_APPLICATION_CREDENTIALS
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    public String uploadFile(String bucketName, MultipartFile file) throws IOException {
        String objectName = file.getOriginalFilename();

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        storage.create(blobInfo, file.getBytes());

        return String.format("gs://%s/%s", bucketName, objectName);
    }

    public String uploadMultipartFile(String bucketName, MultipartFile file) {

        String extension = getFileExtension(file.getOriginalFilename());
        String objectName = UUID.randomUUID() + extension;
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            // Make the object public
            storage.createAcl(blobId, Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

            String publicUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
            log.info("File uploaded successfully: {} -> {}", file.getOriginalFilename(), publicUrl);
            return publicUrl;
        } catch (IOException e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
    public byte[] downloadFile(String bucketName, String objectName) {
        Blob blob = storage.get(bucketName, objectName);
        if (blob == null) {
            log.error("File not found: {}", objectName);
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
