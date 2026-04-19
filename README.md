# Consistent Hashing

A Java implementation of consistent hashing with dynamic node addition and removal.

## The problem with simple hashing

With `N` servers, a naive approach assigns data using `hash(key) % N`. This works until the cluster changes:

- Add a server: `N` becomes `N+1`, so `hash(key) % N+1` remaps **almost every key**
- Remove a server: same problem — massive reshuffling across all nodes

In a distributed cache or database, this means a thundering herd of cache misses and data migrations every time a node joins or leaves.

## What consistent hashing solves

Consistent hashing maps both data and nodes onto a fixed ring. Each node owns a contiguous range of that ring. When the cluster changes:

- **Add a node**: only the data in the new node's range moves — from one neighbor
- **Remove a node**: only that node's data moves — to one neighbor
- **Everything else stays put**

In a cluster of `N` nodes, adding or removing one node moves only `1/N` of the data instead of remapping everything. This makes scaling up/down safe and incremental.

## How it works

The ring is a `TreeMap<Integer, Integer>` where keys are range end-points and values are node IDs.

```
Nodes: [50 → 1, 100 → 2, 150 → 3, 200 → 4]

Node 1 owns: (0,   50]
Node 2 owns: (50,  100]
Node 3 owns: (100, 150]
Node 4 owns: (150, 200]  ← wraps back to Node 1
```

Data is hashed to a position in `[0, maxRange)` and assigned to the node at `ceiling(hash)`.

## Key operations

### addData(int data)
Hashes the data and routes it to the responsible node using `ceilingEntry`. Returns the range key of the assigned node.

### addNode(int nodeId, int range)
Inserts a new node at the given range. Data in `(prevRange, range]` is migrated from the next node to the new node. No other data moves.

### deleteNode(int nodeId)
Removes the node and migrates its data `(prevRange, endRange]` to the next node. If the deleted node is the last one, data wraps to the first node.

## Data migration on node change

```
Before: [50→1, 100→2, 150→3, 200→4]
Add node 5 at range 75:
  → Node 2 (owns 50-100) loses data in (50, 75]
  → Node 5 takes over (50, 75]
After:  [50→1, 75→5, 100→2, 150→3, 200→4]
```

Only data in the affected range moves — this is the core advantage of consistent hashing.

## Visual ring

```
                    0
               _____|_____
              /           \
        200  |    ring     |  50
      (Node4)|             |(Node1)
              \           /
               -----------
        150  |             |  100
      (Node3)|             |(Node2)

  hash(data) lands somewhere on the ring
  → assigned to the next node clockwise
```

## Step-by-step walkthrough

> Hash values below are illustrative. Actual values depend on `String.hashCode() % maxRange`.

```
Initial ring:  [50→Node1, 100→Node2, 150→Node3, 200→Node4]

1. addData(42)
   hash(42) = 42
   ceilingEntry(42) → (50, Node1)
   → data 42 stored under Node1

2. addData(80)
   hash(80) = 80
   ceilingEntry(80) → (100, Node2)
   → data 80 stored under Node2

3. addNode(5, 75)
   New node 5 at range 75, between Node1(50) and Node2(100)
   → data in (50, 75] migrates from Node2 to Node5
   → data 80 stays with Node2 (hash 80 > 75)
   Ring: [50→Node1, 75→Node5, 100→Node2, 150→Node3, 200→Node4]

4. deleteNode(Node5)
   Node5 owned (50, 75]
   → its data migrates to next node: Node2
   Ring: [50→Node1, 100→Node2, 150→Node3, 200→Node4]
```

## Running the prototype

```java
HashRing engine = new HashRing();

// add some data
engine.

addData(42);
engine.

addData(80);
engine.

addData(130);

// scale up — add a new node at range 75
engine.

addNode(5,75);

// scale down — remove a node
engine.

deleteNode(1);
```

## Running tests

```bash
mvn test
```
