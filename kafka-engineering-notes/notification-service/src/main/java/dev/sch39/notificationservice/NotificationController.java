package dev.sch39.notificationservice;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class NotificationController {

    private final NotificationStore store;

    NotificationController(NotificationStore store) {
        this.store = store;
    }

    @GetMapping("/notifications")
    List<Notification> notifications() {
        return store.all();
    }
}