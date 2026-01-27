package com.marketplace.product;

import com.marketplace.service.ImageService;
import com.marketplace.service.S3Service;
import com.marketplace.user.User;
import com.marketplace.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;
    private final S3Service s3Service;
    private final ImageService imageService;
    private final UserService userService;

    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(@ModelAttribute Product product,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam("imageFiles") List<MultipartFile> imageFiles,
            @RequestParam(value = "tags", required = false) String tags,
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        try {
            User seller = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Product savedProduct = productService.createProduct(product, categoryId, seller, tags);

            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    byte[] compressedImage = imageService.compressImage(file);
                    String s3Key = s3Service.uploadFile(compressedImage, file.getOriginalFilename());
                    imageUrls.add(s3Key);
                }
            }
            productService.addImages(savedProduct, imageUrls);

            // Re-fetch product to get full details including images
            return ResponseEntity.ok(productService.findById(savedProduct.getId()));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating product: " + e.getMessage());
        }
    }
}
