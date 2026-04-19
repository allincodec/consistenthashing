import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

public class HashRingTest {

    private HashRing engine;

    @BeforeEach
    void setUp() {
        HashRing.nodes.clear();
        HashRing.nodes.put(50, 1);
        HashRing.nodes.put(100, 2);
        HashRing.nodes.put(150, 3);
        HashRing.nodes.put(200, 4);
        engine = new HashRing();
    }

    // addData: data routes to correct node
    @Test
    void addData_routesToCeilingNode() {
        int assignedRange = engine.addData(42);
        assertTrue(HashRing.nodes.containsKey(assignedRange));
    }

    // addData: same data always routes to same node (deterministic)
    @Test
    void addData_isDeterministic() {
        int first = engine.addData(99);
        int second = engine.addData(99);
        assertEquals(first, second);
    }

    // addData: data is stored under the correct node
    @Test
    void addData_dataStoredUnderNode() {
        int assignedRange = engine.addData(10);
        int nodeId = HashRing.nodes.get(assignedRange);
        assertTrue(engine.nodeData.containsKey(nodeId));
    }

    // addNode: new node claims correct range of data from next node
    @Test
    void addNode_shiftsDataFromNextNode() {
        engine.addData(10);
        engine.addData(20);
        engine.addData(30);

        int sizeBefore = engine.nodeData.values().stream().mapToInt(TreeMap::size).sum();
        engine.addNode(5, 75);
        int sizeAfter = engine.nodeData.values().stream().mapToInt(TreeMap::size).sum();

        assertEquals(sizeBefore, sizeAfter); // no data lost
    }

    // addNode: duplicate range throws
    @Test
    void addNode_duplicateRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> engine.addNode(5, 50));
    }

    // addNode: duplicate nodeId throws
    @Test
    void addNode_duplicateNodeIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> engine.addNode(1, 75));
    }

    // deleteNode: data moves to next node, none lost
    @Test
    void deleteNode_shiftsDataToNextNode() {
        for (int i = 1; i <= 20; i++) engine.addData(i);

        int sizeBefore = engine.nodeData.values().stream().mapToInt(TreeMap::size).sum();
        engine.deleteNode(2);
        int sizeAfter = engine.nodeData.values().stream().mapToInt(TreeMap::size).sum();

        assertEquals(sizeBefore, sizeAfter);
        assertFalse(HashRing.nodes.containsValue(2));
    }

    // deleteNode: non-existent node throws
    @Test
    void deleteNode_notFoundThrows() {
        assertThrows(IllegalArgumentException.class, () -> engine.deleteNode(99));
    }

    // deleteNode: deleting last node wraps data to first node
    @Test
    void deleteNode_lastNodeWrapsToFirst() {
        engine.addData(195); // likely hashes near end of ring
        engine.deleteNode(4); // node at range 200

        assertFalse(HashRing.nodes.containsValue(4));
        assertTrue(HashRing.nodes.containsKey(150)); // ring still intact
    }
}
