package com.inventum.vhp_service_app.other;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

/**
 * Created by Frank on 2-12-2017.
 */

public class UsbDriver {

    private final UsbDevice device;
    private final UsbSerialPort port;

    public UsbDriver(UsbDevice device) {
        this.device = device;
        port = new UsbSerialPort(device);
    }

    public UsbDevice getDevice() {
        return device;
    }

    public List<UsbSerialPort> getPorts() {
        return Collections.singletonList(port);
    }

    private class UsbSerialPort implements UsbPort {

        private static final int DEFAULT_READ_BUFFER_SIZE = 16 * 1024;
        private static final int DEFAULT_WRITE_BUFFER_SIZE = 16 * 1024;
        private static final int TIMEOUT = 1000;

        private final UsbDevice mDevice;
        private UsbInterface mInterface = null;
        private UsbEndpoint mReadEndpoint = null;
        private UsbEndpoint mWriteEndpoint = null;

        // non-null when open()
        private UsbDeviceConnection mConnection = null;

        private final Object mReadBufferLock = new Object();
        private final Object mWriteBufferLock = new Object();

        /** Internal read buffer.  Guarded by {@link #mReadBufferLock}. */
        private byte[] mReadBuffer;

        /** Internal write buffer.  Guarded by {@link #mWriteBufferLock}. */
        private byte[] mWriteBuffer;

        private UsbSerialPort(UsbDevice device) {
            mDevice = device;
            mReadBuffer = new byte[DEFAULT_READ_BUFFER_SIZE];
            mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
        }

        public final UsbDevice getDevice() {
            return mDevice;
        }

        @Override
        public UsbDriver getDriver() {
            return UsbDriver.this;
        }

        @Override
        public boolean open(UsbDeviceConnection connection) {
            if (mConnection != null) {
                return false;
            }

            mConnection = connection;
            if (openInterface()) {
                return true;
            } else {
                mConnection = null;
                mInterface = null;
                mReadEndpoint = null;
                mWriteEndpoint = null;
                return false;
            }
        }

        @Override
        public boolean close() {
            if (mConnection == null) {
                return false;
            }
            mConnection.close();
            mConnection = null;
            mInterface = null;
            mReadEndpoint = null;
            mWriteEndpoint = null;
            return true;
        }

        @Override
        public int read(byte[] dest) {
            final int numBytesRead;
            synchronized (mReadBufferLock) {
                int readAmt = Math.min(dest.length, mReadBuffer.length);
                numBytesRead = mConnection.bulkTransfer(mReadEndpoint, mReadBuffer, readAmt, TIMEOUT);
                if (numBytesRead < 0) {
                    return -1;
                }
                System.arraycopy(mReadBuffer, 0, dest, 0, numBytesRead);
            }
            return numBytesRead;
        }

        @Override
        public int write(byte[] src) {
            int offset = 0;

            while (offset < src.length) {
                final int writeLength;
                final int amtWritten;

                synchronized (mWriteBufferLock) {
                    final byte[] writeBuffer;

                    writeLength = Math.min(src.length - offset, mWriteBuffer.length);
                    if (offset == 0) {
                        writeBuffer = src;
                    } else {
                        // bulkTransfer does not support offsets, make a copy.
                        System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
                        writeBuffer = mWriteBuffer;
                    }

                    amtWritten = mConnection.bulkTransfer(mWriteEndpoint, writeBuffer, writeLength, TIMEOUT);
                }
                if (amtWritten <= 0) {
                    return -1;
                }
                offset += amtWritten;
            }
            return offset;
        }

        private boolean openInterface() {
            for (int i = 0; i < mDevice.getInterfaceCount(); i++) {
                UsbInterface usbIf = mDevice.getInterface(i);
                UsbEndpoint write = null;
                UsbEndpoint read = null;

                for (int j = 0; j < usbIf.getEndpointCount(); j++) {
                    if (usbIf.getEndpoint(j).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if(usbIf.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_OUT){
                            write = usbIf.getEndpoint(j);
                        }else if(usbIf.getEndpoint(j).getDirection() == UsbConstants.USB_DIR_IN){
                            read = usbIf.getEndpoint(j);
                        }
                    }
                }

                if (write != null && read != null) {
                    mInterface = usbIf;
                    mWriteEndpoint = write;
                    mReadEndpoint = read;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setParameters(int baudRate, int dataBits, int stopBits, int parity) {
            byte stopBitsByte;
            switch (stopBits) {
                case STOPBITS_1: stopBitsByte = 0; break;
                case STOPBITS_1_5: stopBitsByte = 1; break;
                case STOPBITS_2: stopBitsByte = 2; break;
                default: throw new IllegalArgumentException("Bad value for stopBits: " + stopBits);
            }

            byte parityBitesByte;
            switch (parity) {
                case PARITY_NONE: parityBitesByte = 0; break;
                case PARITY_ODD: parityBitesByte = 1; break;
                case PARITY_EVEN: parityBitesByte = 2; break;
                case PARITY_MARK: parityBitesByte = 3; break;
                case PARITY_SPACE: parityBitesByte = 4; break;
                default: throw new IllegalArgumentException("Bad value for parity: " + parity);
            }

            byte[] msg = {
                    (byte) ( baudRate & 0xff),
                    (byte) ((baudRate >> 8 ) & 0xff),
                    (byte) ((baudRate >> 16) & 0xff),
                    (byte) ((baudRate >> 24) & 0xff),
                    stopBitsByte,
                    parityBitesByte,
                    (byte) dataBits};

            sendControlMessage(
                    UsbConstants.USB_TYPE_CLASS|USB_DEVICE_ADDRESS,
                    SET_LINE_CODING,
                    0,
                    msg);
        }

        private int sendControlMessage(int type, int request, int value, byte[] buf) {
            return mConnection.controlTransfer(
                    type,
                    request,
                    value,
                    0,
                    buf,
                    buf != null ? buf.length : 0,
                    5000);
        }

        @Override
        public String getManufacturer() {
            byte[] buffer = new byte[255];
            int indexManufacturer = 14;
            String stringManufacturer;

            byte[] rawDescriptors = mConnection.getRawDescriptors();

            int lengthManufacturer = sendControlMessage(
                    UsbConstants.USB_DIR_IN|UsbConstants.USB_TYPE_STANDARD,
                    USB_GET_DESCRIPTOR,
                    (0x03 << 8) | rawDescriptors[indexManufacturer],
                    buffer);
            try {
                stringManufacturer = new String(buffer, 2, lengthManufacturer-2, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                stringManufacturer = e.toString();
            }
            return stringManufacturer;
        }

        @Override
        public String getProduct() {
            byte[] buffer = new byte[255];
            int indexProduct = 15;
            String stringProduct;

            byte[] rawDescriptors = mConnection.getRawDescriptors();

            int lengthProduct = sendControlMessage(
                    UsbConstants.USB_DIR_IN|UsbConstants.USB_TYPE_STANDARD,
                    USB_GET_DESCRIPTOR,
                    (0x03 << 8) | rawDescriptors[indexProduct],
                    buffer);
            try {
                stringProduct = new String(buffer, 2, lengthProduct-2, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                stringProduct = e.toString();
            }
            return stringProduct;
        }
    }
}
