package tc.dag;

import java.util.*;

/**
 * Representation of a dependency graph.
 */
public class Graph<T> {
    private enum Direction {
        PRE,
        SUC;
        public Direction invert() {
            return (this == PRE ? SUC : PRE);
        }
    }
    private Map<T, EnumMap<Direction, Set<T>>> nodes = new HashMap<>();
    /**
     * Get all nodes in the graph.
     * @return a set of all the nodes in the graph.
     */
    public synchronized Set<T> allNodes() {
        return new HashSet<>(this.nodes.keySet());
    }
    /**
     * Adds NODE to the graph.
     * @param node the node to add to the graph.
     */
    public synchronized void addNode(T node) {
        if (node != null && ! this.nodes.containsKey(node)) {
            EnumMap<Direction, Set<T>> emap = new EnumMap<>(Direction.class);
            for (Direction d: Direction.values()) {
                emap.put(d, new HashSet<T>());

            }
            this.nodes.put(node, emap);
        }
    }
    /**
     * Checks if NODE exists in this graph.
     * @param node the node for which to check inclusion in the graph.
     * @return boolean true if the node exists.
     */
    public synchronized boolean hasNode(T node) {
        return this.nodes.containsKey(node);
    }
    /**
     * Get the set of neighbours of NODE in direction DIR.
     * @param node the node for which to find the neighbours.
     * @param dir the EdgeDirection in which to look.
     * @return a set of neighbouring nodes.
     */
    protected synchronized Set<T> getNeighbours(T node, Direction dir) {
        Set<T> result = new HashSet<>();
        if (this.nodes.containsKey(node)) {
            result.addAll(this.nodes.get(node).get(dir));
        }
        return result;
    }

    /**
     * Get the immediate predecessors of NODE.
     * @param node the node for which to get predecessors.
     * @return set of predecessor nodes.
     */
    public Set<T> getPredecessors(T node) {
        return this.getNeighbours(node, Direction.PRE);
    }

    /**
     * Get the immediate succesors of NODE.
     * @param node the node for which to get successors.
     * @return set of successor nodes.
     */
    public Set<T> getSuccessors(T node) {
        return this.getNeighbours(node, Direction.SUC);
    }

    /**
     * Get all reachable nodes from NODE in direction DIR.
     * @param node starting node for the search.
     * @param dir the EdgeDirection in which to search
     * @return a set of all reachable nodes.
     */
    protected synchronized Set<T> traceNodes(T node, Direction dir) {
        Set<T> result = new HashSet<>();
        if (this.hasNode(node)) {
            Set<T> visited = new HashSet<>();
            Deque<T> pending = new LinkedList<>();
            pending.add(node);
            while (!pending.isEmpty()) {
                T current = pending.removeFirst();
                result.add(current);
                for (T neighbour : this.getNeighbours(current, dir)) {
                    if (! visited.contains(neighbour)) {
                        pending.add(neighbour);
                    }
                }
                visited.add(current);
            }
        }
        return result;
    }

    /**
     * Get all upstream nodes from NODE.
     * @param node starting node.
     * @return set of all upstream nodes.
     */
    public Set<T> getUpstreamNodes(T node) {
        return this.traceNodes(node, Direction.PRE);
    }

    /**
     * Get all downstream nodes from NODE.
     * @param node starting node.
     * @return set of all downstream nodes.
     */
    public Set<T> getDownstreamNodes(T node) {
        return this.traceNodes(node, Direction.SUC);
    }

    /**
     * Adds to NODE in direction DIR the NEIGHBOUR node.
     * Nodes are added if they are not yet in the graph.
     * @param node the base node for the relationship.
     * @param dir the direction from the base node of the neighbour.
     * @param neighbour the neighbouring node.
     */
    protected synchronized void addNeighbour(T node, Direction dir, T neighbour) {
        this.addNode(node);
        this.addNode(neighbour);
        this.nodes.get(node).get(dir).add(neighbour);
        this.nodes.get(neighbour).get(dir.invert()).add(node);
    }

