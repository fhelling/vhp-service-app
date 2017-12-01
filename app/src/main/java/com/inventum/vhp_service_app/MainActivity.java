package com.inventum.vhp_service_app;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    TextView textDeviceName;
    TextView textStatus;
    TextView textInterface;
    TextView textConnection;

    private static final int targetVendorID = 1155;
    private static final int targetProductID = 22336;
    UsbDevice device = null;
    UsbInterface usbInterface = null;
    UsbEndpoint endpointOut = null;
    UsbEndpoint endpointIn = null;
    UsbDeviceConnection connection = null;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textDeviceName = findViewById(R.id.textdevicename);
        textStatus = findViewById(R.id.textstatus);
        textInterface = findViewById(R.id.textinterface);
        textConnection = findViewById(R.id.textconnection);

        //register the broadcast receiver
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        registerReceiver(mUsbReceiver, new IntentFilter(ACTION_USB_PERMISSION));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        getDevice();
    }

    @Override
    protected void onDestroy() {
        device = null;
        usbInterface = null;
        endpointIn = null;
        endpointOut = null;
        connection = null;
        super.onDestroy();
    }

    private void showRawDescriptors(){
        final int STD_USB_REQUEST_GET_DESCRIPTOR = 0x06;
        final int LIBUSB_DT_STRING = 0x03;

        byte[] buffer = new byte[255];
        int indexManufacturer = 14;
        int indexProduct = 15;
        String stringManufacturer = "";
        String stringProduct = "";

        byte[] rawDescriptors = connection.getRawDescriptors();

        int lengthManufacturer = connection.controlTransfer(
                UsbConstants.USB_DIR_IN|UsbConstants.USB_TYPE_STANDARD,   //requestType
                STD_USB_REQUEST_GET_DESCRIPTOR,         //request ID for this transaction
                (LIBUSB_DT_STRING << 8) | rawDescriptors[indexManufacturer], //value
                0,   //index
                buffer,  //buffer
                0xFF,  //length
                0);   //timeout
        try {
            stringManufacturer = new String(buffer, 2, lengthManufacturer-2, "UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            textConnection.setText(e.toString());
        }

        int lengthProduct = connection.controlTransfer(
                UsbConstants.USB_DIR_IN|UsbConstants.USB_TYPE_STANDARD,
                STD_USB_REQUEST_GET_DESCRIPTOR,
                (LIBUSB_DT_STRING << 8) | rawDescriptors[indexProduct],
                0,
                buffer,
                0xFF,
                0);
        try {
            stringProduct = new String(buffer, 2, lengthProduct-2, "UTF-16LE");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        textConnection.setText("Manufacturer: " + stringManufacturer + "\n" +
                "Product: " + stringProduct);
    }

    private void setupConnection() {
        final int SET_LINE_CODING = 0x20;
        final int SET_CONTROL_LINE_STATE = 0x22;
        final int USB_RT_ACM = UsbConstants.USB_TYPE_CLASS | 0x01;
        final int BAUDRATE = 38400;
        final int STOPBITS = 0;
        final int PARITY = 0;
        final int DATABITS = 8;

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (manager.hasPermission(device)) {
            connection = manager.openDevice(device);
            if (connection != null) {
                connection.claimInterface(usbInterface, true);
                connection.controlTransfer(0x21, SET_CONTROL_LINE_STATE, 0, 0, null, 0, 0);
                byte[] msg = {
                        (byte) ( BAUDRATE & 0xff),
                        (byte) ((BAUDRATE >> 8 ) & 0xff),
                        (byte) ((BAUDRATE >> 16) & 0xff),
                        (byte) ((BAUDRATE >> 24) & 0xff),
                        (byte) STOPBITS,
                        (byte) PARITY,
                        (byte) DATABITS
                };
                connection.controlTransfer(0x21, SET_LINE_CODING, 0, 0, msg, msg.length, 5000);
                showRawDescriptors();
            }
        }
        else {
            manager.requestPermission(device, mPermissionIntent);

        }
    }

    private void getInterface() {
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbIf = device.getInterface(i);

            UsbEndpoint tOut = null;
            UsbEndpoint tIn = null;

            for (int j = 0; j < usbIf.getEndpointCount(); j++) {
                if (usbIf.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if(usbIf.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT){
                        tOut = usbIf.getEndpoint(j);
                    }else if(usbIf.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN){
                        tIn = usbIf.getEndpoint(j);
                    }
                }
            }

            if (tOut != null && tIn != null) {
                usbInterface = usbIf;
                endpointOut = tOut;
                endpointIn = tIn;
                break;
            }
        }

        if(usbInterface==null){
            textInterface.setText("No suitable interface found!");
        }else{
            textInterface.setText(
                            "UsbInterface found\n" +
                            "Endpoint OUT: " + endpointOut.toString() + "\n" +
                            "Endpoint IN: " + endpointIn.toString());
            setupConnection();
        }
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
            getInterface();
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
                textStatus.setText("Device detached");
                textDeviceName.setText("");
                textInterface.setText("");
                textConnection.setText("");
                if (device != null) {
                    if (device == MainActivity.this.device) {
                        MainActivity.this.device = null;
                    }
                }
            }
        }
    };
}
