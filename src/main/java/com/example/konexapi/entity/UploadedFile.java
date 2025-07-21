package com.example.konexapi.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String cdnUrl;

    @Column
    private String path;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private Long fileSize;

    @Column
    private String contentType;

    
    public UploadedFile() {}

    public UploadedFile(String originalFilename, String cdnUrl, String path, Long fileSize, String contentType) {
        this.originalFilename = originalFilename;
        this.cdnUrl = cdnUrl;
        this.path = path;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedAt = LocalDateTime.now();
    }

    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getCdnUrl() { return cdnUrl; }
    public void setCdnUrl(String cdnUrl) { this.cdnUrl = cdnUrl; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}

