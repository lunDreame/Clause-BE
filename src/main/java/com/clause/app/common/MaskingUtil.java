package com.clause.app.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class MaskingUtil {

    // 주민번호: 6자리-7자리
    private static final Pattern SSN = Pattern.compile("\\b\\d{6}-?\\d{7}\\b");
    
    // 전화번호: 010-1234-5678 형태
    private static final Pattern PHONE = Pattern.compile("\\b01[016789]-?\\d{3,4}-?\\d{4}\\b");
    
    // 이메일
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    
    // 계좌번호: 123-456-789012 형태 또는 은행명 + 숫자
    private static final Pattern ACCOUNT = Pattern.compile("\\b\\d{2,4}-\\d{2,4}-\\d{2,6}\\b|(?:국민|신한|우리|하나|농협|기업|카카오|토스|KB|신한|우리|하나|NH|IBK)\\s*\\d{8,14}\\b");
    
    // 주소 패턴
    private static final Pattern ADDRESS = Pattern.compile("(?:서울|부산|대구|인천|광주|대전|울산|세종|경기|강원|충북|충남|전북|전남|경북|경남|제주).{0,30}(?:구|군|시|읍|면|동|로|길)|아파트\\s*\\d+\\s*동\\s*\\d+\\s*호");

    public String maskForLlm(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;

        // 주민번호 마스킹
        masked = SSN.matcher(masked).replaceAll("******-*******");

        // 전화번호 마스킹 (중간 4자리)
        masked = PHONE.matcher(masked).replaceAll(matchResult -> {
            String phone = matchResult.group();
            String cleaned = phone.replaceAll("-", "");
            if (cleaned.length() == 11) {
                return cleaned.substring(0, 3) + "-****-" + cleaned.substring(7);
            }
            return "010-****-****";
        });

        // 이메일 마스킹
        masked = EMAIL.matcher(masked).replaceAll("***@***.***");

        // 계좌번호 마스킹
        masked = ACCOUNT.matcher(masked).replaceAll("***-***-******");

        // 주소 마스킹 (일부만)
        masked = ADDRESS.matcher(masked).replaceAll("***");

        return masked;
    }
}

