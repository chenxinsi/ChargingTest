package com.example.chargingtest;

import android.util.Log;

import java.io.EOFException;
import java.io.IOException;

public class AFMFunctionCallEx {

    private static final String TAG = "EM/functioncall";

    public static final int FUNCTION_EM_BASEBAND = 10001;
    public static final int FUNCTION_EM_FB0_IOCTL = 30001;
    public static final int FUNCTION_EM_CPU_STRESS_TEST_APMCU = 40001;
    public static final int FUNCTION_EM_CPU_STRESS_TEST_SWCODEC = 40002;
    public static final int FUNCTION_EM_CPU_STRESS_TEST_BACKUP = 40003;
    public static final int FUNCTION_EM_CPU_STRESS_TEST_THERMAL = 40004;
    public static final int FUNCTION_EM_CPU_STRESS_TEST_INIT = 40005;
    public static final int FUNCTION_EM_SENSOR_DO_CALIBRATION = 50001;
    public static final int FUNCTION_EM_SENSOR_CLEAR_CALIBRATION = 50002;
    public static final int FUNCTION_EM_SENSOR_SET_THRESHOLD = 50003;
    public static final int FUNCTION_EM_SENSOR_DO_GSENSOR_CALIBRATION = 50004;
    public static final int FUNCTION_EM_SENSOR_GET_GSENSOR_CALIBRATION = 50005;
    public static final int FUNCTION_EM_SENSOR_CLEAR_GSENSOR_CALIBRATION = 50006;
    public static final int FUNCTION_EM_SENSOR_DO_GYROSCOPE_CALIBRATION = 50007;
    public static final int FUNCTION_EM_SENSOR_GET_GYROSCOPE_CALIBRATION = 50008;
    public static final int FUNCTION_EM_SENSOR_CLEAR_GYROSCOPE_CALIBRATION = 50009;
    public static final int FUNCTION_EM_MSDC_SET_CURRENT = 70001;
    public static final int FUNCTION_EM_MSDC_GET_CURRENT = 70002;
    public static final int FUNCTION_EM_MSDC_SET30_MODE = 70003;
    public static final int FUNCTION_EM_SHELL_CMD_EXECUTION = 80001;

    public static final int RESULT_FIN = 0;
    public static final int RESULT_CONTINUE = 1;
    public static final int RESULT_IO_ERR = -1;

    private static final String ERROR = "ERROR ";

    private Client mSocket = null;

    /**
     * Prepare to call function by EM native server
     *
     * @param functionId
     *            Functional ID
     * @return True if call function success
     */
    public boolean startCallFunctionStringReturn(int functionId) {
        boolean result = false;
        mSocket = new Client();
        mSocket.startClient();
        try {
            mSocket.writeFunctionNo(String.valueOf(functionId));
            result = true;
        } catch (IOException e) {
            Log.w("@M_" + TAG, "StartCallFunctionStringReturnEXP " + e.getMessage());
            result = false;
        }
        return result;
    }

    /**
     * Send parameter number
     *
     * @param number
     *            Parameters number
     * @return True if send success to server
     */
    public boolean writeParamNo(int number) {
        boolean ret = false;
        try {
            mSocket.writeParamNo(number);
            ret = true;
        } catch (IOException e) {
            ret = false;
        }
        return ret;
    }

    /**
     * Send "int" parameter to server
     *
     * @param param
     *            Parameter
     * @return True if send success to server
     */
    public boolean writeParamInt(int param) {
        boolean ret = false;
        try {
            mSocket.writeParamInt(param);
            ret = true;

        } catch (IOException e) {
            ret = false;
        }
        return ret;
    }

    /**
     * Send string to server as parameter
     *
     * @param string
     *            Parameter
     * @return True if send success to server
     */
    public boolean writeParamString(String string) {
        boolean ret = false;
        try {
            mSocket.writeParamString(string);
            ret = true;
        } catch (IOException e) {
            ret = false;
        }
        return ret;
    }

    /**
     * Get response from EM server
     *
     * @return #FunctionReturn returnCode=0 if socket close normally
     *         returnCode=1 has more data to be read. returnCode = -1 close
     *         abnormally.
     */
    public FunctionReturn getNextResult() {
        FunctionReturn ret = new FunctionReturn();
        try {
            ret.mReturnString = mSocket.read();
            if (ret.mReturnString.isEmpty()) {
                ret.mReturnCode = RESULT_FIN;
                endCallFunction();
            } else {
                ret.mReturnCode = RESULT_CONTINUE;
            }
        } catch (EOFException e) {
            endCallFunction();
            ret.mReturnCode = RESULT_FIN;
            ret.mReturnString = "";
        } catch (IOException e) {
            endCallFunction();
            ret.mReturnCode = RESULT_IO_ERR;
            ret.mReturnString = ERROR + e.getMessage();
        }
        return ret;
    }

    /**
     * Stop connection with EM server
     */
    private void endCallFunction() {
        mSocket.stopClient();
    }

}
