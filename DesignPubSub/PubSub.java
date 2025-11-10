interface Subscriber {
    void onMessage(String topic, String message);
}

class Topic {
    private String name;
    private List<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    public void addSubscriber(Subscriber s) { subscribers.add(s); }
    public void publish(String message) {
        subscribers.forEach(s -> s.onMessage(name, message));
    }
}

class PubSubService {
    private Map<String, Topic> topics = new ConcurrentHashMap<>();

    public void subscribe(String topicName, Subscriber s) {
        topics.computeIfAbsent(topicName, k -> new Topic(k)).addSubscriber(s);
    }

    public void publish(String topicName, String message) {
        if (topics.containsKey(topicName)) {
            topics.get(topicName).publish(message);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        PubSubService service = new PubSubService();

        Subscriber s1 = (t, m) -> System.out.println("User1 got: " + m);
        Subscriber s2 = (t, m) -> System.out.println("User2 got: " + m);

        service.subscribe("anime", s1);
        service.subscribe("anime", s2);

        service.publish("anime", "New episode of Jujutsu Kaisen released!");
    }
}