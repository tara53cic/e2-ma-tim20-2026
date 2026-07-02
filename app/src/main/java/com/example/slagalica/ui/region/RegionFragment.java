package com.example.slagalica.ui.region;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.slagalica.data.NotificationRepository;
import com.example.slagalica.domain.models.NotificationType;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.slagalica.data.RegionRepository;
import com.example.slagalica.data.UserRepository;
import androidx.navigation.Navigation;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.example.slagalica.domain.models.ChatMessage;
import com.example.slagalica.domain.models.Challenge;
import com.google.firebase.Timestamp;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Locale;

public class RegionFragment extends Fragment {

    private RegionViewModel viewModel;
    private WebView mapWebView;
    private TextView tvRegionName, btnTabMap, btnTabLeaderboard, btnTabChat, btnTabChallenges;
    private Spinner spinnerRegion;
    private View tabMap, tabLeaderboard, tabChat, tabChallenges;
    private RecyclerView rvLeaderboard, rvChat, rvChallenges;
    private RegionLeaderboardAdapter leaderboardAdapter;
    private ChatAdapter chatAdapter;
    private ChallengeAdapter challengeAdapter;
    private ListenerRegistration chatListener, challengeListener;
    private EditText etChatMessage;
    private View btnSendChat;
    private View btnCreateChallenge;
    private RegionRepository regionRepository = new RegionRepository();
    private boolean mapReady = false;
    private String selectedRegion = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_region, container, false);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RegionViewModel.class);

        mapWebView        = view.findViewById(R.id.mapWebView);
        tvRegionName      = view.findViewById(R.id.tvRegionName);
        spinnerRegion     = view.findViewById(R.id.spinnerRegion);
        btnTabMap         = view.findViewById(R.id.btnTabMap);
        btnTabLeaderboard = view.findViewById(R.id.btnTabLeaderboard);
        btnTabChat        = view.findViewById(R.id.btnTabChat);
        btnTabChallenges  = view.findViewById(R.id.btnTabChallenges);

        tabMap            = view.findViewById(R.id.tabMap);
        tabLeaderboard    = view.findViewById(R.id.tabLeaderboard);
        tabChat           = view.findViewById(R.id.tabChat);
        tabChallenges     = view.findViewById(R.id.tabChallenges);

        rvLeaderboard     = view.findViewById(R.id.rvRegionLeaderboard);
        MaterialButton btnDetails = view.findViewById(R.id.btnRegionDetails);

        btnTabMap.setOnClickListener(v -> showTab(TAB_MAP));
        btnTabLeaderboard.setOnClickListener(v -> showTab(TAB_LEADERBOARD));
        btnTabChat.setOnClickListener(v -> showTab(TAB_CHAT));
        btnTabChallenges.setOnClickListener(v -> showTab(TAB_CHALLENGES));

        // Chat
        rvChat = view.findViewById(R.id.rvChat);
        etChatMessage = view.findViewById(R.id.etChatMessage);
        btnSendChat = view.findViewById(R.id.btnSendChat);
        chatAdapter = new ChatAdapter(regionRepository.getCurrentUid());
        rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChat.setAdapter(chatAdapter);

        btnSendChat.setOnClickListener(v -> sendMessage());

        // Challenges
        rvChallenges = view.findViewById(R.id.rvChallenges);
        btnCreateChallenge = view.findViewById(R.id.btnCreateChallenge);
        challengeAdapter = new ChallengeAdapter(regionRepository.getCurrentUid(), this::onJoinChallenge);
        rvChallenges.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChallenges.setAdapter(challengeAdapter);

        btnCreateChallenge.setOnClickListener(v -> showCreateChallengeDialog());

        leaderboardAdapter = new RegionLeaderboardAdapter();
        leaderboardAdapter.setOnRegionClickListener(stats -> {
            viewModel.loadSelectedRegionStats(stats.region);
        });
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLeaderboard.setAdapter(leaderboardAdapter);

        WebSettings settings = mapWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setAllowFileAccess(true);

        mapWebView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onRegionClick(String regionName) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        selectedRegion = regionName;
                        tvRegionName.setText(regionName);
                        if (mapReady) {
                            mapWebView.evaluateJavascript(
                                    "highlightRegion('" + regionName.replace("'", "\\'") + "')", null);
                        }
                        String[] regions = RegionCoordinates.getAllRegions();
                        for (int i = 0; i < regions.length; i++) {
                            if (regions[i].equals(regionName)) {
                                spinnerRegion.setSelection(i);
                                break;
                            }
                        }
                        viewModel.loadSelectedRegionStats(regionName);
                    });
                }
            }
        }, "Android");

        mapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                mapReady = true;
                List<RegionViewModel.PlayerMapPoint> points = viewModel.getMapPoints().getValue();
                if (points != null && !points.isEmpty()) addMarkersToMap(points);
            }
        });
        mapWebView.loadUrl("file:///android_asset/map.html");

        String[] regions = RegionCoordinates.getAllRegions();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, regions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRegion.setAdapter(adapter);

        loadCurrentUserRegion(regions);

        spinnerRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                selectedRegion = regions[position];
                tvRegionName.setText(selectedRegion);
                if (mapReady) {
                    mapWebView.evaluateJavascript(
                            "highlightRegion('" + selectedRegion.replace("'", "\\'") + "')", null);
                }
                showTab(currentTab);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnDetails.setOnClickListener(v -> {
            if (!selectedRegion.isEmpty()) {
                viewModel.loadSelectedRegionStats(selectedRegion);
            }
        });

        viewModel.getSelectedRegionStats().observe(getViewLifecycleOwner(),
                stats -> { if (stats != null) showRegionStatsDialog(stats); });

        viewModel.getRegionLeaderboard().observe(getViewLifecycleOwner(), list -> {
            if (list != null) leaderboardAdapter.setItems(list, viewModel.getMyRegion());
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        viewModel.getMapPoints().observe(getViewLifecycleOwner(), points -> {
            if (mapReady && points != null && !points.isEmpty()) addMarkersToMap(points);
        });

        viewModel.loadAllMapPoints();
        viewModel.loadRegionLeaderboard();

        setOnlineStatus(true);
    }

    private void showRegionStatsDialog(RegionStats stats) {
        if (!isAdded()) return;

        android.widget.LinearLayout titleLayout = new android.widget.LinearLayout(requireContext());
        titleLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        titleLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        titleLayout.setPadding(48, 32, 48, 0);

        android.widget.ImageView icon = new android.widget.ImageView(requireContext());
        icon.setImageResource(RegionIcons.getIcon(stats.region));
        android.widget.LinearLayout.LayoutParams iconParams =
                new android.widget.LinearLayout.LayoutParams(64, 64);
        iconParams.setMarginEnd(16);
        icon.setLayoutParams(iconParams);

        android.widget.TextView title = new android.widget.TextView(requireContext());
        title.setText(stats.region);
        title.setTextSize(18);
        title.setTextColor(0xFF1a1a2e);
        title.setTypeface(null, android.graphics.Typeface.BOLD);

        titleLayout.addView(icon);
        titleLayout.addView(title);

        String msg = "🥇 Prva mesta: "         + stats.goldMedals    + "\n" +
                "🥈 Druga mesta: "        + stats.silverMedals  + "\n" +
                "🥉 Treća mesta: "        + stats.bronzeMedals  + "\n\n" +
                "🟢 Aktivnih igrača: "    + stats.activePlayers + "\n" +
                "👤 Ukupno registrovanih: " + stats.playerCount;

        new android.app.AlertDialog.Builder(requireContext())
                .setCustomTitle(titleLayout)
                .setMessage(msg)
                .setPositiveButton("Zatvori", null)
                .show();
    }

    private static final int TAB_MAP = 0;
    private static final int TAB_LEADERBOARD = 1;
    private static final int TAB_CHAT = 2;
    private static final int TAB_CHALLENGES = 3;
    private int currentTab = TAB_MAP;

    private void showTab(int tabIndex) {
        currentTab = tabIndex;
        tabMap.setVisibility(tabIndex == TAB_MAP ? View.VISIBLE : View.GONE);
        tabLeaderboard.setVisibility(tabIndex == TAB_LEADERBOARD ? View.VISIBLE : View.GONE);
        tabChat.setVisibility(tabIndex == TAB_CHAT ? View.VISIBLE : View.GONE);
        tabChallenges.setVisibility(tabIndex == TAB_CHALLENGES ? View.VISIBLE : View.GONE);

        btnTabMap.setBackground(ContextCompat.getDrawable(requireContext(), tabIndex == TAB_MAP ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected));
        btnTabMap.setTextColor(ContextCompat.getColor(requireContext(), tabIndex == TAB_MAP ? R.color.background : R.color.primary));

        btnTabLeaderboard.setBackground(ContextCompat.getDrawable(requireContext(), tabIndex == TAB_LEADERBOARD ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected));
        btnTabLeaderboard.setTextColor(ContextCompat.getColor(requireContext(), tabIndex == TAB_LEADERBOARD ? R.color.background : R.color.primary));

        btnTabChat.setBackground(ContextCompat.getDrawable(requireContext(), tabIndex == TAB_CHAT ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected));
        btnTabChat.setTextColor(ContextCompat.getColor(requireContext(), tabIndex == TAB_CHAT ? R.color.background : R.color.primary));

        btnTabChallenges.setBackground(ContextCompat.getDrawable(requireContext(), tabIndex == TAB_CHALLENGES ? R.drawable.bg_filter_selected : R.drawable.bg_filter_unselected));
        btnTabChallenges.setTextColor(ContextCompat.getColor(requireContext(), tabIndex == TAB_CHALLENGES ? R.color.background : R.color.primary));

        if (tabIndex == TAB_CHAT) {
            startChatListener();
        } else {
            stopChatListener();
        }

        if (tabIndex == TAB_CHALLENGES) {
            startChallengeListener();
        } else {
            stopChallengeListener();
        }
    }

    private void sendMessage() {
        String text = etChatMessage.getText().toString().trim();
        if (text.isEmpty() || selectedRegion.isEmpty()) return;

        UserRepository userRepo = new UserRepository();
        String currentUid = regionRepository.getCurrentUid();
        userRepo.getUser(currentUid).addOnSuccessListener(doc -> {
            String name = doc.getString("username");
            ChatMessage msg = new ChatMessage(name, currentUid, text, Timestamp.now());
            regionRepository.sendMessage(selectedRegion, msg);

            // Obaveštenja za igrače koji nisu u aplikaciji
            regionRepository.getPlayersByRegion(selectedRegion).addOnSuccessListener(snapshot -> {
                NotificationRepository notifRepo = new NotificationRepository();
                for (DocumentSnapshot playerDoc : snapshot.getDocuments()) {
                    String playerId = playerDoc.getId();
                    if (playerId.equals(currentUid)) continue;

                    Boolean online = playerDoc.getBoolean("online");
                    if (online == null || !online) {
                        notifRepo.createNotificationForUser(playerId,
                                "Nova poruka - " + selectedRegion,
                                name + ": " + text,
                                NotificationType.CHAT);
                    }
                }
            });

            etChatMessage.setText("");
        });
    }

    private void showCreateChallengeDialog() {
        if (selectedRegion.isEmpty()) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_challenge, null);
        NumberPicker npStars = dialogView.findViewById(R.id.npStars);
        NumberPicker npTokens = dialogView.findViewById(R.id.npTokens);

        npStars.setMinValue(1); npStars.setMaxValue(10);
        npTokens.setMinValue(1); npTokens.setMaxValue(2);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Kreiraj izazov")
                .setView(dialogView)
                .setPositiveButton("Kreiraj", (dialog, which) -> {
                    int stars = npStars.getValue();
                    int tokens = npTokens.getValue();
                    createChallenge(stars, tokens);
                })
                .setNegativeButton("Otkaži", null)
                .show();
    }

    private void createChallenge(int stars, int tokens) {
        UserRepository userRepo = new UserRepository();
        String uid = regionRepository.getCurrentUid();
        
        userRepo.getUser(uid).addOnSuccessListener(doc -> {
            int userStars = doc.getLong("stars") != null ? doc.getLong("stars").intValue() : 0;
            int userTokens = doc.getLong("tokens") != null ? doc.getLong("tokens").intValue() : 0;
            
            if (userStars < stars || userTokens < tokens) {
                Toast.makeText(requireContext(), "Nemaš dovoljno zvezda ili tokena!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String name = doc.getString("username");
            Challenge challenge = new Challenge(null, uid, name, stars, tokens);
            
            userRepo.deductStars(stars);
            userRepo.deductTokens(tokens);
            
            regionRepository.createChallenge(selectedRegion, challenge)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(requireContext(), "Izazov kreiran!", Toast.LENGTH_SHORT).show();
                });
        });
    }

    private void onJoinChallenge(Challenge challenge) {
        String uid = regionRepository.getCurrentUid();
        if (challenge.getPlayerIds().contains(uid)) {
            // Već je unutra
            if (challenge.getStatus().equals("IN_PROGRESS")) {
                startChallengeGame(challenge);
            }
            return;
        }

        if (challenge.getPlayerIds().size() >= 4) {
            Toast.makeText(requireContext(), "Izazov je popunjen!", Toast.LENGTH_SHORT).show();
            return;
        }

        UserRepository userRepo = new UserRepository();
        userRepo.getUser(uid).addOnSuccessListener(doc -> {
            int userStars = doc.getLong("stars") != null ? doc.getLong("stars").intValue() : 0;
            int userTokens = doc.getLong("tokens") != null ? doc.getLong("tokens").intValue() : 0;

            if (userStars < challenge.getBidStars() || userTokens < challenge.getBidTokens()) {
                Toast.makeText(requireContext(), "Nemaš dovoljno zvezda ili tokena!", Toast.LENGTH_SHORT).show();
                return;
            }

            userRepo.deductStars(challenge.getBidStars());
            userRepo.deductTokens(challenge.getBidTokens());

            String name = doc.getString("username");
            regionRepository.joinChallenge(selectedRegion, challenge.getId(), uid, name)
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Pridružio si se izazovu!", Toast.LENGTH_SHORT).show();
                    // Ako je sad 4, prebaci u IN_PROGRESS
                    if (challenge.getPlayerIds().size() + 1 == 4) {
                        regionRepository.updateChallengeStatus(selectedRegion, challenge.getId(), "IN_PROGRESS");
                        startChallengeGame(challenge);
                    }
                });
        });
    }

    private void startChallengeGame(Challenge challenge) {
        Bundle bundle = new Bundle();
        bundle.putString("CHALLENGE_ID", challenge.getId());
        bundle.putString("REGION_ID", selectedRegion);
        Navigation.findNavController(requireView()).navigate(R.id.action_nav_region_to_matchFragment, bundle);
    }

    private void startChatListener() {
        stopChatListener();
        if (selectedRegion == null || selectedRegion.isEmpty()) return;
        chatListener = regionRepository.getChatMessages(selectedRegion)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        android.util.Log.e("RegionFragment", "Chat error: " + e.getMessage());
                        return;
                    }
                    if (snapshot == null) return;
                    List<ChatMessage> messages = snapshot.toObjects(ChatMessage.class);
                    if (chatAdapter != null) {
                        chatAdapter.setMessages(messages);
                        if (!messages.isEmpty()) {
                            rvChat.scrollToPosition(messages.size() - 1);
                        }
                    }
                });
    }

    private void stopChatListener() {
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }

    private void startChallengeListener() {
        stopChallengeListener();
        if (selectedRegion == null || selectedRegion.isEmpty()) return;
        challengeListener = regionRepository.getChallenges(selectedRegion)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        android.util.Log.e("RegionFragment", "Challenge error: " + e.getMessage());
                        return;
                    }
                    if (snapshot == null) return;
                    List<Challenge> challenges = snapshot.toObjects(Challenge.class);
                    
                    // Provera za automatsko pokretanje ili brisanje
                    long now = System.currentTimeMillis();
                    String myUid = regionRepository.getCurrentUid();
                    
                    for (Challenge c : challenges) {
                        if (c.getCreatedAt() == null) continue;
                        long elapsed = now - c.getCreatedAt().toDate().getTime();
                        
                        // Ako je prošlo više od 60s
                        if ("OPEN".equals(c.getStatus()) && elapsed > 60000) {
                            if (c.getPlayerIds().size() > 1) {
                                // Više od jednog igrača -> START
                                if (myUid.equals(c.getChallengerId())) {
                                    regionRepository.updateChallengeStatus(selectedRegion, c.getId(), "IN_PROGRESS");
                                }
                            } else {
                                // Samo kreator -> ISTEKAO (brišemo ili menjamo status)
                                if (myUid.equals(c.getChallengerId())) {
                                    regionRepository.updateChallengeStatus(selectedRegion, c.getId(), "EXPIRED");
                                    // Vratiti ulog kreatoru
                                    UserRepository userRepo = new UserRepository();
                                    userRepo.addStars(c.getBidStars());
                                    userRepo.addTokens(c.getBidTokens());
                                }
                            }
                        }
                        
                        // Ako je status postao IN_PROGRESS, a ja sam unutra, pokreni igru
                        if ("IN_PROGRESS".equals(c.getStatus()) && c.getPlayerIds().contains(myUid)) {
                            // Proveri da li smo već pokrenuli (opciono, navigacija će handle-ovati)
                            startChallengeGame(c);
                        }
                    }

                    if (challengeAdapter != null) {
                        challengeAdapter.setChallenges(challenges);
                    }
                });
    }

    private void stopChallengeListener() {
        if (challengeListener != null) {
            challengeListener.remove();
            challengeListener = null;
        }
    }

    private void loadCurrentUserRegion(String[] regions) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    String myRegion = doc.getString("region");
                    if (myRegion == null) return;
                    viewModel.setMyRegion(myRegion);
                    for (int i = 0; i < regions.length; i++) {
                        if (regions[i].equals(myRegion)) {
                            spinnerRegion.setSelection(i);
                            selectedRegion = myRegion;
                            tvRegionName.setText(myRegion);
                            break;
                        }
                    }
                });
    }

    private void addMarkersToMap(List<RegionViewModel.PlayerMapPoint> points) {
        if (!mapReady) return;
        StringBuilder js = new StringBuilder("clearMarkers();");
        for (RegionViewModel.PlayerMapPoint p : points) {
            js.append(String.format(Locale.US,
                    "addMarker(%f, %f, '%s', '%s');",
                    p.lat, p.lng,
                    p.username.replace("'", "\\'"),
                    p.region.replace("'", "\\'")));
        }
        mapWebView.evaluateJavascript(js.toString(), null);
    }

    private void setOnlineStatus(boolean online) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("online", online);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setOnlineStatus(false);
        if (mapWebView != null) mapWebView.destroy();
    }
}