package com.example.konexapi.controller;

import com.example.konexapi.dto.UploadResponse;
import com.example.konexapi.entity.UploadedFile;
import com.example.konexapi.service.KonexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class KonexController {

    @Autowired
    private KonexService konexService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPhotos(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "input", required = false, defaultValue = "file") String inputKey) {

        try {
            System.out.println("=== Початок процесу завантаження фото ===");
            System.out.println("Кількість файлів: " + (files != null ? files.length : 0));
            System.out.println("Шлях для завантаження: " + (path != null ? path : "не вказано"));
            System.out.println("Input key: " + inputKey);


            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Файли не надані"));
            }

            if (files.length > 50) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Максимум 50 файлів дозволено (згідно з документацією Konex API)"));
            }


            for (MultipartFile file : files) {
                if (!file.getContentType().startsWith("image/")) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Дозволені тільки файли зображень. Файл '" +
                                    file.getOriginalFilename() + "' має тип: " + file.getContentType()));
                }
            }

            System.out.println("Валідація пройшла успішно. Починаємо завантаження...");


            UploadResponse response = konexService.uploadFiles(files, path, inputKey);

            System.out.println("=== Завантаження завершено успішно ===");
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            System.err.println("Помилка вводу/виводу: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Помилка завантаження: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Загальна помилка: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Внутрішня помилка сервера: " + e.getMessage()));
        }
    }
    @GetMapping("/history")
    public ResponseEntity<List<UploadedFile>> getUploadHistory() {
        try {
            List<UploadedFile> history = konexService.getUploadHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/files")
    public ResponseEntity<List<UploadedFile>> getFilesByPath(@RequestParam String path) {
        try {
            List<UploadedFile> files = konexService.getFilesByPath(path);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/auth/token")
    public ResponseEntity<?> getAuthToken() {
        try {
            System.out.println("Запит на отримання токену авторизації");
            String token = konexService.getAuthToken();
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("auth_server", "https://auth.konex.com.ua/login");
            response.put("status", "success");
            System.out.println("Токен успішно отриманий");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Помилка отримання токену: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Не вдалося отримати токен авторизації: " + e.getMessage()));
        }
    }

    @PostMapping("/test-auth")
    public ResponseEntity<?> testAuthentication() {
        try {
            System.out.println("Тестуємо з'єднання з сервером авторизації...");
            String token = konexService.getAuthToken();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Авторизація пройшла успішно");
            response.put("auth_server", "https://auth.konex.com.ua/login");
            response.put("token_preview", token.substring(0, Math.min(20, token.length())) + "...");
            response.put("credentials_used", Map.of(
                    "login", "test1",
                    "user_type", "user_company"
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Тест авторизації не пройшов: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Тест авторизації не пройшов: " + e.getMessage()));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
    @PostMapping("/upload-photos")
    public ResponseEntity<?> uploadPhotosWithProcessing(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "path", required = false) String path,
            @RequestParam(value = "input", required = false, defaultValue = "file") String inputKey) {

        try {
            System.out.println("=== Початок процесу завантаження фото з обробкою ===");
            System.out.println("Кількість файлів: " + (files != null ? files.length : 0));
            System.out.println("Шлях для завантаження: " + (path != null ? path : "не вказано"));
            System.out.println("Input key: " + inputKey);


            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Файли не надані"));
            }

            if (files.length > 50) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Максимум 50 файлів дозволено (згідно з документацією Konex API)"));
            }


            for (MultipartFile file : files) {
                String contentType = file.getContentType();
                if (contentType == null ||
                        (!contentType.equals("image/jpeg") &&
                                !contentType.equals("image/jpg") &&
                                !contentType.equals("image/png"))) {
                    return ResponseEntity.badRequest()
                            .body(createErrorResponse("Дозволені тільки файли зображень (.jpg, .jpeg, .png). Файл '" +
                                    file.getOriginalFilename() + "' має тип: " + contentType));
                }
            }

            System.out.println("Валідація пройшла успішно. Починаємо обробку та завантаження...");

            // Виконуємо послідовність: обробка зображень -> авторизація -> завантаження
            UploadResponse response = konexService.uploadPhotosWithProcessing(files, path, inputKey);

            System.out.println("=== Завантаження з обробкою завершено успішно ===");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.err.println("Помилка валідації: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(createErrorResponse(e.getMessage()));
        } catch (IOException e) {
            System.err.println("Помилка вводу/виводу: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Помилка завантаження: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("Загальна помилка: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Внутрішня помилка сервера: " + e.getMessage()));
        }
    }
}