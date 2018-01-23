package com.ava.zack.wechathoptrainer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Created by Zack on 2018/1/11.
 */

public class ScreenAnalyzer {
    public static final String TAG = "ScreenAnalyzer";
    public static final boolean DBG = true;

    Context mContext;
    Mat mBouncerTemplate;
    Mat mScreenShot;
    Mat mCannyEdges;

    private static final int CV_IMAGE_TYPE = CvType.CV_8UC1;
    private static final int THICKNESS_OF_MARK = 2;
    private static final Scalar SCALAR_OF_MARK = new Scalar(0, 0, 0);
    private static final Scalar SCALAR_OF_MARK_INVERSE = new Scalar(255, 255, 255);
    private static final boolean isConvertToGray = true;
    private static final boolean doGaussianBlur = false;

    //never change
    private static final double RADIAN_OF_DIRECTION = 0.637d;

    private static final double GOOD_TANGENT = 0.61d;

    private static final double TEMPLATE_Y_OFFSET = -2.5d;
    private static final double TEMPLATE_X_OFFSET = 0d;

    private static final double SYMMETRY_POINT_Y_OFFSET_L2R = 15.25d;
    private static final double SYMMETRY_POINT_X_OFFSET_L2R = 14.9d;

    private static final double SYMMETRY_POINT_Y_OFFSET_R2L = 15.25d;
    private static final double SYMMETRY_POINT_X_OFFSET_R2L = 14.9d;

    private static final double HOP_RATIO = 1.39d;


    public ScreenAnalyzer(Context context) {
        mContext = context;
    }

