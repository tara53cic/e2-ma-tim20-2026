package com.example.slagalica.ui.region;

import com.example.slagalica.R;
import java.util.HashMap;
import java.util.Map;

public class RegionIcons {

    private static final Map<String, Integer> ICONS = new HashMap<>();

    static {
        ICONS.put("Vojvodina", R.drawable.ic_region_v);
        ICONS.put("Beograd",                   R.drawable.ic_region_b);
        ICONS.put("Šumadija i Zapadna Srbija", R.drawable.ic_region_s);
        ICONS.put("Južna i Istočna Srbija",    R.drawable.ic_region_j);
        ICONS.put("Kosovo i Metohija",          R.drawable.ic_region_k);
    }

    public static int getIcon(String region) {
        Integer icon = ICONS.get(region);
        return icon != null ? icon : R.drawable.ic_region;
    }
}