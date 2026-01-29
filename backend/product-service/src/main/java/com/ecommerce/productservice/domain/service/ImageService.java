package com.ecommerce.productservice.domain.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final Cloudinary cloudinary;

    public Map<String, String> uploadImage(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "products", "resource_type", "image"));

            return Map.of(
                    "url", (String) uploadResult.get("secure_url"),
                    "public_id", (String) uploadResult.get("public_id")
            );
        } catch (IOException e) {
            throw new RuntimeException("Image upload failed", e);
        }
    }
}