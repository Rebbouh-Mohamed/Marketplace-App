package com.marketplace.controller;

import com.marketplace.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Service s3Service;

    @GetMapping("/**")
    public ResponseEntity<byte[]> getImage(HttpServletRequest request) {
        try {
            // Extract the full path after /images/
            String path = request.getRequestURI().split(request.getContextPath() + "/images/")[1];
            // Decode potential URL encoding if necessary, but S3 keys are usually safe

            byte[] imageBytes = s3Service.downloadFile(path);

            // Determine content type (basic check)
            MediaType mediaType = MediaType.IMAGE_JPEG;
            if (path.toLowerCase().endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else if (path.toLowerCase().endsWith(".webp")) {
                mediaType = MediaType.parseMediaType("image/webp");
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
