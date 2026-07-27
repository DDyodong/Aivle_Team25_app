package com.example.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.LocaleListCompat;

import java.io.File;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

public class MainActivity extends AppCompatActivity {
    private static final int BG = Color.rgb(6, 16, 26);
    private static final int SURFACE = Color.rgb(11, 27, 41);
    private static final int SURFACE_ALT = Color.rgb(13, 32, 47);
    private static final int ORANGE = Color.rgb(255, 116, 23);
    private static final int CYAN = Color.rgb(48, 198, 242);
    private static final int GREEN = Color.rgb(54, 211, 154);
    private static final int RED = Color.rgb(255, 82, 99);
    private static final int TEXT = Color.rgb(236, 244, 251);
    private static final int MUTED = Color.rgb(130, 147, 166);
    private static final int BORDER = Color.rgb(43, 64, 82);

    private static final String[] RISK_CODES = {
            "FALL_HEIGHT", "PPE_MISSING", "FIRE_EXPLOSION", "EQUIPMENT_FAILURE",
            "COLLISION_PINCH", "FALLING_OBJECT_LIFTING", "ELECTRICAL", "ASPHYXIATION_GAS",
            "HAZARDOUS_LEAK", "DANGER_ZONE_ACCESS", "HOUSEKEEPING", "OTHER"
    };
    private static final String[] LANGUAGE_NAMES = {
            "한국어", "English", "Tiếng Việt", "中文", "नेपाली", "O‘zbekcha",
            "සිංහල", "தமிழ்", "Bahasa Indonesia", "ไทย", "Filipino", "မြန်မာ"
    };
    private static final String[] LANGUAGE_TAGS = {
            "ko", "en", "vi", "zh", "ne", "uz", "si", "ta", "id", "th", "fil", "my"
    };

    private enum PhotoPurpose { PPE, REPORT }

    private FrameLayout root;
    private ApiClient apiClient;
    private SessionStore sessionStore;
    private ApiClient.Session session;
    private final AppTaskRunner taskRunner = new AppTaskRunner();
    private int currentTab;

    private ApiClient.Permit permit;
    private boolean permitLoaded;
    private boolean permitLoading;
    private ApiClient.TbmBriefing briefing;
    private boolean briefingLoading;
    private ApiClient.PpeCheck personalCheck;
    private boolean personalCheckLoaded;
    private boolean personalCheckLoading;
    private List<ApiClient.SafetyReport> reports = new ArrayList<>();
    private boolean reportsLoaded;
    private boolean reportsLoading;

    private byte[] ppePhoto;
    private byte[] reportPhoto;
    private boolean safetyShoesChecked;
    private boolean glovesChecked;
    private boolean workwearChecked;
    private int reportTypeIndex;
    private String reportDescription = "";
    private PhotoPurpose photoPurpose;
    private File pendingCameraFile;

