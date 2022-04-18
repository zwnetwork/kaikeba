/*
 * Copyright (C) 2016 Facishare Technology Co., Ltd. All Rights Reserved.
 */
package com.herewhite.kaike.widget;

import static java.lang.Math.ceil;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Description:
 *
 * @author zhaozp
 * @since 2016-05-19
 */
public class FloatView extends FrameLayout {
    private static final String TAG = "AVCallFloatView";

    private int startX;
    private int startY;
    private int startL;//初始时左上X，相对于父容器
    private int startT;//初始时左上Y
    private int startR;//初始时右下X
    private int startB;//初始时右下X
    private int stopX;
    private int stopY;
    private int deltaX;//拖动偏移量
    private int deltaY;

    public FloatView(Context context) {
        super(context);
    }

    public FloatView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                startX = (int) event.getRawX();//相对于屏幕的x坐标,getX()是相对于组件的坐标
                startY = (int) event.getRawY();
                startL = getLeft();
                startT = getTop();
                startR = getRight();
                startB = getBottom();
                Log.d(TAG, "x:" + startX + ",rawX," + event.getRawX() + ",left:" + getLeft());
                break;
            case MotionEvent.ACTION_MOVE:
                stopX = (int) event.getRawX();
                stopY = (int) event.getRawY();
                deltaX = stopX - startX;
                deltaY = stopY - startY;
                Log.e(TAG, " deltaX=  " + deltaX + "  deltaY= " + deltaY + " getTop = " + getTop() + " getBottom = " + getBottom());
                this.moveToPosition(startL + deltaX, startT + deltaY, startR + deltaX, startB + deltaY);
                stopX = (int) event.getRawX();
                stopY = (int) event.getRawY();
                break;
            case MotionEvent.ACTION_UP:
                Point point = getScreenInfo();
                int finalY = point.y - getStatusBarHeight() - dp2px(50);
                //1.超出左边
                if (getLeft() < 0) {
                    if (getTop() > 0) {
                        //超出底部
                        if (getBottom() - finalY > 0) {
                            this.moveToPosition(0, finalY - getHeight(), getWidth(), finalY);
                        } else {
                            this.moveToPosition(0, startT + deltaY, getWidth(), startB + deltaY);
                        }
                    } else {
                        this.moveToPosition(0, 0, getWidth(), getHeight());
                    }
                } else {
                    //2.超出右边
                    if (getRight() > point.x) {
                        int left = point.x - getWidth();
                        if (getTop() > 0) {
                            //超出底部
                            if (getBottom() - finalY > 0) {
                                this.moveToPosition(left, finalY - getHeight(), point.x, finalY);
                            } else {
                                this.moveToPosition(left, startT + deltaY, point.x, startB + deltaY);
                            }
                        } else {
                            this.moveToPosition(left, 0, point.x, getHeight());
                        }
                    } else {
                        //3.顶部溢出
                        if (getTop() > 0) {
                            //超出底部
                            if (getBottom() - finalY > 0) {
                                this.moveToPosition(startL + deltaX, finalY - getHeight(), startR + deltaX, finalY);
                            } else {
                                this.moveToPosition(startL + deltaX, startT + deltaY, startR + deltaX, startB + deltaY);
                            }
                        } else {
                            this.moveToPosition(startL + deltaX, 0, startR + deltaX, getHeight());
                        }
                    }
                }
                invalidate();
                break;
        }
        return true;
    }

    public int dp2px(float dp) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private Point getScreenInfo() {
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        Point point = new Point();
        point.x = dm.widthPixels;
        point.y = dm.heightPixels;
        Log.d(TAG, "screenWidth=" + point.x + " screenHeight=" + point.y);
        return point;
    }

    private int getStatusBarHeight() {
        int identifier = getContext().getResources().getIdentifier("status_bar_height", "dimen", "android");
        int height = 0;
        if (identifier > 0) {
            getContext().getResources().getDimensionPixelSize(identifier);
        } else {
            float density = getContext().getResources().getDisplayMetrics().density;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                height = (int) ceil(24 * density);
            } else {
                height = (int) ceil(25 * density);
            }
        }
        return height;
    }

    private void moveToPosition(int leftX, int leftY, int rightX, int bottom) {
        this.layout(leftX, leftY, rightX, bottom);
    }
}
