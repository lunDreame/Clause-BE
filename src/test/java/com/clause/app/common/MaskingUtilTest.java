package com.clause.app.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MaskingUtilTest {

    @Autowired
    private MaskingUtil maskingUtil;

    @Test
    void testSsnMasking() {
        String text = "주민번호는 123456-1234567입니다.";
        String masked = maskingUtil.maskForLlm(text);
        assertThat(masked).contains("******-*******");
        assertThat(masked).doesNotContain("123456-1234567");
    }

    @Test
    void testPhoneMasking() {
        String text = "연락처는 010-1234-5678입니다.";
        String masked = maskingUtil.maskForLlm(text);
        assertThat(masked).contains("010-****-5678");
    }

    @Test
    void testEmailMasking() {
        String text = "이메일은 test@example.com입니다.";
        String masked = maskingUtil.maskForLlm(text);
        assertThat(masked).contains("***@***.***");
        assertThat(masked).doesNotContain("test@example.com");
    }

    @Test
    void testAccountMasking() {
        String text = "계좌번호는 123-456-789012입니다.";
        String masked = maskingUtil.maskForLlm(text);
        assertThat(masked).contains("***-***-******");
    }
}

