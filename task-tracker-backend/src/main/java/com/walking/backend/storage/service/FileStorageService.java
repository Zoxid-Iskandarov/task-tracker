package com.walking.backend.storage.service;

import com.google.common.collect.Lists;
import com.walking.backend.props.AppProperties;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.DeleteRequest;
import io.minio.messages.DeleteResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {
    private final MinioClient minioClient;
    private final AppProperties.Minio minioProperties;

    @PostConstruct
    public void createBucket() throws MinioException {
        try {
            boolean existsAvatarBucket = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioProperties.getBucketAvatar())
                    .build());

            boolean existsAttachmentBucket = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(minioProperties.getBucketAttachment())
                    .build());

            if (!existsAvatarBucket) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioProperties.getBucketAvatar())
                        .build());
                log.info("Minio bucket '{}' created successfully", minioProperties.getBucketAvatar());

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
                        """.formatted(minioProperties.getBucketAvatar());

                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(minioProperties.getBucketAvatar())
                        .config(policy)
                        .build());
            } else {
                log.info("Minio bucket '{}' already exists", minioProperties.getBucketAvatar());
            }

            if (!existsAttachmentBucket) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(minioProperties.getBucketAttachment())
                        .build());
                log.info("Minio bucket '{}' created successfully", minioProperties.getBucketAttachment());
            } else {
                log.info("Minio bucket '{}' already exists", minioProperties.getBucketAttachment());
            }
        } catch (MinioException e) {
            log.error("Failed to initialize Minio buckets", e);
            throw new MinioException(e);
        }
    }

    public String uploadAvatar(Long userId, MultipartFile file) {
        return upload(minioProperties.getBucketAvatar(), userId, file);
    }

    public String uploadAttachment(Long taskId, MultipartFile file) {
        return upload(minioProperties.getBucketAttachment(), taskId, file);
    }

    public void deleteAvatar(String objectName) {
        delete(minioProperties.getBucketAvatar(), objectName);
    }

    public void deleteAttachment(String objectName) {
        delete(minioProperties.getBucketAttachment(), objectName);
    }

    public String generatePresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(minioProperties.getBucketAttachment())
                    .object(objectName)
                    .method(Http.Method.GET)
                    .expiry(minioProperties.getAttachment().getPresignedUrlExpiration(), TimeUnit.MINUTES)
                    .build());
        } catch (MinioException e) {
            throw new RuntimeException("Error generating presigned URL", e);
        }
    }

    public void deleteAttachments(List<String> objectNames) {
        batchDelete(minioProperties.getBucketAttachment(), objectNames);
    }

    private String upload(String bucket, Long id, MultipartFile file) {
        String objectName = "%d/%s".formatted(id, UUID.randomUUID().toString());

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(inputStream, file.getSize(), -1L)
                    .contentType(file.getContentType())
                    .build());
        } catch (IOException | MinioException e) {
            throw new RuntimeException("Error uploading file to bucket %s".formatted(bucket), e);
        }

        return objectName;
    }

    private void delete(String bucket, String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (MinioException e) {
            log.error("Failed to deleteAttachment file {} from bucket {}", objectName, bucket, e);
        }
    }

    private void batchDelete(String bucket, List<String> objectNames) {
        if (objectNames == null || objectNames.isEmpty()) {
            return;
        }

        List<List<String>> partitions = Lists.partition(objectNames, minioProperties.getBatchDeleteSize());

        for (List<String> partition : partitions) {
            List<DeleteRequest.Object> objects = partition.stream()
                    .map(DeleteRequest.Object::new)
                    .toList();

            try {
                Iterable<Result<DeleteResult.Error>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                                .bucket(bucket)
                                .objects(objects)
                                .build());

                for (Result<DeleteResult.Error> result : results) {
                    DeleteResult.Error error = result.get();
                    if (error != null) {
                        log.error("Failed to delete file: {} with error: {}", error.objectName(), error.message());
                    }
                }

                log.info("Successfully processed batch deletion for {} files", objects.size());
            } catch (Exception e) {
                log.error("Batch deletion request failed for bucket {}", bucket, e);
            }
        }
    }
}
