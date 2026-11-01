package dev.sch39.notificationservice;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

@Component
class NotificationStore {

    private final List<Notification> items = new CopyOnWriteArrayList<>();

    void add(Notification notification) {
        items.add(notification);
    }

    List<Notification> all() {
        return List.copyOf(items);
    }
}