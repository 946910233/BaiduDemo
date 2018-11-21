package com.baidu.location.demo;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.widget.Button;
import android.widget.TextView;

import com.baidu.baidulocationdemo.R;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;

import java.text.NumberFormat;


/**
 * 适配Android 8.0限制后台定位的功能，新增允许后台定位的接口，即开启一个前台定位服务
 */
public class ForegroundActivity extends Activity {
    private LocationClient mClient;
    private MyLocationListener myLocationListener = new MyLocationListener();

    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private Button mForegroundBtn;
    private NotificationUtils mNotificationUtils;
    private Notification notification;

    public static double longitude;
    public static double latitude;
    public static String status;
    private static LatLng latLng;
    private static LatLng oldLatLng;
    private static double speed;
    private static double distance;
    private static double totalDistance;
    private String street;

    private boolean isFirstLoc = true;
    private TextView speedText;
    private TextView totalDistanceText;
    private TextView statusText;
    private TextView distanceText;
    private PowerManager pm;
    private boolean screenOn;
    private int gpsAccuracyStatus;
    private int count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.foreground);
        mClient = new LocationClient(this);
        LocationClientOption mOption = new LocationClientOption();
        mOption.setScanSpan(1000);//设定每1秒输出结果
        mOption.setCoorType("bd09ll");
        mOption.setIsNeedAddress(true);
        mOption.setOpenGps(true);
        mClient.setLocOption(mOption);
        mClient.registerLocationListener(myLocationListener);
        mClient.start();

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        judge();
        initViews();
    }

    private void judge() {
        // 定位初始化

        //设置后台定位
        //android8.0及以上使用NotificationUtils
        if (Build.VERSION.SDK_INT >= 26) {
            mNotificationUtils = new NotificationUtils(this);
            Notification.Builder builder2 = mNotificationUtils.getAndroidChannelNotification
                    ("适配android 8限制后台定位功能", "正在后台定位");
            notification = builder2.build();
        } else {
            //获取一个Notification构造器
            Notification.Builder builder = new Notification.Builder(ForegroundActivity.this);
            Intent nfIntent = new Intent(ForegroundActivity.this, ForegroundActivity.class);

            builder.setContentIntent(PendingIntent.
                    getActivity(ForegroundActivity.this, 0, nfIntent, 0)) // 设置PendingIntent
                    .setContentTitle("适配android 8限制后台定位功能") // 设置下拉列表里的标题
                    .setSmallIcon(R.drawable.ic_launcher) // 设置状态栏内的小图标
                    .setContentText("正在后台定位") // 设置上下文内容
                    .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

            notification = builder.build(); // 获取构建好的Notification
        }
        notification.defaults = Notification.DEFAULT_SOUND; //设置为默认的声音

    }


    private void initViews() {
        mForegroundBtn = (Button) findViewById(R.id.bt_foreground);
        speedText = (TextView) findViewById(R.id.speed);
        statusText = (TextView) findViewById(R.id.status);
        distanceText = (TextView) findViewById(R.id.distance);
        totalDistanceText = (TextView) findViewById(R.id.totalDistance);

        mClient.enableLocInForeground(1001, notification);
        mForegroundBtn.setText(R.string.stopforeground);

        mMapView = (MapView) findViewById(R.id.mv_foreground);
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);
    }


    class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if (bdLocation == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder().accuracy(bdLocation.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(bdLocation.getDirection()).latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude()).build();
            // 设置定位数据
            mBaiduMap.setMyLocationData(locData);
            //地图SDK处理
            if (isFirstLoc) {//这段代码的作用是把你 的圆点位置显示在地图上，如果不设置否则不管你定位在哪都将默认显示北京天安门
                isFirstLoc = false;
                LatLng ll = new LatLng(bdLocation.getLatitude(),
                        bdLocation.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }

            gpsAccuracyStatus = bdLocation.getGpsAccuracyStatus();
            System.out.println("gps精确度" + gpsAccuracyStatus);
            longitude = bdLocation.getLongitude();
            latitude = bdLocation.getLatitude();
            speed = bdLocation.getSpeed();
            street = bdLocation.getAddrStr();
            latLng = new LatLng(latitude, longitude);
            if (bdLocation.getLocType() == BDLocation.TypeGpsLocation) {
                status = "gps定位";
            } else if (bdLocation.getLocType() == BDLocation.TypeNetWorkLocation) {
                status = "网络定位";
            }

            count++;
            screenOn = pm.isScreenOn();
            if (screenOn) {
                count = 0;
            }
            if (!screenOn &&(count == 70||gpsAccuracyStatus==0)) {
                // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
                PowerManager.WakeLock wl = pm.newWakeLock(
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                                PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
                wl.acquire(10000); // 点亮屏幕
                wl.release(); // 释放
                return;
            }
            System.out.println("count=" + count);


            if (oldLatLng != null && latLng != null) {
                distance = DistanceUtil.getDistance(oldLatLng, latLng);//两距离之差
            }
            if (speed <= 0 || distance >= 12) {//假如速度为0 距离大于12  肯定是误差数据，设定为0
                distance = 0;
                speed = 0;
            }

            totalDistance = distance + totalDistance;//每秒根据经纬度距离累加

            NumberFormat nf = NumberFormat.getNumberInstance();
            nf.setMaximumFractionDigits(2);


            System.out.println("总距离" + totalDistance);
            System.out.println("速度" + speed);
            System.out.println("状态" + status);
            oldLatLng = latLng;
            distanceText.setText("地址" + street);
            totalDistanceText.setText("总" + nf.format(totalDistance));
            statusText.setText(status + "");
            speedText.setText("速度" + distance);

            LatLng point = new LatLng(latitude, longitude);//画点

            OverlayOptions dotOption = new DotOptions().center(point).color(0xAAFF0000);
            mBaiduMap.addOverlay(dotOption);
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("onStop");


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mMapView = null;
        mClient.disableLocInForeground(true);
        mClient.unRegisterLocationListener(myLocationListener);
        mClient.stop();
    }


}
