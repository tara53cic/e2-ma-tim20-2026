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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.slagalica.R;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class RegionFragment extends Fragment {

    private RegionViewModel viewModel;
    private WebView mapWebView;
    private TextView tvRegionName, btnTabMap, btnTabLeaderboard;
    private Spinner spinnerRegion;
    private View tabMap, tabLeaderboard;
    private RecyclerView rvLeaderboard;
    private RegionLeaderboardAdapter leaderboardAdapter;
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
        tabMap            = view.findViewById(R.id.tabMap);
        tabLeaderboard    = view.findViewById(R.id.tabLeaderboard);
        rvLeaderboard     = view.findViewById(R.id.rvRegionLeaderboard);
        MaterialButton btnDetails = view.findViewById(R.id.btnRegionDetails);

        btnTabMap.setOnClickListener(v -> showTab(true));
        btnTabLeaderboard.setOnClickListener(v -> showTab(false));

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

    private void showTab(boolean mapTab) {
        if (mapTab) {
            tabMap.setVisibility(View.VISIBLE);
            tabLeaderboard.setVisibility(View.GONE);
            btnTabMap.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_selected));
            btnTabMap.setTextColor(ContextCompat.getColor(requireContext(), R.color.background));
            btnTabLeaderboard.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_unselected));
            btnTabLeaderboard.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
        } else {
            tabMap.setVisibility(View.GONE);
            tabLeaderboard.setVisibility(View.VISIBLE);
            btnTabLeaderboard.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_selected));
            btnTabLeaderboard.setTextColor(ContextCompat.getColor(requireContext(), R.color.background));
            btnTabMap.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_filter_unselected));
            btnTabMap.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary));
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