/*
 * Copyright 2009 Cedric Priscal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.senjucao.serialport_api.serialport;

import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPort {

    private static final String TAG = "SerialPort";

    private static final String DEFAULT_SU_PATH = "/system/bin/su";

    private static String sSuPath = DEFAULT_SU_PATH;

    /**
     * Set the SuPath
     */

    public static void setSuPath(String suPath) {
        if (suPath == null) {
            return;
        }
        sSuPath = suPath;
    }

    public static String getSuPath() {
        return sSuPath;
    }

    /*
     * Do not remove or rename the field mFd: it is used by native method close();
     */
    private FileDescriptor mFd;
    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;

    public SerialPort(File device, int baudrate) throws SecurityException, IOException {
        this(device, baudrate, 0);
    }

    public SerialPort(File device, int baudrate, int flags) throws SecurityException, IOException {
        this(device, baudrate, 0, 8, 1, flags);
    }

    /**
     * 打开串口
     *
     * @param device   串口设备文件
     * @param baudrate 波特率
     * @param parity   奇偶校验，0 None（默认）； 1 Odd； 2 Even
     * @param dataBits 数据位，5 ~ 8  （默认8）
     * @param stopBit  停止位，1 或 2  （默认 1）
     * @param flags    标记 0（默认）
     *                 //	 * @throws SecurityException
     * @throws IOException
     */
    public SerialPort(File device, int baudrate, int parity, int dataBits, int stopBit, int flags)
            throws SecurityException, IOException {
        /* Check access permission */
        if (!device.canRead() || !device.canWrite()) {
            try {
                /* Missing read/write permission, trying to chmod the file */
                Process su;
                su = Runtime.getRuntime().exec("/system/bin/su");
                String cmd = "chmod 666 " + device.getAbsolutePath() + "\n"
                        + "exit\n";
                su.getOutputStream().write(cmd.getBytes());
                if ((su.waitFor() != 0) || !device.canRead()
                        || !device.canWrite()) {
                    throw new SecurityException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new SecurityException();
            }
        }

        mFd = nativeOpen(device.getAbsolutePath(), baudrate, parity, dataBits, stopBit, flags);
        if (mFd == null) {
            Log.e(TAG, "native open returns null");
            throw new IOException();
        }
        mFileInputStream = new FileInputStream(mFd);
        mFileOutputStream = new FileOutputStream(mFd);
    }

    public void close() {
        try {
            if (mFileOutputStream != null)
                mFileOutputStream.close();
            if (mFileInputStream != null)
                mFileInputStream.close();
            mFileInputStream = null;
            mFileOutputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        nativeClose();
    }

    public boolean isOpen() {
        return this.mFd != null && mFileInputStream != null && mFileOutputStream != null;
    }

    public int read(byte[] data) throws IOException {
        if (!this.isOpen()) return -2; //设备没打开
        return mFileInputStream.read(data, 0, data.length);
    }

    public void write(byte[] data) throws IOException {
        if (!isOpen()) return;
        mFileOutputStream.write(data);
    }

    // Getters and setters
    public InputStream getInputStream() {
        return mFileInputStream;
    }

    public OutputStream getOutputStream() {
        return mFileOutputStream;
    }

    // JNI
    public native FileDescriptor nativeOpen(String path, int baudrate, int parity, int dataBits, int stopBit, int flags);

    public native void nativeClose();

    static {
        System.loadLibrary("SerialPort");
    }
}
