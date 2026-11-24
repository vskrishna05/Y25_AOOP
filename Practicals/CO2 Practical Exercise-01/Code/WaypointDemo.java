import java.util.*;

// ---------- TYPED IDENTIFIERS ----------

record CustomerId(int value) {
    public CustomerId {
        if (value <= 0) throw new IllegalArgumentException("Invalid customer ID");
    }
}

record BookingId(int value) {
    public BookingId {
        if (value <= 0) throw new IllegalArgumentException("Invalid booking ID");
    }
}

record ParkingSpotId(int value) {
    public ParkingSpotId {
        if (value <= 0) throw new IllegalArgumentException("Invalid parking spot ID");
    }
}

record PaymentId(int value) {
    public PaymentId {
        if (value <= 0) throw new IllegalArgumentException("Invalid payment ID");
    }
}

// ---------- MONEY VALUE OBJECT ----------

record Money(long paise, String currency) {
    public Money {
        if (paise < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        currency = currency.toUpperCase(Locale.ROOT);
    }

    public Money times(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return new Money(Math.multiplyExact(paise, quantity), currency);
    }

    @Override
    public String toString() {
        return String.format(
            Locale.ROOT, "%s %d.%02d",
            currency, paise / 100, paise % 100
        );
    }
}

// ---------- CUSTOMER ----------

class Customer {
    private final CustomerId customerId;
    private final String name;

    public Customer(CustomerId customerId, String name) {
        if (customerId == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Invalid customer details");
        }
        this.customerId = customerId;
        this.name = name;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }
}

// ---------- PARKING SPOT ----------

enum SpotStatus {
    AVAILABLE, OCCUPIED
}

record ParkingSpotView(
    ParkingSpotId spotId,
    String location,
    String spotType,
    SpotStatus status,
    String vehicleNumber
) {}

// Sealed abstract class: only permitted subclasses can extend it.
sealed abstract class ParkingSpot
    permits CarParkingSpot, BikeParkingSpot, ElectricParkingSpot {

    private final ParkingSpotId spotId;
    private final String location;
    private SpotStatus status;
    private String vehicleNumber;

    public ParkingSpot(ParkingSpotId spotId, String location) {
        if (spotId == null || location == null || location.isBlank()) {
            throw new IllegalArgumentException("Invalid parking spot details");
        }
        this.spotId = spotId;
        this.location = location;
        this.status = SpotStatus.AVAILABLE;
        this.vehicleNumber = null;
    }

    protected abstract Money getHourlyRate();

    public abstract String getSpotType();

    public final Money calculateFare(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be positive");
        }
        return getHourlyRate().times(hours);
    }

    public final boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }

    public final void parkVehicle(String vehicleNumber) {
        if (!isAvailable()) {
            throw new IllegalStateException("Parking spot is already occupied");
        }
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("Vehicle number is required");
        }
        this.vehicleNumber = vehicleNumber;
        this.status = SpotStatus.OCCUPIED;
    }

    public final void releaseSpot() {
        if (isAvailable()) {
            throw new IllegalStateException("Parking spot is already available");
        }
        this.vehicleNumber = null;
        this.status = SpotStatus.AVAILABLE;
    }

    public final ParkingSpotId getSpotId() {
        return spotId;
    }

    public final ParkingSpotView getSnapshot() {
        return new ParkingSpotView(
            spotId, location, getSpotType(), status, vehicleNumber
        );
    }
}

// ---------- PARKING SPOT SUBCLASSES ----------

final class CarParkingSpot extends ParkingSpot {
    public CarParkingSpot(ParkingSpotId id, String location) {
        super(id, location);
    }

    @Override
    protected Money getHourlyRate() {
        return new Money(5000, "INR");
    }

    @Override
    public String getSpotType() {
        return "CAR";
    }
}

final class BikeParkingSpot extends ParkingSpot {
    public BikeParkingSpot(ParkingSpotId id, String location) {
        super(id, location);
    }

    @Override
    protected Money getHourlyRate() {
        return new Money(2000, "INR");
    }

