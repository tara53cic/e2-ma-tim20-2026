package com.example.slagalica.ui.friends;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.example.slagalica.domain.models.Friend;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    private FriendsViewModel viewModel;

    private RecyclerView rvFriends;
    private RecyclerView rvSearch;
    private TextInputEditText etSearch;
    private TextView tvNoFriends;
    private TextView tvNoResults;

    private FriendAdapter friendsAdapter;
    private FriendAdapter searchAdapter;

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

        rvFriends = view.findViewById(R.id.rvFriends);
        rvSearch = view.findViewById(R.id.rvSearchResults);
        etSearch = view.findViewById(R.id.etSearchFriend);
        tvNoFriends = view.findViewById(R.id.tvNoFriends);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        friendsAdapter = new FriendAdapter(
                friend -> { },
                friend -> viewModel.removeFriend(friend.getUid()),
                true
        );

        searchAdapter = new FriendAdapter(
                friend -> viewModel.addFriend(friend.getUid()),
                null,
                false
        );

        rvFriends.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvFriends.setAdapter(friendsAdapter);

        rvSearch.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSearch.setAdapter(searchAdapter);

        // Observe
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
            if (msg != null && !msg.isEmpty()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // Pretraga
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

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadFriends();
    }
}