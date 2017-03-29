package com.example.chargingtest;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class Client {

    private static final String TAG = "EM/client";
    public static final int PARAM_TYPE_STRING = 1;
    public static final int PARAM_TYPE_INT = 2;
    private static final int PARAM_INT_LENGTH = 4;
    private static final String ERROR_NO_INIT = "NOT INIT";
    private static final String ERROR_PARAM_NUM = "param < 0";
    private static final int STATUS_SUCCESS = 0;
    private static final int STATUS_ERROR = -1;
    private static final int MAX_BUFFER_SIZE = 1024;
    private static final String EMPTY = "";
    private static final String EM_SERVER_NAME = "EngineerModeServer";

    private LocalSocket mSocket = null;
    private DataInputStream mInputStream = null;
    private DataOutputStream mOutputStream = null;
    private int mStatus = STATUS_SUCCESS;

    /**
     * Connect to EM native server
     */
    public void startClient() {
        try {
            // mSocket = new Socket("127.0.0.1", 37121);
            // mSocket(10000);
            mSocket = new LocalSocket();
            // LocalSocketAddress.Namespace.FILESYSTEM
            mSocket.connect(new LocalSocketAddress(EM_SERVER_NAME));
            mInputStream = new DataInputStream(mSocket.getInputStream());
            mOutputStream = new DataOutputStream(mSocket.getOutputStream());
            mStatus = STATUS_SUCCESS;
        } catch (IOException e) {
            Log.w("@M_" + TAG, "startclient IOException " + e.getMessage());
            mStatus = STATUS_ERROR;
        }
    }

    /**
     * Read response string from EM server
     *
     * @return Response string
     * @throws IOException
     *             Input stream exception
     */
    synchronized String read() throws IOException {
        if (STATUS_ERROR == mStatus || null == mInputStream) {
            throw new IOException(ERROR_NO_INIT);
        }
        String result = null;
        int len = mInputStream.readInt();
        if (len > MAX_BUFFER_SIZE) {
            len = MAX_BUFFER_SIZE;
        }
        byte bb[] = new byte[len];
        int x = mInputStream.read(bb, 0, len);
        if (-1 == x) {
            result = EMPTY;
        } else {
            result = new String(bb, Charset.defaultCharset());
        }
        return result;
    }

    /**
     * Send function number to EM server
     *
     * @param functionNo
     *            Function ID
     * @throws IOException
     *             Output stream exception
     */
    synchronized void writeFunctionNo(String functionNo) throws IOException {
        if (STATUS_ERROR == mStatus || null == mOutputStream) {
            throw new IOException(ERROR_NO_INIT);
        }
        if (functionNo == null || functionNo.length() == 0) {
            return;
        }
        mOutputStream.writeInt(functionNo.length());
        mOutputStream.write(functionNo.getBytes(Charset.defaultCharset()), 0, functionNo.length());
        return;
    }

    /**
     * Send parameter number to EM server
     *
     * @param paramNum
     *            Parameter total count
     * @throws IOException
     *             Output stream exception
     */
    synchronized void writeParamNo(int paramNum) throws IOException {
        if (STATUS_ERROR == mStatus || null == mOutputStream) {
            throw new IOException(ERROR_NO_INIT);
        }
        if (paramNum < 0) {
            throw new IOException(ERROR_PARAM_NUM);
        }
        mOutputStream.writeInt(paramNum);
    }

    /**
     * Send parameter to EM server
     *
     * @param param
     *            Parameter
     * @throws IOException
     *             Output stream exception
     */
    synchronized void writeParamInt(int param) throws IOException {
        if (STATUS_ERROR == mStatus || null == mOutputStream) {
            throw new IOException(ERROR_NO_INIT);
        }
        mOutputStream.writeInt(PARAM_TYPE_INT);
        mOutputStream.writeInt(PARAM_INT_LENGTH);
        mOutputStream.writeInt(param);
    }

    /**
     * Send string to EM server as parameter
     *
     * @param param
     *            Parameter
     * @throws IOException
     *             Output stream exception
     */
    synchronized void writeParamString(String param) throws IOException {
        if (STATUS_ERROR == mStatus || null == mOutputStream) {
            throw new IOException(ERROR_NO_INIT);
        }
        mOutputStream.writeInt(PARAM_TYPE_STRING);
        mOutputStream.writeInt(param.length());
        mOutputStream.write(param.getBytes(Charset.defaultCharset()), 0, param.length());
    }

    /**
     * Stop connection with EM server
     */
    public void stopClient() {
        if (null == mInputStream || null == mOutputStream || null == mSocket) {
            return;
        }
        try {
            mOutputStream.close();
            mInputStream.close();
            mSocket.close();
        } catch (IOException e) {
            Log.w("@M_" + TAG, "stop client IOException: " + e.getMessage());
        }
    }

}
