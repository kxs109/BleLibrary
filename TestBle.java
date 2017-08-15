package com.example.test3;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.zhh.kzlble.BleManager;
import com.zhh.kzlble.BleService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


/**
 * Created by Administrator on 2017/8/14.
 */

public class TestBle extends AppCompatActivity {

    private BleManager mBleManager;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_SHOW_LOCATION = 0;
    private TextView tvCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBleManager = BleManager.newSkinInstance(getApplicationContext());
        setContentView(R.layout.activity_main);
        mBleManager.checkIsSupportBle(this);//检测手机是否支持ble
        setClicks();
    }

    private void setClicks() {
        findViewById(R.id.bt_conn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_SHOW_LOCATION);//6.0以上扫描并连接蓝牙
            }
        });
        findViewById(R.id.bt_disconn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBleManager.disConnect();//断开连接
            }
        });
        findViewById(R.id.bt_write).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBleManager.writeCode("5A0623000003");//向设备发送指令
            }
        });
        tvCode = (TextView) findViewById(R.id.txt_code);
    }

    private void requestPermission(String permission, int requestCode) {
        if (!isGranted(permission)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {//再次申请
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            } else {//首次申请
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        } else {
            mBleManager.searchBle(this);
        }
    }

    private boolean isGranted(String permission) {
        return !isMarshmallow() || isGranted_(permission);
    }

    private boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        mBleManager.searchBle(this);
        super.onActivityResult(requestCode, resultCode, data);
    }


    private boolean isGranted_(String permission) {
        int checkSelfPermission = ActivityCompat.checkSelfPermission(this, permission);
        return checkSelfPermission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(String action) {
        if (TextUtils.equals(action, BleService.ACTION_GATT_SERVICES_DISCOVERED)) {//收到这个指令才可以读写了
            if (mBleManager.getBluetoothService() != null) {
                mBleManager.getBluetoothService().enableTXNotification();
            }
            Toast.makeText(TestBle.this, "BLE DISCOVERED,you can write and read codes", Toast.LENGTH_LONG).show();
        } else if (TextUtils.equals(action, BleService.ACTION_GATT_CONNECTED)) {//仅仅表示ble连接上设备
            mBleManager.removeHandler();
            Toast.makeText(TestBle.this, "BLE CONNECTED", Toast.LENGTH_LONG).show();
        } else if (TextUtils.equals(action, BleService.ACTION_GATT_DISCONNECTED)) {//ble断开连接
            Toast.makeText(TestBle.this, "BLE DISCONNECTED", Toast.LENGTH_LONG).show();
        } else {//接收到的指令，自己作处理
            tvCode.setText("接收的指令"+action);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //mBleManager.disConnect();//退出app,可以选择断开连接
        return super.onKeyDown(keyCode, event);
    }
}
