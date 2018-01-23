package com.ava.zack.wechathoptrainer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Zack on 2017/2/7.
 */

public class NativeScreenshot {
    public static final String TAG = "NativeScreenshot";

    private WindowManager mWindowManager;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private Matrix mDisplayMatrix;

    private Bitmap mScreenBitmap;

    private static final Object sLock = new Object();

    public NativeScreenshot(Context context) {
        mDisplayMatrix = new Matrix();
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);
    }

    private float getDegreesForRotation(int value) {
        switch (value) {
            case Surface.ROTATION_90:
                return 360f - 90f;
            case Surface.ROTATION_180:
                return 360f - 180f;
            case Surface.ROTATION_270:
                return 360f - 270f;
        }
        return 0f;
    }

    public void takeScreenshot(OutputStream out) throws IOException {
        if (out == null) {
            Log.e(TAG, "take screen shot to a NULL OutputStream");
            return;
        }
        // We need to orient the screenshot correctly (and the Surface api seems to take screenshots
        // only in the natural orientation of the device :!)
        //
        mDisplay.getRealMetrics(mDisplayMetrics);
        float[] dims = {mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels};
        float degrees = getDegreesForRotation(mDisplay.getRotation());
        boolean requiresRotation = (degrees > 0);
        if (requiresRotation) {
            // Get the dimensions of the device in its native orientation
            mDisplayMatrix.reset();
            mDisplayMatrix.preRotate(-degrees);
            mDisplayMatrix.mapPoints(dims);
            dims[0] = Math.abs(dims[0]);
            dims[1] = Math.abs(dims[1]);
        }

        Log.d("takeScreenshot", "takeScreenshot, dims, w-h: " + dims[0] + "-" + dims[1] + "; " +
                "dm w-h: " + mDisplayMetrics.widthPixels + mDisplayMetrics.heightPixels +
                "Thread=" + Thread.currentThread().getName());
        // Take the screenshot
        synchronized (sLock) {
            try {
                String surfaceClassName;
                if (Build.VERSION.SDK_INT <= 17) {
                    surfaceClassName = "android.view.Surface";
                } else {
                    surfaceClassName = "android.view.SurfaceControl";
                }
                Log.d(TAG, "ClassName= " + surfaceClassName);
                Class<?> surfaceControlClass = Class.forName(surfaceClassName);
                Method screenshotMethod = surfaceControlClass.getMethod("screenshot", int.class, int.class);
                mScreenBitmap = (Bitmap) screenshotMethod.invoke(null, (int) dims[0], (int) dims[1]);

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }


            //bmp = (Bitmap) saddMethod1.invoke(null, new Object[]{(int)540,(int)960});
            //mScreenBitmap = SurfaceControl.screenshot((int) dims[0], (int) dims[1]);


            if (mScreenBitmap == null) {
                throw new IOException("Bitmap is null after taking native screenshot!");
                //            notifyScreenshotError(mContext, mNotificationManager);
                //            finisher.run();
                //            return;
            }

            if (requiresRotation) {
                // Rotate the screenshot to the current orientation
                Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                        mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(ss);
                c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
                c.rotate(degrees);
                c.translate(-dims[0] / 2, -dims[1] / 2);
                c.drawBitmap(mScreenBitmap, 0, 0, null);
                c.setBitmap(null);
                // Recycle the previous bitmap
                mScreenBitmap.recycle();
                mScreenBitmap = ss;
            }

            // Optimizations
            mScreenBitmap.setHasAlpha(false);
            mScreenBitmap.prepareToDraw();

            // Start the post-screenshot animation
            //        startAnimation(finisher, mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels,
            //                statusBarVisible, navBarVisible);
            mScreenBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            Log.d("takeScreenshot", "Thread=" + Thread.currentThread().getName());
            mScreenBitmap.recycle();
            // Clear any references to the bitmap
            mScreenBitmap = null;
        }
//        if(mScreenBitmap != null) {
//            mScreenBitmap.recycle();
//            // Clear any references to the bitmap
//            mScreenBitmap = null;
//        }
    }
}