package com.example.slagalica.ui.notifications;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.domain.models.Notification;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {

    private final List<Notification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationsAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<Notification> newNotifications) {
        notifications.clear();

        if (newNotifications != null) {
            notifications.addAll(newNotifications);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);

        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        holder.tvDate.setText(formatDate(notification.getCreatedAt()));

        if (notification.isRead()) {
            holder.notificationCard.setBackground(
                    ContextCompat.getDrawable(
                            holder.itemView.getContext(),
                            R.drawable.bg_notification_read
                    )
            );

            holder.tvTitle.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_on_card_muted)
            );

            holder.tvMessage.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_on_card_muted)
            );

            holder.tvDate.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_on_card_muted)
            );

            holder.tvTitle.setTypeface(null, Typeface.NORMAL);

        } else {
            holder.notificationCard.setBackground(
                    ContextCompat.getDrawable(
                            holder.itemView.getContext(),
                            R.drawable.bg_notification_unread
                    )
            );

            holder.tvTitle.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.background)
            );

            holder.tvMessage.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_on_card)
            );

            holder.tvDate.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.secondary_dark)
            );

            holder.tvTitle.setTypeface(null, Typeface.BOLD);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private String formatDate(long timestamp) {
        if (timestamp == 0) {
            return "";
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.getDefault());
        return formatter.format(new Date(timestamp));
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        LinearLayout notificationCard;
        TextView tvTitle;
        TextView tvMessage;
        TextView tvDate;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            notificationCard = itemView.findViewById(R.id.notificationCard);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvDate = itemView.findViewById(R.id.tvNotificationDate);
        }
    }

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }
}
