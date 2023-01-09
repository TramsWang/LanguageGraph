package exp;

import com.google.gson.Gson;
import util.graph.FeedbackVertexSetSolver;
import util.graph.GraphNode;
import util.graph.Tarjan;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ObservePhraseCycles {

    static class PhraseExplanations {
        final String phrase;
        final String pinyin;
        final String[][] explanations;

        public PhraseExplanations(String phrase, String pinyin, String[][] explanations) {
            this.phrase = phrase;
            this.pinyin = pinyin;
            this.explanations = explanations;
        }
    }

    final Map<String, GraphNode<String>> nodes = new HashMap<>();
    final Map<GraphNode<String>, Set<GraphNode<String>>> graph = new HashMap<>();
    final Set<String> stopPhrases;

    public static void main(String[] args) throws IOException {
        if (2 != args.length) {
            System.err.println("Usage: <Phrase Explanation File> <Stop Phrase File>");
        }
        PhraseExplanations[] phrases = loadPhrases(args[0]);
        Set<String> stop_phrases = loadStopPhrases(args[1]);
        ObservePhraseCycles exp = new ObservePhraseCycles(phrases, stop_phrases);
        exp.findCycles();
    }

    static PhraseExplanations[] loadPhrases(String filePath) throws FileNotFoundException {
        return new Gson().fromJson(new FileReader(filePath), PhraseExplanations[].class);
    }

    static Set<String> loadStopPhrases(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        Set<String> stop_phrases = new HashSet<>();
        String line;
        while (null != (line = reader.readLine())) {
            stop_phrases.add(line);
        }
        return stop_phrases;
    }

    public ObservePhraseCycles(PhraseExplanations[] phrases, Set<String> stopPhrases) {
        this.stopPhrases = stopPhrases;

        /* Construct dependency graph */
        for (PhraseExplanations phrase: phrases) {
            graph.compute(getNode(phrase.phrase), (node, neighbours) -> {
                if (null == neighbours) {
                    neighbours = new HashSet<>();
                }
                for (String[] explanation: phrase.explanations) {
                    for (String dependent_phrase: explanation) {
                        neighbours.add(getNode(dependent_phrase));
                    }
                }
                return neighbours;
            });
        }
    }

    GraphNode<String> getNode(String phrase) {
        return nodes.computeIfAbsent(phrase, k -> new GraphNode<>(phrase));
    }

    public void findCycles() {
        /* Show some statistics of the graph */
        int edges = 0;
        for (Set<GraphNode<String>> neighbours: graph.values()) {
            edges += neighbours.size();
        }
        System.out.printf("Original Graph: %d nodes, %d edges, average degree is %.2f\n", graph.size(), edges, edges * 1.0 / graph.size());

        /* Find cycles with stop phrases */
        System.out.println("Finding cycles with stop phrases:");
        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        int scc_total_size = 0;
        int fvs_total_size = 0;
        for (Set<GraphNode<String>> scc: sccs) {
            scc_total_size += scc.size();
            FeedbackVertexSetSolver<GraphNode<String>> fvs_solver = new FeedbackVertexSetSolver<>(graph, scc);
            Set<GraphNode<String>> fvs = fvs_solver.run();
            fvs_total_size += fvs.size();
        }
        System.out.printf("%d SCCs, %d nodes involved, at most %d FVS needed\n", sccs.size(), scc_total_size, fvs_total_size);

        /* Show some statistics of the graph with cleaning the stop phrases */
        for (String stop_phrase: stopPhrases) {
            Set<GraphNode<String>> neighbours = graph.get(new GraphNode<>(stop_phrase));
            if (null != neighbours) {
                edges -= neighbours.size();
                neighbours.clear();
            }
        }
        System.out.printf("Graph without Stop Phrases: %d nodes, %d edges, average degree is %.2f\n", graph.size(), edges, edges * 1.0 / graph.size());

        /* Find cycles without stop phrases */
        System.out.println("Finding cycles without stop phrases:");
        tarjan = new Tarjan<>(graph, true);
        sccs = tarjan.run();
        scc_total_size = 0;
        fvs_total_size = 0;
        for (Set<GraphNode<String>> scc: sccs) {
            scc_total_size += scc.size();
            FeedbackVertexSetSolver<GraphNode<String>> fvs_solver = new FeedbackVertexSetSolver<>(graph, scc);
            Set<GraphNode<String>> fvs = fvs_solver.run();
            fvs_total_size += fvs.size();
        }
        System.out.printf("%d SCCs, %d nodes involved, at most %d FVS needed\n", sccs.size(), scc_total_size, fvs_total_size);
    }
}
