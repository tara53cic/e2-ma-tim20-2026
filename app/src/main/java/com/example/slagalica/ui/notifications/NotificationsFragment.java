package com.example.slagalica.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.domain.models.Notification;
import com.example.slagalica.domain.models.NotificationType;

import java.util.List;
public class NotificationsFragment extends Fragment {
    private NotificationsViewModel viewModel;
    private NotificationsAdapter adapter;

    private TextView btnAllNotifications;
    private TextView btnUnreadNotifications;
    private TextView btnReadNotifications;
    private TextView tvEmptyNotifications;
    private RecyclerView rvNotifications;

    private FilterType currentFilter = FilterType.ALL;

    private enum FilterType {
        ALL,
        UNREAD,
        READ
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupViewModel();
        setupClickListeners();

        selectFilter(FilterType.ALL);
        viewModel.loadAllNotifications();
    }

    private void initViews(View view) {
        btnAllNotifications = view.findViewById(R.id.btnAllNotifications);
        btnUnreadNotifications = view.findViewById(R.id.btnUnreadNotifications);
        btnReadNotifications = view.findViewById(R.id.btnReadNotifications);
        tvEmptyNotifications = view.findViewById(R.id.tvEmptyNotifications);
        rvNotifications = view.findViewById(R.id.rvNotifications);
    }

    private void setupRecyclerView() {
        adapter = new NotificationsAdapter(notification -> {
            viewModel.markAsRead(notification);
            handleNotificationAction(notification);
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifications -> {
            adapter.setNotifications(notifications);
            updateEmptyState(notifications);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        btnAllNotifications.setOnClickListener(v -> {
            selectFilter(FilterType.ALL);
            viewModel.loadAllNotifications();
        });

        btnUnreadNotifications.setOnClickListener(v -> {
            selectFilter(FilterType.UNREAD);
            viewModel.loadUnreadNotifications();
        });

        btnReadNotifications.setOnClickListener(v -> {
            selectFilter(FilterType.READ);
            viewModel.loadReadNotifications();
        });
    }

    private void selectFilter(FilterType filterType) {
        currentFilter = filterType;

        setFilterButtonState(btnAllNotifications, filterType == FilterType.ALL);
        setFilterButtonState(btnUnreadNotifications, filterType == FilterType.UNREAD);
        setFilterButtonState(btnReadNotifications, filterType == FilterType.READ);
    }

    private void setFilterButtonState(TextView button, boolean selected) {
        if (selected) {
            button.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_selected));
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.background));
        } else {
            button.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_unselected));
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        }
    }

    private void updateEmptyState(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            tvEmptyNotifications.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);

            if (currentFilter == FilterType.UNREAD) {
                tvEmptyNotifications.setText("Nema nepročitanih notifikacija.");
            } else if (currentFilter == FilterType.READ) {
                tvEmptyNotifications.setText("Nema pročitanih notifikacija.");
            } else {
                tvEmptyNotifications.setText("Nema notifikacija.");
            }

        } else {
            tvEmptyNotifications.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private void handleNotificationAction(Notification notification) {
        if (notification == null || notification.getType() == null) {
            return;
        }

        NotificationType type = notification.getType();

        switch (type) {
            case CHAT:
                Toast.makeText(requireContext(), "Otvaranje čet notifikacije...", Toast.LENGTH_SHORT).show();
                break;

            case RANKING:
                Toast.makeText(requireContext(), "Otvaranje rang liste...", Toast.LENGTH_SHORT).show();
                break;

            case REWARDS:
                Toast.makeText(requireContext(), "Otvaranje nagrada...", Toast.LENGTH_SHORT).show();
                break;

            case OTHER:
            default:
                Toast.makeText(requireContext(), "Notifikacija označena kao pročitana.", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
