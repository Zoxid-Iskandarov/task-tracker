package com.walking.backend.integration.util;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.DeleteRequest;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MinioTestHelper {
    private final MinioClient minioClient;

    public void clearBucket(String bucketName) {
        try {
            List<DeleteRequest.Object> deleteObjects = new ArrayList<>();
            Iterable<Result<Item>> objects = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .build());

            for (Result<Item> object : objects) {
                deleteObjects.add(new DeleteRequest.Object(object.get().objectName()));
            }

            if (!deleteObjects.isEmpty()) {
                minioClient.removeObjects(RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean up MinIO bucket: " + bucketName, e);
        }
    }

    public boolean objectExists(String bucketName, String objectName) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new RuntimeException("MinIO error", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to check object existence", e);
        }
    }
}