    @Override
    public String getSpotType() {
        return "BIKE";
    }
}

final class ElectricParkingSpot extends ParkingSpot {
    public ElectricParkingSpot(ParkingSpotId id, String location) {
        super(id, location);
    }

    @Override
    protected Money getHourlyRate() {
        return new Money(7000, "INR");
    }

    @Override
    public String getSpotType() {
        return "ELECTRIC VEHICLE";
    }
}

// ---------- PAYMENT ROLE SEAM ----------

record PaymentReceipt(
    PaymentId paymentId,
    BookingId bookingId,
    Money amount,
    boolean successful
) {
    public PaymentReceipt {
        if (paymentId == null || bookingId == null || amount == null) {
            throw new IllegalArgumentException("Invalid payment receipt");
        }
    }
}

interface PaymentMethod {
    PaymentReceipt pay(PaymentId paymentId, BookingId bookingId, Money amount);
}

class UPIPayment implements PaymentMethod {
    @Override
    public PaymentReceipt pay(
        PaymentId paymentId, BookingId bookingId, Money amount
    ) {
        System.out.println("Processing UPI payment: " + amount);
        return new PaymentReceipt(paymentId, bookingId, amount, true);
    }
}

class CardPayment implements PaymentMethod {
    @Override
    public PaymentReceipt pay(
        PaymentId paymentId, BookingId bookingId, Money amount
    ) {
        System.out.println("Processing Card payment: " + amount);
        return new PaymentReceipt(paymentId, bookingId, amount, true);
    }
}

// ---------- BOOKING ----------

enum BookingStatus {
    PENDING, CONFIRMED, CANCELLED
}

record BookingView(
    BookingId bookingId,
    CustomerId customerId,
    ParkingSpotId spotId,
    String vehicleNumber,
    Money fare,
    BookingStatus status
) {}

class Booking {
    private final BookingId bookingId;
    private final CustomerId customerId;
    private final ParkingSpotId spotId;
    private final String vehicleNumber;
    private final Money fare;

    private BookingStatus status;
    private PaymentReceipt paymentReceipt;

    public Booking(
        BookingId bookingId,
        CustomerId customerId,
        ParkingSpotId spotId,
        String vehicleNumber,
        Money fare
    ) {
        if (bookingId == null || customerId == null ||
            spotId == null || vehicleNumber == null ||
            vehicleNumber.isBlank() || fare == null) {
            throw new IllegalArgumentException("Invalid booking details");
        }

        this.bookingId = bookingId;
        this.customerId = customerId;
        this.spotId = spotId;
        this.vehicleNumber = vehicleNumber;
        this.fare = fare;
        this.status = BookingStatus.PENDING;
        this.paymentReceipt = null;
    }

    public void confirm(PaymentReceipt receipt) {
        if (status != BookingStatus.PENDING) {
            throw new IllegalStateException("Booking is not pending");
        }
        if (receipt == null || !receipt.successful()) {
            throw new IllegalArgumentException("Payment failed");
        }
        if (!receipt.bookingId().equals(bookingId) ||
            !receipt.amount().equals(fare)) {
            throw new IllegalArgumentException("Payment receipt mismatch");
        }

        this.paymentReceipt = receipt;
        this.status = BookingStatus.CONFIRMED;
    }

    public void cancel() {
        if (status != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be cancelled");
        }
        this.status = BookingStatus.CANCELLED;
    }

    public BookingId getBookingId() {
        return bookingId;
    }

    public ParkingSpotId getSpotId() {
        return spotId;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public PaymentReceipt getPaymentReceipt() {
        return paymentReceipt;
    }

    public BookingView getSnapshot() {
        return new BookingView(
            bookingId, customerId, spotId, vehicleNumber, fare, status
        );
    }
}

// ---------- WAYPOINT SERVICE ----------

class WaypointService {
    private final Map<CustomerId, Customer> customers = new HashMap<>();
    private final Map<ParkingSpotId, ParkingSpot> spots = new HashMap<>();
    private final Map<BookingId, Booking> bookings = new HashMap<>();
    private final Set<String> registeredVehicles = new HashSet<>();

