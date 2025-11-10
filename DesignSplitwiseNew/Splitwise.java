class User {
    private String id;
    private String name;
}

class Expense {
    private String description;
    private double amount;
    private User paidBy;
    private Map<User, Double> shareMap; // who owes how much
}

class Group {
    private String groupId;
    private List<User> members;
    private List<Expense> expenses;
    private Map<User, Double> balanceSheet = new HashMap<>();

    public void addExpense(Expense e) {
        expenses.add(e);
        for (var entry : e.getShareMap().entrySet()) {
            User user = entry.getKey();
            double share = entry.getValue();
            balanceSheet.put(user, balanceSheet.getOrDefault(user, 0.0) - share);
        }
        balanceSheet.put(e.getPaidBy(),
                balanceSheet.getOrDefault(e.getPaidBy(), 0.0) + e.getAmount());
    }
}

class SplitwiseService {
    private Map<String, Group> groups = new HashMap<>();

    public void addGroup(Group g) { groups.put(g.getGroupId(), g); }
    public void showBalances(String groupId) {
        Group g = groups.get(groupId);
        g.getBalanceSheet().forEach((u, b) ->
                System.out.println(u.getName() + " => " + b));
    }
}
public class Main {
    public static void main(String[] args) {
        User u1 = new User("U1", "Bhargav");
        User u2 = new User("U2", "Ravi");

        Group g = new Group("AnimeTrip", List.of(u1, u2));
        Expense e1 = new Expense("Tickets", 1000, u1, Map.of(u1, 500.0, u2, 500.0));

        g.addExpense(e1);

        SplitwiseService s = new SplitwiseService();
        s.addGroup(g);
        s.showBalances("AnimeTrip");
    }
}