import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// =============================
// ENUMS & BASIC MODELS
// =============================

enum StreamingQuality {
    P360(1.0), P480(2.0), P720(4.0), P1080(6.0), P4K(15.0);

    private final double requiredMbps;
    StreamingQuality(double requiredMbps) { this.requiredMbps = requiredMbps; }
    public double getRequiredMbps() { return requiredMbps; }
}

enum SubscriptionPlan {
    BASIC(1), PREMIUM(3), ULTRA(5);
    private final int maxDevices;
    SubscriptionPlan(int maxDevices) { this.maxDevices = maxDevices; }
    public int getMaxDevices() { return maxDevices; }
}

// =============================
// CORE DOMAIN CLASSES
// =============================

class User {
    private String userId;
    private String name;
    private SubscriptionPlan plan;
    List<StreamingSession> activeSessions = new ArrayList<>();

    public User(String userId, String name, SubscriptionPlan plan) {
        this.userId = userId;
        this.name = name;
        this.plan = plan;
    }

    public String getUserId() { return userId; }
    public SubscriptionPlan getPlan() { return plan; }
}

class Video {
    private String videoId;
    private String title;
    private String genre;
    private List<StreamingQuality> availableQualities;
    private Map<StreamingQuality, String> qualityUrls;

    public Video(String id, String title, List<StreamingQuality> qualities) {
        this.videoId = id;
        this.title = title;
        this.availableQualities = qualities;
        this.qualityUrls = new HashMap<>();
        for (StreamingQuality q : qualities) {
            qualityUrls.put(q, "https://cdn/video/" + id + "/" + q.name() + ".m3u8");
        }
    }

    public List<StreamingQuality> getAvailableQualities() { return availableQualities; }
    public String getUrlForQuality(StreamingQuality q) { return qualityUrls.get(q); }
}

// =============================
// CDN NODES & STRATEGY PATTERN
// =============================

class CDNNode {
    private String id;
    private String location;
    private double latitude;
    private double longitude;
    private AtomicInteger load = new AtomicInteger(0);
    private boolean healthy = true;

    public CDNNode(String id, String loc, double lat, double lon) {
        this.id = id; this.location = loc;
        this.latitude = lat; this.longitude = lon;
    }

    public boolean isHealthy() { return healthy; }
    public int getLoad() { return load.get(); }
    public void incrementLoad() { load.incrementAndGet(); }
    public void decrementLoad() { load.decrementAndGet(); }

    public double distanceTo(double userLat, double userLon) {
        return Math.sqrt(Math.pow(latitude - userLat, 2) + Math.pow(longitude - userLon, 2));
    }

    public String getId() { return id; }
}

interface CDNSelectionStrategy {
    CDNNode selectCDN(List<CDNNode> nodes, User user, double userLat, double userLon);
}

class NearestCDNStrategy implements CDNSelectionStrategy {
    @Override
    public CDNNode selectCDN(List<CDNNode> nodes, User user, double userLat, double userLon) {
        return nodes.stream()
                .filter(CDNNode::isHealthy)
                .min(Comparator.comparingDouble(n -> n.distanceTo(userLat, userLon)))
                .orElseThrow(() -> new RuntimeException("No healthy CDN node available"));
    }
}

class LeastLoadedCDNStrategy implements CDNSelectionStrategy {
    @Override
    public CDNNode selectCDN(List<CDNNode> nodes, User user, double userLat, double userLon) {
        return nodes.stream()
                .filter(CDNNode::isHealthy)
                .min(Comparator.comparingInt(CDNNode::getLoad))
                .orElseThrow(() -> new RuntimeException("No healthy CDN node available"));
    }
}

// =============================
// OBSERVER PATTERN (Analytics)
// =============================

interface SessionObserver {
    void onEvent(StreamingEvent event);
}

class EventLogger implements SessionObserver {
    @Override
    public void onEvent(StreamingEvent event) {
        System.out.println("[Analytics] Event: " + event.getEventType() + " | Session: " + event.getSessionId());
        // In real system, push to Kafka or analytics pipeline asynchronously
    }
}

class StreamingEvent {
    private String sessionId;
    private String eventType;
    private long timestamp = System.currentTimeMillis();

    public StreamingEvent(String sessionId, String type) {
        this.sessionId = sessionId;
        this.eventType = type;
    }

    public String getSessionId() { return sessionId; }
    public String getEventType() { return eventType; }
}

// =============================
// QUALITY MANAGER (Adaptive Bitrate)
// =============================

class QualityManager {
    private StreamingSession session;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running = false;

    public QualityManager(StreamingSession session) {
        this.session = session;
    }

