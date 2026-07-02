package com.example.slagalica.ui.region;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.slagalica.R;
import com.example.slagalica.domain.models.Challenge;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class ChallengeAdapter extends RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder> {

    private List<Challenge> challenges = new ArrayList<>();
    private String currentUid;
    private OnJoinClickListener onJoinClickListener;

    public interface OnJoinClickListener {
        void onJoin(Challenge challenge);
    }

    public ChallengeAdapter(String currentUid, OnJoinClickListener listener) {
        this.currentUid = currentUid;
        this.onJoinClickListener = listener;
    }

    public void setChallenges(List<Challenge> challenges) {
        this.challenges = challenges;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChallengeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false);
        return new ChallengeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChallengeViewHolder holder, int position) {
        holder.bind(challenges.get(position));
    }

    @Override
    public int getItemCount() {
        return challenges.size();
    }

    class ChallengeViewHolder extends RecyclerView.ViewHolder {
        TextView tvChallenger, tvBid, tvPlayers, tvMyChallengeLabel;
        MaterialButton btnJoin;
        androidx.cardview.widget.CardView cardView;

        public ChallengeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (androidx.cardview.widget.CardView) itemView;
            tvChallenger = itemView.findViewById(R.id.tvChallengerName);
            tvMyChallengeLabel = itemView.findViewById(R.id.tvMyChallengeLabel);
            tvBid = itemView.findViewById(R.id.tvChallengeBid);
            tvPlayers = itemView.findViewById(R.id.tvChallengePlayers);
            btnJoin = itemView.findViewById(R.id.btnJoinChallenge);
        }

        public void bind(Challenge challenge) {
            boolean isMine = challenge.getChallengerId().equals(currentUid);
            tvChallenger.setText("Izazivač: " + challenge.getChallengerName());
            tvMyChallengeLabel.setVisibility(isMine ? View.VISIBLE : View.GONE);

            if (isMine) {
                cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")); // Svetlo zelena
            } else {
                cardView.setCardBackgroundColor(android.graphics.Color.WHITE);
            }

            tvBid.setText("Ulog: " + challenge.getBidStars() + "⭐, " + challenge.getBidTokens() + "🪙");
            tvPlayers.setText("Igrači: " + challenge.getPlayerIds().size() + "/4");

            if (challenge.getPlayerIds().contains(currentUid)) {
                if ("IN_PROGRESS".equals(challenge.getStatus())) {
                    btnJoin.setText("Uđi");
                    btnJoin.setEnabled(true);
                } else {
                    btnJoin.setText("Čekanje...");
                    btnJoin.setEnabled(false);
                }
            } else {
                btnJoin.setText("Pridruži se");
                btnJoin.setEnabled(true);
            }

            btnJoin.setOnClickListener(v -> onJoinClickListener.onJoin(challenge));
        }
    }
}
