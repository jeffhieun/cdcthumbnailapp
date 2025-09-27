package com.cathay.cdc.thumbnail.poc.service;

import com.cathay.cdc.thumbnail.poc.dto.FileMetadata;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private Storage storage;

    @Value("${gcp.project-id}")
    private String projectId;

    @Value("${gcp.bucket-name}")
    private String bucketName;

    @Value("${thumbnail.width:150}")
    private int thumbnailWidth;

    @Value("${thumbnail.folder:thumbnails}")
    private String thumbnailFolder;

    @Value("${gcp.credentials.path}")
    private Resource credentialsResource;

    @PostConstruct
    private void init() throws IOException {
        log.info("Initializing GCP Storage client for project: {}", projectId);
        this.storage = StorageOptions.newBuilder()
                .setProjectId(projectId)
                .setCredentials(ServiceAccountCredentials.fromStream(credentialsResource.getInputStream()))
                .build()
                .getService();
        log.info("✅ GCP Storage initialized with bucket: {}", bucketName);
    }

    @Scheduled(fixedDelay = 60000) // every 1 min, waits for previous run
    public void generateThumbnailsJob() {
        log.info("🚀 Starting thumbnail generation job...");
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            log.error("❌ Bucket not found: {}", bucketName);
            return;
        }

        for (Blob blob : bucket.list(Storage.BlobListOption.pageSize(100)).iterateAll()) {
            if (isImage(blob) && needsThumbnail(blob)) {
                try {
                    createThumbnail(bucket, blob);
                } catch (Exception e) {
                    log.error("❌ Failed processing {}: {}", blob.getName(), e.getMessage(), e);
                }
            }
        }
    }

    public List<FileMetadata> getLastCreatedThumbnails() {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            log.error("❌ Bucket not found: {}", bucketName);
            return List.of();
        }

        List<FileMetadata> thumbnails = new ArrayList<>();

        for (Blob blob : bucket.list(Storage.BlobListOption.prefix(thumbnailFolder + "/")).iterateAll()) {
            // skip folder placeholders
            if (blob.isDirectory()) continue;

            String url = String.format("https://storage.googleapis.com/%s/%s",
                    blob.getBucket(), blob.getName());

            thumbnails.add(FileMetadata.builder()
                    .bucket(blob.getBucket())
                    .name(blob.getName())
                    .contentType(blob.getContentType())
                    .size(blob.getSize())
                    .url(url)
                    .build()
            );
        }
        log.info("📂 Found {} thumbnails in bucket {}/{}", thumbnails.size(), bucketName, thumbnailFolder);
        return thumbnails;
    }

    private boolean isImage(Blob blob) {
        return Optional.ofNullable(blob.getContentType())
                .map(type -> type.startsWith("image/"))
                .orElse(false);
    }

    private boolean needsThumbnail(Blob blob) {
        // Skip if already inside thumbnails folder
        if (blob.getName().startsWith(thumbnailFolder + "/")) {
            return false;
        }

        Map<String, String> metadata = Optional.ofNullable(blob.getMetadata()).orElse(Map.of());
        boolean markedAsGenerated = "true".equals(metadata.get("thumbnailGenerated"));

        if (markedAsGenerated) {
            // ✅ Check if thumbnail actually exists in the bucket
            String originalName = blob.getName().replaceFirst("^" + thumbnailFolder + "/+", "");
            String thumbName = thumbnailFolder + "/" + originalName;
            Blob thumbBlob = storage.get(bucketName, thumbName);

            if (thumbBlob == null || !thumbBlob.exists()) {
                log.warn("⚠️ Thumbnail missing for {}, will regenerate.", blob.getName());
                return true; // regenerate
            }
            return false; // thumbnail exists, skip
        }
        return true; // not marked yet → needs generation
    }

    private FileMetadata createThumbnail(Bucket bucket, Blob blob) throws IOException {
        byte[] content = blob.getContent();
        try (ByteArrayInputStream is = new ByteArrayInputStream(content)) {
            BufferedImage originalImage = ImageIO.read(is);
            if (originalImage == null) {
                log.warn("⚠️ Skipping {} - not a valid image", blob.getName());
                return null;
            }
            // Resize
            BufferedImage thumbnail = Scalr.resize(originalImage, Scalr.Method.QUALITY, thumbnailWidth);
            // Determine format
            String format = Optional.ofNullable(blob.getContentType())
                    .map(type -> type.substring(type.lastIndexOf('/') + 1))
                    .filter(f -> ImageIO.getImageWritersByFormatName(f).hasNext())
                    .orElse("jpg");

            // ✅ Ensure only one "thumbnails/" prefix
            String originalName = blob.getName().replaceFirst("^" + thumbnailFolder + "/+", "");
            String thumbNail = thumbnailFolder + "/" + originalName;

            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                ImageIO.write(thumbnail, format, os);

                Blob created = storage.create(
                        BlobInfo.newBuilder(bucket.getName(), thumbNail)
                                .setContentType("image/" + format)
                                .build(),
                        os.toByteArray()
                );

                // Mark original blob as processed
                Map<String, String> metadata = new HashMap<>(Optional.ofNullable(blob.getMetadata()).orElse(Map.of()));
                metadata.put("thumbnailGenerated", "true");
                blob.toBuilder().setMetadata(metadata).build().update();

                // Construct public URL
                String url = String.format("https://storage.googleapis.com/%s/%s",
                        created.getBucket(), created.getName());

                log.info("✅ Thumbnail created: {}", created.getName());

                return FileMetadata.builder()
                        .name(created.getName())
                        .bucket(created.getBucket())
                        .contentType(created.getContentType())
                        .size(created.getSize())
                        .url(url)
                        .build();
            }
        }
    }
}
