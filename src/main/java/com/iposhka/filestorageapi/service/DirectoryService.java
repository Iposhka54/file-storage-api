package com.iposhka.filestorageapi.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class DirectoryService {
    @Value("${minio.rootBucket}")
    private String rootBucket;
    private final MinioClient minioClient;
    private static final String USER_DIRECTORY = "user-%d-files/";

    @SneakyThrows
    public void createUserFolder(int id) {
        String path = USER_DIRECTORY.formatted(id);
        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(rootBucket)
                        .object(path)
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                .build());
    }
}