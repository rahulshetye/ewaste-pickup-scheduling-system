package com.rahul.ewaste_pickup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SlotRepository slotRepository;
    private final UserRepository userRepository;

    @Autowired
    public BookingService(BookingRepository bookingRepository, SlotRepository slotRepository, UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    public Booking createBooking(Long userId, Long slotId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

        if (!slot.isAvailable()) {
            throw new IllegalStateException("Slot is no longer available");
        }

        slot.setAvailable(false);
        slotRepository.save(slot);

        Booking booking = new Booking(user, slot);
        booking.setStatus(Booking.Status.CONFIRMED);
        return bookingRepository.save(booking);
    }

    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    }

    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    public Booking cancelBooking(Long bookingId) {
        Booking booking = getBooking(bookingId);

        if (booking.getStatus() == Booking.Status.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        booking.setStatus(Booking.Status.CANCELLED);
        Slot slot = booking.getSlot();
        slot.setAvailable(true);
        slotRepository.save(slot);

        return bookingRepository.save(booking);
    }
}
