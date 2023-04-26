package com.shillu.camera2demo;

/**
 * @Author shillu
 * @Description
 * @Version 1.0
 */
import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * 一个可以调整为指定纵横比的 TextureView
 *
 * 这是一个名为AutoFitTextureView的自定义View，它继承自TextureView类。
 * 它允许设置一个特定的宽高比，以便在相机预览时自动调整视图的大小，从而保持所需的宽高比。
 * 如果未设置宽高比，则此视图将显示为标准大小。
 * 该视图重写了onMeasure方法，以便根据宽高比来测量和调整实际视图大小，确保与所需的宽高比匹配。
 * 这在相机应用程序中非常有用，因为预览大小可能与屏幕大小不同。
 */
public class AutoFitTextureView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * 设置此视图的纵横比。视图的大小将基于从参数计算出的比率进行测量。
     * 注意，参数的实际大小并不重要，即调用setAspectRatio(2, 3)和setAspectRatio(4, 6)将得到相同的结果。
     *
     * @param width  相对水平大小
     * @param height 相对垂直大小
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

}