    public void detectAndMeasure() {
        if (mBouncerTemplate == null) {
            Log.d(TAG, "Bouncer template init failure.");
            return;
        }

        Bitmap bitmap = null;
        File file = new File(Config.PATH_SCREEN_SHOT_PNG);
        if (file.exists()) {
            Log.d(TAG, "analyse screencap");
            bitmap = BitmapFactory.decodeFile(Config.PATH_SCREEN_SHOT_PNG);
        } else {
            Log.d(TAG, "no screencap files.");
            return;
        }

        if (bitmap == null) {
            Log.d(TAG, "FAILED to create bitmap");
            return;
        }

        if (DBG)
            Log.d(TAG, "convert to Mat");
        mScreenShot = new Mat(bitmap.getWidth(), bitmap.getHeight(),
                CV_IMAGE_TYPE);

        Utils.bitmapToMat(bitmap, mScreenShot);
//        Mat grayMat = new Mat(bitmap.getWidth(), bitmap.getHeight(),
//                CvType.CV_8UC1);
        if (isConvertToGray) {
            int colorChannels = (mScreenShot.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                    : ((mScreenShot.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);
            //Log.d(TAG, "mat channels=" + mat.channels());
            Imgproc.cvtColor(mScreenShot, mScreenShot, colorChannels);
        }

        //no need to gaussian blur.
        if (doGaussianBlur) {
            Imgproc.GaussianBlur(mScreenShot, mScreenShot, new Size(5, 5), 0);
        }

        Point matchLoc = matchTemplateMethod();
//        detectDstBlock();
//
        double distance = calculateDistanceFromScreenCenter(matchLoc);
        doHop(distance);


        /* convert back to bitmap */
        Log.d(TAG, "convert back to bitmap");
        Utils.matToBitmap(mScreenShot, bitmap);
        try {
            Log.d(TAG, "compress to origin File");
            FileOutputStream fos = new FileOutputStream(Config.PATH_MATCHED_RESULT_PNG);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.getFD().sync();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Point matchTemplateMethod() {
        int result_cols = mScreenShot.cols() - mBouncerTemplate.cols();
        int result_rows = mScreenShot.rows() - mBouncerTemplate.rows();
        Log.d(TAG, "bg cols=" + mScreenShot.cols() + ", rows=" + mScreenShot.rows());
        Log.d(TAG, "template cols=" + mBouncerTemplate.cols() + ", rows=" + mBouncerTemplate.rows());

        Mat matchResult = new Mat();
        matchResult.create(result_rows, result_cols, CV_IMAGE_TYPE);

        int matchMethod = Imgproc.TM_CCOEFF_NORMED;

        //Do the matching and normalize
        Imgproc.matchTemplate(mScreenShot, mBouncerTemplate, matchResult, matchMethod);
        Core.normalize(matchResult, matchResult, 0, 1, Core.NORM_MINMAX, -1);

        Point matchLoc;
        //Process match loc result.
        Core.MinMaxLocResult matchLocResult = Core.minMaxLoc(matchResult);

        if (matchMethod == Imgproc.TM_SQDIFF || matchMethod == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = matchLocResult.minLoc;
        } else {
            matchLoc = matchLocResult.maxLoc;
        }
        Log.d(TAG, "matchLocation: x=" + matchLoc.x + ", y=" + matchLoc.y);

        return matchLoc;

    }

    private Point detectDstBlock(Point bouncerPos, Point symmetryPoint) {
        mCannyEdges = new Mat();
        mScreenShot.copyTo(mCannyEdges);
        Imgproc.Canny(mScreenShot, mCannyEdges, 5, 10);

        if (DBG) {
            Bitmap cannyEdgesBitmap = BitmapFactory.decodeFile(Config.PATH_CANNY_EDGES_PNG);
            Utils.matToBitmap(mCannyEdges, cannyEdgesBitmap);
            try {
                Log.d(TAG, "compress to origin File");
                FileOutputStream fos = new FileOutputStream(Config.PATH_CANNY_EDGES_PNG);
                cannyEdgesBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.getFD().sync();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //convert Mat to byte Array
//        byte[] edgesData = new byte[(int) mCannyEdges.total()];
//
//        mCannyEdges.get(0, 0, edgesData);
//
//        if (bouncerPos.x < symmetryPoint.x) {
//            //left to right
//            for (int j = (int) (mCannyEdges.rows() * 0.3); j < (int) (mCannyEdges.rows() * 0.5); j++) {
//                for (int i = mScreenShot.rows() - 1; i > mScreenShot.rows() / 2; i--) {
//                    if (edgesData[i] == 255) {
//                        return new Point(i, j);
//                    }
//                }
//            }
//        } else {
//            //right to left
//            for (int j = (int) (mCannyEdges.rows() * 0.3); j < (int) (mCannyEdges.rows() * 0.8); j++) {
//                for (int i = 0; i < mScreenShot.rows() / 2; i++) {
//                    if ((int) edgesData[i] == 255)
//                        return new Point(i, j);
//                }
//            }
//        }

        return null;
    }

    private double calculateDistanceFromScreenCenter(Point matchLoc) {

        double bouncerX = matchLoc.x + mBouncerTemplate.cols() / 2 + TEMPLATE_X_OFFSET;
        double bouncerY = matchLoc.y + mBouncerTemplate.rows() + TEMPLATE_Y_OFFSET;

        //mark bouncer position
        Point bouncerPos = new Point(bouncerX, bouncerY);
        Imgproc.circle(mScreenShot, bouncerPos, 1, SCALAR_OF_MARK, THICKNESS_OF_MARK);

        double symmetryPointX = mScreenShot.cols() / 2d;
        double symmetryPointY = mScreenShot.rows() / 2d;

        if (symmetryPointX > bouncerX) {
            symmetryPointX += SYMMETRY_POINT_X_OFFSET_L2R;
            symmetryPointY += SYMMETRY_POINT_Y_OFFSET_L2R;
        } else {
            symmetryPointX += SYMMETRY_POINT_X_OFFSET_R2L;
            symmetryPointY += SYMMETRY_POINT_Y_OFFSET_R2L;
        }

        //mark the symmetryPoint
        Point symmetryPoint = new Point(symmetryPointX, symmetryPointY);
        Imgproc.circle(mScreenShot, symmetryPoint, 1, SCALAR_OF_MARK, THICKNESS_OF_MARK);

        Point dstCenter = detectDstBlock(bouncerPos, symmetryPoint);


        //eliminating the error
        Point fixedStarting = fixStartingPoint(bouncerX, bouncerY, symmetryPointX, symmetryPointY);
        Imgproc.circle(mScreenShot, fixedStarting, 1, SCALAR_OF_MARK_INVERSE, THICKNESS_OF_MARK);


        //symmetryPointY is always less than bouncerY
        double dstBouncerX;
        double dstBouncerY = fixedStarting.y - (fixedStarting.y - symmetryPointY) * 2;
        //calculate nextCenter
        if (symmetryPointX > fixedStarting.x) {
            dstBouncerX = fixedStarting.x + (symmetryPointX - fixedStarting.x) * 2;
        } else {
            dstBouncerX = fixedStarting.x + (symmetryPointX - fixedStarting.x) * 2;
        }

        Point nextCenter = new Point(dstBouncerX, dstBouncerY);
        Imgproc.circle(mScreenShot, nextCenter, 1, SCALAR_OF_MARK, THICKNESS_OF_MARK);

        //calculate distance
        double distance = Math.sqrt(Math.pow(Math.abs(dstBouncerX - fixedStarting.x), 2) + Math.pow((fixedStarting.y - dstBouncerY), 2));

        Log.d(TAG, "distance between the bouncer and the calculate center=" + distance);

        return distance;
    }

    private Point fixStartingPoint(double bouncerX, double bouncerY, double symmetryPointX, double symmetryPointY) {
        //eliminating the error
        double deltaX = Math.abs(symmetryPointX - bouncerX);
        double deltaY = Math.abs(symmetryPointY - bouncerY);
        double tangent = deltaY / deltaX;
        if (DBG) {
            Log.d(TAG, "deltaX = " + deltaX + ", deltaY=" + deltaY);
            Log.d(TAG, "tangent=" + tangent + ", radius=" + Math.atan(tangent));
        }

        //x = ky + b
        double slopeDst;
        double slopeSrc;
        double interceptDst;
        double interceptSrc;
        if (symmetryPointX > bouncerX) {
            slopeSrc = 1 / GOOD_TANGENT;
            slopeDst = -slopeSrc;
        } else {
            slopeDst = 1 / GOOD_TANGENT;
            slopeSrc = -slopeDst;
        }

        interceptSrc = bouncerX - slopeSrc * bouncerY;
        interceptDst = symmetryPointX - slopeDst * symmetryPointY;

        double goodY = (interceptDst - interceptSrc) / (slopeSrc - slopeDst);
        double goodX = goodY * slopeSrc + interceptSrc;

        return new Point(goodX, goodY);
    }

    private void doHop(double distance) {
        try {
            int duration = (int) (distance * HOP_RATIO * 1080 / mScreenShot.cols());

            int randomX = new Random().nextInt(100) + 150;
            int randomY = new Random().nextInt(100) + 200;

            Process p = Config.RunAsRoot(new String[]{"input touchscreen swipe " +
                    randomX + " " + randomY + " " +
                    randomX + " " + randomY + " " +
                    duration});
            Log.d(TAG, "pressint duration=" + duration);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void init() {

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, mContext, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    private void initTemplate() {
        Log.d(TAG, "init template of the bouncer.");
        //set an option otherwise the scale of the bitmap is out of control.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;

        Bitmap bouncerBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.center, options);
        mBouncerTemplate = new Mat(bouncerBitmap.getWidth(), bouncerBitmap.getHeight(),
                CV_IMAGE_TYPE);
        Log.d(TAG, "template width=" + bouncerBitmap.getWidth() + ", height=" + bouncerBitmap.getHeight());
        Utils.bitmapToMat(bouncerBitmap, mBouncerTemplate);
        Log.d(TAG, "mBouncerTemplate init completed.");
        if (doGaussianBlur) {
            Imgproc.GaussianBlur(mBouncerTemplate, mBouncerTemplate, new Size(5, 5), 0);
        }

        if (isConvertToGray) {
            int colorChannels = (mBouncerTemplate.channels() == 3) ? Imgproc.COLOR_BGR2GRAY
                    : ((mBouncerTemplate.channels() == 4) ? Imgproc.COLOR_BGRA2GRAY : 1);
            Imgproc.cvtColor(mBouncerTemplate, mBouncerTemplate, colorChannels);
        }
    }


//    private Bitmap loadBitmapFromResources(int resId) {
//
//        BitmapFactory.Options opts = new BitmapFactory.Options();// 解析图片的选项参数
////            opts.inPreferredConfig = Bitmap.Config.RGB_565;
//
//        // 2.得到图片的宽高属性。
//        opts.inJustDecodeBounds = true;// 不真正的解析这个bitmap ，只是获取bitmap的宽高信息
//        BitmapFactory.decodeResource(mContext.getResources(), resId, opts);
//
//        int imageHeight = opts.outHeight;
//        int imageWidth = opts.outWidth;
//        // 3.计算缩放比例。
//        int dx = imageWidth / srcWidth;
//        int dy = imageHeight / srcHeigth;
//
//        int scale = 1;
//        if (dx >= dy && dx >= 1) {
//            scale = dy;
//        }
//        if (dy >= dx && dx >= 1) {
//            scale = dx;
//        }
//
//        opts.inJustDecodeBounds = false;// 真正的解析bitmap
//        opts.inSampleSize = scale; // 指定图片缩放比例
//
//        return BitmapFactory.decodeResource(mContext.getResources(), resId, opts);
//    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(mContext) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    initTemplate();

                    Log.d(TAG, "cv init successfully.!!!!!!!!!!!!!!!!!!");
                    Log.d(TAG, "cv init successfully.!!!!!!!!!!!!!!!!!!");
                    isRunning = true;
                    new Thread(mRunnable).start();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private static final Object sHopLock = new Object();

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            while (isRunning) {
                ScreenCapture.screenshotAndSave();
                detectAndMeasure();
                Log.d(TAG, "DETECT the TWO CENTER AND measure the distance.");

                try {
//                    long interval = (new Random().nextInt(3) + 1) * 1000;
//                    Log.d(TAG, "interval=" + interval);
                    Thread.sleep(1200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private boolean isRunning = false;

    public void start() {
        isRunning = true;
        new Thread(mRunnable).start();
    }

    public void stop() {
        isRunning = false;
    }
}
