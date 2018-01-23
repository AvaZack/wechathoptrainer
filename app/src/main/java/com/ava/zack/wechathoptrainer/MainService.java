package com.ava.zack.wechathoptrainer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Zack on 2018/1/7.
 */

public class MainService extends Service {

    private static final String TAG = "MainService";

    private boolean isTrainerRunning;

    private Runner mRunner;

    private ScreenAnalyzer mAnalyzer;

    @Override
    public void onCreate() {
        isTrainerRunning = false;
        mRunner = new Runner(this);

        mAnalyzer = new ScreenAnalyzer(this);
        mAnalyzer.init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand, action=" + intent.getAction());
        if (Config.ACTION_KNOCK_SWITCH_TRAINER.equals(intent.getAction())) {
            Log.d(TAG, "KNOCK EVENT received.");
            if (!isTrainerRunning) {
                mRunner.start();
            } else {
                mRunner.stop();
            }
        } else if (Config.ACTION_EXECUTE_DETECTION.equals(intent.getAction())) {
//            if (mAnalyzer != null) {
//                ScreenCapture.screenshotAndSave();
//                mAnalyzer.detectAndMeasure();
//            }
        } else if (Config.ACTION_EXECUTE_START.equals(intent.getAction())) {
            mAnalyzer.start();
        } else if (Config.ACTION_EXECUTE_STOP.equals(intent.getAction())) {
            mAnalyzer.stop();

        }

        return super.onStartCommand(intent, flags, startId);
    }

    public static void startService(Context context, Intent intent) {
        Intent serviceIntent = new Intent(intent);
        serviceIntent.setClass(context, MainService.class);
        context.startService(serviceIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