    private TextToSpeech textToSpeech;
    private boolean ttsReady;

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), success -> {
                if (success && pendingCameraFile != null) {
                    Bitmap bitmap = BitmapFactory.decodeFile(pendingCameraFile.getAbsolutePath());
                    setPhoto(bitmap);
                }
            });
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                try (InputStream stream = getContentResolver().openInputStream(uri)) {
                    setPhoto(BitmapFactory.decodeStream(stream));
                } catch (Exception exception) {
                    toast(getString(R.string.ws_gallery_failed));
                }
            });
    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) launchCamera();
                else toast(getString(R.string.ws_camera_permission));
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        setContentView(root);
        apiClient = new ApiClient(getString(R.string.api_base_url));
        sessionStore = new SessionStore(this);
        initializeTextToSpeech();
        session = sessionStore.load();
        if (session != null && "demo".equals(session.token)) {
            sessionStore.clear();
            session = null;
        }
        if (session != null && session.isWorker()) showShell(0); else showLogin();
    }

    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (!ttsReady) toast(getString(R.string.tts_initialization_failed));
        });
    }

    private void showLogin() {
        root.removeAllViews();
        LinearLayout page = contentColumn();
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.addView(space(52));
        TextView mark = text("⚓", 42, ORANGE, true);
        mark.setGravity(Gravity.CENTER);
        page.addView(mark, fullWrap());
        TextView brand = text("SMART SHIPYARD", 25, TEXT, true);
        brand.setGravity(Gravity.CENTER);
        page.addView(brand, fullWrap());
        TextView subtitle = text(getString(R.string.ws_app_subtitle), 13, CYAN, true);
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle, fullWrap());
        page.addView(space(36));

        LinearLayout box = card();
        box.addView(text(getString(R.string.ws_login_title), 22, TEXT, true));
        box.addView(label(getString(R.string.ws_login_instruction)));
        box.addView(space(20));
        EditText username = input(getString(R.string.ws_username));
        box.addView(username, fullHeight(54));
        box.addView(space(12));
        EditText password = input(getString(R.string.ws_password));
        password.setInputType(0x00000081);
        box.addView(password, fullHeight(54));
        box.addView(space(18));
        Button login = primaryButton(getString(R.string.ws_login));
        login.setOnClickListener(view -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString();
            if (user.isEmpty() || pass.isEmpty()) {
                toast(getString(R.string.ws_login_required));
                return;
            }
            login.setEnabled(false);
            login.setText(R.string.ws_logging_in);
            runAsync(() -> apiClient.login(user, pass), next -> {
                if (!next.isWorker()) {
                    login.setEnabled(true);
                    login.setText(R.string.ws_login);
                    toast(getString(R.string.ws_worker_only));
                    return;
                }
                session = next;
                sessionStore.save(next);
                resetData();
                showShell(0);
            }, error -> {
                login.setEnabled(true);
                login.setText(R.string.ws_login);
            });
        });
        box.addView(login, fullHeight(54));
        page.addView(box, fullWrap());
        root.addView(scroll(page), matchMatch());
    }

    private void showShell(int selected) {
        currentTab = selected;
        root.removeAllViews();
        LinearLayout shell = column();
        shell.addView(header(selected), fullWrap());
        View content = selected == 0 ? home() : selected == 1 ? tbm() : selected == 2
                ? personalCheck() : selected == 3 ? report() : myPage();
        shell.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        shell.addView(bottomNav(selected), fullHeight(74));
        root.addView(shell, matchMatch());
    }

    private View header(int selected) {
        String[] titles = getResources().getStringArray(R.array.ws_page_titles);
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(14), dp(14), dp(12));
        LinearLayout names = column();
        names.addView(text(titles[selected], 20, TEXT, true));
        names.addView(text(session == null ? getString(R.string.ws_worker)
                : displayName() + " · " + getString(R.string.ws_worker), 10, MUTED, false));
        header.addView(names, weight());
        TextView avatar = text(session == null || displayName().isEmpty() ? "W" : displayName().substring(0, 1), 15, CYAN, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(shape(SURFACE_ALT, BORDER, 40));
        header.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));
        return header;
    }

    private View home() {
        LinearLayout page = contentColumn();
        if (!permitLoaded && !permitLoading) loadPermit();
        if (permitLoading) {
            page.addView(statusCard(getString(R.string.ws_today_loading), CYAN));
            return scroll(page);
        }
        if (permitLoaded && permit == null) {
            page.addView(statusCard(getString(R.string.ws_no_work), MUTED));
            Button retry = outlineButton(getString(R.string.ws_refresh));
            retry.setOnClickListener(view -> { permitLoaded = false; loadPermit(); });
            page.addView(space(12));
            page.addView(retry, fullHeight(50));
            return scroll(page);
        }
        if (permit == null) {
            page.addView(statusCard(getString(R.string.ws_work_after_server), MUTED));
            return scroll(page);
        }

        LinearLayout hero = card();
        hero.addView(text(statusText(permit.status), 11, statusColor(permit.status), true));
        hero.addView(space(9));
        hero.addView(text(empty(permit.workTitle, getString(R.string.ws_work_untitled)), 21, TEXT, true));
        hero.addView(label(empty(permit.permitNo, getString(R.string.ws_no_permit_number)) + " · " + locationText()));
        hero.addView(space(14));
        hero.addView(infoRow(getString(R.string.ws_work_time), timeText()));
        hero.addView(infoRow(getString(R.string.ws_work_type), empty(permit.workType, getString(R.string.ws_unspecified))));
        hero.addView(infoRow(getString(R.string.ws_site_risk), permit.highRisk
                ? getString(R.string.ws_high_risk) : getString(R.string.ws_normal_work)));
        page.addView(hero, fullWrap());
        page.addView(space(16));

        LinearLayout conditions = card();
        conditions.addView(text(getString(R.string.ws_required_conditions), 16, CYAN, true));
        conditions.addView(space(8));
        conditions.addView(label(cleanConditions(permit.conditions)));
        page.addView(conditions, fullWrap());
        page.addView(space(20));

        page.addView(sectionTitle(getString(R.string.ws_before_work), getString(R.string.ws_complete_in_order)));
        page.addView(actionCard("1", getString(R.string.ws_tbm_listen), briefing != null && briefing.confirmed
                ? getString(R.string.ws_confirmed) : getString(R.string.ws_listen_and_confirm), 1));
        page.addView(actionCard("2", getString(R.string.ws_personal_ppe_check), personalCheck != null && personalCheck.passed
                ? getString(R.string.ws_analysis_complete) : getString(R.string.ws_photo_ppe_check), 2));
        page.addView(space(12));
        Button danger = dangerButton(getString(R.string.ws_report_hazard));
        danger.setOnClickListener(view -> showShell(3));
        page.addView(danger, fullHeight(56));
        page.addView(space(22));
        return scroll(page);
    }

    private View tbm() {
        LinearLayout page = contentColumn();
        if (briefing == null && !briefingLoading) loadBriefing();
        if (briefingLoading) {
            page.addView(statusCard(getString(R.string.ws_tbm_loading), CYAN));
            return scroll(page);
        }
        if (briefing == null) {
            page.addView(statusCard(getString(R.string.ws_no_tbm), MUTED));
            return scroll(page);
        }

        LinearLayout box = card();
        box.addView(text(empty(briefing.title, getString(R.string.ws_today_tbm)), 20, TEXT, true));
        box.addView(space(8));
        box.addView(label(getString(R.string.ws_tbm_instruction)));
        box.addView(space(18));
        TextView content = text(empty(briefing.content, getString(R.string.ws_no_tbm_content)), 15, TEXT, false);
        content.setLineSpacing(0, 1.45f);
        box.addView(content, fullWrap());
        box.addView(space(18));
        LinearLayout actions = row();
        Button play = outlineButton(getString(R.string.ws_listen_voice));
        play.setTextColor(CYAN);
        play.setOnClickListener(view -> speak(content.getText().toString(), currentLanguageTag()));
        Button stop = outlineButton(getString(R.string.ws_stop));
        stop.setOnClickListener(view -> stopSpeaking());
        actions.addView(play, weightHeight(50));
        actions.addView(horizontalSpace(10));
        actions.addView(stop, weightHeight(50));
        box.addView(actions, fullWrap());
        page.addView(box, fullWrap());
        page.addView(space(18));

        Button confirm = primaryButton(briefing.confirmed ? getString(R.string.ws_tbm_done) : getString(R.string.ws_i_confirmed));
        confirm.setEnabled(!briefing.confirmed);
        confirm.setOnClickListener(view -> {
            confirm.setEnabled(false);
            confirm.setText(R.string.ws_confirming);
            runAsync(() -> {
                apiClient.confirmTbm(session.token, briefing.permitId);
                return true;
            }, ignored -> {
                briefing = new ApiClient.TbmBriefing(briefing.permitId, briefing.title, briefing.content, true);
                toast(getString(R.string.ws_tbm_confirmed_message));
                showShell(1);
            }, error -> { confirm.setEnabled(true); confirm.setText(R.string.ws_i_confirmed); });
        });
        page.addView(confirm, fullHeight(54));
        page.addView(space(22));
        return scroll(page);
    }

    private View personalCheck() {
        LinearLayout page = contentColumn();
        if (!personalCheckLoaded && !personalCheckLoading) loadPersonalCheck();
        if (personalCheck != null && personalCheck.passed) {
            page.addView(statusCard(getString(R.string.ws_ppe_done), GREEN));
            page.addView(space(16));
        }

        LinearLayout guide = card();
        guide.addView(text(getString(R.string.ws_ai_ppe_title), 18, TEXT, true));
        guide.addView(space(7));
        guide.addView(label(getString(R.string.ws_ai_ppe_guide)));
        page.addView(guide, fullWrap());
        page.addView(space(16));

        ImageView preview = photoPreview(ppePhoto, getString(R.string.ws_ppe_photo));
        page.addView(preview, fullHeight(220));
        page.addView(space(10));
        LinearLayout photos = row();
        Button camera = outlineButton(getString(R.string.ws_camera));
        camera.setOnClickListener(view -> openCamera(PhotoPurpose.PPE));
        Button gallery = outlineButton(getString(R.string.ws_gallery));
        gallery.setOnClickListener(view -> openGallery(PhotoPurpose.PPE));
        photos.addView(camera, weightHeight(50));
        photos.addView(horizontalSpace(10));
        photos.addView(gallery, weightHeight(50));
        page.addView(photos, fullWrap());
        page.addView(space(20));

        page.addView(sectionTitle(getString(R.string.ws_manual_items), getString(R.string.ws_unsupported_by_model)));
        CheckBox shoes = checkBox(getString(R.string.ws_safety_shoes_check), safetyShoesChecked);
        shoes.setOnCheckedChangeListener((button, checked) -> safetyShoesChecked = checked);
        page.addView(shoes, fullWrap());
        CheckBox gloves = checkBox(getString(R.string.ws_gloves_check), glovesChecked);
        gloves.setOnCheckedChangeListener((button, checked) -> glovesChecked = checked);
        page.addView(gloves, fullWrap());
        CheckBox workwear = checkBox(getString(R.string.ws_workwear_check), workwearChecked);
        workwear.setOnCheckedChangeListener((button, checked) -> workwearChecked = checked);
        page.addView(workwear, fullWrap());
        page.addView(space(16));

        if (personalCheck != null) {
            page.addView(ppeResultCard(personalCheck), fullWrap());
            page.addView(space(16));
        }
        Button submit = primaryButton(getString(R.string.ws_submit_ppe));
        submit.setOnClickListener(view -> submitPersonalCheck(submit));
        page.addView(submit, fullHeight(56));
        page.addView(space(22));
        return scroll(page);
    }

    private View report() {
        LinearLayout page = contentColumn();
        if (!reportsLoaded && !reportsLoading) loadReports();
        LinearLayout form = card();
        form.addView(text(getString(R.string.ws_report_title), 20, TEXT, true));
        form.addView(label(getString(R.string.ws_report_guide)));
        form.addView(space(18));
        form.addView(text(getString(R.string.ws_risk_type), 13, TEXT, true));
        Spinner risk = spinner(getResources().getStringArray(R.array.ws_risk_labels));
        risk.setSelection(reportTypeIndex);
        risk.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> reportTypeIndex = position));
        form.addView(risk, fullHeight(54));
        form.addView(space(16));

        ImageView preview = photoPreview(reportPhoto, getString(R.string.ws_hazard_photo));
        form.addView(preview, fullHeight(210));
        form.addView(space(10));
        LinearLayout photoActions = row();
        Button camera = outlineButton(getString(R.string.ws_camera));
        camera.setOnClickListener(view -> openCamera(PhotoPurpose.REPORT));
        Button gallery = outlineButton(getString(R.string.ws_gallery));
        gallery.setOnClickListener(view -> openGallery(PhotoPurpose.REPORT));
        photoActions.addView(camera, weightHeight(48));
        photoActions.addView(horizontalSpace(10));
        photoActions.addView(gallery, weightHeight(48));
        form.addView(photoActions, fullWrap());
        form.addView(space(16));

        EditText description = input(getString(R.string.ws_report_hint));
        description.setSingleLine(false);
        description.setMinLines(4);
        description.setGravity(Gravity.TOP);
        description.setText(reportDescription);
        description.setPadding(dp(14), dp(14), dp(14), dp(14));
        form.addView(description, fullWrap());
        form.addView(space(16));
        Button submit = dangerButton(getString(R.string.ws_submit_report));
        submit.setOnClickListener(view -> {
            reportDescription = description.getText().toString().trim();
            submitReport(submit);
        });
        form.addView(submit, fullHeight(56));
        page.addView(form, fullWrap());
        page.addView(space(22));

        page.addView(sectionTitle(getString(R.string.ws_my_reports), reportsLoading
                ? getString(R.string.ws_loading) : getString(R.string.ws_count, reports.size())));
        if (reportsLoaded && reports.isEmpty()) page.addView(statusCard(getString(R.string.ws_no_reports), MUTED));
        for (int index = 0; index < Math.min(5, reports.size()); index++) {
            page.addView(reportCard(reports.get(index)), fullWrap());
        }
        page.addView(space(22));
        return scroll(page);
    }

    private View myPage() {
        LinearLayout page = contentColumn();
        LinearLayout profile = card();
        profile.addView(text(displayName(), 21, TEXT, true));
        profile.addView(label(getString(R.string.ws_worker) + " · " + session.username));
        profile.addView(space(12));
        profile.addView(infoRow(getString(R.string.ws_permissions), getString(R.string.ws_permissions_value)));
        page.addView(profile, fullWrap());
        page.addView(space(18));

        LinearLayout language = card();
        language.addView(text(getString(R.string.ws_app_language), 17, TEXT, true));
        language.addView(label(getString(R.string.ws_app_language_guide)));
        language.addView(space(12));
        Spinner selector = spinner(LANGUAGE_NAMES);
        int active = languageIndex(currentLanguageTag());
        selector.setSelection(active);
        selector.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> {
            if (!LANGUAGE_TAGS[position].equals(currentLanguageTag())) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(LANGUAGE_TAGS[position]));
            }
        }));
        language.addView(selector, fullHeight(54));
        page.addView(language, fullWrap());
        page.addView(space(18));

        Button logout = outlineButton(getString(R.string.ws_logout));
        logout.setTextColor(RED);
        logout.setOnClickListener(view -> logout());
        page.addView(logout, fullHeight(52));
        page.addView(space(22));
        return scroll(page);
    }

    private void loadPermit() {
        permitLoading = true;
        runAsync(() -> apiClient.getTodayPermit(session.token), value -> {
            permit = value;
            permitLoaded = true;
            permitLoading = false;
            if (currentTab == 0) showShell(0);
        }, error -> {
            permitLoading = false;
            permitLoaded = true;
            if (currentTab == 0) showShell(0);
        });
    }

    private void loadBriefing() {
        briefingLoading = true;
        String language = currentLanguageTag();
        runAsync(() -> apiClient.getTodayTbm(session.token, language), value -> {
            briefing = value;
            briefingLoading = false;
            if (currentTab == 1) showShell(1);
        }, error -> briefingLoading = false);
    }

    private void loadPersonalCheck() {
        personalCheckLoading = true;
        runAsync(() -> apiClient.getTodayPersonalCheck(session.token), value -> {
            personalCheck = value;
            personalCheckLoaded = true;
            personalCheckLoading = false;
            if (currentTab == 2) showShell(2);
        }, error -> { personalCheckLoaded = true; personalCheckLoading = false; });
    }

    private void loadReports() {
        reportsLoading = true;
        runAsync(() -> apiClient.getMyReports(session.token), values -> {
            reports = values;
            reportsLoaded = true;
            reportsLoading = false;
            if (currentTab == 3) showShell(3);
        }, error -> { reportsLoaded = true; reportsLoading = false; });
    }

    private void submitPersonalCheck(Button button) {
        if (ppePhoto == null) { toast(getString(R.string.ws_photo_required)); return; }
        if (!safetyShoesChecked || !glovesChecked || !workwearChecked) {
            toast(getString(R.string.ws_manual_required));
            return;
        }
        button.setEnabled(false);
        button.setText(R.string.ws_analyzing_ppe);
        runAsync(() -> {
            long fileId = apiClient.uploadImage(session.token, ppePhoto, "ppe_check", "ppe-check.jpg");
            return apiClient.submitPersonalCheck(session.token, permit == null ? null : permit.id, fileId,
                    safetyShoesChecked, glovesChecked, workwearChecked);
        }, value -> {
            personalCheck = value;
            personalCheckLoaded = true;
            toast(empty(value.message, value.passed ? getString(R.string.ws_ppe_submit_done) : getString(R.string.ws_ppe_retry)));
            showShell(2);
        }, error -> { button.setEnabled(true); button.setText(R.string.ws_submit_ppe); });
    }

    private void submitReport(Button button) {
        if (reportPhoto == null) { toast(getString(R.string.ws_report_photo_required)); return; }
        if (reportDescription.isEmpty()) { toast(getString(R.string.ws_report_detail_required)); return; }
        button.setEnabled(false);
        button.setText(R.string.ws_submitting_report);
        runAsync(() -> {
            long fileId = apiClient.uploadImage(session.token, reportPhoto, "safety_report", "safety-report.jpg");
            return apiClient.createSafetyEvent(session.token, RISK_CODES[reportTypeIndex], fileId, reportDescription);
        }, reportNo -> {
            toast(getString(R.string.ws_report_done, empty(reportNo, getString(R.string.ws_report))));
            reportPhoto = null;
            reportDescription = "";
            reportsLoaded = false;
            showShell(3);
        }, error -> { button.setEnabled(true); button.setText(R.string.ws_submit_report); });
    }

    private void logout() {
        String token = session.token;
        sessionStore.clear();
        session = null;
        resetData();
        runAsync(() -> { apiClient.logout(token); return true; }, ignored -> {}, ignored -> {});
        showLogin();
    }

    private void resetData() {
        permit = null;
        permitLoaded = false;
        permitLoading = false;
        briefing = null;
        briefingLoading = false;
        personalCheck = null;
        personalCheckLoaded = false;
        reports = new ArrayList<>();
        reportsLoaded = false;
        ppePhoto = null;
        reportPhoto = null;
    }

    private void openCamera(PhotoPurpose purpose) {
        photoPurpose = purpose;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        try {
            pendingCameraFile = File.createTempFile("worker-photo-", ".jpg", getCacheDir());
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pendingCameraFile);
            cameraLauncher.launch(uri);
        } catch (Exception exception) {
            toast(getString(R.string.ws_camera_failed));
        }
    }

    private void openGallery(PhotoPurpose purpose) {
        photoPurpose = purpose;
        galleryLauncher.launch("image/*");
    }

    private void setPhoto(Bitmap bitmap) {
        if (bitmap == null) { toast(getString(R.string.ws_photo_failed)); return; }
        byte[] bytes = ImageCodec.toUploadJpeg(bitmap);
        if (bytes.length > 10 * 1024 * 1024) { toast(getString(R.string.ws_photo_too_large)); return; }
        if (photoPurpose == PhotoPurpose.PPE) ppePhoto = bytes; else reportPhoto = bytes;
        showShell(photoPurpose == PhotoPurpose.PPE ? 2 : 3);
    }

    private void speak(String message, String languageTag) {
        if (!ttsReady || textToSpeech == null) { toast(getString(R.string.tts_not_ready)); return; }
        Locale locale = Locale.forLanguageTag(languageTag);
        int availability = textToSpeech.isLanguageAvailable(locale);
        if (availability == TextToSpeech.LANG_MISSING_DATA) {
            toast(getString(R.string.tts_language_data_missing));
            try { startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); }
            catch (Exception ignored) { toast(getString(R.string.tts_install_unavailable)); }
            return;
        }
        if (availability == TextToSpeech.LANG_NOT_SUPPORTED) {
            toast(getString(R.string.tts_language_not_supported));
            return;
        }
        textToSpeech.setLanguage(locale);
        if (textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "worker_tbm") == TextToSpeech.ERROR) {
            toast(getString(R.string.tts_playback_failed));
        }
    }

    private void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) textToSpeech.stop();
    }

    private String currentLanguageTag() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        Locale locale = locales.isEmpty() ? Locale.getDefault() : locales.get(0);
        return locale == null || locale.getLanguage().isEmpty() ? "ko" : locale.getLanguage();
    }

    private <T> void runAsync(
            Callable<T> task,
            AppTaskRunner.Callback<T> success,
            AppTaskRunner.Callback<Exception> failure
    ) {
        taskRunner.run(task, success, exception -> {
            if (session != null && exception instanceof ApiClient.ApiException
                    && ((ApiClient.ApiException) exception).status == 401) {
                sessionStore.clear();
                session = null;
                toast(getString(R.string.ws_session_expired));
                showLogin();
            } else {
                toast(exception.getMessage() == null ? getString(R.string.ws_server_failed) : exception.getMessage());
                failure.accept(exception);
            }
        });
    }

    private LinearLayout bottomNav(int selected) {
        LinearLayout nav = row();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(4), dp(6), dp(4), dp(7));
        nav.setBackground(shape(SURFACE, BORDER, 0));
        String[] labels = getResources().getStringArray(R.array.ws_nav_labels);
        for (int index = 0; index < labels.length; index++) {
            final int tab = index;
            Button button = new Button(this);
            button.setText(labels[index]);
            button.setTextSize(10);
            button.setTextColor(index == selected ? ORANGE : MUTED);
            button.setTypeface(Typeface.DEFAULT, index == selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setAllCaps(false);
            button.setPadding(0, 0, 0, 0);
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setOnClickListener(view -> showShell(tab));
            nav.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        return nav;
    }

    private View actionCard(String number, String title, String caption, int tab) {
        LinearLayout item = row();
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(15), dp(14), dp(15), dp(14));
        item.setBackground(shape(SURFACE, BORDER, 11));
        TextView icon = text(number, 15, ORANGE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(shape(SURFACE_ALT, ORANGE, 24));
        item.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));
        item.addView(horizontalSpace(12));
        LinearLayout copy = column();
        copy.addView(text(title, 14, TEXT, true));
        copy.addView(label(caption));
        item.addView(copy, weight());
        item.addView(text("›", 24, MUTED, false));
        item.setOnClickListener(view -> showShell(tab));
        LinearLayout.LayoutParams params = fullWrap();
        params.setMargins(0, 0, 0, dp(9));
        item.setLayoutParams(params);
        return item;
    }

    private LinearLayout ppeResultCard(ApiClient.PpeCheck result) {
        LinearLayout box = card();
        int color = result.passed ? GREEN : RED;
        box.addView(text(result.passed ? getString(R.string.ws_ai_pass) : getString(R.string.ws_ai_retry), 17, color, true));
        box.addView(space(9));
        box.addView(infoRow(getString(R.string.ws_helmet), equipmentText(result.helmetOn, result.helmetConfidence)));
        box.addView(infoRow(getString(R.string.ws_harness), equipmentText(result.harnessOn, null)));
        box.addView(infoRow(getString(R.string.ws_welding_mask), equipmentText(result.weldingMaskOn, null)));
        if (!result.model.isEmpty()) box.addView(infoRow(getString(R.string.ws_model), result.model));
        return box;
    }

    private View reportCard(ApiClient.SafetyReport report) {
        LinearLayout box = card();
        LinearLayout top = row();
        top.addView(text(empty(report.reportNo, getString(R.string.ws_report)), 11, CYAN, true), weight());
        top.addView(text(reportStatus(report.status), 10, ORANGE, true));
        box.addView(top, fullWrap());
        box.addView(space(7));
        box.addView(text(empty(report.title, getString(R.string.ws_report_title)), 15, TEXT, true));
        TextView description = label(report.description);
        description.setMaxLines(2);
        box.addView(description);
        LinearLayout.LayoutParams params = fullWrap();
        params.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(params);
        return box;
    }

    private ImageView photoPreview(byte[] photo, String description) {
        ImageView preview = new ImageView(this);
        preview.setContentDescription(description);
        preview.setBackground(shape(SURFACE, BORDER, 12));
        if (photo == null) {
            preview.setImageResource(android.R.drawable.ic_menu_camera);
            preview.setColorFilter(MUTED);
            preview.setPadding(dp(70), dp(55), dp(70), dp(55));
        } else {
            preview.setImageBitmap(BitmapFactory.decodeByteArray(photo, 0, photo.length));
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
        return preview;
    }

    private CheckBox checkBox(String value, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(value);
        box.setTextColor(TEXT);
        box.setTextSize(13);
        box.setChecked(checked);
        box.setButtonTintList(android.content.res.ColorStateList.valueOf(ORANGE));
        box.setPadding(dp(12), dp(10), dp(12), dp(10));
        box.setBackground(shape(SURFACE, BORDER, 9));
        LinearLayout.LayoutParams params = fullWrap();
        params.setMargins(0, 0, 0, dp(8));
        box.setLayoutParams(params);
        return box;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextColor(TEXT); view.setTextSize(13); return view;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getDropDownView(position, convertView, parent);
                view.setTextColor(Color.DKGRAY); view.setTextSize(14); view.setPadding(dp(12), dp(12), dp(12), dp(12)); return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPadding(dp(12), 0, dp(8), 0);
        spinner.setBackground(shape(SURFACE_ALT, BORDER, 9));
        return spinner;
    }

    private LinearLayout infoRow(String title, String value) {
        LinearLayout row = row();
        row.setPadding(0, dp(5), 0, dp(5));
        row.addView(label(title), weight());
        TextView data = text(value, 12, TEXT, true);
        data.setGravity(Gravity.END);
        row.addView(data, weight());
        return row;
    }

    private View sectionTitle(String title, String caption) {
        LinearLayout row = row();
        row.setGravity(Gravity.BOTTOM);
        row.addView(text(title, 16, TEXT, true), weight());
        row.addView(text(caption, 10, MUTED, false));
        LinearLayout.LayoutParams params = fullWrap();
        params.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(params);
        return row;
    }

    private LinearLayout statusCard(String message, int color) {
        LinearLayout box = card();
        TextView value = text(message, 15, color, true);
        value.setGravity(Gravity.CENTER);
        box.addView(value, fullWrap());
        return box;
    }

    private String statusText(String status) {
        if ("approved".equals(status)) return getString(R.string.ws_status_approved);
        if ("conditionally_approved".equals(status)) return getString(R.string.ws_status_conditional);
        if ("pending_review".equals(status)) return getString(R.string.ws_status_pending);
        return "● " + empty(status, getString(R.string.ws_status_unknown));
    }

    private int statusColor(String status) {
        return "approved".equals(status) ? GREEN : "conditionally_approved".equals(status) ? ORANGE : CYAN;
    }

    private String locationText() {
        return empty(permit.blockCode, empty(permit.siteName, getString(R.string.ws_location_unknown)));
    }

    private String timeText() {
        return shortTime(permit.startTime) + " - " + shortTime(permit.endTime);
    }

    private String shortTime(String value) {
        if (value == null || value.isBlank()) return getString(R.string.ws_unspecified);
        int t = value.indexOf('T');
        String time = t >= 0 ? value.substring(t + 1) : value;
        return time.length() >= 5 ? time.substring(0, 5) : time;
    }

    private String cleanConditions(String raw) {
        if (raw == null || raw.isBlank()) return getString(R.string.ws_no_conditions);
        if (!raw.trim().startsWith("[")) return raw;
        return raw.replace("[", "").replace("]", "").replace("\"", "").replace(",", "\n• ");
    }

    private String equipmentText(Boolean worn, Double confidence) {
        String value = worn == null ? getString(R.string.ws_unavailable)
                : worn ? getString(R.string.ws_worn) : getString(R.string.ws_not_worn);
        if (confidence != null) value += " · " + NumberFormat.getPercentInstance().format(confidence);
        return value;
    }

    private String reportStatus(String status) {
        if ("received".equals(status)) return getString(R.string.ws_report_received);
        if ("in_progress".equals(status)) return getString(R.string.ws_report_processing);
        if ("resolved".equals(status)) return getString(R.string.ws_report_resolved);
        return empty(status, getString(R.string.ws_report));
    }

    private int languageIndex(String tag) {
        for (int index = 0; index < LANGUAGE_TAGS.length; index++) {
            if (LANGUAGE_TAGS[index].equals(tag)) return index;
        }
        return 0;
    }

    private String displayName() {
        return session == null ? "" : session.name;
    }

    private String empty(String value, String fallback) {
        return value == null || value.isBlank() || "null".equals(value) ? fallback : value;
    }

    private ScrollView scroll(View child) {
        ScrollView view = new ScrollView(this);
        view.setFillViewport(true);
        view.setBackgroundColor(BG);
        view.addView(child);
        return view;
    }

    private LinearLayout contentColumn() {
        LinearLayout layout = column();
        layout.setPadding(dp(18), dp(12), dp(18), dp(28));
        return layout;
    }

    private LinearLayout card() {
        LinearLayout layout = column();
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));
        layout.setBackground(shape(SURFACE, BORDER, 12));
        return layout;
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setHintTextColor(MUTED);
        input.setTextColor(TEXT);
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(shape(Color.rgb(8, 21, 34), BORDER, 9));
        return input;
    }

    private Button primaryButton(String value) {
        return coloredButton(value, ORANGE, Color.WHITE);
    }

    private Button dangerButton(String value) {
        return coloredButton(value, RED, Color.WHITE);
    }

    private Button coloredButton(String value, int background, int color) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(color);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(shape(background, background, 9));
        return button;
    }

    private Button outlineButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(TEXT);
        button.setTextSize(12);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(shape(SURFACE_ALT, BORDER, 9));
        return button;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private TextView label(String value) {
        TextView label = text(value == null ? "" : value, 11, MUTED, false);
        label.setLineSpacing(0, 1.35f);
        return label;
    }

    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout column() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view = new LinearLayout(this); view.setOrientation(LinearLayout.HORIZONTAL); return view; }
    private Space space(int height) { Space view = new Space(this); view.setLayoutParams(fullHeight(height)); return view; }
    private Space horizontalSpace(int width) { Space view = new Space(this); view.setLayoutParams(new LinearLayout.LayoutParams(dp(width), 1)); return view; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private LinearLayout.LayoutParams fullWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private FrameLayout.LayoutParams matchMatch() { return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); }
    private LinearLayout.LayoutParams fullHeight(int height) { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height)); }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1); }
    private LinearLayout.LayoutParams weightHeight(int height) { return new LinearLayout.LayoutParams(0, dp(height), 1); }

    @Override
    protected void onDestroy() {
        stopSpeaking();
        if (textToSpeech != null) textToSpeech.shutdown();
        taskRunner.close();
        super.onDestroy();
    }

    private static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final PositionSelected listener;
        SimpleItemSelectedListener(PositionSelected listener) { this.listener = listener; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { listener.accept(position); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }

    private interface PositionSelected { void accept(int position); }
}
