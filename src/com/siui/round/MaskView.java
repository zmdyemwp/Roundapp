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
    Context mContext;
    
    public MaskView(Context context) {
        super(context);
        mWM = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        mContext = context;
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

    boolean mCurrentReverState = false;
    int getCurrentImageResourceId() {
        int result = R.drawable.black_mask;
        Display display = mWM.getDefaultDisplay();
        int rotation = display.getRotation();
        if(mCurrentReverState) {
            switch(rotation) {
                case Surface.ROTATION_0:
                    result = R.drawable.white_mask;
                    break;
                case Surface.ROTATION_90:
                    result = R.drawable.white_mask_90;
                    break;
                case Surface.ROTATION_180:
                    result = R.drawable.white_mask_180;
                    break;
                case Surface.ROTATION_270:
                    result = R.drawable.white_mask_270;
                    break;
                default:
                    result = R.drawable.white_mask;
                    break;
            }
        } else {
            switch(rotation) {
                case Surface.ROTATION_0:
                    result = R.drawable.black_mask;
                    break;
                case Surface.ROTATION_90:
                    result = R.drawable.black_mask_90;
                    break;
                case Surface.ROTATION_180:
                    result = R.drawable.black_mask_180;
                    break;
                case Surface.ROTATION_270:
                    result = R.drawable.black_mask_270;
                    break;
                default:
                    result = R.drawable.black_mask;
                    break;
            }
        }
        return result;
    }
    public void setMask(boolean isRever) {
        mCurrentReverState = isRever;
        mHeight = 21;
        int id = getCurrentImageResourceId();
        // mMaskCache v = new TypedValue();
        // mContext.getResources().getValue(R.dimen.float_value_for_shape, v, false);
        setImageResource(id);
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
        // return mHeight;
        return -1;
    }

    public void registerOrientationListener(MaskOrientationListener listener) {
        mListener = listener;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
        Display display = mWM.getDefaultDisplay();
        int rotation = display.getRotation();
        // setImageBitmap(mMaskCache[rotation]);
        setImageResource(getCurrentImageResourceId());
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
