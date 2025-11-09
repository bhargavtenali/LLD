import java.util.*;
import java.util.concurrent.*;

// -------------------- Enums --------------------

enum VehicleType { BIKE, CAR, TRUCK }

// -------------------- Vehicle Hierarchy --------------------

abstract class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    public Vehicle(String plate, VehicleType type) {
        this.licensePlate = plate;
        this.type = type;
    }

    public String getLicensePlate() { return licensePlate; }
    public VehicleType getType() { return type; }

    @Override
    public String toString() {
        return type + " [" + licensePlate + "]";
    }
}

class Car extends Vehicle {
    public Car(String plate) { super(plate, VehicleType.CAR); }
}

class Bike extends Vehicle {
    public Bike(String plate) { super(plate, VehicleType.BIKE); }
}

class Truck extends Vehicle {
    public Truck(String plate) { super(plate, VehicleType.TRUCK); }
}

// -------------------- Parking Slot --------------------

class ParkingSlot {
    private final int slotId;
    private final VehicleType supportedType;
    private boolean isOccupied;
    private Vehicle parkedVehicle;
    private final int distanceFromGate; // Used for scalable assignment

    public ParkingSlot(int slotId, VehicleType type, int distanceFromGate) {
        this.slotId = slotId;
        this.supportedType = type;
        this.distanceFromGate = distanceFromGate;
        this.isOccupied = false;
    }

    public int getSlotId() { return slotId; }
    public VehicleType getSupportedType() { return supportedType; }
    public boolean isOccupied() { return isOccupied; }
    public int getDistanceFromGate() { return distanceFromGate; }

    public boolean canPark(Vehicle v) {
        return !isOccupied && v.getType() == supportedType;
    }

    public void park(Vehicle v) {
        if (!canPark(v)) throw new IllegalStateException("Slot not available or type mismatch!");
        this.parkedVehicle = v;
        this.isOccupied = true;
    }

    public void leave() {
        this.parkedVehicle = null;
        this.isOccupied = false;
    }

    @Override
    public String toString() {
        return "Slot{" + slotId + ", type=" + supportedType + ", occupied=" + isOccupied + "}";
    }
}

// -------------------- Parking Level --------------------

class ParkingLevel {
    private final int levelId;
    private final List<ParkingSlot> slots;

    public ParkingLevel(int id, List<ParkingSlot> slots) {
        this.levelId = id;
        this.slots = slots;
    }

    public int getLevelId() { return levelId; }
    public List<ParkingSlot> getSlots() { return slots; }
}

// -------------------- Slot Manager (Scalable Assignment) --------------------

/**
 * Maintains PriorityQueues of free slots per VehicleType.
 * Uses distanceFromGate (or any heuristic) to decide nearest slot.
 * O(log N) assignment and release.
 */
class SlotManager {
    private final Map<VehicleType, PriorityQueue<ParkingSlot>> freeSlotMap = new ConcurrentHashMap<>();

    public SlotManager(List<ParkingLevel> levels) {
        Comparator<ParkingSlot> cmp = Comparator
                .comparingInt(ParkingSlot::getDistanceFromGate)
                .thenComparingInt(ParkingSlot::getSlotId);

        for (VehicleType type : VehicleType.values()) {
            freeSlotMap.put(type, new PriorityQueue<>(cmp));
        }

        // preload free slots
        for (ParkingLevel level : levels) {
            for (ParkingSlot s : level.getSlots()) {
                freeSlotMap.get(s.getSupportedType()).offer(s);
            }
        }
    }

    // Thread-safe allocation
    public synchronized ParkingSlot acquireSlot(Vehicle vehicle) {
        PriorityQueue<ParkingSlot> queue = freeSlotMap.get(vehicle.getType());
        while (!queue.isEmpty()) {
            ParkingSlot slot = queue.poll();
            if (!slot.isOccupied()) {
                slot.park(vehicle);
                return slot;
            }
        }
        return null; // no available slot
    }

    public synchronized void releaseSlot(ParkingSlot slot) {
        slot.leave();
        freeSlotMap.get(slot.getSupportedType()).offer(slot);
    }

    public int getAvailableCount(VehicleType type) {
        return freeSlotMap.get(type).size();
    }
}

