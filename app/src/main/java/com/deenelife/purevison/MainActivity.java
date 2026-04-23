package com.deenelife.purevison;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends AppCompatActivity {

    public static final String SHARED_PREFS_NAME = "PureVisionPrefs";
    public static final String KEY_SERVICE_ENABLED = "ServiceEnabled";
    public static final String KEY_STATIC_TEXT_ENABLED = "StaticTextEnabled";
    public static final String KEY_OVERLAY_TEXT = "OverlayText";
    public static final String KEY_FONT_SIZE = "FontSize";
    public static final String KEY_OPACITY = "Opacity";
    public static final String KEY_FONT_COLOR = "FontColorPos";
    public static final String KEY_BACKGROUND_COLOR = "BackgroundColorPos";
    public static final String KEY_BACKGROUND_ENABLED = "BackgroundEnabled";
    public static final String KEY_BACKGROUND_OPACITY = "BackgroundOpacity";
    public static final String KEY_POS_X = "PosX";
    public static final String KEY_POS_Y = "PosY";
    public static final String KEY_LANGUAGE = "AppLanguage";
    public static final String KEY_IS_FIRST_LAUNCH = "IsFirstLaunch";
    public static final String KEY_CORNER_RADIUS = "CornerRadius";
    public static final String KEY_POSITION_LOCKED = "PositionLocked";

    public static final String KEY_FONT_COLOR_INT = "FontColorInt";
    public static final String KEY_BACKGROUND_COLOR_INT = "BackgroundColorInt";

    public static final String KEY_ZIKR_MODE_ENABLED = "ZikrModeEnabled";
    public static final String KEY_ZIKR_DURATION = "ZikrDuration";
    public static final String KEY_ZIKR_LIST = "ZikrList";
    public static final String ZIKR_DELIMITER = ";;;";

    private SwitchMaterial serviceSwitch, switchBackgroundEnabled, switchLockPosition, switchStaticText;
    private TextInputEditText editTextCustom;
    private TextInputLayout textLayoutCustom, textLayoutTemplate;
    private AutoCompleteTextView spinnerTemplates, spinnerFontColor, spinnerBackgroundColor;
    private Slider sliderFontSize, sliderTextOpacity, sliderBackgroundOpacity, sliderCornerRadius;
    private Toolbar toolbar;
    private Button btnPickTextColor, btnPickBgColor;

    private SwitchMaterial switchZikrMode;
    private MaterialCardView cardZikrSettings, cardNormalTextSettings, cardTextAppearance;
    private Slider sliderZikrDuration;
    private TextView labelZikrDuration;
    private LinearLayout zikrListContainer;
    private Button btnAddNewZikr;

    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private SharedPreferences sharedPreferences;

    private boolean shouldShowPermissionDialog = false;
    private int loadedFontColorPos;
    private int loadedBgColorPos;
    private String loadedSavedText;
    private int currentTextColor;
    private int currentBackgroundColor;

    private AlertDialog helpDialog;
    private ActivityResultLauncher<Intent> backupLauncher;
    private ActivityResultLauncher<Intent> restoreLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        serviceSwitch = findViewById(R.id.switch_service);
        switchStaticText = findViewById(R.id.switch_static_text);
        switchBackgroundEnabled = findViewById(R.id.switch_background_enabled);
        editTextCustom = findViewById(R.id.edit_text_custom);
        textLayoutCustom = findViewById(R.id.text_layout_custom);
        textLayoutTemplate = findViewById(R.id.text_layout_template); // Template layout
        spinnerTemplates = findViewById(R.id.spinner_templates);
        spinnerFontColor = findViewById(R.id.spinner_font_color);
        spinnerBackgroundColor = findViewById(R.id.spinner_background_color);
        sliderFontSize = findViewById(R.id.slider_font_size);
        sliderTextOpacity = findViewById(R.id.slider_text_opacity);
        sliderBackgroundOpacity = findViewById(R.id.slider_background_opacity);
        sliderCornerRadius = findViewById(R.id.slider_corner_radius);
        btnPickTextColor = findViewById(R.id.btn_pick_text_color);
        btnPickBgColor = findViewById(R.id.btn_pick_bg_color);
        switchLockPosition = findViewById(R.id.switch_lock_position);

        cardNormalTextSettings = findViewById(R.id.card_normal_text_settings);
        cardTextAppearance = findViewById(R.id.card_text_appearance);

        switchZikrMode = findViewById(R.id.switch_zikr_mode);
        cardZikrSettings = findViewById(R.id.card_zikr_settings);
        sliderZikrDuration = findViewById(R.id.slider_zikr_duration);
        labelZikrDuration = findViewById(R.id.label_zikr_duration_text);
        zikrListContainer = findViewById(R.id.zikr_list_container);
        btnAddNewZikr = findViewById(R.id.btn_add_new_zikr);

        registerOverlayPermissionLauncher();
        registerBackupRestoreLaunchers();
        setupDefaultZikrList();
        loadAndDisplayZikrList();
        loadSettings();
        setupSpinners();
        setupListeners();

        checkFirstLaunch();
        updateVisibility();
    }

    private void setupDefaultZikrList() {
        String currentList = sharedPreferences.getString(KEY_ZIKR_LIST, null);
        if (currentList == null || currentList.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(getString(R.string.zikr_default_1)).append(ZIKR_DELIMITER);
            sb.append(getString(R.string.zikr_default_2)).append(ZIKR_DELIMITER);
            sb.append(getString(R.string.zikr_default_3)).append(ZIKR_DELIMITER);
            sb.append(getString(R.string.zikr_default_4)).append(ZIKR_DELIMITER);
            sb.append(getString(R.string.zikr_default_5));
            sharedPreferences.edit().putString(KEY_ZIKR_LIST, sb.toString()).apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        MenuItem versionItem = menu.findItem(R.id.action_version);
        if (versionItem != null) {
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionItem.setTitle("v" + pInfo.versionName);
            } catch (Exception e) { e.printStackTrace(); }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_language) { showLanguagePicker(); return true; }
        else if (itemId == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text));
            startActivity(Intent.createChooser(shareIntent, getString(R.string.action_share)));
            return true;
        } else if (itemId == R.id.action_help) {
            boolean hasDisplay = (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) || Settings.canDrawOverlays(this);
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean hasBattery = (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) || pm.isIgnoringBatteryOptimizations(getPackageName());
            showHelpDialog(hasDisplay, hasBattery);
            return true;
        } else if (itemId == R.id.action_github) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/purevisionapp")));
            return true;
        } else if (itemId == R.id.action_backup_settings) { launchBackup(); return true; }
        else if (itemId == R.id.action_restore_settings) { launchRestore(); return true; }
        return super.onOptionsItemSelected(item);
    }

    private void loadLocale() {
        String language = sharedPreferences.getString(KEY_LANGUAGE, "bn");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void setupSpinners() {
        // টেমপ্লেট ড্রপডাউন পুরোপুরি লুকিয়ে রাখা হয়েছে
        if (textLayoutTemplate != null) textLayoutTemplate.setVisibility(View.GONE);
        textLayoutCustom.setVisibility(View.VISIBLE);

        String[] fontColorArray = getResources().getStringArray(R.array.font_colors);
        spinnerFontColor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fontColorArray));
        spinnerFontColor.setText(isPresetColor(currentTextColor, true) ? fontColorArray[loadedFontColorPos] : getString(R.string.template_custom), false);

        String[] bgColorArray = getResources().getStringArray(R.array.background_colors);
        spinnerBackgroundColor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bgColorArray));
        spinnerBackgroundColor.setText(isPresetColor(currentBackgroundColor, false) ? bgColorArray[loadedBgColorPos] : getString(R.string.template_custom), false);
    }

    private boolean isPresetColor(int color, boolean isText) {
        String[] arr = isText ? getResources().getStringArray(R.array.font_colors) : getResources().getStringArray(R.array.background_colors);
        for (int i = 0; i < arr.length; i++) {
            if ((isText ? getFontColorFromPosition(i) : getBackgroundColorFromPosition(i)) == color) return true;
        }
        return false;
    }

    private void setupListeners() {
        serviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) checkOverlayPermission(); else stopOverlayService();
            saveServiceState(isChecked);
        });

        switchLockPosition.setOnCheckedChangeListener((buttonView, isChecked) -> saveSettingsAndNotifyService());
        switchStaticText.setOnCheckedChangeListener((buttonView, isChecked) -> { updateVisibility(); saveSettingsAndNotifyService(); });
        switchZikrMode.setOnCheckedChangeListener((buttonView, isChecked) -> { updateVisibility(); saveSettingsAndNotifyService(); });
        sliderZikrDuration.addOnChangeListener((slider, value, fromUser) -> updateZikrDurationLabel(value));
        btnAddNewZikr.setOnClickListener(v -> addNewZikrItemView(""));

        spinnerFontColor.setOnItemClickListener((parent, view, position, id) -> {
            sharedPreferences.edit().putInt(KEY_FONT_COLOR, position).apply();
            currentTextColor = getFontColorFromPosition(position);
            btnPickTextColor.setBackgroundColor(currentTextColor);
            saveSettingsAndNotifyService();
        });

        btnPickTextColor.setOnClickListener(v -> openColorPicker(true));
        switchBackgroundEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleBackgroundSettings(isChecked);
            saveSettingsAndNotifyService();
        });

        spinnerBackgroundColor.setOnItemClickListener((parent, view, position, id) -> {
            sharedPreferences.edit().putInt(KEY_BACKGROUND_COLOR, position).apply();
            currentBackgroundColor = getBackgroundColorFromPosition(position);
            btnPickBgColor.setBackgroundColor(currentBackgroundColor);
            saveSettingsAndNotifyService();
        });

        btnPickBgColor.setOnClickListener(v -> openColorPicker(false));

        Slider.OnSliderTouchListener sliderSaveListener = new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(@NonNull Slider slider) {}
            @Override public void onStopTrackingTouch(@NonNull Slider slider) { saveSettingsAndNotifyService(); }
        };

        sliderFontSize.addOnSliderTouchListener(sliderSaveListener);
        sliderTextOpacity.addOnSliderTouchListener(sliderSaveListener);
        sliderBackgroundOpacity.addOnSliderTouchListener(sliderSaveListener);
        sliderCornerRadius.addOnSliderTouchListener(sliderSaveListener);
        sliderZikrDuration.addOnSliderTouchListener(sliderSaveListener);

        findViewById(R.id.btn_reset_font_size).setOnClickListener(v -> { sliderFontSize.setValue(16.0f); saveSettingsAndNotifyService(); });
        findViewById(R.id.btn_reset_text_opacity).setOnClickListener(v -> { sliderTextOpacity.setValue(100.0f); saveSettingsAndNotifyService(); });
        findViewById(R.id.btn_reset_bg_opacity).setOnClickListener(v -> { sliderBackgroundOpacity.setValue(93.0f); saveSettingsAndNotifyService(); });
        findViewById(R.id.btn_reset_corner_radius).setOnClickListener(v -> { sliderCornerRadius.setValue(4.0f); saveSettingsAndNotifyService(); });
    }

    private void toggleBackgroundSettings(boolean isEnabled) {
        findViewById(R.id.text_layout_background_color).setEnabled(isEnabled);
        findViewById(R.id.slider_background_opacity).setEnabled(isEnabled);
        findViewById(R.id.slider_corner_radius).setEnabled(isEnabled);
        btnPickBgColor.setEnabled(isEnabled);
    }

    private void openColorPicker(boolean isTextColor) {
        new AmbilWarnaDialog(this, isTextColor ? currentTextColor : currentBackgroundColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                if (isTextColor) { currentTextColor = color; btnPickTextColor.setBackgroundColor(color); spinnerFontColor.setText(getString(R.string.template_custom), false); }
                else { currentBackgroundColor = color; btnPickBgColor.setBackgroundColor(color); spinnerBackgroundColor.setText(getString(R.string.template_custom), false); }
                saveSettingsAndNotifyService();
            }
            @Override public void onCancel(AmbilWarnaDialog dialog) {}
        }).show();
    }

    private void updateVisibility() {
        boolean isStatic = switchStaticText.isChecked();
        boolean isZikr = switchZikrMode.isChecked();
        cardNormalTextSettings.setVisibility(isStatic ? View.VISIBLE : View.GONE);
        cardZikrSettings.setVisibility(isZikr ? View.VISIBLE : View.GONE);
        cardTextAppearance.setVisibility((isStatic || isZikr) ? View.VISIBLE : View.GONE);
    }

    private void updateZikrDurationLabel(float value) { labelZikrDuration.setText(getString(R.string.label_zikr_duration) + ": " + (int) value + "s"); }

    private void addNewZikrItemView(String zikrText) {
        View v = LayoutInflater.from(this).inflate(R.layout.item_zikr_edit, zikrListContainer, false);
        ((TextInputEditText)v.findViewById(R.id.edit_text_zikr_item)).setText(zikrText);
        v.findViewById(R.id.btn_delete_zikr_item).setOnClickListener(view -> zikrListContainer.removeView(v));
        zikrListContainer.addView(v);
    }

    private void loadAndDisplayZikrList() {
        zikrListContainer.removeAllViews();
        String zikrString = sharedPreferences.getString(KEY_ZIKR_LIST, "");
        if (zikrString.isEmpty()) { setupDefaultZikrList(); zikrString = sharedPreferences.getString(KEY_ZIKR_LIST, ""); }
        for (String zikr : zikrString.split(ZIKR_DELIMITER)) { if (!zikr.trim().isEmpty()) addNewZikrItemView(zikr); }
    }

    private void saveSettingsAndNotifyService() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_STATIC_TEXT_ENABLED, switchStaticText.isChecked());
        editor.putBoolean(KEY_ZIKR_MODE_ENABLED, switchZikrMode.isChecked());
        editor.putInt(KEY_ZIKR_DURATION, (int) sliderZikrDuration.getValue());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < zikrListContainer.getChildCount(); i++) {
            String text = ((TextInputEditText)zikrListContainer.getChildAt(i).findViewById(R.id.edit_text_zikr_item)).getText().toString().trim();
            if (!text.isEmpty()) sb.append(text).append(ZIKR_DELIMITER);
        }
        editor.putString(KEY_ZIKR_LIST, sb.toString());
        editor.putString(KEY_OVERLAY_TEXT, editTextCustom.getText().toString());
        editor.putInt(KEY_FONT_SIZE, (int) sliderFontSize.getValue());
        editor.putInt(KEY_OPACITY, (int) sliderTextOpacity.getValue());
        editor.putBoolean(KEY_BACKGROUND_ENABLED, switchBackgroundEnabled.isChecked());
        editor.putInt(KEY_BACKGROUND_OPACITY, (int) sliderBackgroundOpacity.getValue());
        editor.putInt(KEY_CORNER_RADIUS, (int) sliderCornerRadius.getValue());
        editor.putInt(KEY_FONT_COLOR_INT, currentTextColor);
        editor.putInt(KEY_BACKGROUND_COLOR_INT, currentBackgroundColor);
        editor.putBoolean(KEY_POSITION_LOCKED, switchLockPosition.isChecked());
        editor.apply();

        if (serviceSwitch.isChecked() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))) {
            Intent intent = new Intent(this, OverlayService.class).setAction(OverlayService.ACTION_UPDATE_SETTINGS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent);
        }
    }

    private void loadSettings() {
        serviceSwitch.setChecked(sharedPreferences.getBoolean(KEY_SERVICE_ENABLED, false));
        switchLockPosition.setChecked(sharedPreferences.getBoolean(KEY_POSITION_LOCKED, false));
        switchStaticText.setChecked(sharedPreferences.getBoolean(KEY_STATIC_TEXT_ENABLED, true));
        switchZikrMode.setChecked(sharedPreferences.getBoolean(KEY_ZIKR_MODE_ENABLED, false));
        sliderZikrDuration.setValue(sharedPreferences.getInt(KEY_ZIKR_DURATION, 5));
        editTextCustom.setText(sharedPreferences.getString(KEY_OVERLAY_TEXT, "আল্লাহ (ﷲ) আমাকে দেখছেন"));
        sliderFontSize.setValue(sharedPreferences.getInt(KEY_FONT_SIZE, 16));
        sliderTextOpacity.setValue(sharedPreferences.getInt(KEY_OPACITY, 100));
        sliderBackgroundOpacity.setValue(sharedPreferences.getInt(KEY_BACKGROUND_OPACITY, 93));
        sliderCornerRadius.setValue(sharedPreferences.getInt(KEY_CORNER_RADIUS, 4));
        boolean bgEnabled = sharedPreferences.getBoolean(KEY_BACKGROUND_ENABLED, true);
        switchBackgroundEnabled.setChecked(bgEnabled);
        toggleBackgroundSettings(bgEnabled);

        loadedFontColorPos = sharedPreferences.getInt(KEY_FONT_COLOR, 0);
        currentTextColor = sharedPreferences.getInt(KEY_FONT_COLOR_INT, getFontColorFromPosition(loadedFontColorPos));
        btnPickTextColor.setBackgroundColor(currentTextColor);

        loadedBgColorPos = sharedPreferences.getInt(KEY_BACKGROUND_COLOR, 0);
        currentBackgroundColor = sharedPreferences.getInt(KEY_BACKGROUND_COLOR_INT, getBackgroundColorFromPosition(loadedBgColorPos));
        btnPickBgColor.setBackgroundColor(currentBackgroundColor);
    }

    @Override protected void onResume() { super.onResume(); if (shouldShowPermissionDialog) { showRestrictedPermissionGuideDialog(); shouldShowPermissionDialog = false; } else checkAndShowPermissionGuide(); }
    @Override protected void onPause() { super.onPause(); saveSettingsAndNotifyService(); }

    private void startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return;
        saveServiceState(true);
        Intent intent = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent); else startService(intent);
    }

    private void stopOverlayService() { saveServiceState(false); stopService(new Intent(this, OverlayService.class)); }
    private void saveServiceState(boolean isEnabled) { sharedPreferences.edit().putBoolean(KEY_SERVICE_ENABLED, isEnabled).apply(); }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this))
            overlayPermissionLauncher.launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        else startOverlayService();
    }

    private void registerOverlayPermissionLauncher() {
        overlayPermissionLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) startOverlayService();
            else { serviceSwitch.setChecked(false); if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) shouldShowPermissionDialog = true; }
        });
    }

    private void showRestrictedPermissionGuideDialog() {
        new AlertDialog.Builder(this).setTitle("Permission Required").setMessage("Please allow 'Display over other apps' in settings.")
            .setPositiveButton("Settings", (d, w) -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null))))
            .setNegativeButton("Cancel", null).show();
    }

    private void showLanguagePicker() {
        String[] langs = {"English", "বাংলা"};
        new AlertDialog.Builder(this).setTitle("Language").setSingleChoiceItems(langs, sharedPreferences.getString(KEY_LANGUAGE, "bn").equals("bn") ? 1 : 0, (d, w) -> {
            sharedPreferences.edit().putString(KEY_LANGUAGE, w == 1 ? "bn" : "en").apply(); loadLocale(); recreate(); d.dismiss();
        }).show();
    }

    private void checkFirstLaunch() { if (sharedPreferences.getBoolean(KEY_IS_FIRST_LAUNCH, true)) { showHelpDialog(false, false); sharedPreferences.edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply(); } }

    private void checkAndShowPermissionGuide() {
        boolean hasD = true, hasB = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasD = Settings.canDrawOverlays(this);
            hasB = ((PowerManager) getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName());
        }
        if (!hasD || !hasB) showHelpDialog(hasD, hasB);
    }

    private void showHelpDialog(boolean hasD, boolean hasB) {
        View v = getLayoutInflater().inflate(R.layout.dialog_help_guide, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(v).create();
        Button bD = v.findViewById(R.id.btn_permission_display), bB = v.findViewById(R.id.btn_permission_battery);
        if (hasD) { bD.setText("Granted"); bD.setEnabled(false); }
        if (hasB) { bB.setText("Granted"); bB.setEnabled(false); }
        bD.setOnClickListener(view -> { overlayPermissionLauncher.launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); dialog.dismiss(); });
        bB.setOnClickListener(view -> { try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()))); } catch (Exception e) {} dialog.dismiss(); });
        dialog.show();
    }

    private void registerBackupRestoreLaunchers() {
        backupLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> { if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) performBackup(r.getData().getData()); });
        restoreLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> { if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) { performRestore(r.getData().getData()); recreate(); } });
    }

    private void launchBackup() { backupLauncher.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/xml").putExtra(Intent.EXTRA_TITLE, "PureVision_Backup.xml")); }
    private void launchRestore() { restoreLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/xml")); }

    private void performBackup(Uri uri) {
        try (InputStream in = new FileInputStream(new File(getFilesDir().getParent(), "shared_prefs/" + SHARED_PREFS_NAME + ".xml"));
             OutputStream out = getContentResolver().openOutputStream(uri)) {
            byte[] b = new byte[1024]; int l; while ((l = in.read(b)) > 0) out.write(b, 0, l);
            Toast.makeText(this, "Backup Success", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void performRestore(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(new File(getFilesDir().getParent(), "shared_prefs/" + SHARED_PREFS_NAME + ".xml"))) {
            byte[] b = new byte[1024]; int l; while ((l = in.read(b)) > 0) out.write(b, 0, l);
            Toast.makeText(this, "Restore Success", Toast.LENGTH_SHORT).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int getFontColorFromPosition(int p) {
        int[] c = {Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
        return (p >= 0 && p < c.length) ? c[p] : Color.WHITE;
    }

    private int getBackgroundColorFromPosition(int p) {
        int[] c = {Color.TRANSPARENT, Color.BLACK, Color.WHITE, Color.DKGRAY, Color.RED, Color.BLUE};
        return (p >= 0 && p < c.length) ? c[p] : Color.TRANSPARENT;
    }
}
