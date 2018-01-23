package com.ava.zack.wechathoptrainer;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Zack on 2018/1/7.
 */

public class Runner {
    public static final String TAG = "Runner";
    private Context mContext;

    private HandlerThread mHandlerThread;
    private Handler mHandler;

    private boolean isRunning;
    private ScreenCapture mScreenCap;
    private NativeScreenshot mScreenShot;

    public Runner(Context context) {
        mContext = context;
        isRunning = false;

        initScreenCap();
    }

    private void initScreenCap() {
        mScreenCap = new ScreenCapture();
        mScreenShot = new NativeScreenshot(mContext);

    }


    public synchronized void start() {
        Log.d(TAG, "runner start");
        if (!isRunning) {
            isRunning = true;
            mHandlerThread = new HandlerThread("TrainerRunner");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper());
            mHandler.post(mScreenCapRunnable);
        }
    }

    private Runnable mScreenCapRunnable = new Runnable() {
        @Override
        public void run() {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream("/sdcard/screencap.png");
                mScreenShot.takeScreenshot(fos);
                fos.getFD().sync();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            mHandler.removeCallbacks(this);
            mHandler.postDelayed(this, 3000);
        }
    };

    public synchronized void stop() {
        if (isRunning) {
            isRunning = false;
            mHandlerThread.quitSafely();
        }

    }
}
