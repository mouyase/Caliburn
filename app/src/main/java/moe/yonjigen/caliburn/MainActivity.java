package moe.yonjigen.caliburn;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    List<String> shellCommonList = new ArrayList<>();
    List<String> adbCommonList = new ArrayList<>();
    RootShell rootShell = new RootShell();
    AdbShell adbShell = new AdbShell();
    TextView textView;
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textView);
        scrollView = findViewById(R.id.scrollView);
        excuShell();
    }

    public void excuShell() {
        shellCommonList.add("setprop service.adb.tcp.port 5555");
        shellCommonList.add("stop adbd");
        shellCommonList.add("start adbd");
        rootShell.execCommands(shellCommonList, new RootShell.Callback() {
            @Override
            public void onShellOutput(String outputString) {
                textView.setText(outputString);
                excuADB();
            }
        });
    }

    void excuADB() {
        adbCommonList.add("sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh");
        adbCommonList.add("sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh");
        adbCommonList.add("sh /sdcard/Android/data/moe.shizuku.privileged.api/files/start.sh");
        adbCommonList.add("sh /data/data/me.piebridge.brevent/brevent.sh");
        adbShell.setAdbShellListener(new AdbShell.AdbShellListener() {
            @Override
            public void onShellOutput(String outputString) {
                textView.setText(textView.getText() + outputString);
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
        adbShell.execCommands(MainActivity.this, adbCommonList);
    }
}
