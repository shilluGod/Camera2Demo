package com.shillu.camera2demo;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;

/**
 * @author shillu
 * @version 1.0
 * @description 定义相机录像的 fragment
 */
public class Camera2VideoFragment extends Fragment implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * 用于相机预览的 AutoFitTextureView.
     */
    private AutoFitTextureView mTextureView;


    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById(R.id.video).setOnClickListener(this);
        view.findViewById(R.id.info).setOnClickListener(this);
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.video: {
                break;
            }

            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity).setMessage(R.string.intro_message).setPositiveButton(android.R.string.ok, null).show();
                }
                break;
            }
            default:
                break;
        }
    }
}