package com.boltpe.insurance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

class InsurerLogoS3UploaderTest {
    private static final String KEY_PREFIX = "publictpap/insurancelogo/";

    private final S3Client s3Client = mock(S3Client.class);
    private final InsurerLogoService logoService =
            new InsurerLogoService("https://s3.example.test", "test-bucket", "publictpap", "insurancelogo");
    private final InsurerLogoS3Uploader uploader = new InsurerLogoS3Uploader(s3Client, logoService);

    @Test void shouldUploadEveryLogoWhenFolderIsEmpty() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        assertThat(uploader.uploadMissingLogos()).isEqualTo(15);

        ArgumentCaptor<PutObjectRequest> requests = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodies = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client, times(15)).putObject(requests.capture(), bodies.capture());
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.bucket()).isEqualTo("test-bucket");
            assertThat(request.key()).startsWith(KEY_PREFIX);
            assertThat(request.cacheControl()).isEqualTo("public, max-age=604800");
        });
        assertThat(requests.getAllValues()).extracting(PutObjectRequest::key)
                .containsExactlyInAnyOrderElementsOf(logoService.logoFiles().stream().map(f -> KEY_PREFIX + f).toList());
        assertThat(bodies.getAllValues()).allSatisfy(body -> assertThat(body.optionalContentLength()).hasValueSatisfying(
                length -> assertThat(length).isPositive()));
    }

    @Test void shouldSendContentTypeMatchingTheFile() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        uploader.uploadMissingLogos();

        verify(s3Client).putObject(argThat((PutObjectRequest r) -> r.key().equals(KEY_PREFIX + "bajaj_1018.svg")
                && r.contentType().equals("image/svg+xml")), any(RequestBody.class));
        verify(s3Client).putObject(argThat((PutObjectRequest r) -> r.key().equals(KEY_PREFIX + "nia_1025.webp")
                && r.contentType().equals("image/webp")), any(RequestBody.class));
    }

    @Test void shouldSkipLogosAlreadyInS3() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        assertThat(uploader.uploadMissingLogos()).isZero();

        verify(s3Client, times(15)).headObject(argThat((HeadObjectRequest r) -> r.bucket().equals("test-bucket")));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test void shouldTreatS3NotFoundStatusAsMissing() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

        assertThat(uploader.uploadMissingLogos()).isEqualTo(15);
    }

    @Test void shouldLogAndContinueWhenHeadIsDenied() {
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Access Denied").build());

        assertThat(uploader.uploadMissingLogos()).isZero();

        verify(s3Client, times(15)).headObject(any(HeadObjectRequest.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test void shouldKeepUploadingWhenOneUploadFails() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());
        when(s3Client.putObject(argThat((PutObjectRequest r) -> r != null && r.key().endsWith("bajaj_1018.svg")),
                any(RequestBody.class))).thenThrow(SdkClientException.create("connection reset"));

        assertThat(uploader.uploadMissingLogos()).isEqualTo(14);
        verify(s3Client, times(15)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test void shouldLogAndSkipWhenBundledFileIsMissing() {
        InsurerLogoService missingFileService = mock(InsurerLogoService.class);
        when(missingFileService.logoFiles()).thenReturn(List.of("missing_9999.svg"));
        when(missingFileService.objectKey("missing_9999.svg")).thenReturn(KEY_PREFIX + "missing_9999.svg");
        when(missingFileService.bucketName()).thenReturn("test-bucket");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertThat(new InsurerLogoS3Uploader(s3Client, missingFileService).uploadMissingLogos()).isZero();
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test void shouldUploadOnApplicationStartup() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().build());

        uploader.run(null);

        verify(s3Client, times(15)).headObject(any(HeadObjectRequest.class));
    }

    @ParameterizedTest
    @CsvSource({
            "a.svg, image/svg+xml",
            "a.SVG, image/svg+xml",
            "a.png, image/png",
            "a.jpg, image/jpeg",
            "a.webp, image/webp",
            "a.gif, application/octet-stream",
            "noextension, application/octet-stream"})
    void shouldResolveContentType(String fileName, String expected) {
        assertThat(InsurerLogoS3Uploader.contentType(fileName)).isEqualTo(expected);
    }
}
