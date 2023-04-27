package com.shillu.camera2demo;

/**
 * @author shillu
 * @description
 * @version 1.0
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

    /**
     * 继承TextureView的静态方法
     */
    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    /**
     * 继承TextureView的静态方法
     */
    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * 继承TextureView的静态方法
     */
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

        /**
         * requestLayout()是一个View的方法，用于通知View在下一次测量布局时重新计算自己的尺寸和位置，
         * **并且触发onMeasure() -> onLayout() -> onDraw()的整个布局流程。**
         * 通常情况下，当View的属性发生改变时，如宽度、高度、边距等，都需要调用requestLayout()方法来重新布局。
         * 但是，如果只是改变了View的外观而没有改变其尺寸和位置，可以调用invalidate()方法来重绘View，而不需要重新布局。
         */
        requestLayout();
    }


    /**
     * 自定义控件的测量方法，用于确定控件的大小。
     * 在测量方法中，首先调用了父类的onMeasure方法，然后获取控件的宽度和高度。
     * 接着判断宽高比是否为0，如果为0则直接设置控件大小为测量的大小。
     * 如果不为0，则根据宽高比计算控件的大小，使得控件的宽高比与设置的宽高比一致。
     * 最后通过调用setMeasuredDimension方法设置控件的大小。
     *
     * onMeasure()是Android View类中的一个方法，用于测量View的大小。
     * 它被调用时，View会根据父容器的MeasureSpec来计算出自身的大小。
     *
     * MeasureSpec是一个32位的int值，高两位表示测量模式，低30位表示测量值。
     *
     * 测量模式有三种：UNSPECIFIED、EXACTLY和AT_MOST。
     * 1.UNSPECIFIED表示父容器没有对子View做出任何限制，子View可以任意大小。
     * 2.EXACTLY表示父容器已经确切地指定了子View的大小，子View应该匹配这个大小。3.
     * AT_MOST表示子View最大只能是指定大小，或者更小。
     *
     * 在onMeasure()方法中，可以通过调用setMeasuredDimension()方法来设置View的测量大小。
     * 如果View没有设置测量大小，那么它将无法被正确地布局和显示。
     * 因此，在自定义View时，必须实现onMeasure()方法来确保View能够正确地测量大小。
     *
     * @param widthMeasureSpec 父级元素对水平空间的要求。
     *                         在布局网格中，元素的水平空间需求受到其父级元素的限制。
     *                         父级元素可能会设置元素的最小或最大宽度，或者在元素周围留下一定的空白。
     *                         这些限制会影响元素在页面中的位置和尺寸。
     *
     * @param heightMeasureSpec 垂直空间要求是由父元素所施加的限制。
     *
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            /**
             * setMeasuredDimension()是一个Android View类的方法，用于设置View的测量尺寸。
             * 在View的测量过程中，系统会调用measure()方法来测量尺寸，然后调用setMeasuredDimension()方法来设置测量结果。
             * 这个方法接收两个int参数，分别表示View的宽度和高度。
             * 通常情况下，View的测量尺寸是由其父容器来决定的，但是在一些自定义View的情况下，可能需要手动设置View的测量尺寸。
             */
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
