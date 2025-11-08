// Represents a video stream session
class StreamingSession {
    private String sessionId;
    private User user;
    private Video video;
    private CDNNode cdnNode;
    private StreamingQuality quality;
    private long startTime;

    public void startStream() {}
    public void stopStream() {}
    public void switchQuality(StreamingQuality newQuality) {}
}

// Represents user watching content
class User {
    private String userId;
    private String name;
    private SubscriptionPlan plan;
    private List<StreamingSession> activeSessions;
}

// Represents a single video or episode
class Video {
    private String videoId;
    private String title;
    private String genre;
    private List<StreamingQuality> availableQualities;
    private String url;
}

// Enum for video quality levels
enum StreamingQuality {
    P360, P480, P720, P1080, P4K;
}

// Represents a CDN node serving content
class CDNNode {
    private String id;
    private String location;
    private int load;
    public boolean canServe(Video video) { return true; }
}

// Strategy Pattern for choosing CDN nodes
interface CDNSelectionStrategy {
    CDNNode selectCDN(List<CDNNode> cdnNodes, User user);
}

class NearestCDNStrategy implements CDNSelectionStrategy {
    public CDNNode selectCDN(List<CDNNode> cdnNodes, User user) {
        // return geographically closest node
        return cdnNodes.get(0);
    }
}

// Factory for creating StreamingSession
class StreamingSessionFactory {
    private CDNSelectionStrategy cdnStrategy;
    public StreamingSessionFactory(CDNSelectionStrategy strategy) {
        this.cdnStrategy = strategy;
    }

    public StreamingSession createSession(User user, Video video, List<CDNNode> cdnNodes) {
        CDNNode node = cdnStrategy.selectCDN(cdnNodes, user);
        StreamingSession session = new StreamingSession();
        // set session fields
        return session;
    }
}

public class Main {
    public static void main(String[] args) {
        User user = new User();
        Video video = new Video();
        List<CDNNode> cdnNodes = List.of(new CDNNode(), new CDNNode());

        CDNSelectionStrategy strategy = new NearestCDNStrategy();
        StreamingSessionFactory factory = new StreamingSessionFactory(strategy);

        StreamingSession session = factory.createSession(user, video, cdnNodes);
        session.startStream();
    }
}