class Node<K,V> {
    K key;
    V value;
    Node<K,V> prev, next;
    Node(K k, V v) { key = k; value = v; }
}

class LRUCache<K,V> {
    private final int capacity;
    private final Map<K, Node<K,V>> cache = new HashMap<>();
    private Node<K,V> head, tail;

    public LRUCache(int capacity) {
        this.capacity = capacity;
    }

    public synchronized V get(K key) {
        if (!cache.containsKey(key)) return null;
        Node<K,V> node = cache.get(key);
        moveToHead(node);
        return node.value;
    }

    public synchronized void put(K key, V value) {
        if (cache.containsKey(key)) {
            Node<K,V> node = cache.get(key);
            node.value = value;
            moveToHead(node);
            return;
        }
        if (cache.size() == capacity) {
            cache.remove(tail.key);
            removeNode(tail);
        }
        Node<K,V> newNode = new Node<>(key, value);
        addToHead(newNode);
        cache.put(key, newNode);
    }

    private void moveToHead(Node<K,V> node) {
        removeNode(node);
        addToHead(node);
    }
    private void removeNode(Node<K,V> node) { /* update pointers */ }
    private void addToHead(Node<K,V> node) { /* update pointers */ }
}

public class Main {
    public static void main(String[] args) {
        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "Naruto");
        cache.put(2, "One Piece");
        cache.get(1);
        cache.put(3, "Bleach"); // evicts key 2
    }
}