package moe.yonjigen.caliburn;

import android.content.Context;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;

import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public class AdbShell {
    public interface AdbShellListener {
        void onShellOutput(String outputString);
    }

    AdbShellListener mAdbShellListener = new AdbShellListener() {
        @Override
        public void onShellOutput(String outputString) {
        }
    };

    // This implements the AdbBase64 interface required for AdbCrypto
    private AdbBase64 getBase64Impl() {
        return new AdbBase64() {
            @Override
            public String encodeToString(byte[] arg0) {
                return Base64.encodeToString(arg0, Base64.DEFAULT);
            }
        };
    }

    public void setAdbShellListener(AdbShellListener adbShellListener) {
        this.mAdbShellListener = adbShellListener;
    }

    // This function loads a keypair from the specified files if one exists, and if not,
    // it creates a new keypair and saves it in the specified files
    private AdbCrypto setupCrypto(String publicKeyFile, String privateKeyFile)
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        File publicKey = new File(publicKeyFile);
        File privateKey = new File(privateKeyFile);
        AdbCrypto adbCrypto = null;
        // Try to load a key pair from the files
        if (publicKey.exists() && privateKey.exists()) {
            try {
                adbCrypto = AdbCrypto.loadAdbKeyPair(getBase64Impl(), privateKey, publicKey);
            } catch (IOException e) {
                // Failed to read from file
                adbCrypto = null;
            } catch (InvalidKeySpecException e) {
                // Key spec was invalid
                adbCrypto = null;
            } catch (NoSuchAlgorithmException e) {
                // RSA algorithm was unsupported with the crypo packages available
                adbCrypto = null;
            }
        }
        if (adbCrypto == null) {
            // We couldn't load a key, so let's generate a new one
            adbCrypto = AdbCrypto.generateAdbKeyPair(getBase64Impl());
            // Save it
            adbCrypto.saveAdbKeyPair(privateKey, publicKey);
            System.out.println("Generated new keypair");
        } else {
            System.out.println("Loaded existing keypair");
        }

        return adbCrypto;
    }

    public void execCommands(final Context context, final List<String> commands) {
        new Thread() {
            @Override
            public void run() {
                exec(context, commands);
                super.run();
            }
        }.start();
    }

    public void exec(Context context, List<String> commands) {
        AdbConnection adbConnection;
        Socket socket;
        AdbCrypto adbCrypto;
        // Setup the crypto object required for the AdbConnection
        try {
            adbCrypto = setupCrypto(context.getFilesDir() + "/public.key", context.getFilesDir() + "/private.key");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Connect the socket to the remote host
        System.out.println("Socket connecting...");
        try {
            socket = new Socket("127.0.0.1", 5555);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Socket connected");

        // Construct the AdbConnection object
        try {
            adbConnection = AdbConnection.create(socket, adbCrypto);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Start the application layer connection process
        System.out.println("ADB connecting...");
        try {
            adbConnection.connect();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("ADB connected");

        // Open the shell stream of ADB
        final AdbStream adbStream;
        try {
            adbStream = adbConnection.open("shell:");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }
        // Start the receiving thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!adbStream.isClosed())
                    try {
                        // Print each thing we read from the shell stream
                        final String finalOutputString = new String(adbStream.read(), "UTF-8");
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.v("ADBShell", finalOutputString);
                                getAdbShellListener().onShellOutput(finalOutputString);
                            }
                        });
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
            }
        }).start();
        // We become the sending thread
        for (String command : commands) {
            try {
                adbStream.write(" " + command + "\n");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    public AdbShellListener getAdbShellListener() {
        return mAdbShellListener;
    }

    Handler mHandler = new Handler();
}