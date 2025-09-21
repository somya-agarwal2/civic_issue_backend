package com.example.civic_issue.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a MultipartFile to Cloudinary inside the specified folder.
     * Supports both images and audio automatically.
     *
     * @param file   Multipart file to upload
     * @param folder Cloudinary folder name (e.g., "complaints/photos")
     * @return Secure URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    )
            );
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads a Base64 string to Cloudinary inside the specified folder.
     * Supports both images and audio automatically.
     *
     * @param base64Data Base64 encoded file data
     * @param folder     Cloudinary folder name (e.g., "complaints/photos")
     * @return Secure URL of the uploaded file
     */
    public String uploadBase64(String base64Data, String folder) {
        if (base64Data == null || base64Data.isBlank()) {
            throw new IllegalArgumentException("Base64 data must not be empty");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    base64Data,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    )
            );
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload base64 data to Cloudinary: " + e.getMessage(), e);
        }
    }
}
