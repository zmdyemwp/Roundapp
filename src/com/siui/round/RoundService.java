package com.evenwell.round;

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

import android.net.Uri;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.database.ContentObserver;


public class RoundService extends Service{
    public static final String TAG = RoundApplication.TAG;
    private static final String MASK_FILE_BLACK = "system/etc/black_mask.png";
    private static final String MASK_FILE_WHITE = "system/etc/white_mask.png";
    WindowManager mWM = null;
    WindowManager.LayoutParams mParams = null;
    MyHandler mHandler = new MyHandler();
    View mView = null;
    MaskView mMaskView = null;
    MyOrientationListener mListener = new MyOrientationListener();


    ContentObserver observer = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            showWMOverlay();
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }
    };

    boolean isInversionEnabled() {
        return 1 == Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WmService onStartCommand");

        //  TODO: register setting listener
        Uri setting = Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED);
        getContentResolver().registerContentObserver(setting, false, observer);

        showWMOverlay();
        return START_STICKY;
    }

    public void onDestroy() {
        //  TODO: unregister setting listener
        getContentResolver().unregisterContentObserver(observer);
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
        if(null != mView) mWM.removeView(mView);

        mParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY);
        mParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                       WindowManager.LayoutParams.FLAG_FULLSCREEN |
                       WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                       WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE; 
        mParams.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        mParams.setTitle("RoundCorner");
        int rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
        mParams.rotationAnimation = rotationAnimation;
        LayoutInflater inflater = LayoutInflater.from(this);
        if(isInversionEnabled()) {
            mView = inflater.inflate(R.layout.mask_view_invert, null);
        } else {
            mView = inflater.inflate(R.layout.mask_view, null);
        }
        if (mView != null && mView.getBackground() != null) {
            mParams.format = mView.getBackground().getOpacity();
        }
        /* 20180309 - MinSMChien
        mMaskView = (MaskView)mView.findViewById(R.id.iv);
        if (mMaskView != null) {
            // Bitmap maskBitmap = getMaskBitmap();
            // mMaskView.setMask(maskBitmap);
            mMaskView.setMask(isInversionEnabled());
            mMaskView.registerOrientationListener(mListener);
            updateParams();
        }
        */
        mWM.addView(mView, mParams);
    }

    static Bitmap[] sMaskBitmap = new Bitmap[2];
    private Bitmap getMaskBitmap() {
        Bitmap maskBitmap = null;
        
        String FILE_NAME = "";
        int index = 0;
        if(isInversionEnabled()) {
            FILE_NAME = MASK_FILE_WHITE;
            index = 1;
        } else {
            FILE_NAME = MASK_FILE_BLACK;
            index = 0;
        }

        /* MinSMChien - Memory Issue - Too much memory used - 20180306 */
        /*
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try {
                //  Do NOT skip bitmap creation, since we need to load bitmap file dynamically.
                maskBitmap = BitmapFactory.decodeFile(FILE_NAME);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return maskBitmap;
        }

        if (sMaskBitmap[index] == null) {
            if(isInversionEnabled()) {
                sMaskBitmap[index] = BitmapFactory.decodeResource(getResources(), R.drawable.white_mask);
            } else {
                sMaskBitmap[index] = BitmapFactory.decodeResource(getResources(), R.drawable.black_mask);
            }
        }

        return sMaskBitmap[index];
        */
        if(isInversionEnabled()) {
            return BitmapFactory.decodeResource(getResources(), R.drawable.white_mask);
        } else {
            return BitmapFactory.decodeResource(getResources(), R.drawable.black_mask);
        }
        /* MinSMChien - Memory Issue - Too much memory used - 20180306 */
    }

    private void updateParams(){
        Display display = mWM.getDefaultDisplay();
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        int rotation = display.getRotation();
        mParams.x = 0;
        mParams.y = 0;
        mParams.width = displaySize.x;
        mParams.height = displaySize.y;
        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        Log.d("MIN", String.format("<r>[w, h] = <%d>[%d, %d]", rotation, mParams.width, mParams.height));
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
