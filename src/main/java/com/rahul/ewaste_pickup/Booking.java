package com.rahul.ewaste_pickup;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "slot_id", nullable = false)
    private Slot slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.REQUESTED;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status { REQUESTED, CONFIRMED, CANCELLED, COMPLETED }

    public Booking() {}

    public Booking(User user, Slot slot) {
        this.user = user;
        this.slot = slot;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Slot getSlot() { return slot; }
    public void setSlot(Slot slot) { this.slot = slot; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; this.updatedAt = LocalDateTime.now(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
