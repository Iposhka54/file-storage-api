package com.iposhka.filestorageapi.repository;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Repository
@RequiredArgsConstructor
public class MinioRepository {
    @Value("${minio.rootBucket}")
    private String rootBucket;
    private final MinioClient minioClient;

    @PostConstruct
    @SneakyThrows
    public void makeBucket() {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(rootBucket)
                .build());

        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(rootBucket)
                    .build());
        }
    }

    @SneakyThrows
    public void createUserDirectory(String fullPath) {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(rootBucket)
                .object(fullPath)
                .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                .build());
    }

    public Iterable<Result<Item>> listObjects(String fullPath) {
        return minioClient.listObjects(ListObjectsArgs.builder()
                        .bucket(rootBucket)
                        .prefix(fullPath)
                        .recursive(false)
                .build());
    }

    public void createEmptyDirectory(String fullPath) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(rootBucket)
                .object(fullPath)
                .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                .build());
    }

    public GetObjectResponse getObject(String fullPath) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(rootBucket)
                .object(fullPath)
                .build());
    }
}