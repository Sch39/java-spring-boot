package dev.sch39.tus_manual.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import dev.sch39.tus_manual.service.TusUploadService;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class TusUploadController {

    private final TusUploadService service;

    public TusUploadController(TusUploadService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> createUpload(
            @RequestHeader("Upload-Length") long length,
            @RequestHeader(value = "Upload-Metadata", required = false) String metadataHeader) throws IOException {

        Map<String, String> metadata = parseMetadata(metadataHeader);
        String id = service.createUpload(length, metadata);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", "/files/" + id);
        headers.add("Tus-Resumable", "1.0.0");
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headUpload(@PathVariable String id) {
        var info = service.getUploadInfo(id);

        if (info == null) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Upload-Offset", String.valueOf(info.offset));
        headers.set("Upload-Length", String.valueOf(info.length));
        headers.set("Tus-Resumable", "1.0.0");
        headers.set("Cache-Control", "no-store");

        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    @PatchMapping(value = "/{id}", consumes = "application/offset+octet-stream")
    public ResponseEntity<Void> patchUpload(
            @PathVariable String id,
            @RequestHeader("Upload-Offset") long offset,
            @RequestBody byte[] data) throws IOException {

        long newOffset = service.appendData(id, data, offset);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Upload-Offset", String.valueOf(newOffset));
        headers.add("Tus-Resumable", "1.0.0");
        return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
    }

    private Map<String, String> parseMetadata(String header) {
        Map<String, String> metadata = new HashMap<>();
        if (header == null || header.isBlank())
            return metadata;
        for (String pair : header.split(",")) {

            String[] parts = pair.trim().split(" ", 2);
            String key = parts[0];
            String value = parts.length > 1 ? new String(Base64.getDecoder().decode(parts[1])) : "";
            metadata.put(key, value);
        }
        return metadata;
    }
}
