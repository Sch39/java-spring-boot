package dev.sch39.tus_manual.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TusUploadService {

    @Value("${tus.upload-dir}")
    private String uploadDir;

    private final Map<String, UploadInfo> uploads = new ConcurrentHashMap<>();

    public String createUpload(long length, Map<String, String> metadata) throws IOException {
        String id = UUID.randomUUID().toString();
        Path filePath = Paths.get(uploadDir, id);
        Files.createDirectories(filePath.getParent());
        Files.createFile(filePath);
        UploadInfo info = new UploadInfo(id, filePath, length, metadata);
        uploads.put(id, info);
        return id;
    }

    public long getOffset(String id) {
        UploadInfo info = uploads.get(id);
        if (info == null)
            throw new IllegalArgumentException("Upload not found");
        return info.offset;
    }

    public long appendData(String id, byte[] data, long offset) throws IOException {
        UploadInfo info = uploads.get(id);
        if (info == null)
            throw new IllegalArgumentException("Upload not found");
        if (offset != info.offset)
            throw new IllegalStateException("Offset mismatch");

        try (RandomAccessFile raf = new RandomAccessFile(info.path.toFile(), "rw")) {
            raf.seek(offset);
            raf.write(data);
        }
        info.offset += data.length;
        return info.offset;
    }

    public UploadInfo getUploadInfo(String id) {
        return uploads.get(id);
    }

    public static class UploadInfo {
        public final String id;
        public final Path path;
        public final long length;
        public final Map<String, String> metadata;
        public long offset = 0;

        public UploadInfo(String id, Path path, long length, Map<String, String> metadata) {
            this.id = id;
            this.path = path;
            this.length = length;
            this.metadata = metadata;
        }
    }
}
