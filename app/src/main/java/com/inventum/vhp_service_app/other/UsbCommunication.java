package com.inventum.vhp_service_app.other;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

/**
 * Created by Frank on 3-12-2017.
 */

public class UsbCommunication {

    private static final int BAUDRATE = 38400;
    private static final int DATABITS = UsbPort.DATABITS_8;
    private static final int STOPBITS = UsbPort.STOPBITS_1;
    private static final int PARITY = UsbPort.PARITY_NONE;

    private final UsbDevice device;
    private final UsbManager manager;

    private UsbDriver driver = null;
    private UsbDeviceConnection connection = null;
    private UsbPort port = null;

    public UsbCommunication (UsbManager manager, UsbDevice device) {
        this.manager = manager;
        this.device = device;
        setup();
    }

    public UsbDevice getDevice() {
        return device;
    }

    public UsbPort getPort() {
        return port;
    }

    public void stop() {
        port.close();
        driver = null;
        connection = null;
        port = null;
    }

    public synchronized boolean send(String str) {
        if (port == null) {
            return false;
        }
        str += "\r\n";
        byte[] writeBytes = str.getBytes();
        return port.write(writeBytes) == writeBytes.length;
    }

    public synchronized String receive() {
        byte[] readBytes = new byte[1024];
        if (port.read(readBytes) <= 0) {
            return "Could not read data";
        }
        return new String(readBytes);
    }

    public synchronized String sendReceive(String str) {
        if (send(str)) {
            return receive();
        }
        return "Could not send data";
    }

    private void setup() {
        driver = new UsbDriver(device);
        connection = manager.openDevice(device);

        if (connection != null) {
            port = driver.getPorts().get(0);
            port.open(connection);
            port.setParameters(BAUDRATE, DATABITS, STOPBITS, PARITY);
        }
    }
}
