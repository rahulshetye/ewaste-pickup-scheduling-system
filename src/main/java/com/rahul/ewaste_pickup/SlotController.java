package com.rahul.ewaste_pickup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SlotController {

    private final SlotRepository slotRepository;

    @Autowired
    public SlotController(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    @GetMapping("/slots")
    public List<Slot> getAvailableSlots() {
        return slotRepository.findByAvailableTrue();
    }

    @PostMapping("/admin/slots")
    public ResponseEntity<Slot> createSlot(@RequestBody Slot slot) {
        Slot saved = slotRepository.save(slot);
        return ResponseEntity.ok(saved);
    }
}
