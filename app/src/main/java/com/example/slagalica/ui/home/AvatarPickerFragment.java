package com.example.slagalica.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.slagalica.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AvatarPickerFragment extends Fragment {

    private static final int[] AVATAR_ICONS = {
            R.drawable.ic_avatar_1,
            R.drawable.ic_avatar_2,
            R.drawable.ic_avatar_3,
            R.drawable.ic_avatar_4,
            R.drawable.ic_avatar_5,
            R.drawable.ic_avatar_6
    };

    private static final int[] AVATAR_COLORS = {
            0xFF508EFA,
            0xFF2EC27E,
            0xFFE53935,
            0xFFF4A261,
            0xFF9C27B0,
            0xFF00BCD4
    };

    private int selectedColorIndex = 0;
    private AvatarAdapter adapter;
    private ShapeableImageView ivCurrentAvatar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_avatar_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View btnBack = view.findViewById(R.id.btnBack);
        ivCurrentAvatar = view.findViewById(R.id.ivCurrentAvatar);
        GridView gridAvatars = view.findViewById(R.id.gridAvatars);
        MaterialButton btnConfirm = view.findViewById(R.id.btnConfirmAvatar);

        adapter = new AvatarAdapter(requireContext(), AVATAR_ICONS, AVATAR_COLORS, selectedColorIndex);
        gridAvatars.setAdapter(adapter);

        loadCurrentAvatarColor();

        gridAvatars.setOnItemClickListener((parent, v, position, id) -> {
            selectedColorIndex = position;
            adapter.setSelectedIndex(position);
            adapter.notifyDataSetChanged();
            ivCurrentAvatar.setImageResource(AVATAR_ICONS[position]);
            ivCurrentAvatar.setBackgroundTintList(ColorStateList.valueOf(AVATAR_COLORS[position]));
        });

        btnConfirm.setOnClickListener(v -> saveAvatarColor(selectedColorIndex));

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());
    }

    private void loadCurrentAvatarColor() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || !snapshot.exists()) return;
                    Long savedIndex = snapshot.getLong("avatarColorIndex");
                    int index = (savedIndex != null && savedIndex >= 0
                            && savedIndex < AVATAR_COLORS.length)
                            ? savedIndex.intValue() : 0;
                    selectedColorIndex = index;
                    ivCurrentAvatar.setImageResource(AVATAR_ICONS[index]);
                    ivCurrentAvatar.setBackgroundTintList(
                            ColorStateList.valueOf(AVATAR_COLORS[index]));
                    adapter.setSelectedIndex(index);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    ivCurrentAvatar.setImageResource(AVATAR_ICONS[0]);
                    ivCurrentAvatar.setBackgroundTintList(
                            ColorStateList.valueOf(AVATAR_COLORS[0]));
                });
    }

    private void saveAvatarColor(int colorIndex) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("avatarColorIndex", colorIndex)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            getString(R.string.avatar_saved), Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                getString(R.string.avatar_save_error),
                                Toast.LENGTH_SHORT).show());
    }

    private static class AvatarAdapter extends BaseAdapter {

        private final Context context;
        private final int[] icons;
        private final int[] colors;
        private int selectedIndex;

        AvatarAdapter(Context context, int[] icons, int[] colors, int selectedIndex) {
            this.context = context;
            this.icons = icons;
            this.colors = colors;
            this.selectedIndex = selectedIndex;
        }

        public void setSelectedIndex(int index) { this.selectedIndex = index; }

        @Override public int getCount() { return icons.length; }
        @Override public Object getItem(int position) { return icons[position]; }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context)
                        .inflate(R.layout.item_avatar, parent, false);
            }

            ShapeableImageView ivAvatar = convertView.findViewById(R.id.ivAvatarOption);
            ImageView ivCheck = convertView.findViewById(R.id.ivSelectedCheck);

            ivAvatar.setImageResource(icons[position]);
            ivAvatar.setBackgroundTintList(ColorStateList.valueOf(colors[position]));

            if (position == selectedIndex) {
                ivCheck.setVisibility(View.VISIBLE);
                convertView.setAlpha(1.0f);
            } else {
                ivCheck.setVisibility(View.GONE);
                convertView.setAlpha(0.7f);
            }

            return convertView;
        }
    }
}