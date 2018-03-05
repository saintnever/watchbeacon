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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@TargetApi(25)
public class MainActivity extends Activity {
    private static String TAG = "main";
    TextView txtscan, text_connect_info;
    int ringRSSI, last_ringRSSI;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        txtscan = findViewById(R.id.scan_results);
        txtscan.setText("HELLO");
        text_connect_info = findViewById(R.id.connection);
        activity_uithread = (Activity) text_connect_info.getContext();


        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();

//        retrieveDeviceNode();
//        if (nodeId == null){
//            Log.d(TAG, "no node found");
//
//        }

        client = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
//        client.connect();

//        txtscan.setText("Connected to phone! ");

        // bluetooth advertisement
        BluetoothLeScanner btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter.isEnabled()) {
            //Scanning start
            //      ArrayList<ScanFilter> filters = new ArrayList<>();
            //      filters.add(
            //                new ScanFilter.Builder()
            //                        .setServiceUuid(
            //                                new ParcelUuid(UUID.fromString("00001819-0000-1000-8000-00805F9B34FB"))
            //                        )
            //                        .build()
            //      );
            ScanSettings scansettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            btScanner.stopScan(scanCallback);
            btScanner.startScan(null, scansettings, scanCallback);

            // Advertising start
//            btAdvertiser = btAdapter.getBluetoothLeAdvertiser();
//            ParcelUuid puuid = new ParcelUuid(UUID.fromString("00001819-0000-1000-8000-00805F9B34FB"));
//            btAdapter.setName("W0");
//            AdvertiseSettings advsettings = new AdvertiseSettings.Builder()
//                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
//                    .setConnectable(false)
//                    .setTimeout(0)
//                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
//                    .build();
//            AdvertiseData data = new AdvertiseData.Builder()
//                    .setIncludeDeviceName(true)
//                    .addServiceUuid(puuid)
//                    .setIncludeTxPowerLevel(false)
//                    .addManufacturerData(0xff, rssi_data)
//                    .build();
//            btAdvertiser.startAdvertising(
//                    advsettings,
//                    data,
//                    advcallback
//            );
        }


        Button button_connect = findViewById(R.id.button_connect);
        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (socket == null) {
                    new NetworkAsyncTask().execute("101.6.114.22");
                } else {
                    try {
                        listening = false;
                        disconnect();
                    } catch (Exception e) {
                        Log.d("b2wdebug", "button disconnect error: " + e.toString());
                    }
                }
            }
        });
    }



    ScanCallback scanCallback = new ScanCallback() {
        //
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice().getName() != null) {
//                Log.d(TAG, "Device name:" + result.getDevice().getName() + " with RSSI: " + result.getRssi());
                String s = "";
                if (result.getDevice().getName().equals("B0")) {
                    ringRSSI = result.getRssi();
                    txtscan.setText("Device name:" + result.getDevice().getName() + "with RSSI: " + ringRSSI);
                    //Only update if RSSI value changed
                    if (ringRSSI != last_ringRSSI) {
                        last_ringRSSI = ringRSSI;
                        s += result.getTimestampNanos() / 1000000 + ":" + Integer.toString(ringRSSI) + "\n";
                        send(s);
                    }
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            switch (errorCode) {
                case SCAN_FAILED_ALREADY_STARTED:
                    Log.d(TAG, "Failed : Already started");
                    break;
                case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                    Log.d(TAG, "Failed : Application Registration Failed");
                    break;
                case SCAN_FAILED_INTERNAL_ERROR:
                    Log.d(TAG, "Failed : Internal Error");
                    break;
                case SCAN_FAILED_FEATURE_UNSUPPORTED:
                    Log.d(TAG, "Failed : Feature unsupported");
                    break;
                case 5:
                    Log.d(TAG, "Failed : hardware resources");
                    break;
                case 0:
                    Log.d(TAG, "Success : No error");
                    break;
            }
        }
    };

    AdvertiseCallback advcallback = new AdvertiseCallback() {

        private String TAG = "Advertiser";

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Success");
        }

        @Override
        public void onStartFailure(int errorCode) {
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.d(TAG, "Failed : Already started");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    Log.d(TAG, "Failed : Data too large");
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    Log.d(TAG, "Failed : Feature unsupported");
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    Log.d(TAG, "Failed : Internal Error");
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    Log.d(TAG, "Failed : Too many advertisers");
                    break;
            }
        }
    };

//    AdvertisingSetCallback advsetcallback = new AdvertisingSetCallback() {
//
//        private String TAG = "Advertiser";
//
//        @Override
//        public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
//            printstatus(status);
//        }
//
//        @Override
//        public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
//            Log.d(TAG, "Adv Stopped");
//        }
//
//        @Override
//        public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
//            printstatus(status);
//        }
//
//        private void printstatus(int status){
//            switch (status) {
//                case ADVERTISE_FAILED_ALREADY_STARTED:
//                    Log.d(TAG, "Failed : Already started");
//                    break;
//                case ADVERTISE_FAILED_DATA_TOO_LARGE:
//                    Log.d(TAG, "Failed : Data too large");
//                    break;
//                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
//                    Log.d(TAG, "Failed : Feature unsupported");
//                    break;
//                case ADVERTISE_FAILED_INTERNAL_ERROR:
//                    Log.d(TAG, "Failed : Internal Error");
//                    break;
//                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
//                    Log.d(TAG, "Failed : Too many advertisers");
//                    break;
//                case ADVERTISE_SUCCESS:
//                    Log.d(TAG, "Success : Adv Started");
//                    break;
//            }
//        }
//
//    };


    /**
     * Connects to the GoogleApiClient and retrieves the connected device's Node ID. If there are
     * multiple connected devices, the first Node ID is returned.
     */
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                client.connect();
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(client).await();
                List<Node> nodes = result.getNodes();
                if (nodes.size() > 0) {
                    nodeId = nodes.get(0).getId();
                }
                client.disconnect();
            }
        }).start();
    }

    private void sendRSSI(final String data) {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Wearable.MessageApi.sendMessage(client, nodeId, sendpath, data.getBytes());
                }
            }).start();
        }
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
