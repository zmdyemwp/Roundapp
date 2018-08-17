package com.evenwell.round;

import android.app.Service;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
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
import android.view.View.OnLayoutChangeListener;
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
	private static final boolean DEBUG = false;
    private static final String MASK_FILE_BLACK = "system/etc/black_mask.png";
    private static final String MASK_FILE_WHITE = "system/etc/white_mask.png";
    WindowManager mWM = null;
    WindowManager.LayoutParams mParams = null;
    WindowManager.LayoutParams mParamsHide = null;
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
        if(DEBUG) Log.d(TAG, "WmService onStartCommand");

        //  TODO: register setting listener
        Uri setting = Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED);
        getContentResolver().registerContentObserver(setting, false, observer);

        //  TODO: register broadcast receiver for OVERLAY_CHANGED
        if(DEBUG) Log.i(TAG, "Register Broadcast Receiver");
        IntentFilter filter = new IntentFilter(ACTION_OVERLAY_CHANGED);
        filter.addAction("ROUND_APP");
        registerReceiver(br, filter);

        showWMOverlay();
        return START_STICKY;
    }

    private static final String ACTION_OVERLAY_CHANGED = "android.intent.action.OVERLAY_CHANGED";
    BroadcastReceiver  br = new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent) {
            String action = intent.getAction();
            if(DEBUG) Log.i(TAG, "ACTION::" + action);
            if (action.equals(ACTION_OVERLAY_CHANGED)) {
                String data = intent.getData().toString();
                if(DEBUG) Log.i(TAG, "ACTION_OVERLAY_CHANGED::DATA = " + data);
                // removeView();
            } else if (action.equals("ROUND_APP")) {
                int extra = intent.getIntExtra("ex", 0);
                if (null == mView) return;
                if (0 == extra) {
                    mView.setVisibility(View.VISIBLE);
                } else {
                    mView.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    void checkCutOut(View v) {
        if(null == mWM) {
            return;
        }
        Display display = mWM.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        if(DEBUG) Log.d(TAG, String.format("Screen Size (%d, %d)", width, height));
        // if(null == v.getRootWindowInsets()) return;
        // if(null == v.getRootWindowInsets().getDisplayCutout()) {
        if (2034 == width || 2034 == height || 2160 == width || 2160 == height) {
            if(DEBUG) Log.d(TAG, "OnLayoutChangeListener::VISIBLE::" + View.VISIBLE);
            // mView.setVisibility(View.VISIBLE);
            showViewOrNot(true);
        } else {
            if(DEBUG) Log.d(TAG, "OnLayoutChangeListener::INVISIBLE::" + View.INVISIBLE);
            // mView.setVisibility(View.INVISIBLE);
            showViewOrNot(false);
        }
    }
    private void showViewOrNot(boolean show){
        removeView();
        if(show) {
            if(DEBUG) Log.d(TAG, "showViewOrNot::SHOW");
            mWM.addView(mView, mParams);
        } else {
            if(DEBUG) Log.d(TAG, "showViewOrNot::HIDE");
            mWM.addView(mView, mParamsHide);
        }
        mView.addOnLayoutChangeListener(mLayoutChangeListener);
    }
    OnLayoutChangeListener mLayoutChangeListener = new OnLayoutChangeListener() {
        private static final int MAX_WIDTH = 2160;
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom,
                int oldLeft,
                int oldTop, int oldRight, int oldBottom) {
            if(DEBUG) Log.d(TAG, String.format("(%d, %d, %d, %d) =>", oldLeft, oldTop, oldRight, oldBottom));
            if(DEBUG) Log.d(TAG, String.format("=> (%d, %d, %d, %d)", left, top, right, bottom));
            /*
            if(MAX_WIDTH == right || MAX_WIDTH == bottom) {
                // mView.setVisibility(View.VISIBLE);
                showViewOrNot(true);
            } else {
                // mView.setVisibility(View.INVISIBLE);
                showViewOrNot(false);
            }
            */
            checkCutOut(v);
        }
    };

    public void onDestroy() {
        if(DEBUG) Log.i(TAG, "=================== onDestroy");
        //  TODO: unregister setting listener
        getContentResolver().unregisterContentObserver(observer);
        //  TODO: unregister broadcast receiver
        unregisterReceiver(br);
        //  TODO: remove layout change listener
        mView.removeOnLayoutChangeListener(mLayoutChangeListener);
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

    protected void removeView() {
        mWM = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if(null != mView) {
            mView.removeOnLayoutChangeListener(mLayoutChangeListener);
            mWM.removeView(mView);
        }
    }

    protected void handleShowOverlay() {
        mWM = (WindowManager)getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        if(null != mView) mWM.removeView(mView);

        mParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY);
        mParamsHide = new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY);

        mParams.flags = 
                       WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                       WindowManager.LayoutParams.FLAG_FULLSCREEN |
                       WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                       WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        mParamsHide.flags = mParams.flags;

        mParams.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        mParamsHide.privateFlags = mParams.privateFlags;

        mParams.setTitle("RoundCorner");
        mParamsHide.setTitle("RoundCorner");

        int rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_CROSSFADE;
        mParams.rotationAnimation = rotationAnimation;
        mParamsHide.rotationAnimation = rotationAnimation;

        LayoutInflater inflater = LayoutInflater.from(this);
        if(isInversionEnabled()) {
            mView = inflater.inflate(R.layout.mask_view_invert, null);
        } else {
            mView = inflater.inflate(R.layout.mask_view, null);
        }
        if (mView != null && mView.getBackground() != null) {
            mParams.format = mView.getBackground().getOpacity();
            mParamsHide.format = mView.getBackground().getOpacity();
        }

        mParamsHide.width = 0;
        mParamsHide.height = 0;

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
        mView.addOnLayoutChangeListener(mLayoutChangeListener);
        checkCutOut(mView);
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
        if(DEBUG) Log.d(TAG, String.format("<r>[w, h] = <%d>[%d, %d]", rotation, mParams.width, mParams.height));
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