    private final PaymentMethod paymentMethod;

    private int bookingCounter = 1;
    private int paymentCounter = 1;

    public WaypointService(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method is required");
        }
        this.paymentMethod = paymentMethod;
    }

    public void registerCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer is required");
        }
        if (customers.containsKey(customer.getCustomerId())) {
            throw new IllegalArgumentException("Customer already registered");
        }
        customers.put(customer.getCustomerId(), customer);
    }

    public void addParkingSpot(ParkingSpot spot) {
        if (spot == null) {
            throw new IllegalArgumentException("Parking spot is required");
        }
        if (spots.containsKey(spot.getSpotId())) {
            throw new IllegalArgumentException("Parking spot already exists");
        }
        spots.put(spot.getSpotId(), spot);
    }

    public void registerVehicle(String vehicleNumber) {
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("Vehicle number is required");
        }
        String normalized = vehicleNumber.trim().toUpperCase(Locale.ROOT);
        if (!registeredVehicles.add(normalized)) {
            throw new IllegalArgumentException("Vehicle already registered");
        }
    }

    public List<ParkingSpotView> getParkingSpots() {
        List<ParkingSpotView> result = new ArrayList<>();
        for (ParkingSpot spot : spots.values()) {
            result.add(spot.getSnapshot());
        }
        return List.copyOf(result);
    }

    // Preconditions:
    // 1. Customer must exist.
    // 2. Vehicle must be registered.
    // 3. Hours must be positive.
    // 4. Parking spot must exist and be available.
    //
    // Postconditions:
    // 1. Booking must be CONFIRMED.
    // 2. Payment receipt must be attached.
    // 3. Parking spot must be OCCUPIED.

    public Booking reserve(
        CustomerId customerId,
        ParkingSpotId spotId,
        String vehicleNumber,
        int hours
    ) {
        if (!customers.containsKey(customerId)) {
            throw new IllegalArgumentException("Customer not found");
        }
        if (hours <= 0) {
            throw new IllegalArgumentException("Hours must be positive");
        }
        if (vehicleNumber == null || vehicleNumber.isBlank()) {
            throw new IllegalArgumentException("Vehicle number is required");
        }

        String normalizedVehicle =
            vehicleNumber.trim().toUpperCase(Locale.ROOT);

        if (!registeredVehicles.contains(normalizedVehicle)) {
            throw new IllegalArgumentException("Vehicle is not registered");
        }

        ParkingSpot spot = spots.get(spotId);
        if (spot == null) {
            throw new IllegalArgumentException("Parking spot not found");
        }
        if (!spot.isAvailable()) {
            throw new IllegalStateException("Parking spot is not available");
        }

        Money fare = spot.calculateFare(hours);
        BookingId bookingId = new BookingId(bookingCounter++);
        PaymentId paymentId = new PaymentId(paymentCounter++);

        boolean vehicleParked = false;

        try {
            spot.parkVehicle(normalizedVehicle);
            vehicleParked = true;

            Booking booking = new Booking(
                bookingId, customerId, spotId, normalizedVehicle, fare
            );

            PaymentReceipt receipt =
                paymentMethod.pay(paymentId, bookingId, fare);

            booking.confirm(receipt);
            bookings.put(bookingId, booking);
            return booking;

        } catch (RuntimeException exception) {
            if (vehicleParked) {
                spot.releaseSpot();
            }
            throw exception;
        }
    }

    public void cancelBooking(BookingId bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        booking.cancel();
        ParkingSpot spot = spots.get(booking.getSpotId());
        spot.releaseSpot();
    }

    public List<BookingView> getBookings() {
        List<BookingView> result = new ArrayList<>();
        for (Booking booking : bookings.values()) {
            result.add(booking.getSnapshot());
        }
        return List.copyOf(result);
    }

    public void displayBookings() {
        System.out.println("\n----- BOOKING DETAILS -----");
        for (BookingView booking : getBookings()) {
            System.out.println(
                "Booking ID: " + booking.bookingId() +
                " | Customer ID: " + booking.customerId() +
                " | Spot ID: " + booking.spotId() +
                " | Vehicle: " + booking.vehicleNumber() +
                " | Fare: " + booking.fare() +
                " | Status: " + booking.status()
            );
        }
    }
}

