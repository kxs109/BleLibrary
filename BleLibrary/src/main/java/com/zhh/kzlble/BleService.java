package com.zhh.kzlble;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import org.greenrobot.eventbus.EventBus;

import java.util.UUID;



/**
 * ble服务类
 *
 * @author sam
 * @version 1.0
 */
@SuppressLint("NewApi")
public class BleService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    public final static String ACTION_GATT_CONNECTED = "com.zhh.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.zhh.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.zhh.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "com.zhh.DEVICE_DOES_NOT_SUPPORT_UART";

    private static final UUID CCCD = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID RX_SERVICE_UUID = UUID
            .fromString("0000ffe5-0000-1000-8000-00805f9b34fb");
    private static final UUID TX_SERVICE_UUID = UUID
            .fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    private static final UUID RX_CHAR_UUID = UUID
            .fromString("0000ffe9-0000-1000-8000-00805f9b34fb");
    private static final UUID TX_CHAR_UUID = UUID
            .fromString("0000ffe4-0000-1000-8000-00805f9b34fb");
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices();
                EventBus.getDefault().post(ACTION_GATT_CONNECTED);//1.连接上

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnect();
                BleManager.SEND_CODE_STATUS=0;
                BleManager.IS_DEVICE_CONN=false;
                EventBus.getDefault().post(ACTION_GATT_DISCONNECTED);//2.断开连接
            }
        }




        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                EventBus.getDefault().post(ACTION_GATT_SERVICES_DISCOVERED);//3.可读写
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(characteristic);
        }
    };


    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
            EventBus.getDefault().post(ConvertUtil.Bytes2HexString(characteristic.getValue()));//4.发送指令
        }
    }

    public class LocalBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        if (mBluetoothDeviceAddress != null
                && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();

        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    @Override
    public void onDestroy() {
        stopSelf();
        super.onDestroy();
    }




    public void enableTXNotification() {
        if (mBluetoothGatt == null) {
            EventBus.getDefault().post(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattService RxService = mBluetoothGatt
                .getService(TX_SERVICE_UUID);
        if (RxService == null) {
            EventBus.getDefault().post(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            EventBus.getDefault().post(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar, true);

        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    public void writeRXCharacteristic(byte[] value) {
        if (mBluetoothGatt != null) {
            BluetoothGattService RxService = mBluetoothGatt
                    .getService(RX_SERVICE_UUID);
            if (RxService == null) {
                EventBus.getDefault().post(DEVICE_DOES_NOT_SUPPORT_UART);
                return;
            }
            BluetoothGattCharacteristic RxChar = RxService
                    .getCharacteristic(RX_CHAR_UUID);
            if (RxChar == null) {
                EventBus.getDefault().post(DEVICE_DOES_NOT_SUPPORT_UART);
                return;
            }
            RxChar.setValue(value);
            mBluetoothGatt.writeCharacteristic(RxChar);
        }
    }

}
