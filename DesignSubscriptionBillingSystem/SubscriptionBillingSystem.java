import java.util.*;

enum SubscriptionStatus {
    ACTIVE, CANCELLED, PAUSED, EXPIRED;
}

// ----- Feature Class -----
class Feature {
    private String name;
    private String description;

    public Feature(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}

// ----- SubscriptionPlan Class -----
class SubscriptionPlan {
    private String planId;
    private String name;
    private double monthlyCost;
    private List<Feature> features;

    public SubscriptionPlan(String planId, String name, double monthlyCost, List<Feature> features) {
        this.planId = planId;
        this.name = name;
        this.monthlyCost = monthlyCost;
        this.features = features;
    }

    public String getPlanId() { return planId; }
    public String getName() { return name; }
    public double getMonthlyCost() { return monthlyCost; }
    public List<Feature> getFeatures() { return features; }
}

// ----- Subscription Class -----
class Subscription {
    private String subscriptionId;
    private SubscriptionPlan plan;
    private Date startDate;
    private Date endDate;
    private SubscriptionStatus status;

    public Subscription(SubscriptionPlan plan) {
        this.subscriptionId = UUID.randomUUID().toString();
        this.plan = plan;
        this.startDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.MONTH, 1);
        this.endDate = cal.getTime();
        this.status = SubscriptionStatus.ACTIVE;
    }

    public SubscriptionPlan getPlan() { return plan; }
    public SubscriptionStatus getStatus() { return status; }
    public Date getEndDate() { return endDate; }

    public void renew() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.endDate);
        cal.add(Calendar.MONTH, 1);
        this.endDate = cal.getTime();
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void cancel() { this.status = SubscriptionStatus.CANCELLED; }
    public void pause() { this.status = SubscriptionStatus.PAUSED; }
}

// ----- PaymentProcessor Abstraction -----
interface PaymentProcessor {
    boolean processPayment(double amount);
}

// ----- Example Implementation -----
class CreditCardProcessor implements PaymentProcessor {
    public boolean processPayment(double amount) {
        System.out.println("Processing payment of ₹" + amount + " via CreditCard...");
        return true; // simulate success
    }
}

// ----- User Class -----
class User {
    private String userId;
    private String name;
    private Subscription subscription;  // direct reference to subscription

    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public Subscription getSubscription() { return subscription; }
    public String getName() { return name; }
}

// ----- SubscriptionService -----
class SubscriptionService {
    private PaymentProcessor paymentProcessor;

    public SubscriptionService(PaymentProcessor processor) {
        this.paymentProcessor = processor;
    }

    public Subscription createSubscription(User user, SubscriptionPlan plan) {
        boolean success = paymentProcessor.processPayment(plan.getMonthlyCost());
        if (!success) throw new RuntimeException("Payment failed");

        Subscription subscription = new Subscription(plan);
        user.setSubscription(subscription);  // ✅ link subscription directly to user

        System.out.println("Subscription created for " + user.getName() + " on plan: " + plan.getName());
        return subscription;
    }

    public void renewSubscription(User user) {
        Subscription sub = user.getSubscription();
        if (sub == null) {
            System.out.println("User has no active subscription to renew.");
            return;
        }
        boolean success = paymentProcessor.processPayment(sub.getPlan().getMonthlyCost());
        if (success) {
            sub.renew();
            System.out.println("Subscription renewed until: " + sub.getEndDate());
        }
    }

    public void cancelSubscription(User user) {
        Subscription sub = user.getSubscription();
        if (sub != null) {
            sub.cancel();
            System.out.println("Subscription cancelled for " + user.getName());
        }
    }
}

// ----- Demo -----
public class SubscriptionSystemDemo {
    public static void main(String[] args) {
        PaymentProcessor processor = new CreditCardProcessor();
        SubscriptionService service = new SubscriptionService(processor);

        // Define features and plan
        List<Feature> features = List.of(
                new Feature("HD Streaming", "Stream in 1080p resolution"),
                new Feature("Offline Mode", "Download and watch later")
        );

        SubscriptionPlan premiumPlan = new SubscriptionPlan("P1", "Premium", 499.0, features);

        // Create user
        User user = new User("U1", "Bhargav");

        // User buys subscription
        Subscription sub = service.createSubscription(user, premiumPlan);
        System.out.println("User " + user.getName() + " has subscription status: " + sub.getStatus());

        // Renew subscription
        service.renewSubscription(user);

        // Cancel subscription
        service.cancelSubscription(user);
    }
}
