package com.example.slagalica.ui.region;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;

import java.util.ArrayList;
import java.util.List;

public class RegionLeaderboardAdapter
        extends RecyclerView.Adapter<RegionLeaderboardAdapter.ViewHolder> {

    public interface OnRegionClickListener {
        void onClick(RegionStats stats);
    }

    private final List<RegionStats> items = new ArrayList<>();
    private String myRegion = "";
    private OnRegionClickListener listener;

    public void setItems(List<RegionStats> newItems, String myRegion) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        this.myRegion = myRegion != null ? myRegion : "";
        notifyDataSetChanged();
    }

    public void setOnRegionClickListener(OnRegionClickListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_region_leaderboard, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        RegionStats s = items.get(position);

        h.tvRank.setText(String.valueOf(s.rank));

        if (s.rank == 1) {
            h.tvRank.setTextColor(0xFFFFD700); // zlatna
        } else if (s.rank == 2) {
            h.tvRank.setTextColor(0xFFC0C0C0); // srebrna
        } else if (s.rank == 3) {
            h.tvRank.setTextColor(0xFFCD7F32); // bronzana
        } else {
            h.tvRank.setTextColor(0xFFAAAAAA);
        }

        h.ivRegionIcon.setImageResource(RegionIcons.getIcon(s.region));

        h.tvRegionName.setText(s.region);

        h.tvStars.setText("⭐ " + s.totalStars);

        h.tvPlayers.setText("👤 " + s.playerCount);

        if (s.region.equals(myRegion)) {
            h.itemView.setBackgroundColor(0x22508EFA);
            h.tvRegionName.setTextColor(0xFF508EFA);
        } else {
            h.itemView.setBackgroundColor(0x00000000);
            h.tvRegionName.setTextColor(0xFFF6F8FF);
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(s);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvRegionName, tvStars, tvPlayers;
        ImageView ivRegionIcon;

        ViewHolder(@NonNull View v) {
            super(v);
            tvRank       = v.findViewById(R.id.tvRegionRank);
            ivRegionIcon = v.findViewById(R.id.ivRegionIcon);
            tvRegionName = v.findViewById(R.id.tvRegionItemName);
            tvStars      = v.findViewById(R.id.tvRegionStars);
            tvPlayers    = v.findViewById(R.id.tvRegionPlayers);
        }
    }
}