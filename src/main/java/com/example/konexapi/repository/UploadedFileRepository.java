package com.example.konexapi.repository;

import com.example.konexapi.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
    List<UploadedFile> findByPathContaining(String path);
}
