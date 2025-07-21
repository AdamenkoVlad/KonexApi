package com.example.konexapi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Service
public class ImageProcessingService {

    private static final int TARGET_SIZE = 512;
    private static final long MAX_SIZE_BYTES = 512 * 1024; // 0.5 MB
    private static final float COMPRESSION_QUALITY = 0.8f;

    public MultipartFile processImage(MultipartFile file) throws IOException {
        System.out.println("Обробка зображення: " + file.getOriginalFilename());

        // Перевірка формату файлу
        String contentType = file.getContentType();
        if (!isValidImageFormat(contentType)) {
            throw new IllegalArgumentException("Непідтримуваний формат файлу: " + contentType +
                    ". Підтримуються тільки: .jpg, .jpeg, .png");
        }

        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        if (originalImage == null) {
            throw new IOException("Не вдалося прочитати зображення");
        }

        // Обрізати зображення до 512x512 (center/center)
        BufferedImage croppedImage = cropImageCenterCenter(originalImage, TARGET_SIZE, TARGET_SIZE);

        // Конвертувати в JPG та стиснути при необхідності
        byte[] processedImageBytes = convertToJpgAndCompress(croppedImage);

        System.out.println("Розмір після обробки: " + processedImageBytes.length + " bytes");

        return new ProcessedMultipartFile(processedImageBytes, getProcessedFileName(file.getOriginalFilename()));
    }

    private boolean isValidImageFormat(String contentType) {
        return contentType != null &&
                (contentType.equals("image/jpeg") ||
                        contentType.equals("image/jpg") ||
                        contentType.equals("image/png"));
    }

    private BufferedImage cropImageCenterCenter(BufferedImage originalImage, int targetWidth, int targetHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // Визначаємо розміри для обрізки (квадрат по меншій стороні)
        int cropSize = Math.min(originalWidth, originalHeight);

        // Координати для center/center обрізки
        int x = (originalWidth - cropSize) / 2;
        int y = (originalHeight - cropSize) / 2;

        // Обрізаємо зображення
        BufferedImage croppedImage = originalImage.getSubimage(x, y, cropSize, cropSize);

        // Масштабуємо до цільового розміру
        BufferedImage scaledImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();

        // Покращена якість масштабування
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(croppedImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return scaledImage;
    }

    private byte[] convertToJpgAndCompress(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Отримуємо writer для JPEG
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        ImageWriter writer = writers.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        // Налаштовуємо стиснення
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        // Спочатку пробуємо з базовою якістю
        param.setCompressionQuality(COMPRESSION_QUALITY);
        writer.write(null, new javax.imageio.IIOImage(image, null, null), param);

        byte[] imageBytes = baos.toByteArray();

        // Якщо файл більший за 0.5 MB, стискаємо сильніше
        if (imageBytes.length > MAX_SIZE_BYTES) {
            System.out.println("Файл перевищує 0.5 MB (" + imageBytes.length + " bytes), додаткове стиснення...");

            baos.reset();
            param.setCompressionQuality(0.6f); // Зменшуємо якість
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
            imageBytes = baos.toByteArray();

            // Якщо все ще великий, стискаємо ще більше
            if (imageBytes.length > MAX_SIZE_BYTES) {
                baos.reset();
                param.setCompressionQuality(0.4f);
                writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
                imageBytes = baos.toByteArray();
            }
        }

        writer.dispose();
        ios.close();
        baos.close();

        return imageBytes;
    }

    private String getProcessedFileName(String originalFilename) {
        if (originalFilename == null) {
            return "processed_image.jpg";
        }

        // Замінюємо розширення на .jpg
        String nameWithoutExtension = originalFilename.replaceAll("\\.[^.]*$", "");
        return nameWithoutExtension + ".jpg";
    }

    // Клас для створення нового MultipartFile з обробленими даними
    private static class ProcessedMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String filename;

        public ProcessedMultipartFile(byte[] content, String filename) {
            this.content = content;
            this.filename = filename;
        }

        @Override
        public String getName() { return "file"; }

        @Override
        public String getOriginalFilename() { return filename; }

        @Override
        public String getContentType() { return "image/jpeg"; }

        @Override
        public boolean isEmpty() { return content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() { return content; }

        @Override
        public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            throw new UnsupportedOperationException("transferTo not supported");
        }
    }
}