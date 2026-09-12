package com.cat.group.cat.paint;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.cat.group.cat.paint.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private int clickCount = 0;
    private static final String PREFS_NAME = "CatPaintPrefs";
    private static final String KEY_DEV_MODE = "developer_mode";
    private static final String KEY_CAT_LAYOUT = "cat_layout_enabled";

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        updateUI(prefs);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(SettingsFragment.this).popBackStack()
        );

        binding.versionClickArea.setOnClickListener(v -> {
            if (prefs.getBoolean(KEY_DEV_MODE, false)) {
                Toast.makeText(getContext(), "Sei già uno sviluppatore!", Toast.LENGTH_SHORT).show();
                return;
            }

            clickCount++;
            if (clickCount >= 7) {
                prefs.edit().putBoolean(KEY_DEV_MODE, true).apply();
                updateUI(prefs);
                Toast.makeText(getContext(), "Ora sei uno sviluppatore! 🐱💻\nScorri in basso per le nuove opzioni!", Toast.LENGTH_LONG).show();
            } else if (clickCount > 0) {
                int remaining = 7 - clickCount;
                Toast.makeText(getContext(), "Mancano " + remaining + " tocchi per la modalità sviluppatore", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnReinvokeApk.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Rievocazione APK in corso... 🔄", Toast.LENGTH_SHORT).show();
            v.postDelayed(() -> Toast.makeText(getContext(), "APK Rievocato con successo!", Toast.LENGTH_LONG).show(), 2000);
        });

        binding.btnDisableDev.setOnClickListener(v -> {
            prefs.edit().putBoolean(KEY_DEV_MODE, false).apply();
            clickCount = 0;
            updateUI(prefs);
            Toast.makeText(getContext(), "Modalità sviluppatore disattivata", Toast.LENGTH_SHORT).show();
        });

        binding.switchCatLayout.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_CAT_LAYOUT, isChecked).apply();
        });
    }

    private void updateUI(SharedPreferences prefs) {
        boolean isDevMode = prefs.getBoolean(KEY_DEV_MODE, false);
        binding.cardDeveloper.setVisibility(isDevMode ? View.VISIBLE : View.GONE);
        
        if (isDevMode) {
            binding.switchCatLayout.setChecked(prefs.getBoolean(KEY_CAT_LAYOUT, true));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}