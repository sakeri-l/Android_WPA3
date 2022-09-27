package com.sakeri.connetc_wifi;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.StrictMode;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.net.NetworkInfo.State;
import android.annotation.SuppressLint;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

import android.net.wifi.ScanResult;

import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;

public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager = null; //Wifi管理器
    //scan
    private ListView listView;
    private final ArrayList<String> arrayList = new ArrayList<>();
    private final ArrayList<ScanResult> arrayList_info = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final List<String> mListInfo = new ArrayList<>();
    private final Timer timer = new Timer();
    //permissions
    String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int MY_PERMISSIONS_REQUEST_CODE = 10000;
    List<String> mPermissionList = new ArrayList<>();
    public MainActivity() {
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        setContentView(R.layout.activity_main);
        getPermissions();
        listView = findViewById(R.id.wifiList);
        scan_networks();
    }
    @SuppressLint("LongLogTag")
    @Override
    protected void onDestroy() {
        MyLog.d("wifi_delete_timer", "删除所有定时任务！");
        try{
            unregisterReceiver(wifiReceiver);
        }
        catch (Exception e){
            MyLog.d("wifi_wifiReceiver_not_registered", String.valueOf(e));
        }
        if(timer != null){
            timer.cancel();
        }
        super.onDestroy();
    }
    private void getPermissions() {
        mPermissionList.clear();                                    //清空已经允许的没有通过的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                MyLog.d("wifi_permissions","此手机是Android 11或更高的版本，且已获得访问所有文件权限");
                // TODO requestOtherPermissions() 申请其他的权限
            } else {
                MyLog.d("wifi_permissions","此手机是Android 11或更高的版本，且没有访问所有文件权限");
                Intent panelIntent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(panelIntent);
            }
        } else {
            MyLog.d("wifi_permissions","此手机版本小于Android 11，不需要申请文件管理权限");
            // TODO requestOtherPermissions() 申请其他的权限
        }
        for (String permission : permissions) {          //逐个判断是否还有未通过的权限
            if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permission);
            }
        }
        if (mPermissionList.size() > 0) {                           //有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this, permissions, MY_PERMISSIONS_REQUEST_CODE);
        } else {
            MyLog.d("wifi_permissions", "已经授权");     //权限已经都通过了
        }
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int wifiState = wifiManager.getWifiState();
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                MyLog.d("wifi_state", "wifiState = " + wifiState + " WIFI_STATE_ENABLED");
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                MyLog.d("wifi_state", "wifiState = " + wifiState + " WIFI_STATE_DISABLED");
                Toast.makeText(MainActivity.this, "WiFi已关闭，请先打开WiFi...", Toast.LENGTH_SHORT).show();
//                Intent panelIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
//                startActivity(panelIntent);
                break;
            default:
                break;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;      //有权限没有通过
        if (MY_PERMISSIONS_REQUEST_CODE == requestCode) {
            for (int grantResult : grantResults) {
                if (grantResult == -1) {
                    hasPermissionDismiss = true;   //发现有未通过权限
                    break;
                }
            }
        }
        else {
            MyLog.d("wifi_permissions_check", "有未通过权限");
        }
        if (hasPermissionDismiss) {                //如果有没有被允许的权限
            //假如存在有没被允许的权限,可提示用户手动设置 或者不让用户继续操作
            MyLog.d("wifi_permissions_check", "有未通过权限");
        } else {
            MyLog.d("wifi_permissions_check", "已全部授权");
        }
    }
    @SuppressLint("HandlerLeak")
    public void scan_networks() {
        // --- start ---
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(this, "WiFi is disabled,Enabling...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,mListInfo);
        listView.setAdapter(adapter);
        // 添加点击相应事件监视器
        listView.setOnItemClickListener((parent, view, position, id) -> {
            //创建intent对象，并完成两个类的绑定
            ScanResult res = arrayList_info.get(position);
            Intent intent = new Intent(getApplicationContext(), com.sakeri.connetc_wifi.WifiInfo.class);
            Bundle bundle = new Bundle();
            bundle.putString("ssid",res.SSID);
            bundle.putString("bssid",res.BSSID);
            bundle.putString("encryption",res.capabilities);
            bundle.putString("channel_rssi", res.frequency +"      "+ res.level);
            bundle.putInt("position",position);
            //把bundle添加到intent中
            intent.putExtra("object",bundle);
            //有返回的发送请求
            startActivity(intent);
        });
        timer.schedule(new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                scanWifi();
            }
        }, 1000, 30000);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void scanWifi() {
//        MyLog.d("wifi_scan_wifi_start", "=======================");
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, filter);
        wifiManager.startScan();
    }
    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceive(Context context, Intent intent) {

            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
//                MyLog.d("wifi_BroadcastReceiver","recive SCAN_RESULTS_AVAILABLE_ACTION");
                List<ScanResult> results = wifiManager.getScanResults();
                arrayList.clear();
                arrayList_info.clear();
                for (ScanResult scanResult : results) {
                    if (scanResult.SSID.length() > 0){
                        arrayList_info.add(scanResult);
                        arrayList.add(scanResult.SSID + "\n" + scanResult.BSSID + "\n" + scanResult.capabilities);
                        mListInfo.clear();
                        mListInfo.addAll(arrayList);
                        adapter.notifyDataSetChanged();
                    }
                }
//                MyLog.d("scanResult", String.valueOf(mListInfo));
//                MyLog.d("wifi_scan_wifi_end", "=======================");
            }
        }
    };
}