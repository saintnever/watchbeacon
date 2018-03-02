package com.tic.app.tictest;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.UUID;

import static android.bluetooth.le.AdvertisingSetParameters.INTERVAL_LOW;

@TargetApi(23)
public class MainActivity extends Activity {
    private static String TAG = "main";
    TextView txtscan;
    int ringRSSI, last_ringRSSI;
    BluetoothManager manager;
    BluetoothAdapter btAdapter;
    BluetoothLeAdvertiser btAdvertiser;
    AdvertisingSet btadvset;
    byte beacon_index = 2;
    byte[] rssi_data = {beacon_index, (byte) 0xff, (byte) 0xff, (byte) 0xff};
//    AdvertisingSetParameters advparam = new AdvertisingSetParameters.Builder()
//            .setInterval(INTERVAL_LOW)
//            .setScannable(true)
//            .setConnectable(false)
//            .build();

//            results.setText(result.getDevice().getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        txtscan = findViewById(R.id.scan_results);
        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = manager.getAdapter();

        // bluetooth advertisement
        BluetoothLeScanner btScanner = btAdapter.getBluetoothLeScanner();

        if(btAdapter.isEnabled()) {
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
            btAdvertiser = btAdapter.getBluetoothLeAdvertiser();
            ParcelUuid puuid = new ParcelUuid(UUID.fromString("00001819-0000-1000-8000-00805F9B34FB"));
            btAdapter.setName("W0");
            AdvertiseSettings advsettings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setConnectable(false)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .build();
            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .addServiceUuid(puuid)
                    .setIncludeTxPowerLevel(false)
                    .addManufacturerData(0xff,rssi_data)
                    .build();
            btAdvertiser.startAdvertising(
                    advsettings,
                    data,
                    advcallback
            );

//            btAdvertiser.startAdvertisingSet(advparam, data, null,null,null,0,0,advsetcallback);

            Log.d("b2wdebug", "service uuid is " + data.getServiceUuids());
            Log.d("b2wdebug", "manu data is " + data.getManufacturerSpecificData().toString());
            Log.d("b2wdebug", "service data is " + data.getServiceData().toString());
            Log.d("b2wdebug", "all data is " + data.toString());

        }

    }

    ScanCallback scanCallback = new ScanCallback() {
        //
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
//            TextView results = (TextView) ((Activity)context).findViewById(R.id.scan_results);
//            results.setText(result.getDevice().getName());
//            Log.d(TAG, "in scan callback ");
            if(result.getDevice().getName() != null) {
                Log.d(TAG, "Device name:" + result.getDevice().getName() + " with RSSI: " + result.getRssi());

                if (result.getDevice().getName().equals("B0")) {
                    ringRSSI = result.getRssi();
                    txtscan.setText("Device name:" + result.getDevice().getName() + "with RSSI: " +ringRSSI);
                    //Only update if RSSI value changed
                    if (ringRSSI != last_ringRSSI) {
                        last_ringRSSI = ringRSSI;
//                        rssi_data[1] = (byte)ringRSSI;
//                        AdvertiseData data = new AdvertiseData.Builder()
//                                .setIncludeDeviceName(true)
//                                .setIncludeTxPowerLevel(false)
//                                .addManufacturerData(0xff,rssi_data)
//                                .build();
//                        btadvset.
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


}
