package tc.dag;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

public class RabbitTests {
    private Graph<String> dag = null;
    private TestingOperator op = null;
    private Rabbit<String,String> rabbit = null;
    @Before
    public void setUp() {
        this.dag = new Graph<>();
        this.op = new TestingOperator();
        this.rabbit = new Rabbit<>(this.dag, this.op);
    }
    @After
    public void tearDown() {
        List<String> visits = this.op.visits();
        int i = 0;
        for (String v: visits) {
            System.out.println(String.format("%d: %s", i++, v));
        }
        this.rabbit = null;
        this.dag = null;
        this.op = null;
    }
    protected class TestingOperator implements NodeOperator<String, String> {
        private BlockingQueue<String> visitOrder = new LinkedBlockingQueue<>();
        private Random dither = new Random();
        @Override
        public String operate(String node) throws Exception {
            String result = null;
            try {
                if (node.startsWith("TEX")) {
                    throw new OperatorException();
                } else if (node.startsWith("TNT")) {
                    throw new NonTerminatingException();
                } else {
                    result = node;
                }
            } finally {
                this.visitOrder.add(node);
            }
            // Thread.sleep(this.dither.nextInt(1000));
            return result;
        }
        public List<String> visits() {
            List<String> result = new ArrayList<>(this.visitOrder);
            return result;
        }
    }

    @Test
    public void testSingleNodeGraph() throws Exception {
        this.dag.addNode("One");
        this.rabbit.run(4, 100, this.dag.allNodes(), this.dag.allNodes());
        Assert.assertEquals("One", this.rabbit.getResult("One"));
    }

    @Test
    public void testFatalException() throws Exception {
        this.dag.addNode("TEX");
        this.rabbit.run(4, 1000, null, null);
        try {
            this.rabbit.getResult("TEX");
            Assert.fail("did not get exception");
        } catch (OperatorException e) {
            // pass
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
        }
    }

    @Test
    public void testNonFatalException() throws Exception {
        this.dag.addEdge("A", "B");
        this.dag.addEdge("B", "C");
        this.dag.addEdge("A", "TNT");
        this.dag.addEdge("TNT", "D");
        this.rabbit.run(4, 1000, null, null);
        try {
            this.rabbit.getResult("TNT");
            Assert.fail("did not get exception");
        } catch (OperatorException e) {
            // pass
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
        }
        Assert.assertEquals("A", this.rabbit.getResult("A"));
        Assert.assertEquals("B", this.rabbit.getResult("B"));
        Assert.assertEquals("C", this.rabbit.getResult("C"));
        Assert.assertTrue(this.rabbit.getBlocked().contains("D"));
    }

    @Test
    public void testThreeNodeGraph() throws Exception {
        final String pre = "PRE";
        final String mid = "MID";
        final String suc = "SUC";
        this.dag.addEdge(pre, mid);
        this.dag.addEdge(mid, suc);
        this.rabbit.run(4, 5000, null, null);
        try {
            Assert.assertEquals(pre, this.rabbit.getResult(pre));
            Assert.assertEquals(mid, this.rabbit.getResult(mid));
            Assert.assertEquals(suc, this.rabbit.getResult(suc));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw e;
        }
    }

    @Test
    public void testDiamondGraph() throws Exception {
        final String pre = "PRE";
        final String sib = "SIB";
        final String mid = "MID";
        final String suc = "SUC";
        this.dag.addEdge(pre, mid);
        this.dag.addEdge(pre, sib);
        this.dag.addEdge(sib, suc);
        this.dag.addEdge(mid, suc);
        this.rabbit.run(4, 5000, null, null);
        try {
            Assert.assertEquals(pre, this.rabbit.getResult(pre));
            Assert.assertEquals(mid, this.rabbit.getResult(mid));
            Assert.assertEquals(sib, this.rabbit.getResult(sib));
            Assert.assertEquals(suc, this.rabbit.getResult(suc));
        } catch (Exception e) {
            System.err.println(e.getMessage());
            throw e;
        }
    }
}
