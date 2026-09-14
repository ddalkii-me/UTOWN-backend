package com.utown.utownbackend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneUtilTest {

    @Test
    @DisplayName("normalizePhone - removes formatting and adds default country code if needed")
    void normalizePhone_removesFormatting() {
        assertThat(PhoneUtil.normalizePhone("010-1234-5678")).isEqualTo("+821012345678");
        assertThat(PhoneUtil.normalizePhone("123-456-7890")).isEqualTo("+821234567890"); 
        assertThat(PhoneUtil.normalizePhone("(010) 1234-5678")).isEqualTo("+821012345678");
    }

    @Test
    @DisplayName("normalizePhone - preserves leading plus for international format")
    void normalizePhone_preservesLeadingPlus() {
        assertThat(PhoneUtil.normalizePhone("+82-10-1234-5678")).isEqualTo("+821012345678");
        assertThat(PhoneUtil.normalizePhone("+82 10 1234 5678")).isEqualTo("+821012345678");
    }

    @Test
    @DisplayName("normalizePhone - cleans up embedded plus signs and formats international number")
    void normalizePhone_cleansEmbeddedPluses() {
        assertThat(PhoneUtil.normalizePhone("+82+10+1234+5678")).isEqualTo("+821012345678");
    }

    @Test
    @DisplayName("normalizePhone - throws IllegalArgumentException for inputs with fewer than 7 digits")
    void normalizePhone_fewerThanSevenDigits_throwsException() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> PhoneUtil.normalizePhone("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number does not contain enough digits");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> PhoneUtil.normalizePhone("abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number does not contain enough digits");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> PhoneUtil.normalizePhone("+"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number does not contain enough digits");
    }

    @Test
    @DisplayName("normalizePhone - returns null for null or blank string")
    void normalizePhone_nullOrBlank() {
        assertThat(PhoneUtil.normalizePhone(null)).isNull();
        assertThat(PhoneUtil.normalizePhone("")).isNull();
        assertThat(PhoneUtil.normalizePhone("   ")).isNull();
    }
}
