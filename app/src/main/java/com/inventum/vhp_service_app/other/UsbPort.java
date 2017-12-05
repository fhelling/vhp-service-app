package com.inventum.vhp_service_app.other;

import android.hardware.usb.UsbDeviceConnection;

/**
 * Created by Frank on 2-12-2017.
 */

public interface UsbPort {

    public static final int USB_DEVICE_ADDRESS = 0x01;
    public static final int USB_GET_DESCRIPTOR = 0x06;
    public static final int SET_LINE_CODING = 0x20;
    public static final int GET_LINE_CODING = 0x21;
    public static final int SET_CONTROL_LINE_STATE = 0x22;
    public static final int SEND_BREAK = 0x23;

    /** 5 data bits. */
    public static final int DATABITS_5 = 5;

    /** 6 data bits. */
    public static final int DATABITS_6 = 6;

    /** 7 data bits. */
    public static final int DATABITS_7 = 7;

    /** 8 data bits. */
    public static final int DATABITS_8 = 8;

    /** No parity. */
    public static final int PARITY_NONE = 0;

    /** Odd parity. */
    public static final int PARITY_ODD = 1;

    /** Even parity. */
    public static final int PARITY_EVEN = 2;

    /** Mark parity. */
    public static final int PARITY_MARK = 3;

    /** Space parity. */
    public static final int PARITY_SPACE = 4;

    /** 1 stop bit. */
    public static final int STOPBITS_1 = 1;

    /** 1.5 stop bits. */
    public static final int STOPBITS_1_5 = 3;

    /** 2 stop bits. */
    public static final int STOPBITS_2 = 2;

    public UsbDriver getDriver();

    public boolean open(UsbDeviceConnection connection);

    public boolean close();

    public void setParameters(int baudRate, int dataBits, int stopBits, int parity);

    public int read(byte[] dest);

    public int write(byte[] src);

    public String getManufacturer();

    public String getProduct();
}
