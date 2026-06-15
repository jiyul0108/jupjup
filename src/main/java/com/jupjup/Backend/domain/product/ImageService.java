package com.jupjup.Backend.domain.product;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageService {

    private final Cloudinary cloudinary;
    private final ProductImageRepository productImageRepository;

    public List<String> uploadImages(Product product, List<MultipartFile> files) throws IOException {
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            // Cloudinary에 업로드
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "jupjup",
                            "resource_type", "image"
                    ));

            String imageUrl = (String) uploadResult.get("secure_url");
            imageUrls.add(imageUrl);

            // DB에 저장
            ProductImage productImage = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .build();
            productImageRepository.save(productImage);
        }

        return imageUrls;
    }
}