    /**
     * Adds the edge from PREDECESSOR to SUCCESSOR to the graph.
     * @param predecessor the predecessor node in the relationship.
     * @param successor the successor node in the relationship.
     */
    public void addEdge(T predecessor, T successor) {
        this.addNeighbour(predecessor, Direction.SUC, successor);
    }

    /**
     * Returns a set of ndoes that have no neighbours in direction DIR.
     * @param dir the EdgeDirection in which to search for neighbours.
     * @return possibly empty set of nodes.
     */
    protected synchronized Set<T> getNoNeighbourNodes(Direction dir) {
        Set<T> result = new HashSet<>();
        for (T node: this.nodes.keySet()) {
            if (this.nodes.get(node).get(dir).isEmpty()) {
                result.add(node);
            }
        }
        return result;
    }

    /**
     * Get all nodes that have no predecessors.
     * @return a set of nodes without predecessors.
     */
    public Set<T> getNoPredecesssorNodes() {
        return this.getNoNeighbourNodes(Direction.PRE);
    }

    /**
     * Get all nodes with no successors.
     * @return a set of nodes without successors.
     */
    public Set<T> getNoSuccessorNodes() {
        return this.getNoNeighbourNodes(Direction.SUC);
    }

    /**
     * Constants describing the 3 parts returned by getSubgraph()
     */
    public enum Parts {
        INITIALS,
        TERMINALS,
        NODES
    }

    /**
     * Get the initial, terminal and constituent nodes of a subgraph.
     * @param initials set of initial nodes defining the subgraph
     * @param terminals set of terminal ndoes defining the subgraph
     * @return an EnumMap of 3 sets: initial, terminal and constituent nodes.
     */
    public synchronized EnumMap<Parts, Set<T>> getSubgraph(Collection<T> initials, Collection<T> terminals) {
        EnumMap<Parts, Set<T>> result = new EnumMap<>(Parts.class);
        if (initials == null) {
            initials = this.getNoPredecesssorNodes();
        }
        if (terminals == null) {
            terminals = this.getNoSuccessorNodes();
        }
        Set<T> subgraph = new HashSet<>();
        for (T initial: initials) {
            subgraph.addAll(this.getDownstreamNodes(initial));
        }
        Set<T> upstreams = new HashSet<>();
        for (T terminal: terminals) {
            upstreams.addAll(this.getUpstreamNodes(terminal));
        }
        subgraph.retainAll(upstreams);  // intersection of upstreams and downstreams
        Set<T> trueInits = new HashSet<>(initials);
        trueInits.retainAll(subgraph);
        Set<T> trueTerms = new HashSet<>(terminals);
        trueTerms.retainAll(subgraph);
        result.put(Parts.INITIALS, trueInits);
        result.put(Parts.TERMINALS, trueTerms);
        result.put(Parts.NODES, subgraph);
        return result;
    }

    /**
     * Fixed struct to report out an edge.
     * @param <T> immutable node type
     */
    public static final class EdgePair<T> {
        public final T pre;
        public final T suc;
        public EdgePair(T preNode, T sucNode) {
            this.pre = preNode;
            this.suc = sucNode;
        }
    }

    /**
     * List all the edges in a subgraph, as a list of 2-element arrays.
     * @param subgraph subgraph for which to return edges
     * @return a List of 2-element arrays, each being the start and end of an edge.
     */
    public synchronized List<EdgePair<T>> getSubgraphEdges(EnumMap<Parts, Set<T>> subgraph) {
        List<EdgePair<T>> result = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        Deque<T> pending = new LinkedList<>();
        for (T node: subgraph.get(Parts.INITIALS)) {
            pending.add(node);
        }
        while (!pending.isEmpty()) {
            T current = pending.removeFirst();
            for (T neighbour : this.getSuccessors(current)) {
                if (subgraph.get(Parts.NODES).contains(neighbour)) {
                    EdgePair<T> pair = new EdgePair<T>(current, neighbour);
                    result.add(pair);
                    if (! visited.contains(neighbour)) {
                        pending.add(neighbour);
                    }
                }
            }
            visited.add(current);
        }
        return result;
    }
}
