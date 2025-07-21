package com.example.konexapi.service;


import com.example.konexapi.dto.AuthRequest;
import com.example.konexapi.dto.AuthResponse;
import com.example.konexapi.dto.UploadResponse;
import com.example.konexapi.entity.UploadedFile;
import com.example.konexapi.repository.UploadedFileRepository;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class KonexService {

    @Value("${konex.auth.url}")
    private String authUrl;

    @Value("${konex.auth.login}")
    private String login;

    @Value("${konex.auth.password}")
    private String password;

    @Value("${konex.auth.user-type}")
    private String userType;

    @Value("${konex.cdn.upload-url}")
    private String cdnUploadUrl;

    @Autowired
    private UploadedFileRepository fileRepository;
    @Autowired
    private ImageProcessingService imageProcessingService;

    private String authToken;
    private LocalDateTime tokenExpiry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String getAuthToken() throws IOException {
        if (authToken != null && tokenExpiry != null && LocalDateTime.now().isBefore(tokenExpiry)) {
            return authToken;
        }

        return refreshAuthToken();
    }

    private String refreshAuthToken() throws IOException {
        System.out.println("Отримання токену авторизації з https://auth.konex.com.ua/login");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(authUrl);
            post.setHeader("Content-Type", "application/json");
            post.setHeader("Accept", "application/json");

            
            AuthRequest authRequest = new AuthRequest(login, password, userType);
            String jsonBody = objectMapper.writeValueAsString(authRequest);

            System.out.println("Надсилаємо запит авторизації: " + jsonBody);
            post.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("Статус відповіді авторизації: " + statusCode);
                System.out.println("Відповідь сервера: " + responseBody);

                if (statusCode != 200) {
                    throw new RuntimeException("Помилка авторизації на https://auth.konex.com.ua/login: " +
                            statusCode + " - " + responseBody);
                }

                
                AuthResponse authResponse;
                try {
                    authResponse = objectMapper.readValue(responseBody, AuthResponse.class);
                } catch (Exception e) {
                    System.err.println("Помилка парсингу відповіді: " + e.getMessage());
                    System.err.println("Відповідь сервера: " + responseBody);
                    throw new RuntimeException("Не вдалося розпарсити відповідь від сервера авторизації: " + e.getMessage());
                }

                authToken = authResponse.getActualToken();

                if (authToken == null || authToken.trim().isEmpty()) {
                    System.err.println("Деталі відповіді:");
                    System.err.println("  token: " + authResponse.getToken());
                    System.err.println("  jwt: " + authResponse.getJwt());
                    System.err.println("  access_token: " + authResponse.getAccess_token());
                    System.err.println("  status: " + authResponse.getStatus());
                    System.err.println("  success: " + authResponse.isSuccess());
                    System.err.println("  message: " + authResponse.getMessage());
                    throw new RuntimeException("Токен не отриманий від сервера авторизації. Повна відповідь: " + responseBody);
                }

                System.out.println("Токен авторизації успішно отриманий: " + authToken.substring(0, Math.min(20, authToken.length())) + "...");

               
                tokenExpiry = authResponse.getExpiryDateTime();
                System.out.println("Токен дійсний до: " + tokenExpiry);

                return authToken;
            }
        }
    }
    public UploadResponse uploadFiles(MultipartFile[] files, String path, String inputKey) throws IOException {
        System.out.println("Початок завантаження файлів на CDN сервер");

        
        String token = getAuthToken();
        System.out.println("Використовуємо токен для завантаження: " + token.substring(0, Math.min(20, token.length())) + "...");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(cdnUploadUrl);

            
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();

            
            builder.addTextBody("token", token);

            
            if (path != null && !path.trim().isEmpty()) {
                builder.addTextBody("path", path);
                System.out.println("Завантажуємо у директорію: " + path);
            }

            
            String inputKeyName = inputKey != null && !inputKey.trim().isEmpty() ? inputKey : "file";
            builder.addTextBody("input", inputKeyName);
            System.out.println("Використовуємо input key: " + inputKeyName);

           
            System.out.println("Додаємо " + files.length + " файл(ів) до запиту:");
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                if (!file.isEmpty()) {
                    System.out.println("  - " + file.getOriginalFilename() + " (" + file.getSize() + " bytes)");

                    
                    String fieldName = i == 0 ? inputKeyName : inputKeyName + i;

                    builder.addBinaryBody(
                            fieldName,
                            file.getInputStream(),
                            ContentType.parse(file.getContentType()),
                            file.getOriginalFilename()
                    );
                }
            }

            post.setEntity(builder.build());

            System.out.println("Надсилаємо запит на " + cdnUploadUrl);

            try (CloseableHttpResponse response = client.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("Статус відповіді CDN: " + statusCode);
                System.out.println("Відповідь CDN сервера: " + responseBody);

                if (statusCode != 200) {
                    throw new RuntimeException("Помилка завантаження на CDN сервер: " +
                            statusCode + " - " + responseBody);
                }

                UploadResponse uploadResponse = objectMapper.readValue(responseBody, UploadResponse.class);

                
                saveUploadedFiles(files, uploadResponse, path);

                System.out.println("Файли успішно завантажені на CDN!");
                if (uploadResponse.getFiles() != null) {
                    uploadResponse.getFiles().forEach(file ->
                            System.out.println("  - " + file.getFilename() + " -> " + file.getUrl())
                    );
                }

                return uploadResponse;
            }
        }
    }
    public boolean deletePhoto(Long id) {
        try {
            Optional<UploadedFile> photo = fileRepository.findById(id);
            if (photo.isPresent()) {
                fileRepository.deleteById(id);
                System.out.println("Фото з ID " + id + " видалено з бази даних");
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Помилка видалення фото: " + e.getMessage());
            return false;
        }
    }

    public List<UploadedFile> getPhotosByIds(List<Long> ids) {
        return fileRepository.findAllById(ids);
    }

    
    public List<UploadedFile> getPhotosWithPagination(int page, int size) {
        
        List<UploadedFile> allPhotos = fileRepository.findAll();
        int start = page * size;
        int end = Math.min(start + size, allPhotos.size());

        if (start >= allPhotos.size()) {
            return new ArrayList<>();
        }

        return allPhotos.subList(start, end);
    }
    public UploadResponse uploadFiles(MultipartFile[] files, String path) throws IOException {
        return uploadFiles(files, path, "file");
    }
    public UploadResponse uploadPhotosWithProcessing(MultipartFile[] files, String path, String inputKey) throws IOException {
        System.out.println("Початок завантаження фото з обробкою зображень");

       
        MultipartFile[] processedFiles = new MultipartFile[files.length];
        for (int i = 0; i < files.length; i++) {
            System.out.println("Обробка файлу " + (i + 1) + "/" + files.length + ": " + files[i].getOriginalFilename());
            processedFiles[i] = imageProcessingService.processImage(files[i]);
        }

        
        return uploadFiles(processedFiles, path, inputKey);
    }

    
    public UploadResponse uploadPhotosWithProcessing(MultipartFile[] files, String path) throws IOException {
        return uploadPhotosWithProcessing(files, path, "file");
    }
    private void saveUploadedFiles(MultipartFile[] files, UploadResponse response, String path) {
        if (response.getFiles() != null) {
            List<UploadedFile> uploadedFiles = new ArrayList<>();

            for (int i = 0; i < response.getFiles().size() && i < files.length; i++) {
                UploadResponse.FileInfo fileInfo = response.getFiles().get(i);
                MultipartFile originalFile = files[i];

                UploadedFile uploadedFile = new UploadedFile(
                        originalFile.getOriginalFilename(),
                        fileInfo.getUrl(),
                        path,
                        originalFile.getSize(),
                        originalFile.getContentType()
                );

                uploadedFiles.add(uploadedFile);
            }

            fileRepository.saveAll(uploadedFiles);
        }
    }

    public List<UploadedFile> getUploadHistory() {
        return fileRepository.findAll();
    }

    public List<UploadedFile> getFilesByPath(String path) {
        return fileRepository.findByPathContaining(path);
    }
}
