package com.clause.app.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class MaskingUtil {

    private static final Pattern SSN = Pattern.compile("\\b\\d{6}-?\\d{7}\\b");
    private static final Pattern PHONE = Pattern.compile("\\b01[016789]-?\\d{3,4}-?\\d{4}\\b");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern ACCOUNT = Pattern.compile("\\b\\d{2,4}-\\d{2,4}-\\d{2,6}\\b|(?:국민|신한|우리|하나|농협|기업|카카오|토스|KB|신한|우리|하나|NH|IBK)\\s*\\d{8,14}\\b");
    private static final Pattern ADDRESS = Pattern.compile("(?:서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주).{0,30}(?:구|군|시|읍|면|동|로|길)|아파트\\s*\\d+\\s*동\\s*\\d+\\s*호");

    public String maskForLlm(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;

        masked = SSN.matcher(masked).replaceAll("******-*******");
        masked = PHONE.matcher(masked).replaceAll(matchResult -> {
            String phone = matchResult.group();
            String cleaned = phone.replaceAll("-", "");
            if (cleaned.length() == 11) {
                return cleaned.substring(0, 3) + "-****-" + cleaned.substring(7);
            }
            return "010-****-****";
        });
        masked = EMAIL.matcher(masked).replaceAll("***@***.***");
        masked = ACCOUNT.matcher(masked).replaceAll("***-***-******");
        masked = ADDRESS.matcher(masked).replaceAll("***");

        return masked;
    }
}

