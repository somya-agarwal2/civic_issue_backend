package com.example.civic_issue.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WhatsAppCloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads a file from an InputStream to Cloudinary.
     * Works for both photos and voice notes.
     *
     * @param inputStream InputStream of the file
     * @param folder      Cloudinary folder, e.g., "complaints/photos" or "complaints/voice"
     * @return Secure URL of uploaded file
     */
    public String uploadFile(InputStream inputStream, String folder) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream must not be null");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    inputStream,
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
}
