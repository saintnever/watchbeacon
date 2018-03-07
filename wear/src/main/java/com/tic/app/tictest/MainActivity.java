package com.tic.app.tictest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.BluetoothLeAdvertiser;
//import android.bluetooth.le.BluetoothLeScanner;
//import android.bluetooth.le.ScanCallback;
//import android.bluetooth.le.ScanFilter;
//import android.bluetooth.le.ScanResult;
//import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static android.app.PendingIntent.getActivity;
import static android.bluetooth.le.ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT;
import static android.os.SystemClock.elapsedRealtime;
import static java.lang.Math.abs;

@TargetApi(25)
public class MainActivity extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks{
    private static String TAG = "main";
    TextView txtscan, text_connect_info;
    int ringRSSI=0, last_ringRSSI=0;
    BluetoothManager manager;
    BluetoothAdapter btAdapter;
    BluetoothLeAdvertiser btAdvertiser;
    AdvertisingSet btadvset;
    byte beacon_index = 2;
    byte[] rssi_data = {beacon_index, (byte) 0xff, (byte) 0xff, (byte) 0xff};

    private static final String sendpath = "rssidata";
    private String nodeId = null;
    private GoogleApiClient client;
    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static final String MESSAGE = "Hello Wear!";

    // wifi
    Activity activity_uithread;
    static int PORT = 11121;
    Socket socket = null;
    BufferedReader reader;
    PrintWriter writer;
    boolean listening;
    String tmp_s;

    BluetoothLeScannerCompat scanner;
    ScanSettings settings;
    List<ScanFilter> filters;

    long basetime = System.currentTimeMillis() - elapsedRealtime();

    private static final String ANDROID_MESSAGE_PATH = "/message1";
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String START_ACTIVITY = "/start_activity";
    private GoogleApiClient mApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        txtscan = findViewById(R.id.scan_results);
        txtscan.setText(Long.toString(basetime));
        text_connect_info = findViewById(R.id.connection);
        activity_uithread = (Activity) text_connect_info.getContext();

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        WifiManager.WifiLock wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, TAG);
        wifiLock.setReferenceCounted(true);
        wifiLock.acquire();
        Log.d(TAG, "Acquired WiFi lock");

        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initGoogleClient();

        //        BluetoothLeScanner btScanner = btAdapter.getBluetoothLeScanner();
        ParcelUuid puuid = new ParcelUuid(UUID.fromString("00001819-0000-1000-8000-00805F9B34FB"));
        UUID[] uuid = new UUID[]{UUID.fromString("00001819-0000-1000-8000-00805F9B34FB")};

        if (btAdapter.isEnabled()) {

            scanner = BluetoothLeScannerCompat.getScanner();
            settings = new no.nordicsemi.android.support.v18.scanner.ScanSettings.Builder()
                    .setScanMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(100)
                    .setUseHardwareBatchingIfSupported(true)
                    .build();
            filters = new ArrayList<>();
            filters.add(new no.nordicsemi.android.support.v18.scanner.ScanFilter.Builder().setServiceUuid(new ParcelUuid(uuid[0])).build());
//            no.nordicsemi.android.support.v18.scanner.ScanSettings scansettings = new ScanSettings.Builder()
//                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                    .setMatchMode(MATCH_MODE_STICKY)
//                    .setNumOfMatches(MATCH_NUM_MAX_ADVERTISEMENT)
//                    .setReportDelay(100)
//                    .build();
//            ArrayList<ScanFilter> filters = new ArrayList<>();
//            filters.add(new ScanFilter.Builder().setServiceUuid(puuid).build());
//            btScanner.stopScan(scanCallback);
//            btScanner.startScan(filters, scansettings, scanCallback);
        }

        Button button_scan = findViewById(R.id.button_scan);
        button_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanner.startScan(filters, settings, new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
                        Log.d("b2wdebug", "in scan result");

                        if (result.getDevice().getName() != null) {
                            String s = "";
                            if (result.getDevice().getName().equals("B0")) {
                                ringRSSI = result.getRssi();
                                //Only update if RSSI value changed
                                if (abs(ringRSSI - last_ringRSSI)>1) {
                                    last_ringRSSI = ringRSSI;
//                                    String ctime = String.valueOf(System.currentTimeMillis()).substring(6);
                                    String ctime = String.valueOf(basetime+result.getTimestampNanos()/1000000).substring(6);
                                    s += "0:"+ ctime + ":" + Integer.toString(ringRSSI) + "\n";
                                    send(s);
                                    txtscan.setText(s);
                                }
                            }
                        }
                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        super.onBatchScanResults(results);
                        Log.d("b2wdebug", "in batch scan result");

                        String s = "";
                        for (ScanResult result : results) {
                            if (result.getDevice().getName() != null) {
                                if (result.getDevice().getName().equals("B0")) {
                                    ringRSSI = result.getRssi();
                                    if (abs(ringRSSI - last_ringRSSI) > 1) {
                                        last_ringRSSI = ringRSSI;
//                                    String ctime = String.valueOf(System.currentTimeMillis()).substring(6);
                                        String ctime = String.valueOf(basetime + result.getTimestampNanos() / 1000000).substring(6);
                                        s += "0:" + ctime + ":" + Integer.toString(ringRSSI) + "\n";
                                    }
                                }
                            }
                        }
//                        send(s);
                        sendMessage(ANDROID_MESSAGE_PATH, s);
                        txtscan.setText(s);
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        super.onScanFailed(errorCode);
                    }
                });
            }
        });

        Button button_connect = findViewById(R.id.button_connect);
        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (socket == null) {
//                    new NetworkAsyncTask().execute("101.6.114.22");
//                } else {
//                    try {
//                        listening = false;
//                        disconnect();
//                    } catch (Exception e) {
//                        Log.d("b2wdebug", "button disconnect error: " + e.toString());
//                    }
//                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (mApiClient != null){
            mApiClient.unregisterConnectionCallbacks(this);
            mApiClient.disconnect();
        }


        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        connectGoogleApi();
    }


    @Override
    public void onConnected(Bundle bundle) {
        sendMessage(START_ACTIVITY, "");
        Wearable.MessageApi.addListener(mApiClient, this);
    }


    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (messageEvent.getPath().equalsIgnoreCase(WEAR_MESSAGE_PATH)) {
//                    mArrayAdapter.add(new String(messageEvent.getData()));
//                    mArrayAdapter.notifyDataSetChanged();
                }
            }
        });
    }


    @Override
    protected void onStop() {
        if (mApiClient != null) {
            Wearable.MessageApi.removeListener(mApiClient, this);
            if (mApiClient.isConnected())
                mApiClient.disconnect();
        }
        super.onStop();
    }



    @Override
    public void onConnectionSuspended(int i) {

    }

