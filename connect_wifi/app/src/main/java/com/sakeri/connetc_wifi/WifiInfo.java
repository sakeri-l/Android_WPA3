package com.sakeri.connetc_wifi;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.wifi.WifiConfiguration;


public class WifiInfo extends AppCompatActivity {
    private WifiManager wifiManager = null;
    private final String[] starArray = {"None", "WPA3-SAE", "WPA/WPA2-PSK", "WPA/WPA2-PSK/WPA3-SAE"};
    private CheckBox tkip;
    private CheckBox aes;

    //stress
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch mSwitch;
    private TextView time_interval;
    private EditText input_time;
    private TextView test_numbers;
    private EditText input_numbers;
    //stress_information
    private String test_auth;
    private String test_ssid;
    private String test_bssid;
    private String test_password;
    private String test_interval;
    private String test_number;
    private String test_ping;

    private EditText input_ssid;
    private EditText input_bssid;
    private EditText input_password;
    private EditText input_ping;

    private final Timer timer = new Timer();
    private final Timer timer1 = new Timer();
    private long node = 1;
    private long test_result_node = 0;

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_info);
        input_ssid = findViewById(R.id.input_ssid);
        input_bssid = findViewById(R.id.input_bssid);
        input_password = findViewById(R.id.input_password);
        input_ping = findViewById(R.id.input_ping);
        mSwitch = (Switch) findViewById(R.id.stress_switch);
        wireless_information();
        initSpinner();
        stress_test();
        cipher_select();
        try {
            start_test();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(wifiReceivers);
        } catch (Exception e) {
            MyLog.d("wifi_wifiReceivers_not_registered", String.valueOf(e));
        }
        if (timer != null) {
            timer.cancel();
            timer1.cancel();
        }
        super.onDestroy();
    }

    private void wireless_information() {
        TextView encryption_mode = findViewById(R.id.encryption_mode);
        TextView channel_rssi = findViewById(R.id.channel_rssi);
        //接受请求
        Intent intent = getIntent();
        //取出数据
        Bundle bundle = intent.getBundleExtra("object");
        int position = bundle.getInt("position");
        //为editText设置文本
        final String[] ssid = {bundle.getString("ssid")};
        input_ssid.setText(ssid[0]);
        input_bssid.setText(bundle.getString("bssid"));
        encryption_mode.setText(bundle.getString("encryption"));
        channel_rssi.setText(bundle.getString("channel_rssi"));
        test_ssid = input_ssid.getText().toString();
        test_bssid = input_bssid.getText().toString();
        test_password = input_password.getText().toString();
        test_ping = input_ping.getText().toString();
        input_ping.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                MyLog.d("wifi_ssid_change", "ssid changed success");
                test_ping = input_ping.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        input_ssid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                MyLog.d("wifi_ssid_change", "ssid changed success");
                test_ssid = input_ssid.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        input_bssid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                MyLog.d("wifi_bssid_change", "bssid changed success");
                test_bssid = input_bssid.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        input_password.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                MyLog.d("wifi_password_change", "password changed success");
                test_password = input_password.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void initSpinner() {
        //声明一个下拉列表的数组适配器
        ArrayAdapter<String> starAdapter = new ArrayAdapter<>(this, R.layout.item_select, starArray);
        starAdapter.setDropDownViewResource(R.layout.item_dropdown);
        Spinner encryption_spinner = findViewById(R.id.spinner);
        encryption_spinner.setPrompt("");
        //设置下拉框的数组适配器
        encryption_spinner.setAdapter(starAdapter);
        //设置下拉框默认的显示第一项
        encryption_spinner.setSelection(3);
        //给下拉框设置选择监听器，一旦用户选中某一项，就触发监听器的onItemSelected方法
        encryption_spinner.setOnItemSelectedListener(new MySelectedListener());
    }

    class MySelectedListener implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            test_auth = starArray[i];
//            Toast.makeText(WifiInfo.this,"您选择的是："+starArray[i],Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
            test_auth = "WPA/WPA2-PSK/WPA3-SAE";
        }
    }

    private void stress_test() {
        time_interval = findViewById(R.id.time_interval);
        input_time = findViewById(R.id.input_time);
        test_numbers = findViewById(R.id.test_number);
        input_numbers = findViewById(R.id.input_testnumber);
        test_interval = input_time.getText().toString();
        test_number = input_numbers.getText().toString();
        mSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                time_interval.setVisibility(View.VISIBLE);
                test_numbers.setVisibility(View.VISIBLE);
                input_time.setVisibility(View.VISIBLE);
                input_numbers.setVisibility(View.VISIBLE);
            } else {
                MyLog.d("wfii_hide_controls", "");
                time_interval.setVisibility(View.GONE);
                input_time.setVisibility(View.GONE);
                test_numbers.setVisibility(View.GONE);
                input_numbers.setVisibility(View.GONE);
            }
        });
        input_time.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                MyLog.d("wifi_bssid_change", "bssid changed success");
                test_interval = input_time.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        input_numbers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                MyLog.d("wifi_password_change", "password changed success");
                test_number = input_numbers.getText().toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
