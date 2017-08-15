1.本工具类将android原生ble示例简化操作，使用BleManager类进行扫描蓝牙，连接蓝牙，发送指令，断开蓝牙等操作。使用EventBus代替广播接收指令。使用示例参考TestBle类。
2.需要引入EventBus库来代替广播接收消息。
3.若需求不同请自行修改BleManager类。
4.txs for eventbus.