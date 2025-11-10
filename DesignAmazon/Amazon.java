enum OrderStatus { PLACED, SHIPPED, DELIVERED, CANCELLED }

class Product {
    private String id;
    private String name;
    private double price;
    private int stock;
}

class CartItem {
    private Product product;
    private int quantity;
}

class Cart {
    private String userId;
    private List<CartItem> items = new ArrayList<>();
    public void addItem(Product p, int qty) {}
    public void removeItem(Product p) {}
    public double getTotalAmount() { return 0.0; }
}

class Order {
    private String orderId;
    private List<CartItem> items;
    private double total;
    private OrderStatus status;
}

class ProductService {
    private Map<String, Product> catalog = new HashMap<>();
    public Product getProduct(String id) { return catalog.get(id); }
}

class OrderService {
    public Order placeOrder(Cart cart) {
        Order order = new Order();
        order.setItems(cart.getItems());
        order.setTotal(cart.getTotalAmount());
        order.setStatus(OrderStatus.PLACED);
        return order;
    }
}
