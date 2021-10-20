package com.llw.mapdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.llw.mapdemo.util.AMapUtil;

import java.lang.reflect.Array;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements AMapLocationListener,LocationSource,AMap.OnMarkerClickListener,GeocodeSearch.OnGeocodeSearchListener {
    //请求权限码
    private static final int REQUEST_PERMISSIONS = 9527;
    //声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    //内容
    private MapView mapView;
    //地图控制器
    private AMap aMap = null;
    private int MyLocationType;
    //位置更改监听
    private LocationSource.OnLocationChangedListener mListener;
    //定义一个UiSettings对象
    private UiSettings mUiSettings;
    public LatLng myLocation;
    //地理编码搜索
    private GeocodeSearch geocodeSearch;
    //解析成功标识码
    private static final int PARSE_SUCCESS_CODE = 1000;
    private String add;
    private NestedScrollView bottomSheet = null;
    private TextView textName = null;
    private TextView textDistance = null;
    private FloatingActionButton fab_plan = null;
    private FloatingActionButton fab_locate = null;
    private Marker preMarker = null;
    private NavigationView navigationView = null;
    private DrawerLayout drawerLayout = null;
    private ImageButton menu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉顶部标题
        setContentView(R.layout.activity_main);

//        tvContent = findViewById(R.id.tv_content);
//        mapView=findViewById(R.id.map_view);
//        mapView.onCreate(savedInstanceState);
        initLocation();
        initMap(savedInstanceState);
        checkingAndroidVersion();


    }
    /**
     * 检查Android版本
     */
    private void checkingAndroidVersion() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            //Android6.0及以上先获取权限再定位
            requestPermission();
        }else {
            //Android6.0以下直接定位
            mLocationClient.startLocation();

//         mLocationClient.stopLocation();


        }
    }

    /**
     * 动态请求权限
     */
    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    private void requestPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (EasyPermissions.hasPermissions(this, permissions)) {
            //true 有权限 开始定位
            showMsg("已获得权限，可以定位啦！");
            mLocationClient.startLocation();
            //停止定位后，本地定位服务并不会被销毁
        } else {
            //false 无权限
            EasyPermissions.requestPermissions(this, "需要权限", REQUEST_PERMISSIONS, permissions);
        }
    }

    /**
     * 请求权限结果
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //设置权限请求结果
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Toast提示
     * @param msg 提示内容
     */
    private void showMsg(String msg){
        Toast.makeText(this,msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置定位请求超时时间，单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.setHttpTimeOut(20000);
        //关闭缓存机制，高精度定位会产生缓存。
        mLocationOption.setLocationCacheEnable(false);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);

        textName = (TextView) findViewById(R.id.text_name);
        textName.setText("我的位置");
        textDistance = (TextView) findViewById(R.id.text_distance);
        bottomSheet = (NestedScrollView) findViewById(R.id.bottom_sheet_main);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        fab_locate = (FloatingActionButton) findViewById(R.id.fab_locate);
        fab_plan =(FloatingActionButton) findViewById(R.id.fab_plan);
        navigationView = (NavigationView) findViewById(R.id.navigate_view);
        navigationView.setCheckedItem(R.id.map_standard);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        menu = (ImageButton) findViewById(R.id.expanded_menu);

    }

    /**
     * 接收异步返回的定位结果
     *
     * @param aMapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //地址
                String address = aMapLocation.getAddress();
                //获取维度
                double latitude=aMapLocation.getLatitude();
                //获取经度
                double longitude=aMapLocation.getLongitude();
                myLocation =new LatLng(latitude,longitude);
                StringBuffer stringBuffer=new StringBuffer();
                stringBuffer.append("纬度：" + latitude + "\n");
                stringBuffer.append("经度：" + longitude + "\n");
                stringBuffer.append("地址：" + address + "\n");

                Log.d("MainActivity",stringBuffer.toString());
                showMsg(address);
                mLocationClient.stopLocation();
//                tvContent.setText(address == null ? "无地址" : address);
                if(mListener!=null){
                    mListener.onLocationChanged(aMapLocation);
                }

            } else {
                //定位失败时，可通过ErrCode（错误码）信息来确定失败的原因，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //销毁定位客户端，同时销毁本地定位服务。
        mLocationClient.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mapView.onPause();
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 初始化地图
     * @param savedInstanceState
     */
    private void initMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.map_view);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(savedInstanceState);
        //初始化地图控制器对象
        aMap = mapView.getMap();

        geocodeSearch=new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);
        // 设置定位监听
        aMap.setLocationSource(this);
        // 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        aMap.setMyLocationEnabled(true);
//        aMap.setMinZoomLevel(20);
        aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        MyLocationType = AMap.LOCATION_TYPE_LOCATE;

        //开启室内地图
        aMap.moveCamera(CameraUpdateFactory.changeTilt(42));
        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        aMap.showIndoorMap(true);
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setScaleControlsEnabled(true);
        mUiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_CENTER);
        fab_locate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myLocation != null) {
                    aMap.animateCamera(CameraUpdateFactory.newLatLng(myLocation));
                    latlonToAddress(myLocation);
                } else {
                    Snackbar.make(mapView, "定位失败。请检查您的设置。",
                            Snackbar.LENGTH_SHORT).show();
                }

                if(MyLocationType==AMap.LOCATION_TYPE_LOCATE){
                    aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_ROTATE);
                }else if (MyLocationType==AMap.LOCATION_TYPE_MAP_ROTATE){
                    aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
                }
            }
        });
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    //白昼地图（即普通地图），aMap是地图控制器对象。
                    case R.id.map_standard:
                        aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    //夜景地图，aMap是地图控制器对象。
                    case R.id.map_night:
                        aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    //显示实时路况图层，aMap是地图控制器对象。
                    case R.id.map_satellite:
                        aMap.setTrafficEnabled(true);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    // 设置卫星地图模式，aMap是地图控制器对象。
                    case R.id.map_SATELLITE:
                        aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                        drawerLayout.closeDrawer(GravityCompat.START);
                        break;
                    default:
                }
                return true;
            }
        });
        menu = (ImageButton) findViewById(R.id.expanded_menu);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        aMap.setOnMarkerClickListener(this);

