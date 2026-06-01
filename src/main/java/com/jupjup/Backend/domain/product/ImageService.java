package com.jupjup.Backend.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final ProductImageRepository productImageRepository;

    public List<String> uploadImages(Product product, List<MultipartFile> files) throws IOException {
        // 절대 경로로 업로드 폴더 생성
        String absoluteUploadDir = System.getProperty("user.dir") + "/" + uploadDir;
        File dir = new File(absoluteUploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            File dest = new File(absoluteUploadDir + "/" + fileName);
            file.transferTo(dest);

            String imageUrl = "/uploads/" + fileName;
            imageUrls.add(imageUrl);

            ProductImage productImage = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .build();
            productImageRepository.save(productImage);
        }

        return imageUrls;
    }
}