package com.ava.zack.wechathoptrainer;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by Zack on 2018/1/7.
 */

public class Config {
    public static final String ACTION_KNOCK_SWITCH_TRAINER = "com.ava.zack.intent.action.KNOCK_EVENT";
    public static final String ACTION_EXECUTE_DETECTION = "com.ava.zack.intent.action.DETECT";
    public static final String ACTION_EXECUTE_START = "com.ava.zack.intent.action.START";
    public static final String ACTION_EXECUTE_STOP = "com.ava.zack.intent.action.STOP";

    public static final String PATH_SCREEN_SHOT_PNG = "/sdcard/screenshot.png";
    public static final String PATH_MATCHED_RESULT_PNG = "/sdcard/matched.png";
    public static final String PATH_CANNY_EDGES_PNG = "/sdcard/cannyEdges.png";

    public static Process RunAsRoot(String[] cmds) throws IOException {
        Process p = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(p.getOutputStream());
        for (String tmpCmd : cmds) {
            os.writeBytes(tmpCmd + "\n");
        }
        os.writeBytes("exit\n");
        os.flush();
        os.close();
        return p;
    }
}
