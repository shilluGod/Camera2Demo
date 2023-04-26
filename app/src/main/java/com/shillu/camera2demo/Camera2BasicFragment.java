package com.shillu.camera2demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


/**
 *
 * @Author shillu
 * @Description 定义相机拍照的 fragment
 * @Version 1.0
 *
 * 1.View.OnClickListener  view里面的点击侦听器
 * 用于处理视图点击事件的接口。它用于定义回调方法，在视图被点击时调用该方法。
 * 要使用 View.OnClickListener 接口，需要实现其 onClick() 方法。当用户点击已设置此监听器的视图时，将调用 onClick() 方法。
 *
 *
 * 2.ActivityCompat.OnRequestPermissionsResultCallback
 * ActivityCompat.OnRequestPermissionsResultCallback 是 Android 中的一个接口，它允许您在权限请求结果可用时接收回调。
 * 当您的应用请求用户授予某些敏感权限时，系统会向用户显示一个对话框，询问是否授予权限。
 * 在用户响应该对话框之后，系统会调用您的应用的 onRequestPermissionsResult() 方法，该方法在您实现 ActivityCompat.OnRequestPermissionsResultCallback 接口时被调用。
 * 要使用该接口，您需要实现 onRequestPermissionsResult() 方法，并在其中处理权限请求结果。
 * 该方法接受三个参数：requestCode，permissions 和 grantResults。其中 requestCode 是您在请求权限时提供的请求代码，permissions 是请求的权限列表，grantResults 是相应权限的授予状态数组（允许或拒绝）。在 onRequestPermissionsResult() 方法中，您可以根据 grantResults 数组的内容来判断用户是否授予了所请求的权限，并根据情况采取适当的操作。
 */
