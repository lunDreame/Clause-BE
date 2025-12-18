package com.clause.app.domain.rules.engine;

import com.clause.app.domain.rules.enums.ContractType;
import com.clause.app.domain.rules.model.ClauseCandidate;
import com.clause.app.domain.rules.model.RuleRunResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RuleEngineTest {

    @Autowired
    private RuleEngine ruleEngine;

    @Autowired
    private ClauseSegmenter segmenter;

    @Test
    void testFreelanceContractRules() {
        String text = """
                제8조 손해배상
                계약 위반 시 모든 손해를 배상해야 하며, 손해배상 한도가 없습니다.
                직접손해 및 간접손해를 포함합니다.
                
                제10조 저작권
                산출물의 저작권은 회사에 귀속되며, 소스코드도 제공해야 합니다.
                """;

        List<ClauseCandidate> segments = segmenter.segment(text);
        RuleRunResult result = ruleEngine.runRules(text, ContractType.FREELANCE, segments);

        assertThat(result.getCandidates()).isNotEmpty();
        assertThat(result.getTotalTriggers()).isGreaterThan(0);

        // IP_ASSIGNMENT 트리거 확인
        boolean hasIpTrigger = result.getCandidates().stream()
                .flatMap(c -> c.getRuleTriggers().stream())
                .anyMatch(t -> t.getCategory().name().equals("IP_ASSIGNMENT"));
        assertThat(hasIpTrigger).isTrue();

        // DAMAGES_UNLIMITED 트리거 확인
        boolean hasDamagesTrigger = result.getCandidates().stream()
                .flatMap(c -> c.getRuleTriggers().stream())
                .anyMatch(t -> t.getCategory().name().equals("DAMAGES_UNLIMITED"));
        assertThat(hasDamagesTrigger).isTrue();
    }

    @Test
    void testLeaseContractRules() {
        String text = """
                제5조 보증금
                계약 종료 시 보증금에서 청소비와 수리비를 공제할 수 있습니다.
                원상복구 비용도 차감됩니다.
                
                제6조 중도해지
                중도해지 시 위약금을 지급해야 합니다.
                """;

        List<ClauseCandidate> segments = segmenter.segment(text);
        RuleRunResult result = ruleEngine.runRules(text, ContractType.LEASE, segments);

        assertThat(result.getCandidates()).isNotEmpty();

        // DEPOSIT_DEDUCTION 트리거 확인
        boolean hasDepositTrigger = result.getCandidates().stream()
                .flatMap(c -> c.getRuleTriggers().stream())
                .anyMatch(t -> t.getCategory().name().equals("DEPOSIT_DEDUCTION"));
        assertThat(hasDepositTrigger).isTrue();
    }

    @Test
    void testEmploymentContractRules() {
        String text = """
                제3조 임금
                시급은 최저임금을 준수하며, 주휴수당이 포함됩니다.
                연장근로 시 가산금을 지급합니다.
                
                제4조 수습기간
                수습기간은 3개월이며, 수습급여는 정규급여의 90%입니다.
                """;

        List<ClauseCandidate> segments = segmenter.segment(text);
        RuleRunResult result = ruleEngine.runRules(text, ContractType.EMPLOYMENT, segments);

        assertThat(result.getCandidates()).isNotEmpty();

        // PAYMENT 트리거 확인
        boolean hasPaymentTrigger = result.getCandidates().stream()
                .flatMap(c -> c.getRuleTriggers().stream())
                .anyMatch(t -> t.getCategory().name().equals("PAYMENT"));
        assertThat(hasPaymentTrigger).isTrue();
    }

    @Test
    void testSelectTopCandidates() {
        String text = "제1조 테스트\n제2조 테스트\n제3조 테스트";
        List<ClauseCandidate> segments = segmenter.segment(text);
        RuleRunResult result = ruleEngine.runRules(text, ContractType.FREELANCE, segments);

        List<ClauseCandidate> topCandidates = ruleEngine.selectTopCandidates(
                result.getCandidates(), 10, ContractType.FREELANCE);

        assertThat(topCandidates.size()).isLessThanOrEqualTo(10);
    }
}

