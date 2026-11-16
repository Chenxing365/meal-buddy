package com.distributed.base.retrieval.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HashedEmbedding {

    public static final int DIM = 256;

    private HashedEmbedding() {
    }

    public static float[] embed(String text) {
        float[] vector = new float[DIM];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String normalized = text.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        for (int i = 0; i < normalized.length(); i++) {
            bump(vector, "c:" + normalized.charAt(i));
            if (i + 1 < normalized.length()) {
                bump(vector, "b:" + normalized.charAt(i) + normalized.charAt(i + 1));
            }
        }
        normalize(vector);
        return vector;
    }

    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0D;
        }
        double dot = 0D;
        double normA = 0D;
        double normB = 0D;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0D || normB == 0D) {
            return 0D;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static List<Float> toList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }

    public static float[] fromList(List<Float> list) {
        if (list == null || list.isEmpty()) {
            return new float[0];
        }
        float[] vector = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            vector[i] = list.get(i);
        }
        return vector;
    }

    private static void bump(float[] vector, String token) {
        vector[Math.floorMod(token.hashCode(), DIM)] += 1F;
    }

    private static void normalize(float[] vector) {
        double norm = 0D;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm == 0D) {
            return;
        }
        float factor = (float) (1D / Math.sqrt(norm));
        for (int i = 0; i < vector.length; i++) {
            vector[i] *= factor;
        }
    }
}
