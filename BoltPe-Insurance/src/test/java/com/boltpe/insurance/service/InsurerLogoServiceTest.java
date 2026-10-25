package com.boltpe.insurance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InsurerLogoServiceTest {
    private static final String ENDPOINT = "https://s3.example.test";
    private static final String BUCKET = "test-bucket";
    private static final String PUBLIC_FOLDER = "publictpap";
    private static final String LOGO_FOLDER = "insurancelogo";
    private static final String LOGO_PREFIX = "https://s3.example.test/test-bucket/publictpap/insurancelogo/";

    private static final Map<String, String> EXPECTED_FILES = Map.ofEntries(
            Map.entry("1002", "care_1002.png"), Map.entry("1003", "star_1003.svg"),
            Map.entry("1004", "sbi_1004.svg"), Map.entry("1005", "nic_1005.svg"),
            Map.entry("1008", "hdfc_1008.svg"), Map.entry("1009", "icici_1009.svg"),
            Map.entry("1011", "tata_1011.svg"), Map.entry("1017", "generali_1017.jpg"),
            Map.entry("1018", "bajaj_1018.svg"), Map.entry("1019", "reliance_1019.png"),
            Map.entry("1021", "united_1021.svg"), Map.entry("1022", "iffco_1022.png"),
            Map.entry("1023", "digit_1023.svg"), Map.entry("1024", "niva_1024.svg"),
            Map.entry("1025", "nia_1025.webp"));

    private final InsurerLogoService service = new InsurerLogoService(ENDPOINT, BUCKET, PUBLIC_FOLDER, LOGO_FOLDER);

    @Test void shouldResolveS3UrlForEveryKnownInsurer() {
        EXPECTED_FILES.forEach((companyId, file) ->
                assertThat(service.resolveLogoUrl(companyId, "vendor.png")).isEqualTo(LOGO_PREFIX + file));
    }

    @Test void shouldExposeLogoFilesBucketAndObjectKeys() {
        assertThat(service.logoFiles()).containsExactlyInAnyOrderElementsOf(EXPECTED_FILES.values());
        assertThat(service.bucketName()).isEqualTo(BUCKET);
        assertThat(service.objectKey("bajaj_1018.svg")).isEqualTo("publictpap/insurancelogo/bajaj_1018.svg");
    }

    @Test void shouldBuildObjectKeyWithoutExtraSlashes() {
        var slashedService = new InsurerLogoService(ENDPOINT, "/test-bucket/", "/publictpap/", "/insurancelogo/");
        assertThat(slashedService.bucketName()).isEqualTo(BUCKET);
        assertThat(slashedService.objectKey("star_1003.svg")).isEqualTo("publictpap/insurancelogo/star_1003.svg");
    }

    @Test void shouldTrimCompanyIdBeforeLookup() {
        assertThat(service.resolveLogoUrl(" 1018 ", null)).isEqualTo(LOGO_PREFIX + "bajaj_1018.svg");
    }

    @Test void shouldNormalizeSlashesAndSpacesInEveryPart() {
        var slashedService = new InsurerLogoService(" https://s3.example.test// ", "/test-bucket/",
                " /publictpap/ ", "//insurancelogo//");
        assertThat(slashedService.resolveLogoUrl("1003", null)).isEqualTo(LOGO_PREFIX + "star_1003.svg");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", " // "})
    void shouldRejectMissingEndpoint(String endpoint) {
        assertThatThrownBy(() -> new InsurerLogoService(endpoint, BUCKET, PUBLIC_FOLDER, LOGO_FOLDER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("s3.endpoint must be configured");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectMissingBucketName(String bucketName) {
        assertThatThrownBy(() -> new InsurerLogoService(ENDPOINT, bucketName, PUBLIC_FOLDER, LOGO_FOLDER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("s3.bucket-name must be configured");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectMissingPublicFolder(String publicFolder) {
        assertThatThrownBy(() -> new InsurerLogoService(ENDPOINT, BUCKET, publicFolder, LOGO_FOLDER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("s3.public-folder must be configured");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectMissingLogoFolder(String logoFolder) {
        assertThatThrownBy(() -> new InsurerLogoService(ENDPOINT, BUCKET, PUBLIC_FOLDER, logoFolder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("insurance.logo.folder must be configured");
    }

    @Test void shouldKeepAbsoluteVendorUrlForUnknownInsurer() {
        assertThat(service.resolveLogoUrl("9999", " https://cdn.example.test/logo.png "))
                .isEqualTo("https://cdn.example.test/logo.png");
        assertThat(service.resolveLogoUrl(null, "HTTP://cdn.example.test/logo.png"))
                .isEqualTo("HTTP://cdn.example.test/logo.png");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bajaj.svg", "  ", "ftp://cdn.example.test/logo.png", "/assets/logo.png"})
    void shouldReturnNullForUnknownInsurerWithoutAbsoluteVendorUrl(String vendorLogo) {
        assertThat(service.resolveLogoUrl("9999", vendorLogo)).isNull();
    }

    @Test void shouldReturnNullWhenNothingIsKnown() {
        assertThat(service.resolveLogoUrl(null, null)).isNull();
    }
}
