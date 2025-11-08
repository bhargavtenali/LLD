enum ChannelType { EMAIL, SMS, PUSH }

class Notification {
    private String userId;
    private String message;
    private ChannelType channelType;
}

// Strategy for sending notifications
interface NotificationChannel {
    void send(Notification notification);
}

class EmailChannel implements NotificationChannel {
    public void send(Notification n) { /* SMTP logic */ }
}

class SMSChannel implements NotificationChannel {
    public void send(Notification n) { /* Twilio logic */ }
}

class PushChannel implements NotificationChannel {
    public void send(Notification n) { /* FCM logic */ }
}

// Factory to create channel
class NotificationFactory {
    public static NotificationChannel getChannel(ChannelType type) {
        return switch (type) {
            case EMAIL -> new EmailChannel();
            case SMS -> new SMSChannel();
            case PUSH -> new PushChannel();
        };
    }
}

// Async service layer
class NotificationService {
    private ExecutorService executor = Executors.newFixedThreadPool(10);

    public void sendNotification(Notification notification) {
        executor.submit(() -> {
            NotificationChannel channel = NotificationFactory.getChannel(notification.getChannelType());
            channel.send(notification);
        });
    }
}

public class Main {
    public static void main(String[] args) {
        NotificationService service = new NotificationService();
        Notification n = new Notification();
        n.setUserId("U123");
        n.setMessage("New episode released!");
        n.setChannelType(ChannelType.PUSH);

        service.sendNotification(n);
    }
}