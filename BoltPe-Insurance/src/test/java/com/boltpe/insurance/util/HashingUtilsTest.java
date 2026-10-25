package com.boltpe.insurance.util;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
class HashingUtilsTest {
    @Test void shouldReturnStableSha256() {
        assertThat(HashingUtils.sha256("abc")).isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
    @Test void shouldHashEmptyStringToKnownDigest() {
        assertThat(HashingUtils.sha256("")).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
    @Test void shouldWrapMissingAlgorithmInIllegalStateException() {
        NoSuchAlgorithmException cause = new NoSuchAlgorithmException("SHA-256 not installed");
        try (var digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(cause);
            assertThatThrownBy(() -> HashingUtils.sha256("abc"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("SHA-256 algorithm is not available")
                    .hasCause(cause);
        }
    }
}