//    private void init() {
//        mListView = (ListView) findViewById(R.id.list);
//        mArrayAdapter = new ArrayAdapter<>(this, R.layout.list_item);
//        mListView.setAdapter(mArrayAdapter);
//
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//        mButton = (Button) findViewById(R.id.btn_send_wear);
//        mButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                sendMessage(ANDROID_MESSAGE_PATH, "From Wear");
//            }
//        });
//    }

    private void initGoogleClient() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        connectGoogleApi();
    }

    private void connectGoogleApi() {
        if (mApiClient != null && !(mApiClient.isConnected() || mApiClient.isConnecting())) {
            mApiClient.connect();
        }
    }

    private void sendMessage(final String path, final String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mApiClient).await();
                for (Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mApiClient, node.getId(), path, text.getBytes()).await();

                }
            }
        }).start();
    }

    //WiFi Network upload
    void disconnect() {
        try {
            //if (reader != null) reader.close();
            //if (writer != null) writer.close();
            socket.close();
            socket = null;
            text_connect_info.setText("disconnected");
        } catch (Exception e) {
            text_connect_info.setText(e.toString());
        }
    }

    void send(String s) {
        tmp_s = s;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    writer.write(tmp_s);
                    writer.flush();
                }
            }
        }).start();
    }

    void recv(String s) {
        Log.d("b2wdebug", "receive: " + s);
        tmp_s = s;
        activity_uithread.runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                text_0.setText(tmp_s);
            }
        });
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }


    class NetworkAsyncTask extends AsyncTask<String, Integer, String> {

        protected String doInBackground(String... params) {
            try {
                socket = new Socket(params[0], PORT);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                Thread.sleep(300);
                writer.print("Client Send!");
                writer.flush();
                listening = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("b2wdebug", "listening");
                        while (listening) {
                            try {
                                String s = reader.readLine();
                                if (s == null) listening = false;
                                recv(s);
                            } catch (Exception e) {
                                Log.d("b2wdebug", "listen thread error: " + e.toString());
                                listening = false;
                                break;
                            }
                        }
                        activity_uithread.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                disconnect();
                            }
                        });
                    }
                }).start();
                return socket.toString();
            } catch (Exception e) {
                socket = null;
                return e.toString();
            }
        }

        protected void onPostExecute(String string) {
            Log.d("b2wdebug", "connect info: " + string);
            text_connect_info.setText(string);
        }
    }
}
