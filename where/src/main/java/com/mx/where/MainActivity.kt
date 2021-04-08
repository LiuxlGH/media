package com.mx.where

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blankj.utilcode.util.PermissionUtils
import com.mx.where.databinding.ActivityMainBinding
import java.util.*
import kotlin.math.pow


class MainActivity : AppCompatActivity() {
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private lateinit var vb:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vb = ActivityMainBinding.inflate(layoutInflater)
        setContentView(vb.root)

        vb.btnStart.setOnClickListener {
            scanBluth()
        }

        PermissionUtils.permission("android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN","android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION").request()

        // 获取本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        // 判断手机是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show()
            finish()
        }
        // 判断是否打开蓝牙
        if (!mBluetoothAdapter.isEnabled) {
            // 弹出对话框提示用户是后打开
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, 1)
            // 不做提示，强行打开
            // mBluetoothAdapter.enable();
        } else {
            // 不做提示，强行打开
            mBluetoothAdapter.enable()
        }
        val filter = IntentFilter()
        // 用BroadcastReceiver来取得搜索结果
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        // 两种情况会触发ACTION_DISCOVERY_FINISHED：1.系统结束扫描（约12秒）；2.调用cancelDiscovery()方法主动结束扫描
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)
    }
    private fun scanBluth() {
        // 设置进度条
        setProgressBarIndeterminateVisibility(true)
        title = "正在搜索..."
        // 判断是否在搜索,如果在搜索，就取消搜索
        if (mBluetoothAdapter.isDiscovering) {
            mBluetoothAdapter.cancelDiscovery()
        }
        // 开始搜索
        mBluetoothAdapter.startDiscovery()
        vb.tvTxt.text =""
    }
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            // 收到的广播类型
            val action = intent.action
            // 发现设备的广播
            if (BluetoothDevice.ACTION_FOUND == action) {
                // 从intent中获取设备
                val device = intent
                    .getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val aa: String = vb.tvTxt.text.toString()
//                if (aa.contains(device.address)) {
//                    return
//                } else {
                    // 判断是否配对过
//                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        // 添加到列表
                        val rssi = intent.extras!!.getShort(
                            BluetoothDevice.EXTRA_RSSI
                        )
                        val iRssi: Int = kotlin.math.abs(rssi.toInt())
                        // 将蓝牙信号强度换算为距离
                        val power = (iRssi - 59) / 25.0
                        val mm: String = Formatter().format("%.2f", 10.0.pow(power)).toString()
                        vb.tvTxt.append(
                            "${device.name}:${device.address} ：${mm}m --$iRssi\n"
                        )
//                    } else {
//                    }
//                }
                // 搜索完成
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                == action
            ) {
                // 关闭进度条
                setProgressBarIndeterminateVisibility(true)
                title = "搜索完成！"
                mBLHandler.sendEmptyMessageDelayed(1, 1000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }

    // 用于循环扫描蓝牙的hangdler
    var mBLHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                1 -> scanBluth()
                else -> {
                }
            }
        }
    }
}