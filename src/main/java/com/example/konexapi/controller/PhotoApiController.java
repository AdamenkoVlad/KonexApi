package com.example.konexapi.controller;

import com.example.konexapi.entity.UploadedFile;
import com.example.konexapi.service.KonexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/photos")
@CrossOrigin(origins = "*")
public class PhotoApiController {

    @Autowired
    private KonexService konexService;

    @GetMapping("/gallery")
    public ResponseEntity<?> getPhotosForGallery(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
            List<UploadedFile> allPhotos = konexService.getUploadHistory();


            int start = page * size;
            int end = Math.min(start + size, allPhotos.size());
            List<UploadedFile> pagePhotos = allPhotos.subList(start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("photos", pagePhotos);
            response.put("currentPage", page);
            response.put("totalPages", (allPhotos.size() + size - 1) / size);
            response.put("totalElements", allPhotos.size());
            response.put("hasNext", end < allPhotos.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("Помилка завантаження галереї: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePhoto(@PathVariable Long id) {
        try {
            boolean deleted = konexService.deletePhoto(id);
            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Фото успішно видалено");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("Помилка видалення фото: " + e.getMessage()));
        }
    }

    @GetMapping("/download-all")
    public ResponseEntity<byte[]> downloadAllPhotos(@RequestParam(required = false) List<Long> ids) {
        try {
            List<UploadedFile> photos;
            if (ids != null && !ids.isEmpty()) {
                photos = konexService.getPhotosByIds(ids);
            } else {
                photos = konexService.getUploadHistory();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {

                for (int i = 0; i < photos.size(); i++) {
                    UploadedFile photo = photos.get(i);
                    try {
                        // Завантажуємо фото з CDN
                        URL url = new URL(photo.getCdnUrl());
                        byte[] imageData = url.openStream().readAllBytes();

                        // Додаємо до архіву
                        String fileName = String.format("%03d_%s", i + 1, photo.getOriginalFilename());
                        ZipEntry entry = new ZipEntry(fileName);
                        zos.putNextEntry(entry);
                        zos.write(imageData);
                        zos.closeEntry();

                    } catch (Exception e) {
                        System.err.println("Не вдалося додати фото " + photo.getOriginalFilename() + " до архіву: " + e.getMessage());
                    }
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "photos.zip");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(baos.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}