public class Camera2BasicFragment extends Fragment implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * 该方法用于将屏幕旋转转换为JPEG方向
     *
     * 这段代码定义了一个SparseIntArray类型的常量ORIENTATIONS和两个常量REQUEST_CAMERA_PERMISSION和FRAGMENT_DIALOG。
     * SparseIntArray是一个在Android开发中经常使用的类，它可以将整数映射到另一个整数。在这里，它被用来将屏幕旋转角度映射为JPEG图像方向。
     * 四个键值分别对应着0度、90度、180度和270度的屏幕旋转角度，对应的值分别为90、0、270和180，这些值分别代表JPEG图像的方向。
     * REQUEST_CAMERA_PERMISSION和FRAGMENT_DIALOG常量被用于处理相机权限和对话框的相关操作。
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }


    /**
     * Log标签
     */
    private static final String TAG = "Camera2BasicFragment";

    /**
     * 相机状态：显示相机预览
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * 相机状态：等待对焦被锁定
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * 相机状态：等待曝光进入预捕获状态
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * 相机状态：等待曝光状态不再是预拍照状态
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * 相机状态：照片已拍摄
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Camera2 API所保证的最大预览宽度
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Camera2 API所保证的最大预览高度
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    /**
     * 锁超时时间吧，自己定义的，防止魔法值
     */
    private static final int CLOSE_LOCK_TIME = 2500;

    /**
     *
     * 这是一个 TextureView 的监听器，用于监听 TextureView 的 SurfaceTexture 状态变化。
     * 其中包括 SurfaceTexture 可用、尺寸变化、销毁等事件。在这个监听器中，我们根据不同的事件分别执行不同的操作。
     *
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {


        // 在 onSurfaceTextureAvailable() 方法中，当 SurfaceTexture 可用时，我们调用 openCamera() 方法来打开相机并启动预览。
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        // 在 onSurfaceTextureSizeChanged() 方法中，当 SurfaceTexture 尺寸发生变化时，
        // 我们调用 configureTransform() 方法来重新配置相机预览的变换矩阵，以确保预览画面的正确显示。
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        // 在 onSurfaceTextureDestroyed() 方法中，当 SurfaceTexture 被销毁时，我们返回 true，
        // 表示此 SurfaceTexture 已经被处理了，否则会收到 logcat 错误提示。
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        // 在 onSurfaceTextureUpdated() 方法中，当 SurfaceTexture 更新时，我们不需要做任何操作，因此该方法留空。
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * CameraDevice ID
     */
    private String mCameraId;

    /**
     * 用于相机预览的 AutoFitTextureView
     */
    private AutoFitTextureView mTextureView;

    /**
     * 用于相机预览的 CameraCaptureSession
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * 一种指向已打开的 CameraDevice 的引用
     */
    private CameraDevice mCameraDevice;

    /**
     * 预览尺寸
     */
    private Size mPreviewSize;

    /**
     *
     * 相机状态回调
     * 这段代码定义了一个 CameraDevice 的状态回调接口 mStateCallback
     * 它实现了三个回调方法：onOpened()、onDisconnected()、onError(),这些回调方法会在相机设备的状态发生变化时被调用。
     *
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        // 当相机设备成功打开并准备开始工作时，onOpened() 方法会被调用，释放相机打开关闭锁，
        // 保存当前的相机设备对象 mCameraDevice，调用 createCameraPreviewSession() 方法开始预览。
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            // 开启相机预览
            createCameraPreviewSession();
        }

        // 当相机设备断开连接时，onDisconnected() 方法会被调用，释放相机打开关闭锁，关闭相机设备，清空 mCameraDevice 对象。
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        // 当相机设备遇到错误时，onError() 方法会被调用，释放相机打开关闭锁，关闭相机设备，清空 mCameraDevice 对象，并结束当前 Activity。
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    /**
     * 一个额外的线程是在 Android 应用程序中创建的一个独立线程，用于执行可能需要很长时间的任务或不应该阻塞 UI 线程的任务。
     * UI 线程是 Android 应用程序的主线程，负责处理用户交互和更新 UI。
     * 如果在 UI 线程上执行长时间运行的任务，它可能会导致应用程序变得无响应，甚至崩溃。
     * 因此，建议创建一个额外的线程来运行此类任务。
     * 通过这样做，UI 线程可以继续处理用户交互并更新 UI，而长时间运行的任务则在后台线程中执行。
     */
    private HandlerThread mBackgroundThread;

    /**
     * 一个用于在后台运行任务的 Handler 是指一种在 Android 应用中允许你在单独的线程上调度和运行代码的类，
     * 通常用于执行长时间运行或后台任务，以避免阻塞 UI 线程。
     * 可以创建一个具有特定线程以执行其消息队列的 Handler 实例，或将其附加到当前线程的 Looper 上以在该线程上运行。
     * 可以将一个 Runnable 或一个 Message 对象发布到 Handler 的消息队列中，
     * 当 Handler 接收到消息时，它会在适当的线程上执行代码。
     * 这可用于实现后台处理、网络操作或根据后台线程中的数据更改更新 UI 等功能。
     */
    private Handler mBackgroundHandler;

    /**
     * 是指一个 Android 类，它允许你从设备相机中捕获静止图像。
     * 它提供了一种访问原始图像数据的方法，可以将其保存为图像文件或根据需要进一步处理。
     * ImageReader 通常与 Camera2 API 结合使用，并提供一种在单独的线程中异步捕获相机图像的方法，允许主线程继续运行而不被阻塞。
     * 这对于实现连续图像捕获或在录制视频时捕获图像等功能非常有用。
     */
    private ImageReader mImageReader;

    /**
     * 照片的输出文件
     */
    private File mFile;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * 这段代码实现了一个CameraCaptureSession.CaptureCallback的回调函数，用于处理摄像头捕获画面时的不同状态。
     *
     * 其中，process()函数根据当前状态来进行不同的处理，可以用来控制捕获图片的流程。
     * 在STATE_PREVIEW状态下，即正常预览状态下，不需要进行任何处理；
     * 在STATE_WAITING_LOCK状态下，等待对焦完成，可以判断对焦状态并启动预拍照流程；
     * 在STATE_WAITING_PRECAPTURE状态下，等待曝光预捕获状态，可以判断曝光状态并启动预拍照流程；
     * 在STATE_WAITING_NON_PRECAPTURE状态下，等待曝光完成状态，可以判断曝光状态并启动拍照流程。
     *
     * onCaptureProgressed()和onCaptureCompleted()方法则分别对应不同阶段的捕获结果，可以调用process()函数来处理不同状态下的结果。
     *
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // 相机预览正常，则无操作
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE 在某些设备中可能为空
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE 在某些设备中可能为空
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE 在某些设备中可能为空
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
                default:
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }
    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    /**
     * 这段代码是一个用于选择合适的相机预览尺寸的方法，它接收一些参数包括预览视图的宽和高、最大的宽和高以及一个期望的宽高比。
     * 它会遍历相机支持的预览尺寸，将宽高比符合要求的尺寸分别放入 bigEnough 和 notBigEnough 两个列表中。
     * 如果 bigEnough 列表不为空，那么它会返回其中面积最小的一个尺寸；
     * 如果 bigEnough 列表为空而 notBigEnough 不为空，那么它会返回其中面积最大的一个尺寸；
     * 如果 bigEnough 和 notBigEnough 列表都为空，那么它会返回相机支持的第一个尺寸。
     * 同时，这个类还提供了一个静态方法 newInstance()，用于创建一个新的 Camera2BasicFragment 实例。
     *
     * @param choices           根据预览SurfaceView的得到的摄像头所支持的流配置
     * @param textureViewWidth  TextureView的宽
     * @param textureViewHeight TextureView的高
     * @param maxWidth          可显示区域的宽
     * @param maxHeight         可显示区域的高
     * @param aspectRatio       可使用的最大尺寸
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     *
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }


    /**
     *
     * onCreateView() 绑定 Layout
     * 这是一个 Android Fragment 的方法，用于创建并返回与该 fragment 关联的视图层次结构.
     *
     * 该方法的实现将一个布局资源 R.layout.fragment_camera2_basic 转换为一个 View 对象，并将其添加到 container 中。
     * false 参数表示不将该 View 添加到其父 View 中，因为该 View 将在返回时自动添加到容器中。
     * 最后，该方法返回创建的 View 对象。
     *
     *
     * @param inflater 用于将布局资源转换为 View 对象的布局填充器。
     * @param container 该 fragment 的父级 View 的容器。
     * @param savedInstanceState 保存了当前 fragment 的先前状态信息的 Bundle 对象。
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    /**
     * onViewCreated() 设置点击事件和绑定view
     * 用于在创建视图之后对其进行进一步的操作。在该方法中，可以使用传递给方法的视图对象（View）来查找和引用视图层次结构中的各个元素，并对它们进行操作。
     *
     * 在该方法中，首先通过 view.findViewById() 方法来查找 R.id.picture 和 R.id.info 两个按钮，
     * 并将当前的 Fragment 实现了 OnClickListener 接口来响应按钮的点击事件。
     * 接下来，通过 view.findViewById() 方法查找 R.id.texture 并将其转换为 AutoFitTextureView 对象，以便我们可以使用它来进行相机预览操作。
     *
     * @param view 该 fragment 的根视图对象
     * @param savedInstanceState 保存了当前 fragment 先前状态信息的 Bundle 对象
     */
    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById(R.id.picture).setOnClickListener(this);
        view.findViewById(R.id.info).setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    /**
     *
     * 这是一个 Android Fragment 中的方法，用于在 Activity 创建后完成 Fragment 的启动。
     * 在该方法中，我们可以执行一些初始化工作，例如初始化变量、绑定数据等。
     *
     * 在该方法中，首先调用父类方法 super.onActivityCreated(savedInstanceState)。
     * 然后，使用 getActivity() 方法获取 Fragment 所在的 Activity 对象，
     * 并使用 getExternalFilesDir(null) 方法获取应用程序的外部存储目录，
     * 最后将其与文件名 "pic.jpg" 拼接成一个 File 对象 mFile，以便我们可以在拍照时保存图片到该文件中。
     *
     * @param savedInstanceState 保存了当前 fragment 先前状态信息的 Bundle 对象
     *
     *
     * 过时方法，后续看看怎么替换掉
     *
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");
    }


    /**
     * onResume() 开启相机
     * 这是一个 Android Fragment 的方法，用于在 Fragment 可见且位于前台时执行一些逻辑。
     * 在该方法中，我们通常会初始化一些变量、启动后台线程、注册一些监听器等。
     *
     * 在该方法中，首先调用了 super.onResume() 方法以确保 Fragment 生命周期的正常进行。
     * 接着调用了 startBackgroundThread() 方法，该方法用于启动一个后台线程，以便我们可以在后台线程中进行相机操作。
     * 然后，我们检查 mTextureView 是否已经准备好显示相机预览，
     * 并根据情况分别调用 openCamera() 方法或设置 mSurfaceTextureListener 监听器，
     * 以便在 onSurfaceTextureAvailable() 方法回调中打开相机并启动预览。
     *
     * 需要注意的是，如果屏幕被关闭并重新打开，SurfaceTexture 已经可用，此时 onSurfaceTextureAvailable() 方法将不会被调用。
     * 因此，在这种情况下，我们需要在 onResume() 方法中手动打开相机并启动预览。
     */
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();    // 开启一个后台线程处理相机数据

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            // 设置 mSurfaceTextureListener 监听器, 以便在 onSurfaceTextureAvailable() 方法回调中打开相机并启动预览
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    /**
     * 关闭相机后台线程
     */
    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(getString(R.string.request_permission))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     *
     * setUpCameraOutputs 是用于设置与相机相关的成员变量的方法
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // 在这个样例中，我们不使用前置摄像头。
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                // 如果使用前置摄像头，就不处理
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                // 得到该摄像头设备支持的可用流配置，还包括每种格式/尺寸组合的最小帧时长和停顿时长。
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // 对于静态图想捕获，我们使用最大的可用大小
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                // /*maxImages*/2
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                // 找出是否需要交换尺寸以获得相对于传感器坐标的预览尺寸
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // 危险！尝试使用太大的预览大小可能会超过相机总线的底宽限制，导致华丽的预览，但是会存储垃圾捕获数据
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest);

                // 我们将 TextureView 的宽高比与我们选择的预览大小相匹配
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // 检查flash是否可用
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // 目前，当使用Camera2API时抛出一个NPE，但在运行此代码的设备上不支持
            ErrorDialog.newInstance(getString(R.string.camera_error)).show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * openCamera() 这是一个用于打开相机的方法
     *
     * 在这个方法中，我们首先检查是否已经获取了相机权限，如果没有获取到权限，就调用 requestCameraPermission() 方法来请求权限，然后直接返回。
     * 如果已经获取到相机权限，就调用 setUpCameraOutputs() 方法来设置相机输出参数，
     * 然后调用 configureTransform() 方法来配置相机预览的变换矩阵，以确保预览画面的正确显示。
     * 接着，我们获取到当前的 Activity 对象，然后通过调用 getSystemService() 方法来获取相机管理器 CameraManager 的实例。
     * 接下来，我们调用 mCameraOpenCloseLock.tryAcquire() 方法来尝试获取相机开启的锁，并设置等待时间为 2500 毫秒。
     * 如果在等待时间内没有获取到锁，就抛出一个 RuntimeException 异常。
     * 如果获取到了锁，就通过调用 manager.openCamera() 方法来打开相机，并传入相机 ID、相机状态回调和后台线程的 Handler 对象。
     * 在这个方法中，我们通过相机状态回调来处理相机开启和关闭的状态，例如当相机已经关闭时，我们可以通过调用 mCameraOpenCloseLock.release() 方法来释放相机开启的锁。
     */
    private void openCamera(int width, int height) {

        // 获取权限
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }
        // 设置相机特性，设置图片存储监听，拍照图片有效会通知ImageSaver线程保存图片，设置AE，AF等
        setUpCameraOutputs(width, height);
        // 设置矩阵变换，配置预览图的大小、方向、角度
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(CLOSE_LOCK_TIME, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            // 获取CameraManager对象，正式打开相机
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }


    /**
     *
     * 这是一个用于启动后台线程的方法，用于在后台线程中执行一些需要耗时操作的任务，例如打开相机、拍照、保存图片等操作。
     * 在这个方法中，我们创建一个名为 "CameraBackground" 的 HandlerThread 对象，并通过调用 start() 方法来启动线程。
     * 接着，我们使用 getLooper() 方法获取到线程的 Looper 对象，然后通过传入该 Looper 对象来创建一个 Handler 对象，
     * 这个 Handler 对象可以用于在后台线程中执行一些需要耗时操作的任务。
     * 通过这种方式，我们可以避免在主线程中执行耗时任务，从而提高应用的响应速度和用户体验。
     *
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     *
     * 该方法用于停止后台线程，即在 onPause() 方法中被调用，以便释放资源和停止正在进行的任务。
     *
     * 首先调用 mBackgroundThread.quitSafely() 方法来请求停止 mBackgroundThread，并在完成当前消息队列中的所有消息后停止该线程。
     * 然后，使用 mBackgroundThread.join() 方法阻止调用线程（通常是主线程）直到后台线程终止。
     * 在等待期间，如果中断发生，则会抛出 InterruptedException。
     * 最后，将 mBackgroundThread 和 mBackgroundHandler 设置为 null，以便它们可以被垃圾回收。
     *
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * 这是一个创建摄像头预览会话的方法。
     * 它首先从mTextureView中获取SurfaceTexture对象，然后设置其默认缓冲区大小为所需的预览尺寸。
     * 接下来，它创建一个Surface对象，并将其添加到CaptureRequest.Builder中。
     * 然后，它使用createCaptureSession()方法创建一个CameraCaptureSession对象，并传递两个Surface对象：一个用于预览，另一个用于捕捉图像。
     * 当CameraCaptureSession准备好后，它设置自动对焦模式为连续图片模式，并启用自动闪光灯功能，最后开始显示相机预览。
     * 如果配置失败，则显示一个简单的消息。
     *
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // 将默认缓冲区大小配置为所需的相机预览尺寸
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // 这是我们需要开始预览的输出surface
            Surface surface = new Surface(texture);

            // 我们用输出的surface设置CaptureRequest.Builder
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // 在这里，为相机预览创建一个CameraCaptureSession
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // 相机已经关闭
                            if (null == mCameraDevice) {
                                return;
                            }

                            // 当会话准备好后，开始显示预览
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // 相机预览时自动对焦应连续
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // 必要时启动flash
                                setAutoFlash(mPreviewRequestBuilder);

                                // 最后，显示相机预览
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * 这段代码中的 configureTransform 方法是用来配置 TextureView 的变换矩阵，使预览画面能够正确地显示在 TextureView 上。
     *
     * 首先，该方法会获取当前设备的旋转角度，然后创建一个 Matrix 对象，用于设置变换矩阵。
     * 接着，它会创建两个 RectF 对象，一个用于表示 TextureView 的区域，一个用于表示预览画面的区域。
     * 然后计算出这两个区域的中心点坐标，并将预览画面的区域进行偏移，使其中心点与 TextureView 的中心点重合。
     * 接下来，根据设备的旋转角度来设置变换矩阵。如果设备的旋转角度是 90 度或 270 度，就需要将预览画面旋转，并进行缩放，以确保预览画面能够填满 TextureView。
     * 如果设备的旋转角度是 180 度，则只需要将预览画面旋转 180 度即可。
     * 最后，将配置好的变换矩阵应用到 TextureView 上，以正确地显示预览画面。
     *
     * @param viewWidth  mTextureView 宽
     * @param viewHeight mTextureView 高
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * 这是一个用于拍摄静态照片的方法。
     * 在该方法中，首先判断活动(Activity)和相机设备(CameraDevice)是否为空，如果为空，则直接返回。
     * 然后创建一个CaptureRequest.Builder来拍摄静态照片，设置目标Surface为ImageReader的Surface。
     * 使用与预览相同的自动曝光(AE)和自动对焦(AF)模式。然后设置照片方向，以确保图像按正确的方向显示。
     * 接下来创建一个CaptureCallback，当照片成功保存时会显示一条消息并释放对焦。
     * 最后，停止预览，中止所有当前正在进行的捕获请求，然后执行新的拍摄请求。
     * 如果发生相机访问异常(CameraAccessException)，则打印堆栈跟踪。
     *
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // 这是用来拍照的CaptureRequest.Builder
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // 使用与预览相同的AE and AF
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);
                    Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * 这段代码的作用是从指定的屏幕旋转中获取JPEG方向。
     *
     * 在这段代码中，首先会检查Activity和相机设备是否存在，如果不存在则会直接返回。
     * 然后会创建一个CaptureRequest.Builder对象用于拍摄静态图片，并将目标设置为ImageReader的Surface。
     * 然后将自动曝光和自动对焦模式设置为连续图片模式。
     * 接下来，将图片的方向设置为设备的方向，因为设备的方向可能是90度或270度，所以需要考虑这个问题。
     * 最后，创建一个CaptureCallback对象，当图片捕获完成时会调用该对象的onCaptureCompleted方法。
     * 然后停止连续预览，中止之前未完成的捕获请求，并开始捕获图片。如果出现相机访问异常，将会打印异常堆栈信息。
     *
     * @param rotation 屏幕的旋转方向
     * @return JPEG 方向（取值为 0、90、270 和 360 中的一个）
     *
     */
    private int getOrientation(int rotation) {
        // 对于大多数设备，传感器方向是90度，而对于某些设备（例如Nexus 5X），传感器方向是270度。
        // 因此，我们需要将这一点考虑在内，并正确旋转JPEG图像。
        // 对于传感器方向为90度的设备，我们可以直接使用ORIENTATIONS映射来确定JPEG的方向。
        // 对于传感器方向为270度的设备，我们需要将JPEG旋转180度。
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     *
     * 解锁对焦
     *
     * 当静态图像捕获序列完成时应调用此方法。该方法将重置自动对焦触发器，并将相机会话恢复为预览状态。
     */
    private void unlockFocus() {
        try {
            // 重置自动对焦触发器
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            // 之后，摄像头会回到正常的预览状态，也就是之前的状态。
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.picture: {
                takePicture();
                break;
            }
            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }
            default:
                break;
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.request_permission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }


}