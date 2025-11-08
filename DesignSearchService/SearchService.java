class Anime {
    private String id;
    private String title;
    private String genre;
    private double rating;
}

// Trie node for prefix search
class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    List<String> titles = new ArrayList<>();
}

// Core Trie-based index
class Trie {
    private TrieNode root = new TrieNode();

    public void insert(String title) { /* build trie */ }
    public List<String> searchPrefix(String prefix) { return List.of(); }
}

// SearchService
class SearchService {
    private Trie trie;
    private Map<String, List<Anime>> cache = new ConcurrentHashMap<>();

    public SearchService(Trie trie) {
        this.trie = trie;
    }

    public List<Anime> search(String query) {
        if (cache.containsKey(query))
            return cache.get(query);

        List<String> matches = trie.searchPrefix(query);
        List<Anime> results = matches.stream().map(this::fetchFromDB).toList();
        cache.put(query, results);
        return results;
    }

    private Anime fetchFromDB(String title) {
        // simulate DB call
        return new Anime();
    }
}

public class Main {
    public static void main(String[] args) {
        Trie trie = new Trie();
        trie.insert("Naruto");
        trie.insert("One Piece");
        trie.insert("One Punch Man");

        SearchService service = new SearchService(trie);
        service.search("One"); // autocomplete
    }
}