// 将点标记添加到地图map上

        LatLng latlng5 = new LatLng(30.31171287210056,120.39243819985757);
        LatLng latlng6 = new LatLng(30.309606874393683,120.39414408476185);
        LatLng latlng7 = new LatLng(30.310309650512067,120.3919339445714);
        LatLng latlng8 = new LatLng(30.310316597203077,120.38960310497734);
        LatLng latlng9 = new LatLng(30.30952582904772,120.38964333811187);
        LatLng latlng10 = new LatLng(30.31029691491061,120.39230274830463);
        LatLng latlng11 = new LatLng(30.309415838828155,120.39158793961441);
        LatLng latlng12 = new LatLng(30.307214850698266,120.39243015323066);
        LatLng latlng13 = new LatLng(30.30836455765973,120.39044800080258);
        aMap.addMarker(new MarkerOptions().position(latlng5).title("浙江工商大学钱江湾生活区厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
        aMap.addMarker(new MarkerOptions().position(latlng6).title("浙江工商大学教学区管理楼厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
        aMap.addMarker(new MarkerOptions().position(latlng7).title("浙江工商大学教学区C楼厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
        aMap.addMarker(new MarkerOptions().position(latlng8).title("浙江工商大学教学区图书馆厕所)").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
        aMap.addMarker(new MarkerOptions().position(latlng9).title("浙江工商大学教学区图书馆厕所)").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
        aMap.addMarker(new MarkerOptions().position(latlng10).title("浙江工商大学教学区C楼东厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
        aMap.addMarker(new MarkerOptions().position(latlng11).title("浙江工商大学教学区A楼厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
        aMap.addMarker(new MarkerOptions().position(latlng12).title("浙江工商大学教学区逸夫楼厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
        aMap.addMarker(new MarkerOptions().position(latlng13).title("浙江工商大学教学区文科实验楼厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));

        LatLng latlng4 = new LatLng(30.307188220870692,120.3908248511627);
        aMap.addMarker(new MarkerOptions().position(latlng4).title("浙江工商大学教学区贝因美楼厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));

        LatLng latlng3 = new LatLng(30.307353788811767,120.39008053817382);
        aMap.addMarker(new MarkerOptions().position(latlng3).title("浙江工商大学教学区信息楼东厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));

        LatLng latlng = new LatLng(30.30730863394648,120.38926514664752);
        aMap.addMarker(new MarkerOptions().position(latlng).title("浙江工商大学教学区信息楼厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));

        LatLng latlng2 = new LatLng(30.307601561292117,120.38861068765878);
        aMap.addMarker(new MarkerOptions().position(latlng2).title("浙江工商大学教学区公共厕所").icon(BitmapDescriptorFactory.fromResource(R.drawable.ww)));
    }

    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mLocationClient == null) {
            mLocationClient.startLocation();//启动定位
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    /**
     * 地址转坐标
     * @param geocodeResult
     * @param rCode
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int rCode) {

    }

    private void latlonToAddress(LatLng latLng) {
        //位置点  通过经纬度进行构建
        LatLonPoint latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
        //逆编码查询  第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 5, GeocodeSearch.AMAP);
        //异步获取地址信息
        geocodeSearch.getFromLocationAsyn(query);
    }
    //开始地理位置逆编码
    private void geocodeSearch(LatLonPoint location) {
        final GeocodeSearch geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);
        final RegeocodeQuery query = new RegeocodeQuery(location, 50, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int rCode) {
        //解析result获取地址描述信息
        if (rCode == PARSE_SUCCESS_CODE) {
            RegeocodeAddress regeocodeAddress = regeocodeResult.getRegeocodeAddress();
            //显示解析后的地
            add=regeocodeAddress.getFormatAddress();
        } else {
            showMsg("获取地址失败");
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        latlonToAddress(marker.getPosition());

        showMsg("名称："+marker.getTitle()+"\n距离："+ AMapUtils.calculateLineDistance(myLocation, marker.getPosition())+"米");

        textName.setText(marker.getTitle());
        if (marker != null) {
            float distance = AMapUtils.calculateLineDistance(myLocation, marker.getPosition());
            textDistance.setText(String.format("%s",
                    "距离" + distance));
        } else {
            textDistance.setText("距离不详");
        }

        fab_plan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            Bundle bundle = new Bundle();
            bundle.putDoubleArray("start",new double[]{myLocation.latitude,myLocation.longitude});
            bundle.putDoubleArray("end",new double[]{marker.getPosition().latitude,marker.getPosition().longitude});
            Intent intent = new Intent(getBaseContext(),WalkRouteActivity.class);
            intent.putExtras(bundle);
            startActivity(intent);
            }
        });

        BottomSheetBehavior behavior = BottomSheetBehavior.from(bottomSheet);
        if (preMarker!=null&&preMarker.equals(marker)&&
                behavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        preMarker = marker;
        return false;

    }


}