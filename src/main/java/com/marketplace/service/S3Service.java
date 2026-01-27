package com.marketplace.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void createBucketIfNotExists() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            System.out.println("Bucket " + bucketName + " already exists.");
        } catch (NoSuchBucketException e) {
            System.out.println("Creating bucket " + bucketName);
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        } catch (Exception e) {
            System.err.println("Error checking/creating bucket: " + e.getMessage());
        }
    }

    public String uploadFile(byte[] content, String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = "products/" + UUID.randomUUID().toString() + extension;

        s3Client.putObject(PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build(), RequestBody.fromBytes(content));

        return key;
    }

    public byte[] downloadFile(String key) throws IOException {
        ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
        return s3Object.readAllBytes();
    }
}
