package com.quyen.shoplite.controller;

import com.quyen.shoplite.domain.response.ResNotificationDTO;
import com.quyen.shoplite.service.NotificationService;
import com.quyen.shoplite.util.annotation.ApiMessage;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @ApiMessage("Get notifications success")
    public ResponseEntity<List<ResNotificationDTO>> findMine() {
        return ResponseEntity.ok(notificationService.findMine());
    }

    @PatchMapping("/{id}/read")
    @ApiMessage("Mark notification read success")
    public ResponseEntity<ResNotificationDTO> markRead(
            @PathVariable("id") @Positive(message = "id must be greater than 0") Long id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }
}
