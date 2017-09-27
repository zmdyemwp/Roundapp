package com.siui.round;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Binder;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Surface;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Point;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.util.Log;
import java.io.File;

public class RoundService extends Service{
    public static final String TAG = RoundApplication.TAG;
    private static final String MASK_FILE = "system/etc/black_mask.png";
    WindowManager mWM = null;
    WindowManager.LayoutParams mParams = null;
    MyHandler mHandler = new MyHandler();
    View mView = null;
    MaskView mMaskView = null;
    MyOrientationListener mListener = new MyOrientationListener();

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WmService onStartCommand");
        showWMOverlay();
        return START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new RoundServiceBinder();
    
    public class RoundServiceBinder extends Binder {
        RoundService getService() {
            return RoundService.this;
        }
    }

    protected void showWMOverlay() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MyHandler.SHOW_MSG);
        }
    }

    protected void handleShowOverlay() {
        mWM = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        mParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY);
        mParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                       WindowManager.LayoutParams.FLAG_FULLSCREEN |
                       WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                       WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE; 
        mParams.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        mParams.setTitle("RoundCorner");
        LayoutInflater inflater = LayoutInflater.from(this);
        mView = inflater.inflate(R.layout.mask_view, null);
        if (mView != null && mView.getBackground() != null) {
            mParams.format = mView.getBackground().getOpacity();
        }
        mMaskView = (MaskView)mView.findViewById(R.id.iv);
        if (mMaskView != null) {
            Bitmap maskBitmap = getMaskBitmap();
            mMaskView.setMask(maskBitmap);
            mMaskView.registerOrientationListener(mListener);
            updateParams();
        }
        mWM.addView(mView, mParams);
    }

    private Bitmap getMaskBitmap() {
        Bitmap maskBitmap = null;
        File file = new File(MASK_FILE);
        if (file.exists()) {
            try {
                maskBitmap = BitmapFactory.decodeFile(MASK_FILE);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (maskBitmap == null) {
            maskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.black_mask);
        }
        return maskBitmap;
    }

    private void updateParams(){
        Display display = mWM.getDefaultDisplay();
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        int rotation = display.getRotation();
        switch(rotation) {
            case Surface.ROTATION_0:
                mParams.x = 0;
                mParams.y = 0;
                mParams.width = displaySize.x;
                mParams.height = mMaskView.getMaskHeight();
                mParams.gravity = Gravity.LEFT | Gravity.TOP;
            break;
            case Surface.ROTATION_90:
                mParams.x = 0;
                mParams.y = 0;
                mParams.width = mMaskView.getMaskHeight();
                mParams.height = displaySize.y;
                mParams.gravity = Gravity.LEFT | Gravity.TOP;
            break;
            case Surface.ROTATION_180:
                mParams.x = 0;
                mParams.y = 0;
                mParams.width = displaySize.x;
                mParams.height = mMaskView.getMaskHeight();
                mParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
            break;
            case Surface.ROTATION_270:
                mParams.x = 0;
                mParams.y = 0;
                mParams.width = mMaskView.getMaskHeight();
                mParams.height = displaySize.y;
                mParams.gravity = Gravity.RIGHT | Gravity.TOP;
            break;
        }
    }

    class MyHandler extends Handler {
        static final int SHOW_MSG = 1;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_MSG: {
                    handleShowOverlay();
                } break;
            }
        }
    }

    class MyOrientationListener implements MaskView.MaskOrientationListener {
        public void onOrientationChanged(int rot) {
            updateParams();
            mWM.updateViewLayout(mView, mParams);
        }
    }
}
