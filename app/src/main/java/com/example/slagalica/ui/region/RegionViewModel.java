package com.example.slagalica.ui.region;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.slagalica.data.RegionRepository;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionViewModel extends ViewModel {

    private final RegionRepository repository;

    private final MutableLiveData<RegionStats> selectedRegionStats = new MutableLiveData<>();
    private final MutableLiveData<List<RegionStats>> regionLeaderboard = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<PlayerMapPoint>> mapPoints = new MutableLiveData<>(new ArrayList<>());

    private String myRegion = "";

    public RegionViewModel() {
        repository = new RegionRepository();
    }

    public LiveData<RegionStats> getSelectedRegionStats() { return selectedRegionStats; }
    public LiveData<List<RegionStats>> getRegionLeaderboard() { return regionLeaderboard; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<List<PlayerMapPoint>> getMapPoints() { return mapPoints; }
    public String getMyRegion() { return myRegion; }
    public void setMyRegion(String region) { this.myRegion = region; }

    public void loadSelectedRegionStats(String region) {
        isLoading.setValue(true);

        repository.getPlayersByRegion(region).addOnSuccessListener(allSnap -> {
            int totalPlayers = allSnap.size();

            repository.getActivePlayersByRegion(region).addOnSuccessListener(activeSnap -> {
                int activePlayers = activeSnap.size();


                repository.getAllRegionMedals().addOnSuccessListener(medalsSnap -> {
                    int gold = 0, silver = 0, bronze = 0;
                    for (DocumentSnapshot doc : medalsSnap.getDocuments()) {
                        if (region.equals(doc.getId())) {
                            Long g = doc.getLong("gold");
                            Long s = doc.getLong("silver");
                            Long b = doc.getLong("bronze");
                            gold   = g != null ? g.intValue() : 0;
                            silver = s != null ? s.intValue() : 0;
                            bronze = b != null ? b.intValue() : 0;
                            break;
                        }
                    }
                    RegionStats stats = new RegionStats(
                            region, 0, totalPlayers, activePlayers, gold, silver, bronze);
                    selectedRegionStats.setValue(stats);
                    isLoading.setValue(false);

                }).addOnFailureListener(e -> {
                    RegionStats stats = new RegionStats(region, 0, totalPlayers, activePlayers, 0, 0, 0);
                    selectedRegionStats.setValue(stats);
                    isLoading.setValue(false);
                });

            }).addOnFailureListener(e -> {
                RegionStats stats = new RegionStats(region, 0, totalPlayers, 0, 0, 0, 0);
                selectedRegionStats.setValue(stats);
                isLoading.setValue(false);
            });

        }).addOnFailureListener(e -> {
            errorMessage.setValue("Greška pri učitavanju statistike regiona.");
            isLoading.setValue(false);
        });
    }

    public void loadRegionLeaderboard() {
        isLoading.setValue(true);
        repository.getAllPlayers().addOnSuccessListener(querySnapshot -> {

            Map<String, Integer> starsPerRegion  = new HashMap<>();
            Map<String, Integer> playersPerRegion = new HashMap<>();

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                String region = doc.getString("region");
                if (region == null || region.isEmpty() || region.equals("Unknown")) continue;

                Long stars = doc.getLong("monthlyStars");
                int s = stars != null ? stars.intValue() : 0;

                starsPerRegion.put(region,  starsPerRegion.getOrDefault(region, 0) + s);
                playersPerRegion.put(region, playersPerRegion.getOrDefault(region, 0) + 1);
            }

            repository.getAllRegionMedals().addOnSuccessListener(medalsSnap -> {
                Map<String, int[]> medals = new HashMap<>();
                for (DocumentSnapshot doc : medalsSnap.getDocuments()) {
                    Long g = doc.getLong("gold");
                    Long s = doc.getLong("silver");
                    Long b = doc.getLong("bronze");
                    medals.put(doc.getId(), new int[]{
                            g != null ? g.intValue() : 0,
                            s != null ? s.intValue() : 0,
                            b != null ? b.intValue() : 0
                    });
                }

                List<RegionStats> list = new ArrayList<>();
                for (String region : starsPerRegion.keySet()) {
                    int[] m = medals.getOrDefault(region, new int[]{0, 0, 0});
                    list.add(new RegionStats(
                            region,
                            starsPerRegion.getOrDefault(region, 0),
                            playersPerRegion.getOrDefault(region, 0),
                            0, m[0], m[1], m[2]
                    ));
                }

                Collections.sort(list, (a, b2) -> b2.totalStars - a.totalStars);
                for (int i = 0; i < list.size(); i++) list.get(i).rank = i + 1;

                regionLeaderboard.setValue(list);
                isLoading.setValue(false);

            }).addOnFailureListener(e -> {
                List<RegionStats> list = new ArrayList<>();
                for (String region : starsPerRegion.keySet()) {
                    list.add(new RegionStats(region,
                            starsPerRegion.getOrDefault(region, 0),
                            playersPerRegion.getOrDefault(region, 0),
                            0, 0, 0, 0));
                }
                Collections.sort(list, (a, b2) -> b2.totalStars - a.totalStars);
                for (int i = 0; i < list.size(); i++) list.get(i).rank = i + 1;
                regionLeaderboard.setValue(list);
                isLoading.setValue(false);
            });

        }).addOnFailureListener(e -> {
            errorMessage.setValue("Greška pri učitavanju rang liste.");
            isLoading.setValue(false);
        });
    }

    public void loadAllMapPoints() {
        repository.getAllPlayers().addOnSuccessListener(querySnapshot -> {
            List<PlayerMapPoint> points = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                String region = doc.getString("region");
                String username = doc.getString("username");
                if (region == null || region.isEmpty() || region.equals("Unknown")) continue;
                double[] coords = RegionCoordinates.getRandomPointInRegion(region);
                if (coords != null) {
                    points.add(new PlayerMapPoint(
                            doc.getId(),
                            username != null ? username : "?",
                            region, coords[0], coords[1]));
                }
            }
            mapPoints.setValue(points);
        }).addOnFailureListener(e -> errorMessage.setValue("Greška pri učitavanju igrača."));
    }

    public static class PlayerMapPoint {
        public final String uid;
        public final String username;
        public final String region;
        public final double lat;
        public final double lng;

        public PlayerMapPoint(String uid, String username, String region, double lat, double lng) {
            this.uid = uid;
            this.username = username;
            this.region = region;
            this.lat = lat;
            this.lng = lng;
        }
    }
}