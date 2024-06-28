package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Button

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.example.async.AsyncBluetoothEscPosPrint
import com.example.async.AsyncEscPosPrint
import com.example.async.AsyncEscPosPrinter

import com.example.bluetoothprinter.R

class MainActivity : AppCompatActivity() {

    private var selectedDevice: BluetoothConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.button_bluetooth_browse).setOnClickListener { browseBluetoothDevice() }
        findViewById<Button>(R.id.button_bluetooth).setOnClickListener { printBluetooth() }
    }

    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    interface OnBluetoothPermissionsGranted {
        fun onPermissionsGranted()
    }

    private var onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted? = null

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                PERMISSION_BLUETOOTH,
                PERMISSION_BLUETOOTH_ADMIN,
                PERMISSION_BLUETOOTH_CONNECT,
                PERMISSION_BLUETOOTH_SCAN -> checkBluetoothPermissions(onBluetoothPermissionsGranted)
            }
        }
    }

    private fun checkBluetoothPermissions(onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted?) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), PERMISSION_BLUETOOTH)
            }
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN), PERMISSION_BLUETOOTH_ADMIN)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_BLUETOOTH_CONNECT)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED -> {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), PERMISSION_BLUETOOTH_SCAN)
            }
            else -> onBluetoothPermissionsGranted?.onPermissionsGranted()
        }
    }

    @SuppressLint("MissingPermission")
    private fun browseBluetoothDevice() {
        checkBluetoothPermissions(object : OnBluetoothPermissionsGranted {
            override fun onPermissionsGranted() {
                val bluetoothDevicesList = BluetoothPrintersConnections().list
                if (bluetoothDevicesList != null) {
                    val items = arrayOfNulls<String>(bluetoothDevicesList.size + 1)
                    items[0] = "Default printer"
                    bluetoothDevicesList.forEachIndexed { i, device -> items[i + 1] = device.device.name }

                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Bluetooth printer selection")
                        .setItems(items) { _, which ->
                            selectedDevice = if (which == 0) null else bluetoothDevicesList[which - 1]
                            findViewById<Button>(R.id.button_bluetooth_browse).text = items[which]
                        }
                        .setCancelable(false)
                        .show()
                }
            }
        })
    }

    private fun printBluetooth() {
        checkBluetoothPermissions(object : OnBluetoothPermissionsGranted {
            override fun onPermissionsGranted() {
                AsyncBluetoothEscPosPrint(
                    this@MainActivity,
                    object : AsyncEscPosPrint.OnPrintFinished() {
                        override fun onError(asyncEscPosPrinter: AsyncEscPosPrinter, codeException: Int) {
                            Log.e("Async.OnPrintFinished", "An error occurred!")
                        }

                        override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter) {
                            Log.i("Async.OnPrintFinished", "Print is finished!")
                        }
                    }
                ).execute(getAsyncEscPosPrinter(selectedDevice))
            }
        })
    }

    @SuppressLint("SimpleDateFormat")
    private fun getAsyncEscPosPrinter(printerConnection: DeviceConnection?): AsyncEscPosPrinter {
        val format = SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss")
        val printer = AsyncEscPosPrinter(printerConnection, 203, 48f, 32)
        return printer.addTextToPrint(
            "[L]\n" +
                    "[C]<u><font size='big'>MEGAV X PRINTER</font></u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]<b>Tra sua chan trau</b>[R]220.000\n" +
                    "[L]  + So luong :</b>[R]1\n" +
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]\n" +
                    "[L]<b>Thanh tien :</b>[R]440.000\n" +
                    "[L]  + Giam gia :<b>[R]0\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]<b>Note :</b>\n" +
                    "[L]<font siza='normal'>This bill is printerd from the</font>\n" +
                    "[L]<b>MegaV - Android app</b>\n" +
                    "[L]<b>With a Bluetooth printer</b>\n" +
                    "\n" +
                    "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.applicationContext.resources.getDrawableForDensity(R.drawable.print2, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n" +
                    "\n" +
                    "[C]<b>VU DINH SAM</b>\n" +
                    "[C]<b>TECHCOMBANK</b>\n" +
                    "\n" +
                    "[L]<font siza='normal'>We can also print barcodes :</font>\n" +
                    "[C]<barcode>8312547845511</barcode>\n"
        )
    }

    companion object {
        const val PERMISSION_BLUETOOTH = 1
        const val PERMISSION_BLUETOOTH_ADMIN = 2
        const val PERMISSION_BLUETOOTH_CONNECT = 3
        const val PERMISSION_BLUETOOTH_SCAN = 4
    }
}