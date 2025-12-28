package com.doanltmmt.Backend.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SimilarityService {

    public double cosineSimilarity(String a, String b) {
        Map<String, Integer> va = termFreq(a);
        Map<String, Integer> vb = termFreq(b);
        Set<String> terms = new HashSet<>();
        terms.addAll(va.keySet());
        terms.addAll(vb.keySet());
        double dot = 0, na = 0, nb = 0;
        for (String t : terms) {
            int xa = va.getOrDefault(t, 0);
            int xb = vb.getOrDefault(t, 0);
            dot += xa * xb;
            na += xa * xa;
            nb += xb * xb;
        }
        if (na == 0 || nb == 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private Map<String, Integer> termFreq(String s) {
        Map<String, Integer> tf = new HashMap<>();
        String[] tokens = s.toLowerCase().replaceAll("[^a-z0-9\\s]", " ").split("\\s+");
        for (String t : tokens) {
            if (t.isBlank()) continue;
            tf.put(t, tf.getOrDefault(t, 0) + 1);
        }
        return tf;
    }
}