    public void startMonitoring() {
        running = true;
        scheduler.scheduleAtFixedRate(this::adjustQuality, 3, 3, TimeUnit.SECONDS);
    }

    public void stopMonitoring() {
        running = false;
        scheduler.shutdownNow();
    }

    private void adjustQuality() {
        if (!running) return;

        // Mock network bandwidth measurement (1 to 20 Mbps)
        double currentBandwidth = 1 + Math.random() * 20;

        StreamingQuality best = selectBestQuality(currentBandwidth, session.getVideo().getAvailableQualities());
        if (best != session.getCurrentQuality()) {
            System.out.println("[QualityManager] Bandwidth: " + String.format("%.2f", currentBandwidth) +
                    " Mbps → Switching to " + best);
            session.switchQuality(best);
        } else {
            System.out.println("[QualityManager] Bandwidth: " + String.format("%.2f", currentBandwidth) +
                    " Mbps → Keeping " + session.getCurrentQuality());
        }
    }

    private StreamingQuality selectBestQuality(double bandwidth, List<StreamingQuality> available) {
        return available.stream()
                .filter(q -> q.getRequiredMbps() <= bandwidth)
                .max(Comparator.comparingDouble(StreamingQuality::getRequiredMbps))
                .orElse(StreamingQuality.P360);
    }
}

// =============================
// STREAMING SESSION CLASS
// =============================

class StreamingSession {
    private String sessionId = UUID.randomUUID().toString();
    private User user;
    private Video video;
    private CDNNode cdnNode;
    private StreamingQuality currentQuality;
    private long startTime;
    private List<SessionObserver> observers = new ArrayList<>();
    private QualityManager qualityManager;

    public StreamingSession(User user, Video video, CDNNode node, StreamingQuality quality) {
        this.user = user;
        this.video = video;
        this.cdnNode = node;
        this.currentQuality = quality;
        this.startTime = System.currentTimeMillis();
        this.qualityManager = new QualityManager(this);
    }

    public void addObserver(SessionObserver obs) { observers.add(obs); }
    private void notifyObservers(String eventType) {
        for (SessionObserver obs : observers)
            obs.onEvent(new StreamingEvent(sessionId, eventType));
    }

    public void startStream() {
        cdnNode.incrementLoad();
        notifyObservers("START_STREAM");
        qualityManager.startMonitoring();
    }

    public void stopStream() {
        cdnNode.decrementLoad();
        notifyObservers("STOP_STREAM");
        qualityManager.stopMonitoring();
    }

    public void switchQuality(StreamingQuality newQuality) {
        if (video.getAvailableQualities().contains(newQuality)) {
            this.currentQuality = newQuality;
            notifyObservers("QUALITY_SWITCH");
        }
    }

    public StreamingQuality getCurrentQuality() { return currentQuality; }
    public Video getVideo() { return video; }
}

// =============================
// SESSION FACTORY
// =============================

class StreamingSessionFactory {
    private CDNSelectionStrategy cdnStrategy;

    public StreamingSessionFactory(CDNSelectionStrategy strategy) {
        this.cdnStrategy = strategy;
    }

    public synchronized StreamingSession createSession(User user, Video video, List<CDNNode> nodes,
                                                       double userLat, double userLon) {
        if (user.activeSessions.size() >= user.getPlan().getMaxDevices())
            throw new RuntimeException("Max device limit reached for plan " + user.getPlan());

        CDNNode node = cdnStrategy.selectCDN(nodes, user, userLat, userLon);
        StreamingSession session = new StreamingSession(user, video, node, StreamingQuality.P720);
        session.addObserver(new EventLogger());
        user.activeSessions.add(session);
        return session;
    }
}

// =============================
// MAIN DEMO
// =============================

public class Main {
    public static void main(String[] args) throws InterruptedException {
        User user = new User("U1", "Bhargav", SubscriptionPlan.PREMIUM);
        Video video = new Video("VID101", "Attack on Titan", List.of(
                StreamingQuality.P360, StreamingQuality.P480, StreamingQuality.P720, StreamingQuality.P1080));

        List<CDNNode> cdnNodes = List.of(
                new CDNNode("CDN1", "Delhi", 28.6, 77.2),
                new CDNNode("CDN2", "Mumbai", 19.0, 72.8)
        );

        CDNSelectionStrategy strategy = new NearestCDNStrategy();
        StreamingSessionFactory factory = new StreamingSessionFactory(strategy);

        StreamingSession session = factory.createSession(user, video, cdnNodes, 28.5, 77.1);
        session.startStream();

        // Let the QualityManager adjust quality for a while
        Thread.sleep(15000);

        session.stopStream();
    }
}
