package com.boltpe.insurance.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Uploads the bundled insurer logos to the S3 logo folder on startup. Only logos missing
 * from S3 are uploaded; the first upload creates the folder. Failures are logged and never
 * stop the service, because plan quotes still work without logos.
 */
@Component
public class InsurerLogoS3Uploader implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(InsurerLogoS3Uploader.class);
    private static final String CLASSPATH_DIR = "insurer-logos/";
    private static final String CACHE_CONTROL = "public, max-age=604800";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "svg", "image/svg+xml",
            "png", "image/png",
            "jpg", "image/jpeg",
            "webp", "image/webp");

    private final S3Client s3Client;
    private final InsurerLogoService logoService;

    public InsurerLogoS3Uploader(S3Client s3Client, InsurerLogoService logoService) {
        this.s3Client = s3Client;
        this.logoService = logoService;
    }

    @Override
    public void run(ApplicationArguments args) {
        uploadMissingLogos();
    }

    /** Returns the number of logos uploaded. */
    int uploadMissingLogos() {
        int uploaded = 0;
        for (String fileName : logoService.logoFiles()) {
            String key = logoService.objectKey(fileName);
            try {
                if (!exists(key)) {
                    upload(fileName, key);
                    uploaded++;
                }
            } catch (SdkException | IOException ex) {
                LOGGER.warn("Insurer logo upload to S3 failed: key={}, exceptionType={}, message={}",
                        key, ex.getClass().getSimpleName(), ex.getMessage());
            }
        }
        LOGGER.info("Insurer logo S3 sync finished: uploaded={}", uploaded);
        return uploaded;
    }

    private boolean exists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(logoService.bucketName())
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException ex) {
            return false;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) return false;
            throw ex;
        }
    }

    private void upload(String fileName, String key) throws IOException {
        byte[] content;
        try (InputStream in = new ClassPathResource(CLASSPATH_DIR + fileName).getInputStream()) {
            content = in.readAllBytes();
        }
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(logoService.bucketName())
                        .key(key)
                        .contentType(contentType(fileName))
                        .cacheControl(CACHE_CONTROL)
                        .build(),
                RequestBody.fromBytes(content));
    }

    static String contentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.getOrDefault(extension, DEFAULT_CONTENT_TYPE);
    }
}
