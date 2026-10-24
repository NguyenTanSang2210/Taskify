package com.doanltmmt.Backend.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Đo độ giống nhau giữa hai văn bản (cosine trên tần suất từ), hỗ trợ tiếng Việt có dấu.
 * <p>
 * Văn bản được chuẩn hóa NFC (cùng một chữ có dấu có thể được gõ theo hai cách mã hóa), bỏ dấu câu nhưng giữ mọi
 * chữ cái Unicode, bỏ hư từ phổ biến. Tiếng Việt là ngôn ngữ đơn lập — một từ thường gồm nhiều tiếng — nên ngoài từng
 * tiếng còn tính cả cặp tiếng liền nhau ("hệ thống", "cơ sở") để thứ tự từ cũng được tính đến.
 */
@Service
public class SimilarityService {

    /** Ngưỡng coi là trùng lặp đáng cảnh báo. */
    public static final double ALERT_THRESHOLD = 0.8;

    private static final Locale VI = Locale.forLanguageTag("vi");

    static final Set<String> STOPWORDS = Set.of(
            "và", "của", "là", "các", "những", "có", "được", "cho", "với", "trong", "một", "này", "đã", "để",
            "thì", "mà", "khi", "từ", "theo", "về", "ra", "vào", "cũng", "như", "đến", "tại", "nên", "nhưng",
            "hay", "hoặc", "rằng", "bị", "do", "vì", "đó", "sẽ", "đang", "rất", "lại", "còn", "nếu",
            "the", "a", "an", "and", "of", "to", "in", "is", "for", "on", "with");

    /** Báo cáo ứng viên để so sánh. */
    public record Candidate(Long id, String content) {
    }

    /** Kết quả giống nhất: id báo cáo và điểm trong [0, 1]. */
    public record Match(Long reportId, double score) {
    }

    public double cosineSimilarity(String a, String b) {
        return cosine(termFreq(a), termFreq(b));
    }

    /** Báo cáo giống {@code content} nhất trong {@code candidates}; {@code null} nếu không có ứng viên nào có nội dung. */
    public Match mostSimilar(String content, List<Candidate> candidates) {
        Map<String, Integer> target = termFreq(content);
        Match best = null;
        for (Candidate c : candidates) {
            if (c.content() == null || c.content().isBlank()) continue;
            double score = cosine(target, termFreq(c.content()));
            if (best == null || score > best.score()) best = new Match(c.id(), score);
        }
        return best;
    }

    static List<String> tokenize(String text) {
        if (text == null) return List.of();
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(VI)
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        return Arrays.stream(normalized.split("\\s+"))
                .filter(t -> !t.isBlank() && !STOPWORDS.contains(t))
                .toList();
    }

    private static Map<String, Integer> termFreq(String text) {
        List<String> tokens = tokenize(text);
        Map<String, Integer> tf = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            tf.merge(tokens.get(i), 1, Integer::sum);
            if (i + 1 < tokens.size()) tf.merge(tokens.get(i) + " " + tokens.get(i + 1), 1, Integer::sum);
        }
        return tf;
    }

    private static double cosine(Map<String, Integer> va, Map<String, Integer> vb) {
        if (va.isEmpty() || vb.isEmpty()) return 0.0;
        Set<String> terms = new HashSet<>(va.keySet());
        terms.retainAll(vb.keySet());
        double dot = 0;
        for (String t : terms) dot += (double) va.get(t) * vb.get(t);
        return dot / (norm(va) * norm(vb));
    }

    private static double norm(Map<String, Integer> v) {
        double sum = 0;
        for (int x : v.values()) sum += (double) x * x;
        return Math.sqrt(sum);
    }
}
