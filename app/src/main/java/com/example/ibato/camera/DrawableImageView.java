package com.example.ibato.camera;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.example.ibato.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 5/23/2018.
 */

public class DrawableImageView extends AppCompatImageView
{

    private static final String TAG = "DrawableImageView";

    private static final int SIZE_CHANGE_SPEED = 2;
    private static final int STICKER_STARTING_WIDTH = 300;
    private static final int STICKER_STARTING_HEIGHT = 300;
    private static final int MIN_STICKER_WIDTH = 50;
    private static final int MIN_STICKER_HEIGHT = 50;
    private static final int TRASH_ICON_ENLARGED_SIZE = 55;
    private static final int TRASH_ICON_NORMAL_SIZE = 44;

    //vars
    private int color;
    private float width = 8f;
    private List<Pen> mPenList = new ArrayList<Pen>();
    private Activity mHostActivity;
    private boolean mIsDrawingEnabled = false;


    // Scales
    float mMinWidth = 8f;
    float mMaxWidth = 500f;
    private ScaleGestureDetector mScaleGestureDetector;
    private boolean mIsSizeChanging = false;
    private Circle mCircle;
    private int mScreenWidth;


    // Stickers
    private ArrayList<Sticker> mStickers = new ArrayList<>();
    int mPrevStickerX, mPrevStickerY;
    int mSelectedStickerIndex = -1;
    private boolean mIsStickerResizing = false;

    // Trash can location
    Rect trashRect;

    private class Sticker{

        Paint paint;
        Bitmap bitmap;
        Drawable drawable;
        int x, y;
        Rect rect;


        Sticker(Bitmap bitmap, Drawable drawable, int x, int y){
            paint = new Paint();
            this.x = x;
            this.y = y;
            this.bitmap = bitmap;
            this.drawable = drawable;
            rect = new Rect(x, y, x + STICKER_STARTING_WIDTH, y + STICKER_STARTING_HEIGHT);
        }

        public void adjustRect(){
            rect.left = x;
            rect.top = y;
            rect.right = x + bitmap.getWidth();
            rect.bottom = y + bitmap.getHeight();
        }
    }


    private class Circle {

        float x, y;
        Paint paint;

        Circle(int color, float x, float y) {
            this.x = x;
            this.y = y;
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(width);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
    }

    private class Pen {
        Path path;
        Paint paint;

        Pen(int color, float width ) {
            path = new Path();
            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStrokeWidth(width);
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
    }



    public DrawableImageView(Context context) {
        super(context);
        init(context);
    }

    public DrawableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context);
    }

    public DrawableImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {
        mPenList.add(new Pen(color, width));
        setDrawingCacheEnabled(true);
        if(context instanceof Activity) {
            mHostActivity = (Activity) context;

            mScaleGestureDetector = new ScaleGestureDetector(mHostActivity, new ScaleListener());

            DisplayMetrics displayMetrics = new DisplayMetrics();
            mHostActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            mScreenWidth = displayMetrics.widthPixels;

            int screenHeight = displayMetrics.heightPixels;

            float density = displayMetrics.density;
            int bottomMargin = (int)mHostActivity.getResources().getDimension(R.dimen.cam_widget_margin_bottom);

            int left = (mScreenWidth / 2) - (int) ( (TRASH_ICON_NORMAL_SIZE ) * density + 0.5f);
            int top = screenHeight - (int) ( (bottomMargin + TRASH_ICON_NORMAL_SIZE ) * density + 0.5f);;
            int right = (mScreenWidth / 2) + (int) ( (TRASH_ICON_NORMAL_SIZE ) * density + 0.5f);
            int bottom = screenHeight;

            trashRect = new Rect(left, top, right, bottom);
        }
    }

    public void reset() {
        for (Pen pen : mPenList) {
            pen.path.reset();
        }
        width = mMinWidth;
        mStickers.clear();
        invalidate();
    }

    public void setWidth(float width) {
        this.width = width;
    }


    private void hideStatusBar() {

        if(mHostActivity != null){
            View decorView = mHostActivity.getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }

    }


    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();

            if(scaleFactor > 1.011 || scaleFactor < 0.99) {

                if(mSelectedStickerIndex != -1){
                    mStickers.get(mSelectedStickerIndex).bitmap
                            = resizeBitmap(
                            mStickers.get(mSelectedStickerIndex).drawable,
                            mStickers.get(mSelectedStickerIndex).bitmap,
                            scaleFactor
                    );

                    mIsStickerResizing = true;

                }
                else{
                    float prevWidth = width;
                    if(scaleFactor > 1){
                        width += ( (SIZE_CHANGE_SPEED + (width * 0.05)) * scaleFactor );
                    }
                    else{
                        width -= ( (SIZE_CHANGE_SPEED + (width * 0.05)) * scaleFactor );
                    }
                    if ( width > mMaxWidth) {
                        width = prevWidth;
                    }
                    else if (width < mMinWidth) {
                        width = prevWidth;
                    }
                }

            }


            return true;
        }
    }

    public static Bitmap resizeBitmap(Drawable drawable, Bitmap currentBitmap, float scale){
        Bitmap bitmap = null;

        int newWidth = 0;
        int newHeight = 0;
        if(scale > 1){
            newWidth = currentBitmap.getWidth() + (int)((currentBitmap.getWidth() * 0.04) * scale);
            newHeight = currentBitmap.getHeight() + (int)((currentBitmap.getHeight() * 0.04) * scale);
        }
        else{
            newWidth = currentBitmap.getWidth() - (int)((currentBitmap.getHeight() * 0.04) * scale);
            newHeight = currentBitmap.getHeight() - (int)((currentBitmap.getHeight() * 0.04) * scale);
        }

        if(newWidth < MIN_STICKER_WIDTH){
            newWidth = currentBitmap.getWidth();
        }
        if(newHeight < MIN_STICKER_HEIGHT){
            newHeight = currentBitmap.getHeight();
        }

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return Bitmap.createScaledBitmap(
                        bitmapDrawable.getBitmap(),
                        newWidth,
                        newHeight,
                        false);
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        }
        else {
            bitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, newWidth, newHeight);
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), STICKER_STARTING_WIDTH, STICKER_STARTING_HEIGHT, false);
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        }
        else {
            bitmap = Bitmap.createBitmap(STICKER_STARTING_WIDTH, STICKER_STARTING_WIDTH, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}

