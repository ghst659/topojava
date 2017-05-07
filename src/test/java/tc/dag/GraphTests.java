package tc.dag;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * Created by tsc on 10/29/16.
 */
public class GraphTests {
    private Graph<String> dag = null;
    private static String nodeSuffix(int depth, int j) {
        return Integer.toString(depth) + "-" + Integer.toString(j);
    }
    private static int iPow(int x, int power) {
        int result = 1;
        for (int p = 0; p < power; ++p) {
            result *= x;
        }
        return result;
    }
    public static List<Graph.EdgePair<String>> genPyramid(int nodeDepth, int fanout, boolean upperHalf) {
        List<Graph.EdgePair<String>> result = new LinkedList<>();
        String prefix = upperHalf ? "U" : "L";
        for (int depth = 1; depth < nodeDepth; ++depth) {
            String nodePrefix = (depth + 1) == nodeDepth ? "X" : prefix;
            int rowCount = iPow(fanout, depth);
            for (int j = 0; j < rowCount; ++j) {
                String tagCurrent = nodePrefix + nodeSuffix(depth, j);
                String tagParent = prefix + nodeSuffix(depth - 1, j / fanout);
                Graph.EdgePair<String> edge;
                if (upperHalf) {
                    edge = new Graph.EdgePair(tagParent, tagCurrent);
                } else {
                    edge = new Graph.EdgePair(tagCurrent, tagParent);
                }
                result.add(edge);
            }
        }
        return result;
    }
    public static List<Graph.EdgePair<String>> genDiamond(int halfDepth, int fanout) {
        List<Graph.EdgePair<String>> result = new LinkedList<>();
        result.addAll(genPyramid(halfDepth, fanout, true));
        result.addAll(genPyramid(halfDepth, fanout, false));
        return result;
    }
    @Before
    public void setUp() {
        this.dag = new Graph<>();
    }
    @After
    public void tearDown() {
        this.dag = null;
    }
    @Test
    public void testEmptyGraphHasNoNodes() {
        Assert.assertTrue(this.dag.allNodes().isEmpty());
    }
    @Test
    public void testSingleNodeGraph() {
        final String node = "foo";
        this.dag.addNode(node);
        Assert.assertTrue(this.dag.allNodes().contains(node));
    }
    @Test
    public void testSingleNodeSubgraph() {
        final String node = "bar";
        this.dag.addNode(node);
        EnumMap<Graph.Parts, Set<String>> sub = this.dag.getSubgraph(null, null);
        Assert.assertTrue(sub.get(Graph.Parts.INITIALS).contains(node));
        Assert.assertTrue(sub.get(Graph.Parts.TERMINALS).contains(node));
        Assert.assertTrue(sub.get(Graph.Parts.NODES).contains(node));
    }
    @Test
    public void testThreeNodeDependency() {
        final String pre = "PRE";
        final String mid = "MID";
        final String suc = "SUC";
        this.dag.addEdge(pre, mid);
        this.dag.addEdge(mid, suc);
        Assert.assertTrue(this.dag.getPredecessors(pre).isEmpty());
        Assert.assertTrue(this.dag.getSuccessors(pre).contains(mid));
        Assert.assertTrue(this.dag.getPredecessors(mid).contains(pre));
        Assert.assertTrue(this.dag.getSuccessors(mid).contains(suc));
        Assert.assertTrue(this.dag.getPredecessors(suc).contains(mid));
        Assert.assertTrue(this.dag.getSuccessors(suc).isEmpty());
    }
    @Test
    public void testThreeNodeSubgraph() {
        final String pre = "PRE";
        final String mid = "MID";
        final String suc = "SUC";
        this.dag.addEdge(pre, mid);
        this.dag.addEdge(mid, suc);
        EnumMap<Graph.Parts, Set<String>> sub = this.dag.getSubgraph(null, null);
        Assert.assertTrue(sub.get(Graph.Parts.INITIALS).contains(pre));
        Assert.assertTrue(sub.get(Graph.Parts.TERMINALS).contains(suc));
        Assert.assertTrue(sub.get(Graph.Parts.NODES).contains(mid));
        Assert.assertTrue(sub.get(Graph.Parts.NODES).contains(pre));
        Assert.assertTrue(sub.get(Graph.Parts.NODES).contains(suc));
    }
    private void makeGraph() {
        this.dag.addEdge("a","b0");
        this.dag.addEdge("a","b1");
        this.dag.addEdge("b0","c00");
        this.dag.addEdge("b0","c01");
        this.dag.addEdge("b1","c10");
        this.dag.addEdge("b1","c11");
        this.dag.addEdge("c00","dx0");
        this.dag.addEdge("c01","dx1");
        this.dag.addEdge("c10","dx0");
        this.dag.addEdge("c11","dx1");
        this.dag.addEdge("b0", "dx1");
    }
    @Test
    public void testSubgraphEdges() {
        this.makeGraph();
        final String[] ini = {"b0"};
        final String[] ter = {"dx1"};
        List<String> inits = Arrays.asList(ini);
        List<String> terms = Arrays.asList(ter);
        EnumMap<Graph.Parts, Set<String>> subgraph = this.dag.getSubgraph(inits, terms);
        List<Graph.EdgePair<String>> edges = this.dag.getSubgraphEdges(subgraph);
        String[][] expected = {
            {"b0", "c01"},
            {"b0", "dx1"},
            {"c01", "dx1"}
        };
        Assert.assertEquals(expected.length, edges.size());
        for (int i = 0; i < expected.length; ++i) {
            String[] expectation = expected[i];
            boolean found = false;
            for (int j = 0; j < edges.size(); ++j) {
                if (expectation[0] == edges.get(j).pre && expectation[1] == edges.get(j).suc) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(String.format("edge (%s, %s)", expectation[0], expectation[1]), found);
        }
    }
    @Test public void testDiamond() {
        final int DEPTH = 3;
        final int FANOUT = 3;
        for (Graph.EdgePair<String> e: genDiamond(DEPTH, FANOUT)) {
            this.dag.addEdge(e.pre, e.suc);
        }
        final String[] ini = {"U0-0"};
        final String[] ter = {"L1-1"};
        List<String> inits = Arrays.asList(ini);
        List<String> terms = Arrays.asList(ter);
        EnumMap<Graph.Parts, Set<String>> subgraph = this.dag.getSubgraph(inits, terms);
        for (Graph.EdgePair<String> e: this.dag.getSubgraphEdges(subgraph)) {
            System.err.println(e.pre + " -> " + e.suc);
        }
    }
}