// -------------------- Parking Ticket --------------------

class ParkingTicket {
    private final Vehicle vehicle;
    private final ParkingSlot slot;
    private final long entryTime;
    private long exitTime;
    private double amount;

    public ParkingTicket(Vehicle v, ParkingSlot slot) {
        this.vehicle = v;
        this.slot = slot;
        this.entryTime = System.currentTimeMillis();
    }

    public void closeTicket(double ratePerHour) {
        this.exitTime = System.currentTimeMillis();
        long durationHrs = Math.max(1, (exitTime - entryTime) / (1000 * 60 * 60));
        this.amount = durationHrs * ratePerHour;
    }

    public double getAmount() { return amount; }
    public Vehicle getVehicle() { return vehicle; }
    public ParkingSlot getSlot() { return slot; }

    @Override
    public String toString() {
        return "Ticket{" + vehicle + ", Slot=" + slot.getSlotId() + ", Amount=" + amount + "}";
    }
}

// -------------------- Parking Lot (Main Controller) --------------------

class ParkingLot {
    private final List<ParkingLevel> levels;
    private final SlotManager slotManager;
    private final Map<String, ParkingTicket> activeTickets = new ConcurrentHashMap<>();
    private static final double RATE_PER_HOUR = 50.0;

    public ParkingLot(List<ParkingLevel> levels) {
        this.levels = levels;
        this.slotManager = new SlotManager(levels);
    }

    // Park a vehicle and return ticket
    public synchronized ParkingTicket parkVehicle(Vehicle v) {
        ParkingSlot slot = slotManager.acquireSlot(v);
        if (slot == null) throw new RuntimeException("No available slots for type: " + v.getType());

        ParkingTicket ticket = new ParkingTicket(v, slot);
        activeTickets.put(v.getLicensePlate(), ticket);
        System.out.println(v + " parked at Slot " + slot.getSlotId());
        return ticket;
    }

    // Unpark and return amount
    public synchronized double unparkVehicle(Vehicle v) {
        ParkingTicket ticket = activeTickets.remove(v.getLicensePlate());
        if (ticket == null) throw new RuntimeException("No active ticket for vehicle " + v);

        ticket.closeTicket(RATE_PER_HOUR);
        slotManager.releaseSlot(ticket.getSlot());
        System.out.println(v + " left Slot " + ticket.getSlot().getSlotId() + " | Amount: ₹" + ticket.getAmount());
        return ticket.getAmount();
    }

    public void printAvailability() {
        for (VehicleType type : VehicleType.values()) {
            System.out.println(type + " free slots: " + slotManager.getAvailableCount(type));
        }
    }
}

// -------------------- Demo --------------------

public class ParkingLotSystem {
    public static void main(String[] args) {
        // --- Prepare Slots ---
        List<ParkingSlot> level1Slots = Arrays.asList(
                new ParkingSlot(1, VehicleType.CAR, 10),
                new ParkingSlot(2, VehicleType.CAR, 20),
                new ParkingSlot(3, VehicleType.BIKE, 5),
                new ParkingSlot(4, VehicleType.TRUCK, 15)
        );

        List<ParkingSlot> level2Slots = Arrays.asList(
                new ParkingSlot(5, VehicleType.CAR, 5),
                new ParkingSlot(6, VehicleType.CAR, 30),
                new ParkingSlot(7, VehicleType.BIKE, 10)
        );

        ParkingLevel level1 = new ParkingLevel(1, level1Slots);
        ParkingLevel level2 = new ParkingLevel(2, level2Slots);

        ParkingLot lot = new ParkingLot(Arrays.asList(level1, level2));

        // --- Park Vehicles ---
        Vehicle car1 = new Car("AP01AA1234");
        Vehicle car2 = new Car("AP02BB9999");
        Vehicle bike = new Bike("AP03CC5678");

        lot.printAvailability();

        ParkingTicket t1 = lot.parkVehicle(car1);
        ParkingTicket t2 = lot.parkVehicle(bike);
        ParkingTicket t3 = lot.parkVehicle(car2);

        lot.printAvailability();

        // --- Unpark ---
        lot.unparkVehicle(car1);
        lot.printAvailability();
    }
}
