package com.marketplace.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageService {

    private static final int MAX_WIDTH = 1200;
    private static final int MAX_HEIGHT = 1200;
    private static final double OUTPUT_QUALITY = 0.8;

    public byte[] compressImage(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "jpg"; // Default to jpg for consistency, or extract from filename

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        Thumbnails.of(file.getInputStream())
                .size(MAX_WIDTH, MAX_HEIGHT)
                .outputQuality(OUTPUT_QUALITY)
                .outputFormat(extension)
                .toOutputStream(outputStream);

        return outputStream.toByteArray();
    }
}