//        MyLog.d("wifi_test_interval", String.valueOf(test_interval));
//        MyLog.d("wifi_test_interval", String.valueOf(test_number));
    }

    private ArrayList cipher_select() {
        tkip = findViewById(R.id.tkip);
        aes = findViewById(R.id.aes);
        ArrayList<String> cipher = new ArrayList<>();
        tkip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (tkip.isChecked() == true) {
                    cipher.add("TKIP");
                } else {
                    cipher.remove("TKIP");
                }
            }
        });
        aes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (aes.isChecked() == true) {
                    cipher.add("CCMP");
                } else {
                    cipher.remove("CCMP");
                }
            }
        });
        return cipher;
    }


    public void add_networks(String ssid, String bssid, String password, String authentication, String cipher) throws IOException {
        MyLog.d("wifi_add_networds", ssid + "_" + bssid + "_" + password + "_" + authentication + "_" + cipher);
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";
        config.status = WifiConfiguration.Status.ENABLED;


        /**
         *  设置认证方式和密码
         *
         *  Open System authentication (required for WPA/WPA2)
         *  public static final int OPEN = 0;
         *  Shared Key authentication (requires static WEP keys)
         *  public static final int SHARED = 1;
         *  LEAP/Network EAP (only used with LEAP)
         *  public static final int LEAP = 2;
         */
        if (authentication.equals("None")) {

        } else if (authentication.equals("WPA-PSK")) {
            config.preSharedKey = "\"" + password + "\"";
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        } else if (authentication.equals("WPA2-PSK")) {
            config.preSharedKey = "\"" + password + "\"";
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        } else if (authentication.equals("WPA3-SAE")) {
            //WPA3
            config.preSharedKey = "\"" + password + "\"";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
            }
        } else if (authentication.equals("WPA/WPA2-PSK")) {
            config.preSharedKey = "\"" + password + "\"";
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        } else if (authentication.equals("WPA/WPA2-PSK/WPA3-SAE")) {
            config.preSharedKey = "\"" + password + "\"";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                config.setSecurityParams(WifiConfiguration.SECURITY_TYPE_SAE);
            }
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        } else {
            //未知加密
            Toast.makeText(WifiInfo.this, "未知认证方式！", Toast.LENGTH_SHORT).show();
        }

        /**
         *  设置加密协议
         *  WPA/IEEE 802.11i/D3.0
         *  public static final int WPA = 0;
         *  WPA2/IEEE 802.11i
         *  public static final int RSN = 1;
         */
        if (authentication.equals("None")) {

        } else if (authentication.equals("WPA-PSK")) {
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        } else if (authentication.equals("WPA2-PSK")) {
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        } else if (authentication.equals("WPA3-SAE")) {
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        } else if (authentication.equals("WPA/WPA2-PSK")) {
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//            config.allowedProtocols.set(WifiConfiguration.Protocol.WAPI);
        } else if (authentication.equals("WPA/WPA2-PSK/WPA3-SAE")) {
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        } else {
            Toast.makeText(WifiInfo.this, "未知加密协议类型！", Toast.LENGTH_SHORT).show();
        }

        /**
         *  设置密码管理方式
         *
         *  WPA is not used; plaintext or static WEP could be used.
         *  public static final int NONE = 0;
         *  WPA pre-shared key (requires {@code preSharedKey} to be specified).
         *  public static final int WPA_PSK = 1;
         *  WPA using EAP authentication. Generally used with an external authentication server.
         *  public static final int WPA_EAP = 2;
         *  IEEE 802.1X using EAP authentication and (optionally) dynamically generated WEP keys.
         *  public static final int IEEE8021X = 3;
         */
        if (authentication.equals("None")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (authentication.equals("WPA-PSK")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        } else if (authentication.equals("WPA2-PSK")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        } else if (authentication.equals("WPA3-SAE")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.SAE);

        } else if (authentication.equals("WPA/WPA2-PSK")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        } else if (authentication.equals("WPA/WPA2-PSK/WPA3-SAE")) {
//            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.SAE);
        } else {
            Toast.makeText(WifiInfo.this, "未知密码管理方式！", Toast.LENGTH_SHORT).show();
        }

        /**
         *  设置PairwiseCipher和GroupCipher
         */
        if (!cipher.equals("")) {
            if (cipher.contains("CCMP")) {
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.GCMP_256);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.GCMP_256);

            }
            if (cipher.contains("TKIP")) {
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            }
        } else {
            Toast.makeText(WifiInfo.this, "未知鉴权方式！", Toast.LENGTH_SHORT).show();
        }
        /**
         *  设置Bssid
         */
        config.BSSID = bssid;
        //connect
//        MyLog.d("wifi_add_wifi_configure", String.valueOf(config));
        // 删除历史保存的profile
