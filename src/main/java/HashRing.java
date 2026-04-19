import java.util.*;

public class HashRing {

    /**
     * Ranges [ring with defined range a closed loop]
     * Add data to nodes
     */

    public static final TreeMap<Integer,Integer> nodes;
    public final TreeMap<Integer, TreeMap<Integer, List<Integer>>> nodeData;

    public HashRing() {
        nodeData = new TreeMap<>();
    }

    static {
        nodes = new TreeMap<>();
        nodes.put(50, 1);
        nodes.put(100, 2);
        nodes.put(150, 3);
        nodes.put(200, 4);
    }

    public Map.Entry<Integer, Integer> findNextNode(int range) {
        return Optional.ofNullable(nodes.higherEntry(range)).orElse(nodes.firstEntry());
    }

    public Map.Entry<Integer, Integer> findPrevNode(int range) {
        return nodes.lowerEntry(range);
    }

    private int hash(int data) {
        return Math.abs(String.valueOf(data).hashCode()) % nodes.lastKey();
    }

    public int addData(int data) {
        int hash = hash(data);
        Map.Entry<Integer, Integer> node = Optional.ofNullable(nodes.ceilingEntry(hash)).orElse(nodes.firstEntry());
        nodeData.putIfAbsent(node.getValue(), new TreeMap<>());
        nodeData.get(node.getValue()).putIfAbsent(hash, new ArrayList<>());
        nodeData.get(node.getValue()).get(hash).add(data);
        return node.getKey();
    }

    public TreeMap<Integer, List<Integer>> addNode(int nodeId, int range) {
        if(nodes.containsKey(range)) {
            throw new IllegalArgumentException("Node with same range already exists");
        }

        nodes.values().stream().filter(id -> id == nodeId).findFirst().ifPresent(id -> {
            throw new IllegalArgumentException("Node with same id already exists");
        });

        nodes.put(range, nodeId);

        // shuffle data to new node
        int nextNodeId = findNextNode(range).getValue();
        int startRange = findPrevNode(range).getKey();

        nodeData.putIfAbsent(nextNodeId, new TreeMap<>());
        SortedMap<Integer, List<Integer>> subData = nodeData.get(nextNodeId).subMap(startRange, false, range, true);
        nodeData.putIfAbsent(nodeId, new TreeMap<>());
        nodeData.get(nodeId).putAll(subData);
        nodeData.get(nextNodeId).subMap(startRange, false, range, true).clear();
        return nodeData.get(nodeId);
    }

    public TreeMap<Integer, List<Integer>> deleteNode(int nodeId) {

        Map.Entry<Integer, Integer> rangeNodeId = nodes.entrySet().stream()
                .filter(entry -> nodeId == entry.getValue()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Node not found"));

        Integer endRange = rangeNodeId.getKey();
        // shuffle data to next node
        int nextNodeId = findNextNode(endRange).getValue();
        int startRange = findPrevNode(endRange).getKey();

        if (nodeData.containsKey(nodeId)) {
            SortedMap<Integer, List<Integer>> subData = nodeData.get(nodeId).subMap(startRange, false, endRange, true);
            nodeData.putIfAbsent(nextNodeId, new TreeMap<>());
            nodeData.get(nextNodeId).putAll(subData);
        }
        nodeData.remove(nodeId);
        nodes.remove(endRange);
        return nodeData.get(nextNodeId);
    }



}
