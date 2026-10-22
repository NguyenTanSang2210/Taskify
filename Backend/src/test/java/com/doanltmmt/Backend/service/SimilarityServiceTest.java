package com.doanltmmt.Backend.service;

import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** P2-08 — so trùng lặp với văn bản tiếng Việt có dấu. */
class SimilarityServiceTest {

    private final SimilarityService service = new SimilarityService();

    private static final String A = "Tuần này em đã xây dựng hệ thống quản lý đề tài, hoàn thành chức năng đăng nhập "
            + "và phân quyền người dùng bằng Spring Security.";
    private static final String A_REWORDED = "Trong tuần này em xây dựng hệ thống quản lý đề tài: đã hoàn thành chức năng "
            + "đăng nhập, phân quyền người dùng bằng Spring Security!";
    private static final String OTHER = "Nghiên cứu thuật toán học máy để dự báo lượng mưa tại đồng bằng sông Cửu Long "
            + "dựa trên dữ liệu vệ tinh.";

    @Test
    void keepsVietnameseLetters_andDropsPunctuationAndStopwords() {
        assertEquals(List.of("xây", "dựng", "hệ", "thống"), SimilarityService.tokenize("Xây dựng, hệ thống!"));
        assertEquals(List.of("báo", "cáo", "tiến", "độ"), SimilarityService.tokenize("Báo cáo của các tiến độ"));
    }

    @Test
    void nearlyIdenticalVietnameseReports_scoreAboveThreshold() {
        assertTrue(service.cosineSimilarity(A, A_REWORDED) >= SimilarityService.ALERT_THRESHOLD,
                () -> "điểm = " + service.cosineSimilarity(A, A_REWORDED));
    }

    @Test
    void unrelatedReports_scoreLow() {
        assertTrue(service.cosineSimilarity(A, OTHER) < 0.2, () -> "điểm = " + service.cosineSimilarity(A, OTHER));
    }

    @Test
    void decomposedAndComposedUnicode_areTheSameText() {
        String nfd = Normalizer.normalize(A, Normalizer.Form.NFD);
        assertEquals(1.0, service.cosineSimilarity(A, nfd), 1e-9);
    }

    @Test
    void wordOrderMatters_throughSyllablePairs() {
        assertTrue(service.cosineSimilarity("hệ thống quản lý", "quản lý hệ thống")
                < service.cosineSimilarity("hệ thống quản lý", "hệ thống quản lý"));
    }

    @Test
    void mostSimilar_picksTheClosestReport() {
        SimilarityService.Match m = service.mostSimilar(A, List.of(
                new SimilarityService.Candidate(1L, OTHER),
                new SimilarityService.Candidate(2L, A_REWORDED),
                new SimilarityService.Candidate(3L, null)));
        assertEquals(2L, m.reportId());
        assertNull(service.mostSimilar(A, List.of()));
        assertEquals(0.0, service.cosineSimilarity("", A));
    }
}
