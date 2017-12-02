package com.inventum.vhp_service_app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int targetVendorID = 1155;
    private static final int targetProductID = 22336;

    TextView textDeviceName;
    TextView textStatus;
    TextView textInfo;
    TextView textResponse;

    EditText editCommand;

    Button buttonSend;

    UsbDevice device = null;
    UsbDriver driver = null;
    UsbPort port = null;
    UsbDeviceConnection connection = null;


    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textDeviceName = findViewById(R.id.textdevicename);
        textStatus = findViewById(R.id.textstatus);
        textInfo = findViewById(R.id.textinfo);
        textResponse = findViewById(R.id.textresponse);

        editCommand = findViewById(R.id.editcommand);

        buttonSend = findViewById(R.id.buttonsend);
        buttonSend.setOnClickListener(buttonSendOnClickListener);
        buttonSend.setEnabled(false);

        //register the broadcast receiver
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        getDevice();
    }

    View.OnClickListener buttonSendOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (device == null) {
                textResponse.setText("No device connected");
                return;
            }
            String cmd = editCommand.getText().toString();
            if (cmd.equals("")) return;
            port.write(cmd + "\r\n");
            textResponse.setText(port.read());
        }
    };

    @Override
    protected void onDestroy() {
        clearAll();
        super.onDestroy();
    }

    private void clearAll() {
        port.close();
        device = null;
        driver = null;
        port = null;
        connection = null;
        textStatus.setText("Device detached");
        textDeviceName.setText("");
        textInfo.setText("");
        buttonSend.setEnabled(false);
    }

    private void getDevice() {
        if (device == null) {
            UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

            for (UsbDevice device : deviceList.values()) {
                if (device.getVendorId() == targetVendorID && device.getProductId() == targetProductID) {
                    this.device = device;
                }
            }
        }

        if (device == null) {
            textStatus.setText("Device not found");
            textDeviceName.setText("");
        }
        else {
            textStatus.setText("Device found");
            String s = "DeviceID: " + device.getDeviceId() + "\n" +
                    "DeviceName: " + device.getDeviceName() + "\n" +
                    "VendorID: " + device.getVendorId() + "\n" +
                    "ProductID: " + device.getProductId();
            textDeviceName.setText(s);
            setupConnection();
        }
    }

    private void setupConnection() {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager.hasPermission(device)) {

            driver = new UsbDriver(device);
            connection = manager.openDevice(device);

            if (connection != null) {
                port = driver.getPorts().get(0);
                port.open(connection);
                port.setParameters(38400, UsbPort.DATABITS_8, UsbPort.STOPBITS_1, UsbPort.PARITY_NONE);
                textInfo.setText(port.getManufacturer() + "\n" + port.getProduct());
                buttonSend.setEnabled(true);
            }
        }
        else {
            manager.requestPermission(device, mPermissionIntent);
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            getDevice();
                        }
                    }
                    else {
                        textStatus.setText("Permission denied for device " + device);
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                getDevice();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (device != null) {
                    if (device == MainActivity.this.device) {
                        clearAll();
                    }
                }
            }
        }
    };
}
