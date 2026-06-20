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

    public interface OnFriendActionListener { void onAction(Friend friend); }

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
    private final OnFriendActionListener addListener;
    private final OnFriendActionListener removeListener;
    private final OnFriendActionListener playListener;
    private final boolean showFriendButtons;

    public FriendAdapter(OnFriendActionListener addListener,
                         OnFriendActionListener removeListener,
                         OnFriendActionListener playListener,
                         boolean showFriendButtons) {
        this.addListener = addListener;
        this.removeListener = removeListener;
        this.playListener = playListener;
        this.showFriendButtons = showFriendButtons;
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
        holder.tvStars.setText("⭐ " + friend.getStars());

        int leagueIdx = Math.max(0, Math.min(friend.getLeague(), LEAGUE_NAMES.length - 1));
        holder.tvLeague.setText(LEAGUE_NAMES[leagueIdx]);
        holder.ivLeague.setImageResource(LEAGUE_ICONS[leagueIdx]);

        int avatarIdx = Math.max(0, Math.min(friend.getAvatarColorIndex(), AVATAR_ICONS.length - 1));
        holder.ivAvatar.setImageResource(AVATAR_ICONS[avatarIdx]);
        holder.ivAvatar.setBackgroundTintList(ColorStateList.valueOf(AVATAR_COLORS[avatarIdx]));

        if (holder.tvMonthlyRank != null) {
            if (friend.getMonthlyRank() > 0) {
                holder.tvMonthlyRank.setText("🏆 #" + friend.getMonthlyRank() + " mesečno");
            } else {
                holder.tvMonthlyRank.setText("Bez ranga");
                holder.tvMonthlyRank.setTextColor(0xFF607D8B);
            }
        }

        if (holder.tvOnlineStatus != null) {
            if (friend.isOnline()) {
                holder.tvOnlineStatus.setText("● Online");
                holder.tvOnlineStatus.setTextColor(0xFF2EC27E);
            } else {
                holder.tvOnlineStatus.setText("● Offline");
                holder.tvOnlineStatus.setTextColor(0xFF607D8B);
            }
        }

        if (showFriendButtons) {
            if (holder.btnPlay != null) {
                holder.btnPlay.setVisibility(View.VISIBLE);
                if (friend.canPlay()) {
                    holder.btnPlay.setAlpha(1f);
                    holder.btnPlay.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF508EFA));
                    holder.btnPlay.setOnClickListener(v -> {
                        if (playListener != null) playListener.onAction(friend);
                    });
                } else {
                    holder.btnPlay.setAlpha(0.4f);
                    holder.btnPlay.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(0xFF607D8B));
                    holder.btnPlay.setOnClickListener(null);
                }
            }
            holder.btnAction.setText("Ukloni");
            holder.btnAction.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onAction(friend);
            });
        } else {
            if (holder.btnPlay != null) holder.btnPlay.setVisibility(View.GONE);
            holder.btnAction.setText("Dodaj");
            holder.btnAction.setOnClickListener(v -> {
                if (addListener != null) addListener.onAction(friend);
            });
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvUsername, tvStars, tvLeague, tvOnlineStatus, tvMonthlyRank;
        ImageView ivLeague;
        MaterialButton btnAction, btnPlay;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar       = itemView.findViewById(R.id.ivFriendAvatar);
            tvUsername     = itemView.findViewById(R.id.tvFriendUsername);
            tvStars        = itemView.findViewById(R.id.tvFriendStars);
            tvLeague       = itemView.findViewById(R.id.tvFriendLeague);
            tvOnlineStatus  = itemView.findViewById(R.id.tvFriendOnlineStatus);
            tvMonthlyRank   = itemView.findViewById(R.id.tvFriendMonthlyRank);
            ivLeague       = itemView.findViewById(R.id.ivFriendLeague);
            btnAction      = itemView.findViewById(R.id.btnFriendAction);
            btnPlay        = itemView.findViewById(R.id.btnFriendPlay);
        }
    }
}