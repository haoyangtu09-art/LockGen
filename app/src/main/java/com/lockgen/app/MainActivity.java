package com.lockgen.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String SCRIPT_PATH = "/storage/emulated/0/666/dy/gen_lock.sh";
    private static final String OUTPUT_DIR = "/storage/emulated/0/666/dy";
    private static final int REQ_MANAGE_STORAGE = 1001;
    private static final int REQ_PICK_ICON = 1002;

    private final LinkedHashMap<String, EditText> fields = new LinkedHashMap<>();
    private TextView statusText;
    private ImageView iconPreview;
    private Uri iconUri;
    private String iconPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createUI());
        ensureStoragePermission();
    }

    private void ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + getPackageName())), REQ_MANAGE_STORAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) {
                Toast.makeText(this, "文件权限已获取", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_PICK_ICON && resultCode == RESULT_OK && data != null) {
            iconUri = data.getData();
            if (iconUri != null) {
                iconPath = copyIconToCache(iconUri);
                try {
                    Bitmap bmp = BitmapFactory.decodeStream(getContentResolver().openInputStream(iconUri));
                    if (bmp != null) iconPreview.setImageBitmap(
                            Bitmap.createScaledBitmap(bmp, dp(72), dp(72), true));
                } catch (Exception ignored) {}
                statusText.setText("图标已选择: " + getFileName(iconUri));
            }
        }
    }

    private String copyIconToCache(Uri uri) {
        try {
            // Read entire file into memory once
            InputStream in = getContentResolver().openInputStream(uri);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            in.close();
            byte[] data = bos.toByteArray();
            if (data.length == 0) return null;

            // Write to cache
            File f = new File(getCacheDir(), "custom_icon.png");
            FileOutputStream out = new FileOutputStream(f);
            out.write(data);
            out.close();

            // Write to output dir (same bytes)
            try {
                File outDir = new File(OUTPUT_DIR);
                if (!outDir.exists()) outDir.mkdirs();
                File dest = new File(outDir, "custom_icon.png");
                FileOutputStream out2 = new FileOutputStream(dest);
                out2.write(data);
                out2.close();
                return dest.getAbsolutePath();
            } catch (Exception e2) {
                return f.getAbsolutePath();
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String getFileName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) return c.getString(i);
            }
        } catch (Exception ignored) {}
        return "icon.png";
    }

    private View createUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.parseColor("#1a1a2e"));

        ScrollView scroll = new ScrollView(this);
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);

        c.addView(txt("锁机APK生成器", 26, Color.parseColor("#e94560"), true));
        c.addView(txt("配置锁机应用，一键生成定制APK", 14, Color.parseColor("#888888"), false));
        c.addView(spacer(dp(16)));

        // ── Text config ──
        section(c, "文本配置");
        field(c, "应用名称", "app_name", "系统安全");
        field(c, "中文锁机标题", "lock_title_cn", "你的设备已经被锁定");
        field(c, "英文锁机标题", "lock_title_en", "Your device has been locked");
        field(c, "密码", "password", "i m sb");
        field(c, "密码框提示", "password_hint", "请输入密码");
        field(c, "密码提示文字", "hint_text", "密码是 i m sb");
        field(c, "底部文字", "bottom_text", "神秘小鸡踹倒android");
        field(c, "错误提示", "error_wrong", "密码错误! Wrong password!");
        field(c, "正确提示", "error_ok", "Password correct! Unlocking...");
        field(c, "通知标题", "notif_title", "系统安全");
        field(c, "通知内容", "notif_text", "设备安全保护中");

        // ── Colors ──
        section(c, "颜色配置");
        field(c, "背景色", "bg_color", "#FF000000");
        field(c, "标题色", "title_color", "#FFFF0000");
        field(c, "提示色", "hint_color", "#FFFF6666");
        field(c, "底部色", "bottom_color", "#FFFF4444");
        field(c, "边框色", "input_border", "#FFFF0000");
        field(c, "错误色", "error_color", "#FFFF0000");

        // ── Package / Version ──
        section(c, "包名与版本");
        field(c, "包名", "pkg_name", "com.lockscreen.app");
        field(c, "版本名", "version_name", "1.0");
        field(c, "版本号", "version_code", "1");

        // ── Icon ──
        section(c, "应用图标");
        LinearLayout iconRow = new LinearLayout(this);
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setGravity(Gravity.CENTER_VERTICAL);
        iconPreview = new ImageView(this);
        iconPreview.setBackgroundColor(Color.parseColor("#333355"));
        iconPreview.setImageResource(android.R.drawable.ic_menu_gallery);
        iconPreview.setPadding(dp(12), dp(12), dp(12), dp(12));
        iconPreview.setLayoutParams(new LinearLayout.LayoutParams(dp(72), dp(72)));
        iconRow.addView(iconPreview);
        Button pickBtn = new Button(this);
        pickBtn.setText("选择图标 PNG");
        pickBtn.setTextColor(Color.WHITE);
        pickBtn.setBackgroundColor(Color.parseColor("#0f3460"));
        pickBtn.setOnClickListener(v -> startActivityForResult(
                new Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .setType("image/png")
                        .addCategory(Intent.CATEGORY_OPENABLE), REQ_PICK_ICON));
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(dp(140), dp(42));
        pl.setMargins(dp(12), 0, 0, 0);
        iconRow.addView(pickBtn, pl);
        c.addView(iconRow);

        // ── Presets ──
        c.addView(spacer(dp(12)));
        c.addView(makePresets());

        // ── Status ──
        c.addView(spacer(dp(12)));
        statusText = txt("", 13, Color.parseColor("#aaaaaa"), false);
        c.addView(statusText);

        // ── Generate button ──
        c.addView(spacer(dp(16)));
        Button genBtn = new Button(this);
        genBtn.setText("一键生成 APK");
        genBtn.setTextColor(Color.WHITE);
        genBtn.setTextSize(18);
        genBtn.setTypeface(Typeface.DEFAULT_BOLD);
        genBtn.setBackgroundColor(Color.parseColor("#e94560"));
        genBtn.setPadding(dp(24), dp(16), dp(24), dp(16));
        genBtn.setOnClickListener(v -> generateApk());
        c.addView(genBtn, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        c.addView(spacer(dp(24)));

        scroll.addView(c);
        root.addView(scroll);
        return root;
    }

    private TextView txt(String text, int size, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(size);
        tv.setGravity(Gravity.CENTER);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    private void section(LinearLayout p, String name) {
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextColor(Color.parseColor("#e94560"));
        tv.setTextSize(16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(14), 0, dp(6));
        p.addView(tv);
    }

    private void field(LinearLayout p, String label, String key, String def) {
        TextView lb = new TextView(this);
        lb.setText(label);
        lb.setTextColor(Color.parseColor("#cccccc"));
        lb.setTextSize(12);
        lb.setPadding(0, dp(4), 0, dp(2));
        p.addView(lb);

        EditText et = new EditText(this);
        et.setText(def);
        et.setTextColor(Color.WHITE);
        et.setTextSize(14);
        et.setBackgroundColor(Color.parseColor("#16213e"));
        et.setPadding(dp(12), dp(8), dp(12), dp(8));
        et.setSingleLine(true);
        p.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        fields.put(key, et);

        if (key.contains("color")) {
            View preview = new View(this);
            try { preview.setBackgroundColor(Color.parseColor(def)); } catch (Exception e) {}
            preview.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(16)));
            p.addView(preview, new LinearLayout.LayoutParams(dp(48), dp(16)));
        }
    }

    private LinearLayout makePresets() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        addPreset(row, "红黑经典", () -> {
            sf("bg_color", "#FF000000"); sf("title_color", "#FFFF0000");
            sf("hint_color", "#FFFF6666"); sf("bottom_color", "#FFFF4444");
            sf("input_border", "#FFFF0000"); sf("error_color", "#FFFF0000");
        });
        addPreset(row, "赛博蓝", () -> {
            sf("bg_color", "#FF0a0a2e"); sf("title_color", "#FF00d4ff");
            sf("hint_color", "#FF00aacc"); sf("bottom_color", "#FF0088aa");
            sf("input_border", "#FF00d4ff"); sf("error_color", "#FF00ffff");
        });
        addPreset(row, "绿屏黑客", () -> {
            sf("bg_color", "#FF001a00"); sf("title_color", "#FF00ff00");
            sf("hint_color", "#FF00cc00"); sf("bottom_color", "#FF009900");
            sf("input_border", "#FF00ff00"); sf("error_color", "#FF00ff00");
        });
        return row;
    }

    private void addPreset(LinearLayout row, String label, Runnable action) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(12);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#0f3460"));
        btn.setOnClickListener(v -> action.run());
        row.addView(btn, new LinearLayout.LayoutParams(0, dp(38), 1f));
    }

    private void sf(String key, String val) {
        EditText et = fields.get(key);
        if (et != null) et.setText(val);
    }

    // ══════════════════  Generate  ══════════════════

    private void generateApk() {
        // 1. Copy base.apk from assets to output dir
        try {
            File outDir = new File(OUTPUT_DIR);
            if (!outDir.exists()) outDir.mkdirs();
            File baseDest = new File(outDir, "base.apk");
            InputStream in = getAssets().open("base.apk");
            FileOutputStream out = new FileOutputStream(baseDest);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close(); out.close();
        } catch (Exception e) {
            Toast.makeText(this, "复制base.apk失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // 2. Build config + save
        StringBuilder sb = new StringBuilder("# 锁机APK配置\n");
        for (Map.Entry<String, EditText> e : fields.entrySet()) {
            sb.append(envKey(e.getKey())).append("=\"")
              .append(esc(e.getValue().getText().toString())).append("\"\n");
        }
        if (iconPath != null) {
            sb.append("ICON_FILE=\"").append(iconPath).append("\"\n");
        }
        String content = sb.toString();
        String saved = saveFile(content);
        if (saved == null) {
            Toast.makeText(this, "保存配置失败，请检查权限", Toast.LENGTH_LONG).show();
            return;
        }
        statusText.setText("配置已保存 | base.apk已就绪");

        // 3. Try auto-run, fallback to manual
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("正在生成APK...\n约需10秒");
        pd.setCancelable(false);
        pd.show();

        new Thread(() -> {
            String result = runScript();
            new Handler(Looper.getMainLooper()).post(() -> {
                pd.dismiss();
                showResult(result);
            });
        }).start();
    }

    private String saveFile(String content) {
        // Try output dir
        try {
            File f = new File(OUTPUT_DIR, "lock_config.conf");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(content.getBytes());
            fos.close();
            return f.getAbsolutePath();
        } catch (Exception e1) {}
        // Try app external
        try {
            File dir = getExternalFilesDir(null);
            if (dir != null) {
                FileOutputStream fos = new FileOutputStream(new File(dir, "lock_config.conf"));
                fos.write(content.getBytes());
                fos.close();
                return dir + "/lock_config.conf";
            }
        } catch (Exception e2) {}
        // Try app internal
        try {
            FileOutputStream fos = openFileOutput("lock_config.conf", MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
            return getFilesDir() + "/lock_config.conf";
        } catch (Exception e3) { return null; }
    }

    private String runScript() {
        StringBuilder out = new StringBuilder();
        try {
            Process p = new ProcessBuilder("sh", SCRIPT_PATH, "-y").redirectErrorStream(true).start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = r.readLine()) != null) out.append(line).append("\n");
            p.waitFor();
        } catch (Exception e) {
            return "AUTO_FAIL";
        }
        return out.toString();
    }

    private void showResult(String output) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("生成结果");

        if (!output.equals("AUTO_FAIL") && (output.contains("签名完成") || output.contains("APK 已生成到") || output.contains("APK →"))) {
            b.setMessage("APK 生成成功!\n\n输出: " + OUTPUT_DIR);
            b.setPositiveButton("查看文件", (d, w) -> {
                try { startActivity(new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse(OUTPUT_DIR), "resource/folder"));
                } catch (Exception e) {}
            });
        } else {
            String cmd = "bash /storage/emulated/0/666/dy/gen_lock.sh -y";
            b.setMessage("请在 Termux 运行:\n\n" + cmd);
            b.setPositiveButton("复制命令", (d, w) -> {
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                        .setPrimaryClip(ClipData.newPlainText("cmd", cmd));
                Toast.makeText(this, "已复制！到Termux粘贴运行", Toast.LENGTH_LONG).show();
            });
        }
        b.setNegativeButton("关闭", null);
        b.show();
    }

    private String envKey(String k) { return k.toUpperCase(); }
    private String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
