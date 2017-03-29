package com.example.chargingtest;

import com.example.chargingtest.AFMFunctionCallEx;
import com.example.chargingtest.FunctionReturn;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class ShellExe {

    private static final String TAG = "EM/shellexe";
    public static final String ERROR = "ERROR";
    public static final int RESULT_SUCCESS = 0;
    public static final int RESULT_FAIL = -1;
    public static final int RESULT_EXCEPTION = -2;
    private static final String OPERATION_ERROR_PREFIX = "#$ERROR^&";
    private static StringBuilder sResultBuilder = new StringBuilder("");

    /**
     * Get shell command output
     *
     * @return Shell command output
     */
    public static String getOutput() {
        return sResultBuilder.toString();
    }

    /**
     * Execute shell command
     * @param command Command string need to execute
     * @return Result
     * @throws IOException Throws when occurs #IOException
     */
    public static int execCommand(String command) throws IOException {
        return execCommand(new String[] { "sh", "-c", command });
    }

    public static int execCommand(String[] command) throws IOException {
        return execCommandOnServer(command);
    }

    public static int execCommand(String command, boolean execOnLocal) throws IOException {
        return execCommand(new String[] {"sh", "-c", command}, execOnLocal);
    }

    public static int execCommand(String[] command, boolean execOnLocal) throws IOException {
        if (execOnLocal) {
            return execCommandLocal(command);
        } else {
            return execCommandOnServer(command);
        }
    }

    /**
     * Execute shell command
     * @param command Shell command array
     * @return Result
     * @throws IOException Throws when occurs #IOException
     */
    public static int execCommandLocal(String[] command) throws IOException {
        int result = RESULT_FAIL;
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(command);
        BufferedReader bufferedReader = null;
        sResultBuilder.delete(0, sResultBuilder.length());
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(proc
                    .getInputStream(), Charset.defaultCharset()));
            if (proc.waitFor() == 0) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    sResultBuilder.append(line);
                    while (true) {
                        line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        } else {
                            sResultBuilder.append('\n');
                            sResultBuilder.append(line);
                        }
                    }
                }
                result = RESULT_SUCCESS;
            } else {
                Log.i("@M_" + TAG, "exit value = " + proc.exitValue());
                sResultBuilder.append(ERROR);
                result = RESULT_FAIL;
            }
        } catch (InterruptedException e) {
            Log.i("@M_" + TAG, "exe shell command InterruptedException: "
                    + e.getMessage());
            sResultBuilder.append(ERROR);
            result = RESULT_EXCEPTION;
        } finally {
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    Log.w("@M_" + TAG, "close reader in finally block exception: " + e.getMessage());
                }
            }
        }
        return result;
    }

    public static int execCommandOnServer(String[] command) throws IOException {
        if (command == null || command.length == 0) {
            throw new IllegalArgumentException("Invalid shell command to execute");
        }
        StringBuilder cmdBuilder = new StringBuilder();
        int i = 0;
        // ignore specified sh, use the server default sh
        if ("sh".equals(command[0]) || command[0].endsWith("/sh")) {
            if (command.length < 3) {
                throw new IllegalArgumentException("invalid or unknown cmd to execute");
            }
            i = 2;
        }
        for (; i < command.length; i++) {
            cmdBuilder.append(command[i]);
            if (i < command.length - 1) {
                cmdBuilder.append(" ");
            }
        }
        String cmd = cmdBuilder.toString();
        cmd = cmd.trim();
        sResultBuilder.delete(0, sResultBuilder.length());
        boolean ret = true;
        AFMFunctionCallEx functionCall = new AFMFunctionCallEx();
        ret = functionCall.startCallFunctionStringReturn(AFMFunctionCallEx.FUNCTION_EM_SHELL_CMD_EXECUTION);
        if (ret) {
            functionCall.writeParamNo(1);
            functionCall.writeParamString(cmd);
            FunctionReturn funcRet = null;
            do {
                funcRet = functionCall.getNextResult();
                if (funcRet.mReturnString != null && funcRet.mReturnString.length() > 0) {
                    sResultBuilder.append(funcRet.mReturnString);
                }
            } while (funcRet.mReturnCode == AFMFunctionCallEx.RESULT_CONTINUE);
            // trim the tail newline character
            while (true) {
                int len = sResultBuilder.length();
                if (len <= 0) {
                    break;
                }
                char c = sResultBuilder.charAt(len - 1);
                if (c == '\n') {
                    sResultBuilder.delete(len - 1, len);
                } else {
                    break;
                }
            }
            String output = sResultBuilder.toString();
            if (output != null && output.startsWith(OPERATION_ERROR_PREFIX)) {
                Log.d("@M_" + TAG, "error operation:" + output);
                sResultBuilder.delete(0, OPERATION_ERROR_PREFIX.length() + 1);
                return RESULT_FAIL;
            }
            return RESULT_SUCCESS;
        }
        Log.d("@M_" + TAG, "Function call start fail");
        sResultBuilder.append(ERROR);
        return RESULT_FAIL;
    }
}
