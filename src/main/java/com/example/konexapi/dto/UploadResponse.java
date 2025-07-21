package com.example.konexapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadResponse {
    private int status;
    private long timestamp;
    private List<FileInfo> files;


    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public List<FileInfo> getFiles() { return files; }
    public void setFiles(List<FileInfo> files) { this.files = files; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileInfo {
        private String filename;
        private String url;

        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        @Override
        public String toString() {
            return "FileInfo{" +
                    "filename='" + filename + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "UploadResponse{" +
                "status=" + status +
                ", timestamp=" + timestamp +
                ", files=" + files +
                '}';
    }
}
