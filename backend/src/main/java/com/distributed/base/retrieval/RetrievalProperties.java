package com.distributed.base.retrieval;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "meal-buddy.retrieval")
public class RetrievalProperties {

    private Tree tree = new Tree();
    private Leaf leaf = new Leaf();
    private Era era = new Era();
    private Graph graph = new Graph();

    @Data
    public static class Tree {
        private boolean enabled = true;
        private int maxDepth = 3;
        private int topNodes = 3;
        private int leafTopK = 5;
        private int sessionWindowDays = 7;
        private int topicTurnSize = 6;
    }

    @Data
    public static class Leaf {
        private boolean enabled = true;
        private int topK = 5;
        private double minScore = 0.05;
    }

    @Data
    public static class Era {
        private boolean enabled = true;
        private int minCorpus = 2000;
        private int changeThreshold = 200;
        private double changeRatio = 0.2;
        private long refreshIntervalMs = 300000;
    }

    @Data
    public static class Graph {
        private boolean enabled = false;
        private int maxSeeds = 3;
        private int topK = 3;
    }
}
