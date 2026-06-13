package com.example.slagalica.data;

import androidx.annotation.NonNull;

import com.example.slagalica.domain.models.Notification;
import com.example.slagalica.domain.models.NotificationType;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private final DatabaseReference notificationsRef;
    private final FirebaseAuth auth;

    public NotificationRepository() {
        auth = FirebaseAuth.getInstance();
        notificationsRef = FirebaseDatabase
                .getInstance("https://slagalica-837b3-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("notifications");
    }

    private String getCurrentUserId() {
        if (auth.getCurrentUser() == null) {
            return null;
        }
        return auth.getCurrentUser().getUid();
    }

    public void createNotification(String title, String message, NotificationType type) {
        String userId = getCurrentUserId();

        android.util.Log.e("NOTIFICATIONS", "createNotification called");

        if (userId == null) {
            android.util.Log.e("NOTIFICATIONS", "User is null. Notification not created.");
            return;
        }

        android.util.Log.e("NOTIFICATIONS", "Current user id: " + userId);

        DatabaseReference userNotificationsRef = notificationsRef.child(userId);
        String id = userNotificationsRef.push().getKey();

        if (id == null) {
            android.util.Log.e("NOTIFICATIONS", "Generated notification id is null.");
            return;
        }

        Notification notification = new Notification(
                id,
                title,
                message,
                type,
                false,
                System.currentTimeMillis()
        );

        userNotificationsRef.child(id)
                .setValue(notification)
                .addOnSuccessListener(unused -> {
                    android.util.Log.e("NOTIFICATIONS", "Notification created successfully.");
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NOTIFICATIONS", "Error creating notification: " + e.getMessage());
                });
    }

    public void getAllNotifications(NotificationCallback callback) {
        String userId = getCurrentUserId();

        if (userId == null) {
            callback.onResult(new ArrayList<>());
            return;
        }

        notificationsRef.child(userId)
                .orderByChild("createdAt")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Notification> notifications = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Notification notification = child.getValue(Notification.class);

                            if (notification != null) {
                                notifications.add(0, notification);
                            }
                        }

                        callback.onResult(notifications);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void getUnreadNotifications(NotificationCallback callback) {
        getNotificationsByReadStatus(false, callback);
    }

    public void getReadNotifications(NotificationCallback callback) {
        getNotificationsByReadStatus(true, callback);
    }

    private void getNotificationsByReadStatus(boolean read, NotificationCallback callback) {
        String userId = getCurrentUserId();

        if (userId == null) {
            callback.onResult(new ArrayList<>());
            return;
        }

        notificationsRef.child(userId)
                .orderByChild("read")
                .equalTo(read)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Notification> notifications = new ArrayList<>();

                        for (DataSnapshot child : snapshot.getChildren()) {
                            Notification notification = child.getValue(Notification.class);

                            if (notification != null) {
                                notifications.add(0, notification);
                            }
                        }

                        callback.onResult(notifications);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void markAsRead(String notificationId) {
        String userId = getCurrentUserId();

        if (userId == null || notificationId == null) {
            return;
        }

        notificationsRef.child(userId)
                .child(notificationId)
                .child("read")
                .setValue(true);
    }

    public void markAllAsRead() {
        String userId = getCurrentUserId();

        if (userId == null) {
            return;
        }

        notificationsRef.child(userId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        child.getRef().child("read").setValue(true);
                    }
                });
    }

    public interface NotificationCallback {
        void onResult(List<Notification> notifications);

        void onError(String errorMessage);
    }

    public interface NotificationCountCallback {
        void onCountChanged(int count);
    }

    public void observeUnreadCount(NotificationCountCallback callback) {
        String userId = getCurrentUserId();

        if (userId == null) {
            callback.onCountChanged(0);
            return;
        }

        notificationsRef.child(userId)
                .orderByChild("read")
                .equalTo(false)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int count = 0;

                        for (DataSnapshot ignored : snapshot.getChildren()) {
                            count++;
                        }

                        callback.onCountChanged(count);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onCountChanged(0);
                    }
                });
    }
}
