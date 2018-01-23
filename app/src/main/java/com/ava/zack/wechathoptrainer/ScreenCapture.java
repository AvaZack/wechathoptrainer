package com.ava.zack.wechathoptrainer;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Zack on 2018/1/7.
 */

public class ScreenCapture {

    private static final String TAG = "ScreenCapture";

    public static void main(String[] args) {

        screenshotAndSave();
    }

    public static void screenshotAndSave() {
        Log.d(TAG, "screenshotAndSave");

        try {
            Process p = Config.RunAsRoot(new String[]{"screencap -p > " + Config.PATH_SCREEN_SHOT_PNG});
            p.waitFor();
            File scrnFile = new File(Config.PATH_SCREEN_SHOT_PNG);
            Log.d(TAG, "screenshot size=" + (scrnFile.exists() ? scrnFile.length() : 0));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void screenshotNative(Context context) {
        Log.d(TAG, "screenshotNative");
        try {
            File file = new File(Config.PATH_SCREEN_SHOT_PNG);
            if (!file.exists()) {
                Log.d(TAG, "create new file");
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(Config.PATH_SCREEN_SHOT_PNG);
            new NativeScreenshot(context).takeScreenshot(fos);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
