// Payment Request & Response
class PaymentRequest {
    private String userId;
    private double amount;
    private String currency;
}

class PaymentResponse {
    private boolean success;
    private String transactionId;
    private String message;
}

// Strategy Pattern for payment gateways
interface PaymentGateway {
    PaymentResponse processPayment(PaymentRequest request);
}

class StripeGateway implements PaymentGateway {
    public PaymentResponse processPayment(PaymentRequest request) {
        // Call Stripe API
        return new PaymentResponse();
    }
}

class PayPalGateway implements PaymentGateway {
    public PaymentResponse processPayment(PaymentRequest request) {
        // Call PayPal API
        return new PaymentResponse();
    }
}

// Factory Pattern for selecting gateway
class PaymentGatewayFactory {
    public static PaymentGateway getGateway(String type) {
        return switch (type.toLowerCase()) {
            case "stripe" -> new StripeGateway();
            case "paypal" -> new PayPalGateway();
            default -> throw new IllegalArgumentException("Unknown gateway");
        };
    }
}

// PaymentService as entry point
class PaymentService {
    public PaymentResponse makePayment(String gatewayType, PaymentRequest request) {
        PaymentGateway gateway = PaymentGatewayFactory.getGateway(gatewayType);
        return gateway.processPayment(request);
    }
}

public class Main {
    public static void main(String[] args) {
        PaymentService service = new PaymentService();
        PaymentRequest req = new PaymentRequest();
        req.setAmount(50.0);
        req.setCurrency("USD");

        PaymentResponse res = service.makePayment("stripe", req);
        System.out.println("Payment Success: " + res.isSuccess());
    }
}