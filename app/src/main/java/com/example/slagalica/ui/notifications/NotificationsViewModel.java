package com.example.slagalica.ui.notifications;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.domain.models.Notification;
import com.example.slagalica.domain.models.NotificationType;

import java.util.ArrayList;
import java.util.List;

public class NotificationsViewModel extends ViewModel {
    private final NotificationRepository repository;

    private final MutableLiveData<List<Notification>> notifications = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public NotificationsViewModel() {
        repository = new NotificationRepository();
    }

    public LiveData<List<Notification>> getNotifications() {
        return notifications;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadAllNotifications() {
        repository.getAllNotifications(new NotificationRepository.NotificationCallback() {
            @Override
            public void onResult(List<Notification> result) {
                notifications.setValue(result);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    public void loadUnreadNotifications() {
        repository.getUnreadNotifications(new NotificationRepository.NotificationCallback() {
            @Override
            public void onResult(List<Notification> result) {
                notifications.setValue(result);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    public void loadReadNotifications() {
        repository.getReadNotifications(new NotificationRepository.NotificationCallback() {
            @Override
            public void onResult(List<Notification> result) {
                notifications.setValue(result);
            }

            @Override
            public void onError(String error) {
                errorMessage.setValue(error);
            }
        });
    }

    public void markAsRead(Notification notification) {
        if (notification == null || notification.isRead()) {
            return;
        }

        repository.markAsRead(notification.getId());
    }

    public void markAllAsRead() {
        repository.markAllAsRead();
    }

    // test
    public void createTestNotification() {
        repository.createNotification(
                "Test notifikacija",
                "Ovo je probna notifikacija iz Realtime Database.",
                NotificationType.OTHER
        );
    }
}
