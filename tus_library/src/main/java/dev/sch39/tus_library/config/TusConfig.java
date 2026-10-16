package dev.sch39.tus_library.config;

import me.desair.tus.server.TusFileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TusConfig {

    @Bean
    public TusFileUploadService tusFileUploadService(
            @Value("${tus.upload-uri}") String uploadUri,
            @Value("${tus.storage-path}") String storagePath,
            @Value("${tus.max-upload-size}") Long maxUploadSize,
            @Value("${tus.expiration-period}") Long expirationPeriod) {

        return new TusFileUploadService()
                .withUploadUri(uploadUri)
                .withStoragePath(storagePath)
                .withMaxUploadSize(maxUploadSize)
                .withUploadExpirationPeriod(expirationPeriod)
                .withThreadLocalCache(true)
                .withUploadDeduplication(true);
    }
}
