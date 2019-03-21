package moe.yonjigen.caliburn;

import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Android运行linux命令
 */
public class RootShell {
    public interface Callback {
        void onShellOutput(String outputString);
    }

    public void execCommand(String command, Callback callback) {
        List<String> commands = new ArrayList<String>();
        commands.add(command);
    }

    /**
     * 执行命令并且输出结果
     */
    public void execCommands(final List<String> commands, final Callback rootShellCallback) {
        new Thread() {
            @Override
            public void run() {
                DataOutputStream dataOutputStream = null;
                InputStreamReader inputStreamReader = null;
                try {

                    Process p = Runtime.getRuntime().exec("su");// 经过Root处理的android系统即有su命令
                    dataOutputStream = new DataOutputStream(p.getOutputStream());
                    inputStreamReader = new InputStreamReader(p.getInputStream());
                    for (String command : commands) {
                        dataOutputStream.writeBytes(command + "\n");
                    }
                    dataOutputStream.flush();
                    dataOutputStream.writeBytes("exit\n");
                    dataOutputStream.flush();
                    String line = null;
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String outputString = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        outputString = outputString + line;
                    }
                    p.waitFor();
                    final String finalOutputString = outputString;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.v("Shell", finalOutputString);
                            rootShellCallback.onShellOutput(finalOutputString);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (dataOutputStream != null) {
                        try {
                            dataOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (inputStreamReader != null) {
                        try {
                            inputStreamReader.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                super.run();
            }
        }.start();
    }

    Handler mHandler = new Handler();
}