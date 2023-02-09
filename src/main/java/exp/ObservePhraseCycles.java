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
    final Set<String> explainedPhrases = new HashSet<>();

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
            explainedPhrases.add(phrase.phrase);
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
        for (GraphNode<String> node: nodes.values()) {  // Sync all nodes to the graph's entry set
            graph.computeIfAbsent(node, k -> new HashSet<>());
        }
    }

    GraphNode<String> getNode(String phrase) {
        return nodes.computeIfAbsent(phrase, k -> new GraphNode<>(phrase));
    }

    public void findCycles() {
        /* Show some statistics of the graph */
        int edges = 0;
        for (Map.Entry<GraphNode<String>, Set<GraphNode<String>>> entry: graph.entrySet()) {
            String phrase = entry.getKey().content;
            Set<GraphNode<String>> neighbours = entry.getValue();
            edges += neighbours.size();
        }
        System.out.printf(
                "Original Graph: %d nodes, %d edges, average degree is %.2f; average explanation cardinality: %.2f\n",
                nodes.size(), edges, edges * 2.0 / nodes.size(), edges * 1.0 / explainedPhrases.size()
        );

        /* Find cycles with stop phrases */
        System.out.println("Finding cycles with stop phrases:");
        Tarjan<GraphNode<String>> tarjan = new Tarjan<>(graph, false);
        List<Set<GraphNode<String>>> sccs = tarjan.run();
        int scc_total_size = 0;
        int fvs_total_size = 0;
        int self_loops = 0;
        int unexceptional_singleton_scc = 0;
        for (Set<GraphNode<String>> scc: sccs) {
            if (1 == scc.size()) {
                GraphNode<String> node = scc.iterator().next();
                if (graph.get(node).contains(node)) {
                    System.out.println("自环：" + node.content);
                    self_loops++;
                } else {
                    System.out.println("单点SCC：" + node.content);
                    unexceptional_singleton_scc++;
                }
            } else {
                scc_total_size += scc.size();
                FeedbackVertexSetSolver<GraphNode<String>> fvs_solver = new FeedbackVertexSetSolver<>(graph, scc);
                Set<GraphNode<String>> fvs = fvs_solver.run();
                fvs_total_size += fvs.size();
            }
        }
        System.out.printf(
                "%d SCCs, %d nodes involved, at most %d FVS needed\n",
                sccs.size() - self_loops - unexceptional_singleton_scc, scc_total_size, fvs_total_size
        );
        System.out.printf("%d self loops; %d unexceptional singleton SCCs\n", self_loops, unexceptional_singleton_scc);

        /* Show some statistics of the graph with cleaning the stop phrases */
        edges = 0;
        for (Map.Entry<GraphNode<String>, Set<GraphNode<String>>> entry: graph.entrySet()) {
            String phrase = entry.getKey().content;
            Set<GraphNode<String>> neighbours = entry.getValue();
            neighbours.removeIf(e -> stopPhrases.contains(e.content));
            edges += neighbours.size();
        }
        System.out.printf(
                "Original Graph: %d nodes, %d edges, average degree is %.2f; average explanation cardinality: %.2f\n",
                nodes.size(), edges, edges * 2.0 / nodes.size(), edges * 1.0 / explainedPhrases.size()
        );

        /* Find cycles without stop phrases */
        System.out.println("Finding cycles without stop phrases:");
        tarjan = new Tarjan<>(graph, true);
        sccs = tarjan.run();
        scc_total_size = 0;
        fvs_total_size = 0;
        self_loops = 0;
        unexceptional_singleton_scc = 0;
        for (Set<GraphNode<String>> scc: sccs) {
            if (1 == scc.size()) {
                GraphNode<String> node = scc.iterator().next();
                if (graph.get(node).contains(node)) {
                    System.out.println("自环：" + node.content);
                    self_loops++;
                } else {
                    System.out.println("单点SCC：" + node.content);
                    unexceptional_singleton_scc++;
                }
            } else {
                scc_total_size += scc.size();
                FeedbackVertexSetSolver<GraphNode<String>> fvs_solver = new FeedbackVertexSetSolver<>(graph, scc);
                Set<GraphNode<String>> fvs = fvs_solver.run();
                fvs_total_size += fvs.size();
            }
        }
        System.out.printf(
                "%d SCCs, %d nodes involved, at most %d FVS needed\n",
                sccs.size() - self_loops - unexceptional_singleton_scc, scc_total_size, fvs_total_size
        );
        System.out.printf("%d self loops; %d unexceptional singleton SCCs\n", self_loops, unexceptional_singleton_scc);
    }
}
