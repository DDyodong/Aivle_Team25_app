package com.example.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;

import java.util.ArrayList;
import java.util.Locale;

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

    private final TbmRepository repository = new MockTbmRepository();
    private FrameLayout root;
    private EditText briefingInput;
    private ImageView photoPreview;
    private boolean hasPhoto;

    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> texts = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (texts != null && !texts.isEmpty() && briefingInput != null) {
                        briefingInput.setText(normalizeSafetyTerms(texts.get(0)));
                        briefingInput.setSelection(briefingInput.length());
                    }
                }
            });

    private final ActivityResultLauncher<Void> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(), this::setPhoto);

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && photoPreview != null) {
                    photoPreview.setImageURI(uri);
                    photoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    hasPhoto = true;
                }
            });

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) cameraLauncher.launch(null);
                else toast(getString(R.string.camera_permission_required));
            });

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        setContentView(root);
        showLogin();
    }

    private void showLogin() {
        root.removeAllViews();
        LinearLayout page = column(24);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(24), dp(54), dp(24), dp(28));
        page.addView(space(30));
        TextView mark = text("◆", 40, ORANGE, true);
        mark.setGravity(Gravity.CENTER);
        page.addView(mark, matchWrap());
        TextView title = text("SMART SHIPYARD", 25, TEXT, true);
        title.setGravity(Gravity.CENTER);
        page.addView(title, matchWrap());
        TextView sub = text(getString(R.string.login_subtitle), 13, CYAN, true);
        sub.setGravity(Gravity.CENTER);
        page.addView(sub, matchWrap());
        page.addView(space(42));

        LinearLayout card = card();
        card.setPadding(dp(22), dp(24), dp(22), dp(24));
        card.addView(text(getString(R.string.login_title), 22, TEXT, true));
        card.addView(label(getString(R.string.login_instruction)));
        card.addView(space(22));
        EditText employee = input(getString(R.string.employee_hint));
        card.addView(employee, fullHeight(54));
        card.addView(space(12));
        EditText password = input(getString(R.string.password));
        password.setInputType(0x00000081);
        card.addView(password, fullHeight(54));
        card.addView(space(18));
        Button login = primaryButton(getString(R.string.login));
        login.setOnClickListener(v -> {
            if (repository.login(employee.getText().toString(), password.getText().toString())) showShell(0);
            else toast(getString(R.string.login_instruction));
        });
        card.addView(login, fullHeight(54));
        card.addView(space(12));
        TextView hint = label(getString(R.string.login_mock_hint));
        hint.setGravity(Gravity.CENTER);
        card.addView(hint, matchWrap());
        page.addView(card, fullWrap());
        page.addView(space(24));
        page.addView(label(getString(R.string.system_tagline)), matchWrap());
        root.addView(scroll(page), matchMatch());
    }

    private void showShell(int selected) {
        root.removeAllViews();
        LinearLayout shell = column(0);
        LinearLayout header = row(12);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(14), dp(14), dp(12));
        LinearLayout names = column(1);
        int[] pageTitles = {R.string.page_safety, R.string.page_tbm, R.string.page_history, R.string.page_my};
        names.addView(text(getString(pageTitles[selected]), 20, TEXT, true));
        names.addView(text(getString(R.string.site_block), 10, MUTED, false));
        header.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView avatar = text("김", 15, CYAN, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(shape(SURFACE_ALT, BORDER, 40));
        header.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));
        shell.addView(header, fullWrap());

        View content = selected == 0 ? dashboard() : selected == 1 ? tbmForm() : selected == 2 ? history() : myPage();
        shell.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        shell.addView(bottomNav(selected), fullHeight(72));
        root.addView(shell, matchMatch());
    }

    private View dashboard() {
        TbmRecord today = repository.getTodayTbm();
        LinearLayout page = contentColumn();
        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView chip = text(getString(today.completed ? R.string.tbm_complete_chip : R.string.tbm_pending_chip), 11,
                today.completed ? GREEN : ORANGE, true);
        hero.addView(chip);
        hero.addView(space(12));
        hero.addView(text(today.workName, 21, TEXT, true));
        hero.addView(label(today.permitId + "  ·  " + today.block));
        hero.addView(space(20));
        Button action = primaryButton(getString(today.completed ? R.string.review_complete : R.string.start_tbm));
        action.setOnClickListener(v -> showShell(1));
        hero.addView(action, fullHeight(50));
        page.addView(hero, fullWrap());
        page.addView(space(14));

        LinearLayout stats = row(10);
        stats.addView(statCard(getString(R.string.completion_status), getString(today.completed ? R.string.completed : R.string.incomplete), today.completed ? GREEN : ORANGE), weight());
        stats.addView(statCard(getString(R.string.participants), getString(R.string.people_count, today.participants), CYAN), weight());
        page.addView(stats, fullWrap());
        page.addView(space(24));
        page.addView(sectionTitle(getString(R.string.checklist), getString(R.string.required_before_work)));
        page.addView(checkItem(getString(R.string.check_harness), true));
        page.addView(checkItem(getString(R.string.check_access), true));
        page.addView(checkItem(getString(R.string.check_fall_protection), false));
        page.addView(space(24));
        page.addView(sectionTitle(getString(R.string.recent_tbm), getString(R.string.recent_three)));
        java.util.List<TbmRecord> recentRecords = repository.getRecentRecords();
        for (TbmRecord record : recentRecords.subList(0, Math.min(3, recentRecords.size()))) {
            page.addView(recordCard(record));
        }
        return scroll(page);
    }

    private View tbmForm() {
        TbmRecord today = repository.getTodayTbm();
        LinearLayout page = contentColumn();
        LinearLayout info = card();
        info.addView(text("TODAY'S PERMIT", 10, CYAN, true));
        info.addView(space(7));
        info.addView(text(today.workName, 18, TEXT, true));
        info.addView(label(today.permitId + "  ·  " + today.block));
        page.addView(info, fullWrap());
        page.addView(space(20));
        page.addView(sectionTitle(getString(R.string.participants), getString(R.string.tbm_attendees)));
        EditText participants = input(getString(R.string.count_hint));
        participants.setInputType(2);
        if (today.participants > 0) participants.setText(String.valueOf(today.participants));
        page.addView(participants, fullHeight(52));
        page.addView(space(20));
        page.addView(sectionTitle(getString(R.string.briefing_title), getString(R.string.briefing_caption)));
        briefingInput = input(getString(R.string.briefing_hint));
        briefingInput.setGravity(Gravity.TOP);
        briefingInput.setPadding(dp(14), dp(14), dp(14), dp(14));
        briefingInput.setMinLines(5);
        briefingInput.setText(today.briefing);
        page.addView(briefingInput, fullWrap());
        page.addView(space(10));
        Button speech = outlineButton(getString(R.string.speech_input));
        speech.setTextColor(CYAN);
        speech.setOnClickListener(v -> startSpeech());
        page.addView(speech, fullHeight(50));
        page.addView(space(16));
        page.addView(languageDeliveryCard());
        page.addView(space(22));
        page.addView(sectionTitle(getString(R.string.worker_photo), getString(R.string.photo_caption)));
        photoPreview = new ImageView(this);
        photoPreview.setImageResource(android.R.drawable.ic_menu_camera);
        photoPreview.setColorFilter(MUTED);
        photoPreview.setPadding(dp(60), dp(38), dp(60), dp(38));
        photoPreview.setBackground(shape(SURFACE, BORDER, 12));
        page.addView(photoPreview, fullHeight(180));
        page.addView(space(10));
        LinearLayout photoActions = row(10);
        Button camera = outlineButton(getString(R.string.take_photo));
        camera.setOnClickListener(v -> openCamera());
        Button gallery = outlineButton(getString(R.string.choose_gallery));
        gallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        photoActions.addView(camera, weightHeight(48));
        photoActions.addView(gallery, weightHeight(48));
        page.addView(photoActions, fullWrap());
        page.addView(space(24));
        Button complete = primaryButton(getString(today.completed ? R.string.update_tbm : R.string.submit_tbm));
        complete.setOnClickListener(v -> {
            int count;
            try { count = Integer.parseInt(participants.getText().toString()); }
            catch (Exception e) { count = 0; }
            String briefing = briefingInput.getText().toString().trim();
            if (count < 1) { toast(getString(R.string.enter_participants)); return; }
            if (briefing.isEmpty()) { toast(getString(R.string.enter_briefing)); return; }
            repository.completeTodayTbm(count, briefing, hasPhoto);
            toast(getString(R.string.tbm_completed_message));
            showShell(0);
        });
        page.addView(complete, fullHeight(54));
        page.addView(space(20));
        return scroll(page);
    }

    private View history() {
        LinearLayout page = contentColumn();
        page.addView(label(getString(R.string.history_description)));
        page.addView(space(14));
        for (TbmRecord record : repository.getRecentRecords()) page.addView(recordCard(record));
        return scroll(page);
    }

    private View myPage() {
        LinearLayout page = contentColumn();
        LinearLayout profile = card();
        profile.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView avatar = text("김", 25, CYAN, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(shape(SURFACE_ALT, CYAN, 64));
        profile.addView(avatar, new LinearLayout.LayoutParams(dp(64), dp(64)));
        profile.addView(space(12));
        TextView name = text("김반장", 20, TEXT, true);
        name.setGravity(Gravity.CENTER);
        profile.addView(name, matchWrap());
        TextView role = label(getString(R.string.profile_role));
        role.setGravity(Gravity.CENTER);
        profile.addView(role, matchWrap());
        profile.addView(space(16));
        LinearLayout employee = row(8);
        employee.addView(label(getString(R.string.employee_number)), weight());
        employee.addView(text("240071", 12, TEXT, true));
        profile.addView(employee, fullWrap());
        page.addView(profile, fullWrap());
        page.addView(space(22));
        page.addView(sectionTitle(getString(R.string.app_language), getString(R.string.app_language_caption)));

        LinearLayout languageCard = card();
        languageCard.addView(label(getString(R.string.app_language_description)));
        languageCard.addView(space(10));

        LinearLayout languageHeader = row(8);
        languageHeader.setGravity(Gravity.CENTER_VERTICAL);
        languageHeader.setPadding(dp(14), dp(12), dp(14), dp(12));
        languageHeader.setBackground(shape(SURFACE_ALT, BORDER, 9));
        TextView selectedLanguage = text(currentLanguageName(), 14, TEXT, true);
        TextView expandIndicator = text("▼", 12, ORANGE, true);
        languageHeader.addView(selectedLanguage, weight());
        languageHeader.addView(expandIndicator);
        languageHeader.setContentDescription(getString(R.string.language_selector_collapsed, currentLanguageName()));

        RadioGroup group = new RadioGroup(this);
        group.setVisibility(View.GONE);
        String[] languages = {
                "한국어", "English", "Tiếng Việt", "नेपाली", "O‘zbekcha", "中文",
                "스리랑카 (සිංහල)", "스리랑카 (தமிழ்)", "Bahasa Indonesia", "ไทย",
                "Filipino", "မြန်မာဘာသာ"
        };
        String[] languageTags = {
                "ko", "en", "vi", "ne", "uz", "zh", "si", "ta", "id", "th", "fil", "my"
        };
        String activeTag = currentLanguageTag();
        for (int index = 0; index < languages.length; index++) {
            String language = languages[index];
            String languageTag = languageTags[index];
            RadioButton radio = new RadioButton(this);
            radio.setText(language);
            radio.setTextColor(TEXT);
            radio.setTextSize(14);
            radio.setButtonTintList(android.content.res.ColorStateList.valueOf(ORANGE));
            radio.setPadding(0, dp(7), 0, dp(7));
            radio.setChecked(languageTag.equals(activeTag));
            radio.setOnClickListener(v -> {
                if (!languageTag.equals(currentLanguageTag())) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
                }
            });
            group.addView(radio, fullWrap());
        }
        languageHeader.setOnClickListener(v -> {
            boolean shouldExpand = group.getVisibility() != View.VISIBLE;
            group.setVisibility(shouldExpand ? View.VISIBLE : View.GONE);
            expandIndicator.setText(shouldExpand ? "▲" : "▼");
            languageHeader.setContentDescription(getString(
                    shouldExpand ? R.string.language_selector_expanded : R.string.language_selector_collapsed,
                    currentLanguageName()));
        });
        languageCard.addView(languageHeader, fullWrap());
        languageCard.addView(group, fullWrap());
        page.addView(languageCard, fullWrap());
        page.addView(space(14));

        LinearLayout notice = card();
        notice.addView(text(getString(R.string.language_scope), 14, CYAN, true));
        notice.addView(space(6));
        notice.addView(label(getString(R.string.language_scope_items)));
        page.addView(notice, fullWrap());
        page.addView(space(24));
        Button logout = outlineButton(getString(R.string.logout));
        logout.setTextColor(RED);
        logout.setOnClickListener(v -> showLogin());
        page.addView(logout, fullHeight(50));
        return scroll(page);
    }

    private View languageDeliveryCard() {
        String language = currentLanguageName();
        LinearLayout box = card();
        box.setBackground(shape(SURFACE_ALT, CYAN, 10));
        LinearLayout title = row(8);
        title.addView(text(getString(R.string.auto_translation), 13, CYAN, true), weight());
        title.addView(text(language, 11, ORANGE, true));
        box.addView(title);
        box.addView(space(9));
        box.addView(text(getString(R.string.safety_message), 13, TEXT, false));
        box.addView(space(8));
        box.addView(label(getString(R.string.translation_pipeline)));
        return box;
    }

    private String currentLanguageTag() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        Locale locale = locales.isEmpty() ? Locale.getDefault() : locales.get(0);
        String language = locale == null ? "ko" : locale.getLanguage();
        switch (language) {
            case "en":
            case "vi":
            case "ne":
            case "uz":
            case "zh":
            case "si":
            case "ta":
            case "id":
            case "th":
            case "fil":
            case "my":
                return language;
            default:
                return "ko";
        }
    }

    private String currentLanguageName() {
        switch (currentLanguageTag()) {
            case "en": return "English";
            case "vi": return "Tiếng Việt";
            case "ne": return "नेपाली";
            case "zh": return "中文";
            case "uz": return "O‘zbekcha";
            case "si": return "스리랑카 (සිංහල)";
            case "ta": return "스리랑카 (தமிழ்)";
            case "id": return "Bahasa Indonesia";
            case "th": return "ไทย";
            case "fil": return "Filipino";
            case "my": return "မြန်မာဘာသာ";
            default: return "한국어";
        }
    }

    private LinearLayout bottomNav(int selected) {
        LinearLayout nav = row(0);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(7), dp(8), dp(8));
        nav.setBackground(shape(SURFACE, BORDER, 0));
        String[] labels = {getString(R.string.nav_home), getString(R.string.nav_tbm),
                getString(R.string.nav_history), getString(R.string.nav_my)};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button button = new Button(this);
            button.setText(labels[i]);
            button.setTextSize(11);
            button.setTextColor(i == selected ? ORANGE : MUTED);
            button.setTypeface(Typeface.DEFAULT, i == selected ? Typeface.BOLD : Typeface.NORMAL);
            button.setAllCaps(false);
            button.setBackgroundColor(Color.TRANSPARENT);
            button.setOnClickListener(v -> showShell(index));
            nav.addView(button, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        return nav;
    }

    private View recordCard(TbmRecord record) {
        LinearLayout item = card();
        item.setPadding(dp(16), dp(15), dp(16), dp(15));
        LinearLayout top = row(8);
        top.addView(text(record.date, 11, CYAN, true), weight());
        top.addView(text(getString(record.completed ? R.string.completed : R.string.in_progress), 10, record.completed ? GREEN : ORANGE, true));
        item.addView(top);
        item.addView(space(7));
        item.addView(text(record.workName, 15, TEXT, true));
        item.addView(label(getString(R.string.record_participants, record.block, record.participants)));
        item.addView(space(10));
        TextView body = label(record.briefing);
        body.setMaxLines(2);
        item.addView(body);
        LinearLayout.LayoutParams lp = fullWrap();
        lp.setMargins(0, 0, 0, dp(10));
        item.setLayoutParams(lp);
        item.setOnClickListener(v -> toast(getString(R.string.record_selected, record.date)));
        return item;
    }

    private LinearLayout statCard(String title, String value, int color) {
        LinearLayout box = card();
        box.setPadding(dp(15), dp(15), dp(15), dp(15));
        box.addView(label(title));
        box.addView(space(6));
        box.addView(text(value, 21, color, true));
        return box;
    }

    private View checkItem(String value, boolean checked) {
        LinearLayout item = row(12);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), dp(13), dp(14), dp(13));
        item.setBackground(shape(SURFACE, BORDER, 10));
        TextView icon = text(checked ? "✓" : "!", 15, checked ? GREEN : ORANGE, true);
        icon.setGravity(Gravity.CENTER);
        item.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        item.addView(text(value, 13, TEXT, true), weight());
        LinearLayout.LayoutParams lp = fullWrap();
        lp.setMargins(0, 0, 0, dp(8));
        item.setLayoutParams(lp);
        return item;
    }

    private View sectionTitle(String title, String caption) {
        LinearLayout row = row(8);
        row.setGravity(Gravity.BOTTOM);
        row.addView(text(title, 16, TEXT, true), weight());
        row.addView(text(caption, 9, MUTED, false));
        LinearLayout.LayoutParams lp = fullWrap();
        lp.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(lp);
        return row;
    }

    private void startSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt));
        try { speechLauncher.launch(intent); }
        catch (Exception e) { toast(getString(R.string.speech_unavailable)); }
    }

    private String normalizeSafetyTerms(String raw) {
        return raw.replace("데나오시", "재작업")
                .replace("그라인딩", "사상")
                .replace("리프팅", "양중");
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            cameraLauncher.launch(null);
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void setPhoto(Bitmap bitmap) {
        if (bitmap != null && photoPreview != null) {
            photoPreview.clearColorFilter();
            photoPreview.setPadding(0, 0, 0, 0);
            photoPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photoPreview.setImageBitmap(bitmap);
            hasPhoto = true;
        }
    }

    private ScrollView scroll(View child) {
        ScrollView view = new ScrollView(this);
        view.setFillViewport(true);
        view.setBackgroundColor(BG);
        view.addView(child);
        return view;
    }

    private LinearLayout contentColumn() {
        LinearLayout layout = column(0);
        layout.setPadding(dp(18), dp(12), dp(18), dp(28));
        return layout;
    }

    private LinearLayout card() {
        LinearLayout layout = column(0);
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
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(shape(ORANGE, ORANGE, 9));
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
        TextView label = text(value, 11, MUTED, false);
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

    private LinearLayout column(int gapIgnored) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row(int gapIgnored) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private Space space(int height) { Space s = new Space(this); s.setLayoutParams(fullHeight(height)); return s; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private LinearLayout.LayoutParams fullWrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams matchWrap() { return fullWrap(); }
    private FrameLayout.LayoutParams matchMatch() { return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT); }
    private LinearLayout.LayoutParams fullHeight(int height) { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(height)); }
    private LinearLayout.LayoutParams weight() { return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1); }
    private LinearLayout.LayoutParams weightHeight(int height) { return new LinearLayout.LayoutParams(0, dp(height), 1); }
}
