package com.lifayu.testlbs;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.jar.Manifest;

public class MainActivity extends AppCompatActivity
{
    TextView tv;
    private EditText editText;
    private LocationManager lm;
    private static final String TAG = "GPSActivity";

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        lm.removeUpdates(locationListener);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);







        tv = (TextView)findViewById(R.id.tv);
        editText = (EditText) findViewById(R.id.editText);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //判断GPS是否正常启动
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "请开启定位...", Toast.LENGTH_SHORT).show();
            //返回开启GPS设置界面
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
            return;
        }
        //为获取地理位置信息时设置查询条件
        String bestProvider = lm.getBestProvider(getCriteria(), true);
        //获取位置信息
        //如果不设置查询要求，getLastKnownLocaion方法传入的参数为LocationManager.GPS_PROVIDER
        Location location = lm.getLastKnownLocation(bestProvider);
        updateView(location);
        //监听状态
        lm.addGpsStatusListener(listener);
        //绑定监听，4个参数
        //参数1，设备：有GPS_PROVIDER和NETWORK_PROVIDER两种
        //参数2，位置信息更新周期，单位毫秒
        //参数3，位置变化最小距离
        //参数4，监听
        //备注：参数2和3，如果参数3不为0，则以参数3为准；参数3为0，则通过时间来定时更新；两者为0，则随时刷新
        // 注意：此处更新准确度非常低，推荐在service里面启动一个Thread，在run中
        //sleep(10000);然后执行handler.sendMessage(),更新位置
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
    }
        //位置监听
    private LocationListener locationListener = new LocationListener()
        {
        @Override
        public void onLocationChanged(Location location) {
            //位置信息变化时触发
            updateView(location);
            Log.i(TAG, "时间:" + location.getTime());
            Log.i(TAG, "经度:" + location.getLongitude());
            Log.i(TAG, "纬度:" + location.getLatitude());
            Log.i(TAG, "海拔:" + location.getAltitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //GPS状态变化时触发
            switch (status)
            {
                case LocationProvider.AVAILABLE:
                    tv.setText("当前GPS状态为可见状态\n");
                    Log.i(TAG, "当前GPS状态为可见状态");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    tv.setText("当前GPS状态为服务区外状态\n");
                    Log.i(TAG, "当前GPS状态为服务区外状态");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    tv.setText("当前GPS状态为暂停服务状态\n");
                    Log.i(TAG, "当前GPS状态为暂停服务状态");
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            //GPS开启时触发
            Location location = lm.getLastKnownLocation(provider);
            updateView(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            //GPS禁用时触发
            updateView(null);
        }
    };

    //状态监听
    GpsStatus.Listener listener = new GpsStatus.Listener()
    {
        public void onGpsStatusChanged(int event)
        {
            switch (event)
            {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    tv.setText("第一次定位\n");
                    Log.i(TAG, "第一次定位");
                    break;
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    tv.setText("卫星状态改变\n");
                    Log.i(TAG, "卫星状态改变");
                    //获取当前状态
                    GpsStatus gpsStatus = lm.getGpsStatus(null);
                    //获取卫星颗数的默认最大值
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    //创建一个迭代器保存所有卫星
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                    int count = 0;
                    while(iters.hasNext() && count <= maxSatellites)
                    {
                        GpsSatellite s = iters.next();
                        count++;
                    }
                    tv.setText("搜索到:" + count + "颗卫星\n");
                    Log.i(TAG, "搜索到:" + count + "颗卫星");


                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    tv.setText("定位启动\n");
                    Log.i(TAG, "定位启动");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    tv.setText("定位结束\n");
                    Log.i(TAG, "定位结束");
                    break;
            }
        };
    };
    //实时更新文本内容
    private void updateView(Location location)
    {
        if(location != null)
        {
            editText.setText("设备位置信息\n\n经度:");
            editText.append(String.valueOf(location.getLongitude()));
            editText.append("\n纬度:");
            editText.append(String.valueOf(location.getLatitude()));
        }
        else
        {
            //清空EditText对象
            editText.getEditableText().clear();
        }
    }

    //返回查询条件
    private Criteria getCriteria()
    {
        Criteria criteria = new Criteria();
        //设置定位精确度
        criteria.setAccuracy(Criteria.ACCURACY_FINE);

        //设置是否要求速度
        criteria.setSpeedRequired(false);
        //设置是否允许运营商收费
        criteria.setCostAllowed(false);
        //设置是否需要方位信息
        criteria.setBearingAccuracy(0);
        //设置是否需要海拔信息
        criteria.setAltitudeRequired(false);
        //设置对电源的需求
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }


    //获取权限
    private void getPersimmions()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            ArrayList<String> permissions = new ArrayList<String>();
            //定位权限为必须权限，用户如果禁止，则每次进入都会申请
            //定位精确位置
            if(checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED)
            {
                permissions.add("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            if(checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED)
            {
                permissions.add("Manifest.permission.ACCESS_COARSE_LOCATION");
            }
            //读写权限和电话状态权限为非必要权限，只会申请一次
            //读写权限
            if(addPermission(permissions, "Manifest.permission.WRITE_EXTERNAL_STORAGE"))
            {
                //permissions +=
            }
        }

    }


    private boolean addPermission(ArrayList<String> permissionsList, String permission)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
            {
                Log.i("TAG:", "正在请求权限");
                if(shouldShowRequestPermissionRationale(permission))
                {
                    return true;
                }
                else
                {
                    permissionsList.add(permission);
                    return false;
                }
            }
        }
        else
        {
            return false;
        }
    }




}


































