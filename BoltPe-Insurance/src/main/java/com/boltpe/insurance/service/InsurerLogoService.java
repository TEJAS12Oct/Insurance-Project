package com.boltpe.insurance.service;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Resolves insurer logo URLs. Policy Planner only returns a bare file name (for example
 * "bajaj.svg"), so plan responses use the BoltPe logo stored in the public S3 folder for
 * every known Policy Planner companyId:
 * {endpoint}/{bucket}/{publicFolder}/{logoFolder}/{file}
 */
@Service
public class InsurerLogoService {

    // Policy Planner companyId -> logo file in the S3 logo folder.
    private static final Map<String, String> LOGO_FILES = Map.ofEntries(
            Map.entry("1002", "care_1002.png"),
            Map.entry("1003", "star_1003.svg"),
            Map.entry("1004", "sbi_1004.svg"),
            Map.entry("1005", "nic_1005.svg"),
            Map.entry("1008", "hdfc_1008.svg"),
            Map.entry("1009", "icici_1009.svg"),
            Map.entry("1011", "tata_1011.svg"),
            Map.entry("1017", "generali_1017.jpg"),
            Map.entry("1018", "bajaj_1018.svg"),
            Map.entry("1019", "reliance_1019.png"),
            Map.entry("1021", "united_1021.svg"),
            Map.entry("1022", "iffco_1022.png"),
            Map.entry("1023", "digit_1023.svg"),
            Map.entry("1024", "niva_1024.svg"),
            Map.entry("1025", "nia_1025.webp"));

    private final String bucketName;
    private final String objectKeyPrefix;
    private final String logoUrlPrefix;

    public InsurerLogoService(@Value("${s3.endpoint}") String endpoint,
                              @Value("${s3.bucket-name}") String bucketName,
                              @Value("${s3.public-folder}") String publicFolder,
                              @Value("${insurance.logo.folder}") String logoFolder) {
        this.bucketName = trimSlashes(required("s3.bucket-name", bucketName));
        this.objectKeyPrefix = trimSlashes(required("s3.public-folder", publicFolder)) + "/"
                + trimSlashes(required("insurance.logo.folder", logoFolder)) + "/";
        this.logoUrlPrefix = trimSlashes(required("s3.endpoint", endpoint)) + "/"
                + this.bucketName + "/" + objectKeyPrefix;
    }

    /** Logo files that must exist in the S3 logo folder. */
    public Collection<String> logoFiles() {
        return LOGO_FILES.values();
    }

    public String bucketName() {
        return bucketName;
    }

    /** S3 object key for a logo file, for example "publictpap/insurancelogo/bajaj_1018.svg". */
    public String objectKey(String fileName) {
        return objectKeyPrefix + fileName;
    }

    /**
     * Returns the S3 logo URL for a known companyId. For unknown insurers the vendor
     * value is kept only when it is already an absolute http(s) URL; otherwise null.
     */
    public String resolveLogoUrl(String companyId, String vendorLogo) {
        String file = companyId == null ? null : LOGO_FILES.get(companyId.trim());
        if (file != null) return logoUrlPrefix + file;
        if (vendorLogo == null) return null;
        String trimmed = vendorLogo.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://") ? trimmed : null;
    }

    private static String required(String name, String value) {
        if (value == null || trimSlashes(value).isEmpty()) {
            throw new IllegalArgumentException(name + " must be configured");
        }
        return value;
    }

    private static String trimSlashes(String value) {
        String result = value.trim();
        int start = 0;
        int end = result.length();
        while (start < end && result.charAt(start) == '/') start++;
        while (end > start && result.charAt(end - 1) == '/') end--;
        return result.substring(start, end);
    }
}