// ---------- MAIN CLASS ----------

public class WaypointDemo {
    public static void main(String[] args) {
        // Role seam: UPI payment can be replaced with Card payment.
        PaymentMethod payment = new UPIPayment();

        // To use card payment, replace the previous line with:
        // PaymentMethod payment = new CardPayment();

        WaypointService waypoint = new WaypointService(payment);

        // Register customers
        Customer customer1 = new Customer(new CustomerId(101), "Ramya");
        Customer customer2 = new Customer(new CustomerId(102), "Sateesh");

        waypoint.registerCustomer(customer1);
        waypoint.registerCustomer(customer2);

        // Add different parking spot types
        waypoint.addParkingSpot(
            new CarParkingSpot(new ParkingSpotId(201), "Bachupally")
        );
        waypoint.addParkingSpot(
            new BikeParkingSpot(new ParkingSpotId(202), "Nizampet")
        );
        waypoint.addParkingSpot(
            new ElectricParkingSpot(new ParkingSpotId(203), "Bowrampet")
        );

        // Register vehicles
        waypoint.registerVehicle("TS09AB1234");
        waypoint.registerVehicle("TS10CD5678");
        waypoint.registerVehicle("TS11EF9012");

        // Display initial parking availability
        System.out.println("----- INITIAL PARKING AVAILABILITY -----");
        for (ParkingSpotView spot : waypoint.getParkingSpots()) {
            System.out.println(
                "Spot: " + spot.spotId() +
                " | Location: " + spot.location() +
                " | Type: " + spot.spotType() +
                " | Status: " + spot.status()
            );
        }

        // Reserve a car parking spot for 2 hours
        System.out.println("\n----- CAR BOOKING -----");
        Booking booking1 = waypoint.reserve(
            new CustomerId(101),
            new ParkingSpotId(201),
            "TS09AB1234",
            2
        );

        System.out.println("Booking ID: " + booking1.getBookingId());
        System.out.println("Payment: " + booking1.getPaymentReceipt().amount());
        System.out.println("Status: " + booking1.getStatus());

        // Reserve a bike parking spot for 3 hours
        System.out.println("\n----- BIKE BOOKING -----");
        Booking booking2 = waypoint.reserve(
            new CustomerId(102),
            new ParkingSpotId(202),
            "TS10CD5678",
            3
        );

        System.out.println("Booking ID: " + booking2.getBookingId());
        System.out.println("Payment: " + booking2.getPaymentReceipt().amount());
        System.out.println("Status: " + booking2.getStatus());

        // Attempt to book an already occupied parking spot
        System.out.println("\n----- INVALID BOOKING TEST -----");
        try {
            waypoint.reserve(
                new CustomerId(102),
                new ParkingSpotId(201),
                "TS10CD5678",
                1
            );
        } catch (IllegalStateException exception) {
            System.out.println("Booking rejected: " + exception.getMessage());
        }

        // Cancel the first booking
        System.out.println("\n----- CANCELLATION -----");
        waypoint.cancelBooking(booking1.getBookingId());
        System.out.println(
            "Booking " + booking1.getBookingId() +
            " status: " + booking1.getStatus()
        );

        // Display final parking availability
        System.out.println("\n----- FINAL PARKING AVAILABILITY -----");
        for (ParkingSpotView spot : waypoint.getParkingSpots()) {
            System.out.println(
                "Spot: " + spot.spotId() +
                " | Location: " + spot.location() +
                " | Type: " + spot.spotType() +
                " | Status: " + spot.status() +
                " | Vehicle: " + spot.vehicleNumber()
            );
        }

        // Display all bookings
        waypoint.displayBookings();
    }
}