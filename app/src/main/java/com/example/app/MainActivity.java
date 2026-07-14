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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
                else toast("사진 촬영을 위해 카메라 권한이 필요합니다.");
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
        TextView sub = text("현장 안전관리 · TBM", 13, CYAN, true);
        sub.setGravity(Gravity.CENTER);
        page.addView(sub, matchWrap());
        page.addView(space(42));

        LinearLayout card = card();
        card.setPadding(dp(22), dp(24), dp(22), dp(24));
        card.addView(text("반장 로그인", 22, TEXT, true));
        card.addView(label("사번과 비밀번호를 입력해 주세요."));
        card.addView(space(22));
        EditText employee = input("사번 (예: 240071)");
        card.addView(employee, fullHeight(54));
        card.addView(space(12));
        EditText password = input("비밀번호");
        password.setInputType(0x00000081);
        card.addView(password, fullHeight(54));
        card.addView(space(18));
        Button login = primaryButton("로그인");
        login.setOnClickListener(v -> {
            if (repository.login(employee.getText().toString(), password.getText().toString())) showShell(0);
            else toast("사번과 비밀번호를 입력해 주세요.");
        });
        card.addView(login, fullHeight(54));
        card.addView(space(12));
        TextView hint = label("목업: 임의의 사번과 비밀번호로 로그인할 수 있습니다.");
        hint.setGravity(Gravity.CENTER);
        card.addView(hint, matchWrap());
        page.addView(card, fullWrap());
        page.addView(space(24));
        page.addView(label("AI 기반 스마트조선소 안전 관리 시스템"), matchWrap());
        root.addView(scroll(page), matchMatch());
    }

    private void showShell(int selected) {
        root.removeAllViews();
        LinearLayout shell = column(0);
        LinearLayout header = row(12);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(14), dp(14), dp(12));
        LinearLayout names = column(1);
        names.addView(text(selected == 0 ? "오늘의 안전" : selected == 1 ? "오늘 TBM" : "최근 기록", 20, TEXT, true));
        names.addView(text("거제 사업장 · B-07 블록", 10, MUTED, false));
        header.addView(names, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView avatar = text("김", 15, CYAN, true);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(shape(SURFACE_ALT, BORDER, 40));
        header.addView(avatar, new LinearLayout.LayoutParams(dp(40), dp(40)));
        shell.addView(header, fullWrap());

        View content = selected == 0 ? dashboard() : selected == 1 ? tbmForm() : history();
        shell.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        shell.addView(bottomNav(selected), fullHeight(72));
        root.addView(shell, matchMatch());
    }

    private View dashboard() {
        TbmRecord today = repository.getTodayTbm();
        LinearLayout page = contentColumn();
        LinearLayout hero = card();
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        TextView chip = text(today.completed ? "● TBM 완료" : "● TBM 진행 전", 11,
                today.completed ? GREEN : ORANGE, true);
        hero.addView(chip);
        hero.addView(space(12));
        hero.addView(text(today.workName, 21, TEXT, true));
        hero.addView(label(today.permitId + "  ·  " + today.block));
        hero.addView(space(20));
        Button action = primaryButton(today.completed ? "완료 내용 확인" : "오늘 TBM 시작하기");
        action.setOnClickListener(v -> showShell(1));
        hero.addView(action, fullHeight(50));
        page.addView(hero, fullWrap());
        page.addView(space(14));

        LinearLayout stats = row(10);
        stats.addView(statCard("완료 여부", today.completed ? "완료" : "미완료", today.completed ? GREEN : ORANGE), weight());
        stats.addView(statCard("참여 인원", today.participants + "명", CYAN), weight());
        page.addView(stats, fullWrap());
        page.addView(space(24));
        page.addView(sectionTitle("승인 조건 체크리스트", "작업 전 필수 확인"));
        page.addView(checkItem("안전벨트 이중 체결", true));
        page.addView(checkItem("작업 반경 10m 하부 통행 차단", true));
        page.addView(checkItem("추락 방지시설 상태 확인", false));
        page.addView(space(24));
        page.addView(sectionTitle("최근 TBM", "최근 3건"));
        for (TbmRecord record : repository.getRecentRecords().subList(0,
                Math.min(3, repository.getRecentRecords().size()))) page.addView(recordCard(record));
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
        page.addView(sectionTitle("참여 인원", "TBM 참석 인원"));
        EditText participants = input("예: 8");
        participants.setInputType(2);
        if (today.participants > 0) participants.setText(String.valueOf(today.participants));
        page.addView(participants, fullHeight(52));
        page.addView(space(20));
        page.addView(sectionTitle("TBM 브리핑 내용", "음성 인식 후 직접 수정할 수 있습니다."));
        briefingInput = input("오늘 작업의 위험요인과 안전조치를 입력해 주세요.");
        briefingInput.setGravity(Gravity.TOP);
        briefingInput.setPadding(dp(14), dp(14), dp(14), dp(14));
        briefingInput.setMinLines(5);
        briefingInput.setText(today.briefing);
        page.addView(briefingInput, fullWrap());
        page.addView(space(10));
        Button speech = outlineButton("🎙  음성으로 TBM 입력");
        speech.setTextColor(CYAN);
        speech.setOnClickListener(v -> startSpeech());
        page.addView(speech, fullHeight(50));
        page.addView(space(22));
        page.addView(sectionTitle("작업자 사진", "교육 실시 증빙용 사진"));
        photoPreview = new ImageView(this);
        photoPreview.setImageResource(android.R.drawable.ic_menu_camera);
        photoPreview.setColorFilter(MUTED);
        photoPreview.setPadding(dp(60), dp(38), dp(60), dp(38));
        photoPreview.setBackground(shape(SURFACE, BORDER, 12));
        page.addView(photoPreview, fullHeight(180));
        page.addView(space(10));
        LinearLayout photoActions = row(10);
        Button camera = outlineButton("사진 촬영");
        camera.setOnClickListener(v -> openCamera());
        Button gallery = outlineButton("갤러리 선택");
        gallery.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        photoActions.addView(camera, weightHeight(48));
        photoActions.addView(gallery, weightHeight(48));
        page.addView(photoActions, fullWrap());
        page.addView(space(24));
        Button complete = primaryButton(today.completed ? "TBM 내용 업데이트" : "TBM 완료 및 제출");
        complete.setOnClickListener(v -> {
            int count;
            try { count = Integer.parseInt(participants.getText().toString()); }
            catch (Exception e) { count = 0; }
            String briefing = briefingInput.getText().toString().trim();
            if (count < 1) { toast("참여 인원을 입력해 주세요."); return; }
            if (briefing.isEmpty()) { toast("TBM 브리핑 내용을 입력해 주세요."); return; }
            repository.completeTodayTbm(count, briefing, hasPhoto);
            toast("오늘 TBM이 완료되었습니다.");
            showShell(0);
        });
        page.addView(complete, fullHeight(54));
        page.addView(space(20));
        return scroll(page);
    }

    private View history() {
        LinearLayout page = contentColumn();
        page.addView(label("완료된 TBM 브리핑과 교육 기록입니다."));
        page.addView(space(14));
        for (TbmRecord record : repository.getRecentRecords()) page.addView(recordCard(record));
        return scroll(page);
    }

    private LinearLayout bottomNav(int selected) {
        LinearLayout nav = row(0);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(7), dp(8), dp(8));
        nav.setBackground(shape(SURFACE, BORDER, 0));
        String[] labels = {"⌂\n홈", "●\nTBM 작성", "▤\n기록"};
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
        top.addView(text(record.completed ? "완료" : "진행 중", 10, record.completed ? GREEN : ORANGE, true));
        item.addView(top);
        item.addView(space(7));
        item.addView(text(record.workName, 15, TEXT, true));
        item.addView(label(record.block + "  ·  참여 " + record.participants + "명"));
        item.addView(space(10));
        TextView body = label(record.briefing);
        body.setMaxLines(2);
        item.addView(body);
        LinearLayout.LayoutParams lp = fullWrap();
        lp.setMargins(0, 0, 0, dp(10));
        item.setLayoutParams(lp);
        item.setOnClickListener(v -> toast(record.date + " TBM 기록을 선택했습니다."));
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREAN.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "TBM 내용을 말씀해 주세요");
        try { speechLauncher.launch(intent); }
        catch (Exception e) { toast("이 기기에서 음성 인식을 사용할 수 없습니다."); }
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
