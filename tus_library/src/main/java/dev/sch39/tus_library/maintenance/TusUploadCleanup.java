package dev.sch39.tus_library.maintenance;

import me.desair.tus.server.TusFileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TusUploadCleanup {

    private static final Logger log = LoggerFactory.getLogger(TusUploadCleanup.class);

    private final TusFileUploadService tusFileUploadService;

    public TusUploadCleanup(TusFileUploadService tusFileUploadService) {
        this.tusFileUploadService = tusFileUploadService;
    }

    @Scheduled(fixedDelayString = "${tus.cleanup-interval:3600000}")
    public void cleanupExpiredUploads() {
        try {
            tusFileUploadService.cleanup();
        } catch (IOException exception) {
            log.error("Gagal membersihkan upload Tus yang kedaluwarsa", exception);
        }
    }
}
