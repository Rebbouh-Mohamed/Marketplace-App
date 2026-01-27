package com.marketplace.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;
    private static final String BUCKET_NAME = "marketplace-images";

    public String uploadFile(MultipartFile file) {
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        try {
            // Ensure bucket exists (for localstack dev convenience)
            createBucketIfNotExists();

            java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                throw new RuntimeException("Invalid image file");
            }

            // Resize if width > 1080
            java.awt.image.BufferedImage processedImage = resizeImage(originalImage, 1080);

            // Compress to JPG (0.8 quality)
            java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
            java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO
                    .getImageWritersByFormatName("jpg");
            if (!writers.hasNext())
                throw new IllegalStateException("No JPG writer found");

            javax.imageio.ImageWriter writer = writers.next();
            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.8f);

            try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(os)) {
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(processedImage, null, null), param);
            }
            writer.dispose();

            byte[] imageBytes = os.toByteArray();

            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(BUCKET_NAME)
                    .key(fileName)
                    .contentType("image/jpeg")
                    .build();

            s3Client.putObject(putOb, RequestBody.fromBytes(imageBytes));

            // LocalStack URL structure
            return "http://localhost:4566/" + BUCKET_NAME + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    private java.awt.image.BufferedImage resizeImage(java.awt.image.BufferedImage original, int targetWidth) {
        if (original.getWidth() <= targetWidth) {
            // Ensure RGB for JPG consistency (remove alpha if present)
            if (original.getType() != java.awt.image.BufferedImage.TYPE_INT_RGB) {
                java.awt.image.BufferedImage newImage = new java.awt.image.BufferedImage(
                        original.getWidth(), original.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = newImage.createGraphics();
                g.drawImage(original, 0, 0, null);
                g.dispose();
                return newImage;
            }
            return original;
        }

        int targetHeight = (int) (original.getHeight() * ((double) targetWidth / original.getWidth()));
        java.awt.image.BufferedImage resized = new java.awt.image.BufferedImage(targetWidth, targetHeight,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resized;
    }

    private void createBucketIfNotExists() {
        try {
            s3Client.createBucket(b -> b.bucket(BUCKET_NAME));
        } catch (Exception e) {
            // Ignore if already exists
        }
    }
}
