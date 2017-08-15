package com.zhh.kzlble;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import static android.content.Context.BIND_AUTO_CREATE;


/**
 * ble管理类，该类处理ble搜索连接发送指令和接收指令处理的所有方法
 *
 * @author sam
 * @version 1.0
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleManager {
    private BluetoothDevice mDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private BleService mBluetoothLeService;
    private boolean isBind;
    private boolean mScanning;
    private static final int REQUEST_ENABLE_BT = 1;
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 15000;
    private static long scanPeriod;
    private Context mCtx;
    private static BleManager mBleManager;
    public static boolean IS_DEVICE_CONN;//判断蓝牙是否连接
    public static int SEND_CODE_STATUS;//同步状态值
    private boolean isScanOver;//设置这个值是因为可能蓝牙扫描时长到了，但可能正在连接设备，却弹框提示用户设置，返回后主界面却显示已经连接状态

    public BleManager(Context ctx) {
        this.mCtx = ctx;
    }

    public static BleManager newSkinInstance(Context ctx) {
        if (mBleManager == null) {
            mBleManager = new BleManager(ctx);
            mBleManager.bindService();
        }
        return mBleManager;
    }

    /**
     * 检测是否支持Ble
     */
    public void checkIsSupportBle(Activity act) {
        if (!act.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(act, act.getResources().getString(R.string.not_support_ble), Toast.LENGTH_LONG).show();
            return;
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) act
                .getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(act, act.getResources().getString(R.string.not_support_ble), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 断开ble
     */
    public void disConnect() {
        unBindService();
        if (mBluetoothLeService != null) {
            BleManager.IS_DEVICE_CONN = false;
            BleManager.SEND_CODE_STATUS = 0;
            mBluetoothLeService.disconnect();
        }
    }


    /**
     * 搜索ble
     */
    public void searchBle(Activity act) {
        isScanOver = false;
        if (!mBluetoothAdapter.isEnabled()) {// 弹框提示用户打开蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (act != null)
                act.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {// 开始扫描蓝牙
            scanLeDevice(true, act);
        }
    }

    /**
     * 解绑服务
     */
    public void unBindService() {
        if (isBind)
            mCtx.unbindService(mServiceConnection);
    }


    /**
     * 主要用于扫描蓝牙的时候的处理
     * 在扫描页面activity的onStop方法应该调用这个来移除handler
     */
    public void removeHandler() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    /**
     * 扫描设备 传的参数为true表示扫描，false表示不扫描
     *
     * @param enable
     */
    public void scanLeDevice(final boolean enable, final Activity act) {
        if (scanPeriod == 0)
            scanPeriod = SCAN_PERIOD;
        if (enable) {
            if (mHandler == null) {
                mHandler = new Handler();
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanOver = true;
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    showMissingPermissionDialog(R.string.need_location_permission, act);
                }
            }, scanPeriod);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private void showMissingPermissionDialog(int strId, final Activity act) {
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(mCtx.getResources().getString(R.string.ble_permission_dialog_text1));
        builder.setMessage(mCtx.getResources().getString(strId)/* + mCtx.getResources().getString(R.string.ble_permission_dialog_text2)*/);
        // 拒绝, 退出应用
        builder.setNegativeButton(R.string.button_deny,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        act.finish();
                    }
                });
        builder.setPositiveButton(R.string.button_setting,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        act.finish();
                        startAppSettings(act);
                    }
                });
        builder.setCancelable(false);
        builder.show();
    }

    private void startAppSettings(Activity act) {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        act.startActivity(intent);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            if (null != device && !TextUtils.isEmpty(device.getName())
                    && device.getName().contains("your device name")) {
                mDevice = device;
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                if (!isScanOver)
                    connDevice();
            }
        }
    };


    /**
     * 绑定服务
     */
    public void bindService() {
        Intent gattServiceIntent = new Intent(mCtx, BleService.class);
        isBind = mCtx.bindService(gattServiceIntent, mServiceConnection,
                BIND_AUTO_CREATE);
    }


    /**
     * 设置扫描时长
     *
     * @param time
     */
    public void setScanPeriod(long time) {
        scanPeriod = time;
    }

    /**
     * 扫描绑定服务后连接设备
     */
    public void connDevice() {
        if (mDevice != null) {
            mBluetoothLeService.connect(mDevice.getAddress());
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BleService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeService.initialize()) {
                Toast.makeText(mCtx, mCtx.getResources().getString(R.string.ble_init_failed), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    public BleService getBluetoothService() {
        if (mBluetoothLeService != null) {
            return mBluetoothLeService;
        }
        return null;
    }

    /**
     * 向设备发送指令
     *
     * @param hexStr
     */
    public void writeCode(String hexStr) {
        if (mBluetoothLeService != null)
            mBluetoothLeService.writeRXCharacteristic(ConvertUtil
                    .HexString2Bytes(hexStr));
    }

}


