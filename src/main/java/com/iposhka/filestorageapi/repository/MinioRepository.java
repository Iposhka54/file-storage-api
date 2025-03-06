package com.iposhka.filestorageapi.repository;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
    private static final ByteArrayInputStream EMPTY_BYTE_STREAM = new ByteArrayInputStream(new byte[0]);

    @PostConstruct
    public void makeBucket() throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(rootBucket)
                .build());

        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(rootBucket)
                    .build());
        }
    }

    public void createUserDirectory(String path) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(rootBucket)
                .object(path)
                .stream(EMPTY_BYTE_STREAM, 0, -1)
                .build());
    }

    public Iterable<Result<Item>> listObjects(String path, boolean recursive) {
        return minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(rootBucket)
                .prefix(path)
                .recursive(recursive)
                .build());
    }

    public void createEmptyDirectory(String path) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(rootBucket)
                .object(path)
                .stream(EMPTY_BYTE_STREAM, 0, -1)
                .build());
    }

    public GetObjectResponse getObject(String path) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(rootBucket)
                .object(path)
                .build());
    }

    public StatObjectResponse statObject(String path) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        return minioClient.statObject(StatObjectArgs.builder()
                .bucket(rootBucket)
                .object(path)
                .build());
    }

    public void deleteObject(String fullPath) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(rootBucket)
                        .object(fullPath)
                .build());
    }

    public void copyObject(String from, String to) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        minioClient.copyObject(CopyObjectArgs.builder()
                        .bucket(rootBucket)
                        .object(to)
                        .source(CopySource.builder()
                                .bucket(rootBucket)
                                .object(from)
                                .build())
                .build());
    }
}