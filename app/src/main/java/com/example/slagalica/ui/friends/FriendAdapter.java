package com.example.slagalica.ui.friends;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.domain.models.Friend;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    public interface OnFriendActionListener {
        void onAction(Friend friend);
    }

    private static final int[] AVATAR_COLORS = {
            0xFF508EFA, 0xFF2EC27E, 0xFFE53935,
            0xFFF4A261, 0xFF9C27B0, 0xFF00BCD4
    };

    private static final int[] AVATAR_ICONS = {
            R.drawable.ic_avatar_1, R.drawable.ic_avatar_2,
            R.drawable.ic_avatar_3, R.drawable.ic_avatar_4,
            R.drawable.ic_avatar_5, R.drawable.ic_avatar_6
    };

    private static final String[] LEAGUE_NAMES = {
            "Početna", "Bronzana", "Srebrna", "Zlatna", "Platinasta", "Dijamantska"
    };

    private static final int[] LEAGUE_ICONS = {
            R.drawable.ic_league_0, R.drawable.ic_league_1,
            R.drawable.ic_league_2, R.drawable.ic_league_3,
            R.drawable.ic_league_4, R.drawable.ic_league_5
    };

    private final List<Friend> items = new ArrayList<>();
    private final OnFriendActionListener primaryListener;
    private final OnFriendActionListener removeListener;
    private final boolean showRemoveButton; // true = lista prijatelja, false = rezultati pretrage

    public FriendAdapter(OnFriendActionListener primaryListener,
                         OnFriendActionListener removeListener,
                         boolean showRemoveButton) {
        this.primaryListener = primaryListener;
        this.removeListener = removeListener;
        this.showRemoveButton = showRemoveButton;
    }

    public void setItems(List<Friend> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        Friend friend = items.get(position);

        holder.tvUsername.setText(friend.getUsername() != null ? friend.getUsername() : "?");
        holder.tvStars.setText(String.valueOf(friend.getStars()));

        int leagueIdx = Math.max(0, Math.min(friend.getLeague(), LEAGUE_NAMES.length - 1));
        holder.tvLeague.setText(LEAGUE_NAMES[leagueIdx]);
        holder.ivLeague.setImageResource(LEAGUE_ICONS[leagueIdx]);

        int avatarIdx = Math.max(0, Math.min(friend.getAvatarColorIndex(), AVATAR_ICONS.length - 1));
        holder.ivAvatar.setImageResource(AVATAR_ICONS[avatarIdx]);
        holder.ivAvatar.setBackgroundTintList(ColorStateList.valueOf(AVATAR_COLORS[avatarIdx]));

        if (showRemoveButton) {
            holder.btnAction.setText("Ukloni");
            holder.btnAction.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onAction(friend);
            });
        } else {
            holder.btnAction.setText("Dodaj");
            holder.btnAction.setOnClickListener(v -> {
                if (primaryListener != null) primaryListener.onAction(friend);
            });
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvUsername;
        TextView tvStars;
        TextView tvLeague;
        ImageView ivLeague;
        MaterialButton btnAction;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivFriendAvatar);
            tvUsername = itemView.findViewById(R.id.tvFriendUsername);
            tvStars = itemView.findViewById(R.id.tvFriendStars);
            tvLeague = itemView.findViewById(R.id.tvFriendLeague);
            ivLeague = itemView.findViewById(R.id.ivFriendLeague);
            btnAction = itemView.findViewById(R.id.btnFriendAction);
        }
    }
}