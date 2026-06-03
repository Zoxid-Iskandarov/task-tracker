package com.walking.backend.service.impl;

import com.walking.backend.service.FileStorageService;
import io.minio.*;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {
    private final MinioClient minioClient;

    @Value("${app.minio.bucket}")
    private final String bucket;

    @PostConstruct
    public void createBucket() throws MinioException {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build());

            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build());
                log.info("Minio bucket '{}' created successfully", bucket);

                String policy = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": {"AWS": ["*"]},
                            "Action": ["s3:GetObject"],
                            "Resource": ["arn:aws:s3:::%s/*"]
                        }
                    ]
                }
                """.formatted(bucket);

                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(policy)
                        .build());
            } else {
                log.info("Minio bucket '{}' already exists", bucket);
            }
        } catch (MinioException e) {
            log.error("Failed to initialize Minio bucket '{}'", bucket, e);
            throw new MinioException(e);
        }
    }

    @Override
    public String upload(Long userId, MultipartFile file) {
        String objectName = "%d/%s".formatted(userId, UUID.randomUUID().toString());

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
        } catch (IOException | MinioException e) {
            throw new RuntimeException("Error uploading file to MinIO", e);
        }

        return objectName;
    }

    @Override
    public void delete(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (MinioException e) {
            log.error("Failed to delete file {} from MinIO", objectName, e);
        }
    }
}