//        remove_networks();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.addNetwork(config);
        MyLog.d("wifi_configure", String.valueOf(config));
    }

    public void connect_networks(String ssid) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            MyLog.d("wifi_Permissions_check", "没有授权");
            return;
        }
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {
                MyLog.d("wifi_BroadcastReceiver", "Profile添加成功！");
                MyLog.d("wifi_connect_wifi_start", "=======================");
                MyLog.d("wifi_connect_wifi", "断开WiFi连接");
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                MyLog.d("wifi_connect_wifi", "重新连接WiFi");
//                MyLog.d("wifi_networks_id", String.valueOf(i.networkId));
                wifiManager.reconnect();
//                MyLog.d("wifi_connect_wifi_end", "=======================");
                break;
            }
        }
    }

    public void remove_networks() throws IOException {
        //需要devices_owner权限
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            if (wifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLING)
                wifiManager.setWifiEnabled(true);
        }
        MyLog.d("wifi_remove_wifi_start", "=======================");
        @SuppressLint("MissingPermission") List<WifiConfiguration> wifi_list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : wifi_list) {
            MyLog.d("wifi_remove_wifi", "删除历史保存的profile,需要devices_owner权限!");
//            MyLog.d("wifi_ssid", i);
            MyLog.d("wifi_ssid", i.SSID);
//            wifiManager.removeNetwork(i.networkId);
        }
        MyLog.d("wifi_remove_wifi_end", "=======================");
    }

    private void start_test() throws IOException {
        ArrayList<String> cipher = cipher_select();
        View connect = findViewById(R.id.button);
        //点击事件
        connect.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                node = 0;
                test_result_node = 0;
                String test_cipher = String.join("/", cipher);
                try {
                    remove_networks();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    add_networks(test_ssid, test_bssid, test_password, test_auth, test_cipher);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mSwitch.isChecked()) {
                    MyLog.d("wifi_start_test", "wifi: " + test_ssid + "_" + test_bssid + "_" + test_password + "_" + test_auth + "_" + cipher + "_" + test_interval + "_" + test_number);
                    MyLog.d("wifi_stress_end", "测试开始！");
                    timer.schedule(new TimerTask() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void run() {
                            connect_networks(test_ssid);
                            MyLog.d("wifi_testing_node", "当前测试" + String.valueOf(node + 1) + "次！");
                            get_wifi_info();
                            node = node + 1;
                            if (node >= Integer.parseInt(test_number) && Integer.parseInt(test_number) != 0) {
                                MyLog.d("wifi_stress_end", "测试结束！");
                                timer.cancel();
                            }
                        }
                    }, 1000, Integer.parseInt(test_interval) * 1000);

                } else {
                    MyLog.d("wifi_start_test", "wifi: " + test_ssid + "_" + test_bssid + "_" + test_password + "_" + test_auth + "_" + cipher);
                    connect_networks(test_ssid);
                    node = node + 1;
                    get_wifi_info();
                }
            }
        });
    }

    private void get_wifi_info() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceivers, filter);
    }

    BroadcastReceiver wifiReceivers = new BroadcastReceiver() {
        //        @RequiresApi(api = Build.VERSION_CODES.M)
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            //监听WiFi的连接结果
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
//                MyLog.d("wifi_BroadcastReceiver", "收到NETWORK_STATE_CHANGED_ACTION连接消息！！！");

                Parcelable parcelableExtra = intent
                        .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (null != parcelableExtra) {
                    NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                    NetworkInfo.State states = networkInfo.getState();
                    MyLog.d("wifi_BroadcastReceiver", "当前连接状态为："+String.valueOf(states));
//                    boolean isConnected = states == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                    if (states == NetworkInfo.State.CONNECTED){
                        MyLog.d("wifi_BroadcastReceiver", "---第"+String.valueOf(node)+"次连接成功!");
                        test_result_node = test_result_node + 1;
                        MyLog.d("wifi_Test_Result", "当前测试"+String.valueOf(node)+"次，连接成功"+String.valueOf(test_result_node)+"次！");
                        Toast.makeText(WifiInfo.this, "连接成功！", Toast.LENGTH_SHORT).show();
                        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                        android.net.wifi.WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                        String bss = wifiInfo.getBSSID();
                        String ss = wifiInfo.getSSID();
                        MyLog.d("wifi_ssid",ss);
                        MyLog.d("wifi_bssid",bss);
//                        new NetPing().execute();
                        timer1.schedule(new TimerTask() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            @Override
                            public void run() {
                                try {
                                    PingCheck s = new PingCheck();
                                    s.executeProcess(5000,test_ping);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }, 5000);
                    }else if (states == NetworkInfo.State.CONNECTING) {
                        MyLog.d("wifi_BroadcastReceiver", "---连接中！---");
                    }else if(states == NetworkInfo.State.DISCONNECTED){
//                        Toast.makeText(WifiInfo.this, "连接失败！", Toast.LENGTH_SHORT).show();
                        MyLog.d("wifi_BroadcastReceiver", "---连接失败!---");
                    }else if(states == NetworkInfo.State.DISCONNECTING){
                        MyLog.d("wifi_BroadcastReceiver", "---断开连接中!---");
                    }else {
                        MyLog.d("wifi_BroadcastReceiver", "---未知错误！---");
                    }
                }
            }
        }
    };
}
