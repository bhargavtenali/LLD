class User {
    private String userId;
    private List<String> watchedVideos;
    private List<String> likedGenres;
}

class Video {
    private String id;
    private String title;
    private List<String> genres;
}

// Recommendation strategy abstraction
interface RecommendationStrategy {
    List<Video> recommend(User user);
}

// Concrete strategies
class ContentBasedStrategy implements RecommendationStrategy {
    public List<Video> recommend(User user) {
        // Match genres and tags
        return List.of();
    }
}

class CollaborativeFilteringStrategy implements RecommendationStrategy {
    public List<Video> recommend(User user) {
        // Similar users’ history
        return List.of();
    }
}

// Caching layer (thread-safe)
class RecommendationCache {
    private Map<String, List<Video>> userCache = new ConcurrentHashMap<>();
    public List<Video> get(String userId) { return userCache.get(userId); }
    public void put(String userId, List<Video> recs) { userCache.put(userId, recs); }
}

// Service layer
class RecommendationService {
    private RecommendationStrategy strategy;
    private RecommendationCache cache;

    public RecommendationService(RecommendationStrategy strategy, RecommendationCache cache) {
        this.strategy = strategy;
        this.cache = cache;
    }

    public List<Video> getRecommendations(User user) {
        if (cache.get(user.getUserId()) != null)
            return cache.get(user.getUserId());

        List<Video> recs = strategy.recommend(user);
        cache.put(user.getUserId(), recs);
        return recs;
    }
}

public class Main {
    public static void main(String[] args) {
        User user = new User();
        RecommendationCache cache = new RecommendationCache();

        RecommendationStrategy strategy = new ContentBasedStrategy();
        RecommendationService service = new RecommendationService(strategy, cache);

        List<Video> recs = service.getRecommendations(user);
        recs.forEach(v -> System.out.println(v.getTitle()));
    }
}