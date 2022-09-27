# Android_WPA3
1、直接安装APK既可使用，暂时只测试了华为（HarmonyOS 2.0）和小米(Android 11)，安装完成后授予对应的权限（Android版本大于11时，因为Android修改存储机制，需要授予ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION权限）

2、可以在连接前删除手机已有的profile，这个功能需要授予应用device_owner权限，非测试手机不建议开启（没有此权限不影响测试）

授权：adb shell dpm set-device-owner "com.sakeri.connetc_wifi/.AdminReceiver"

删除授权：adb shell dpm remove-active-admin "com.sakeri.connetc_wifi/.AdminReceiver"

3、小米手机WiFi权限通过Apk只能授予单次运行的权限，需要在设置中手动授予WiFi管理权限为始终允许

当前功能：

1、	对BSSID做了区分，可以根据BSSID连接对应的热点；

2、	显示支持的能力集，可以看到热点是否支持加密、鉴权、密钥管理方式、WPS、KVR等（需要手机支持KVR）；

3、	后台每隔30s扫描一次，使用时如果不显示扫描结果，请检查是否开启了位置权限；

4、	支持设置None、WPA/WPA2-PSK、WPA3-SAE、WPA/WPA2-PSK/WPA3-SAE四种加密，对应加密只能连接对应加密方法，WPA/WPA2-PSK/WPA3-SAE只能连接WPA3混合的加密热点，WAP2混合请选择WPA/WPA2-PSK；

5、	可以单独设置TKIP和AES（建议同时设置）；

6、	开启Stress_Test后设置Time_Interval（两次连接之间时间间隔）Test_Number（连接次数）即可以进行压力测试挂机，Test_Number为0即不限次数；
