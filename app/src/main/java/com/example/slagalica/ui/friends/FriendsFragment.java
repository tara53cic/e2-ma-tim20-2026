package com.example.slagalica.ui.friends;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.MainActivity;
import com.example.slagalica.R;
import com.example.slagalica.domain.models.Friend;
import com.example.slagalica.domain.models.FriendRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.util.ArrayList;

public class FriendsFragment extends Fragment {

    private FriendsViewModel viewModel;
    private RecyclerView rvFriends, rvSearch;
    private TextInputEditText etSearch;
    private TextView tvNoFriends, tvNoResults;
    private FriendAdapter friendsAdapter, searchAdapter;
    private AlertDialog waitingDialog;
    private AlertDialog incomingDialog;
    private ActivityResultLauncher<ScanOptions> qrLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        qrLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                viewModel.addFriendByUid(result.getContents());
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(FriendsViewModel.class);

        rvFriends   = view.findViewById(R.id.rvFriends);
        rvSearch    = view.findViewById(R.id.rvSearchResults);
        etSearch    = view.findViewById(R.id.etSearchFriend);
        tvNoFriends = view.findViewById(R.id.tvNoFriends);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        friendsAdapter = new FriendAdapter(
                friend -> viewModel.addFriend(friend.getUid()),
                friend -> viewModel.removeFriend(friend.getUid()),
                friend -> viewModel.sendGameRequest(friend),
                true
        );

        searchAdapter = new FriendAdapter(
                friend -> viewModel.addFriend(friend.getUid()),
                null, null, false
        );

        rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFriends.setAdapter(friendsAdapter);
        rvSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearch.setAdapter(searchAdapter);

        view.findViewById(R.id.btnScanQr).setOnClickListener(v -> {
            ScanOptions options = new ScanOptions();
            options.setPrompt("Skeniraj QR kod prijatelja");
            options.setBeepEnabled(true);
            options.setOrientationLocked(true);
            qrLauncher.launch(options);
        });

        viewModel.getFriends().observe(getViewLifecycleOwner(), friends -> {
            friendsAdapter.setItems(friends);
            tvNoFriends.setVisibility(friends == null || friends.isEmpty() ? View.VISIBLE : View.GONE);
            rvFriends.setVisibility(friends != null && !friends.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getSearchResults().observe(getViewLifecycleOwner(), results -> {
            searchAdapter.setItems(results);
            boolean hasQuery = etSearch.getText() != null && etSearch.getText().length() > 0;
            tvNoResults.setVisibility(hasQuery && (results == null || results.isEmpty()) ? View.VISIBLE : View.GONE);
            rvSearch.setVisibility(results != null && !results.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getPendingRequestId().observe(getViewLifecycleOwner(), requestId -> {
            if (requestId != null) showWaitingDialog();
            else dismissWaitingDialog();
        });

        viewModel.getIncomingRequest().observe(getViewLifecycleOwner(), request -> {
            if (request != null) {
                showIncomingRequestDialog(request);
            } else {
                if (incomingDialog != null && incomingDialog.isShowing()) {
                    incomingDialog.dismiss();
                    incomingDialog = null;
                }
            }
        });

        viewModel.getNavigateToMatch().observe(getViewLifecycleOwner(), matchId -> {
            if (matchId != null) {
                dismissWaitingDialog();
                Intent intent = new Intent(requireActivity(), MainActivity.class);
                intent.putExtra("NAVIGATE_TO", "MATCH");
                intent.putExtra("MATCH_ID", matchId);
                startActivity(intent);
                requireActivity().finish();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    searchAdapter.setItems(new ArrayList<>());
                    rvSearch.setVisibility(View.GONE);
                    tvNoResults.setVisibility(View.GONE);
                } else {
                    viewModel.searchUsers(query);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        viewModel.loadFriends();
    }

    private void showWaitingDialog() {
        if (waitingDialog != null && waitingDialog.isShowing()) return;
        waitingDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Čekanje odgovora...")
                .setMessage("Čekamo da prijatelj prihvati partiju.\nAuto-odbijanje za 10 sekundi.")
                .setNegativeButton("Otkaži zahtev", (d, w) -> viewModel.cancelRequest())
                .setCancelable(false)
                .show();
    }

    private void dismissWaitingDialog() {
        if (waitingDialog != null && waitingDialog.isShowing()) {
            waitingDialog.dismiss();
            waitingDialog = null;
        }
    }

    private void showIncomingRequestDialog(FriendRequest request) {
        if (!isAdded()) return;
        if (incomingDialog != null && incomingDialog.isShowing()) {
            incomingDialog.dismiss();
        }
        incomingDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Poziv za partiju!")
                .setMessage(request.getFromUsername() + " te poziva na prijateljsku partiju!")
                .setPositiveButton("Prihvati", (d, w) -> viewModel.acceptRequest(request))
                .setNegativeButton("Odbij", (d, w) -> viewModel.declineRequest(request))
                .setCancelable(false)
                .create();
        incomingDialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadFriends();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissWaitingDialog();
        if (incomingDialog != null && incomingDialog.isShowing()) {
            incomingDialog.dismiss();
            incomingDialog = null;
        }
    }
}