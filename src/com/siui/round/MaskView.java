package com.siui.round;

import android.annotation.Nullable;
import android.content.Context;
import android.content.res.Configuration;
import android.view.Display;
import android.view.WindowManager;
import android.view.Surface;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;

public class MaskView extends ImageView {
    public static final String TAG = RoundApplication.TAG;
    WindowManager mWM = null;
    Bitmap[] mMaskCache = new Bitmap[4];
    int mHeight = 0;
    MaskOrientationListener mListener = null;
    
    public MaskView(Context context) {
        super(context);
        mWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
    }

    public MaskView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaskView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MaskView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void setMask(Bitmap maskBitmap) {
        mHeight = maskBitmap.getHeight();
        if (maskBitmap != null) {
            for (int i=0; i<mMaskCache.length; i ++) {
                switch(i) {
                    case Surface.ROTATION_0:
                        mMaskCache[i] = maskBitmap;
                    break;
                    case Surface.ROTATION_90:
                        mMaskCache[i] = rotate(maskBitmap, -90);
                    break;
                    case Surface.ROTATION_180:
                        mMaskCache[i] = rotate(maskBitmap, 180);
                    break;
                    case Surface.ROTATION_270:
                        mMaskCache[i] = rotate(maskBitmap, 90);
                    break;
                }
            }
        }
        setImageBitmap(mMaskCache[0]);
    }

    public int getMaskHeight() {
        return mHeight;
    }

    public void registerOrientationListener(MaskOrientationListener listener) {
        mListener = listener;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        Display display = mWM.getDefaultDisplay();
        int rotation = display.getRotation();
        setImageBitmap(mMaskCache[rotation]);
        if (mListener != null) {
            mListener.onOrientationChanged(rotation);
        }
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public interface MaskOrientationListener {
        public void onOrientationChanged(int rot);
    }
}
