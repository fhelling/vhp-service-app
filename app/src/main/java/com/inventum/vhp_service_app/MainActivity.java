package com.inventum.vhp_service_app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
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

    UsbCommunication comm = null;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textStatus = findViewById(R.id.textstatus);
        textDeviceName = findViewById(R.id.textdevicename);
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

        textStatus.setText("No device connected");
        getDevice();
    }

    OnClickListener buttonSendOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (comm == null) {
                textResponse.setText("No device connected");
                return;
            }
            final String cmd = editCommand.getText().toString();
            if (cmd.equals("")) return;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String resp = comm.sendReceive(cmd);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textResponse.setText(resp);
                        }
                    });
                }
            });
        }
    };

    @Override
    protected void onDestroy() {
        clearAll();
        super.onDestroy();
    }

    private void clearAll() {
        if (comm != null) {
            comm.stop();
            comm = null;
        }
        textDeviceName.setText("");
        textInfo.setText("");
        buttonSend.setEnabled(false);
    }

    private void getDevice() {
        if (comm == null) {
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

            for (UsbDevice device : deviceList.values()) {
                if (checkDevice(device) && checkPermission(device)) {
                    comm = new UsbCommunication(this, device);
                    setDeviceInfo();
                    break;
                }
            }
        }
    }

    private void setDeviceInfo() {
        if (comm == null) return;

        textStatus.setText("Device found");
        String s = "DeviceID: " + comm.getDevice().getDeviceId() + "\n" +
                "DeviceName: " + comm.getDevice().getDeviceName() + "\n" +
                "VendorID: " + comm.getDevice().getVendorId() + "\n" +
                "ProductID: " + comm.getDevice().getProductId();
        textDeviceName.setText(s);
        textInfo.setText(comm.getPort().getManufacturer() + "\n" + comm.getPort().getProduct());
        buttonSend.setEnabled(true);
    }

    private boolean checkDevice(UsbDevice device) {
        return comm == null &&
                device.getVendorId() == targetVendorID &&
                device.getProductId() == targetProductID;
    }

    private boolean checkPermission(UsbDevice device) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager.hasPermission(device)) {
            return true;
        }
        manager.requestPermission(device, mPermissionIntent);
        return false;
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (comm == null) {
                            comm = new UsbCommunication(MainActivity.this, device);
                            setDeviceInfo();
                        }
                    }
                    else {
                        textStatus.setText("Permission denied for device " + device);
                        clearAll();
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
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                // Correct device and permission granted?
                if (checkDevice(device) && checkPermission(device)) {
                    comm = new UsbCommunication(MainActivity.this, device);
                    setDeviceInfo();
                }

            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (comm != null && device.equals(comm.getDevice())) {
                    textStatus.setText("Device detached");
                    clearAll();
                }
            }
        }
    };
}
