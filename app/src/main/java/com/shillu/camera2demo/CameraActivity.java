package com.shillu.camera2demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;


/**
 *
 * @Author shillu
 * @Description 显示相机预览界面的 Activity
 * @Version 1.0
 *
 *
 * 这是一个继承自AppCompatActivity的CameraActivity类，其作用是用于显示相机预览界面。
 * 在onCreate()方法中，首先调用了父类的onCreate()方法，然后通过setContentView()方法设置了相机预览界面的布局，
 * 接着判断savedInstanceState是否为空，如果为空，就创建一个Camera2BasicFragment实例，并将其添加到activity_camera布局文件中的container容器中。
 * Camera2BasicFragment类是用于相机操作的核心类，负责预览、拍照、录像等工作。
 *
 *
 * AppCompatActivity是Android Support Library中的一个类，它是继承自FragmentActivity的一个基类。
 * 它在Android平台上提供了向后兼容的支持，使得开发者可以在低版本的Android系统上使用新版的Material Design和其他新特性。
 * AppCompatActivity提供了许多实用的功能，包括：
 * 1. 支持ActionBar和Toolbar
 * 2. 支持DrawerLayout和NavigationView
 * 3. 支持Fragment和FragmentManager
 * 4. 支持ViewPager和TabLayout
 * 5. 支持OptionsMenu和onOptionsItemSelected
 * 6. 支持Activity的生命周期管理
 * 使用AppCompatActivity开发应用程序，可以避免因为Android系统版本限制而无法使用最新的API或特性的问题。同时，这也使得应用程序更加稳定和可靠，提高了用户体验。
 */
public class CameraActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (null == savedInstanceState) {
            Log.d("shillu", "savedInstanceState = null");

            /**
             * 这段代码使用了Android的Fragment机制。具体来说：
             * 1. `getSupportFragmentManager()`：获取当前Activity的FragmentManager，它是管理Fragment的类。
             * 2. `beginTransaction()`：开启一个事务，用于处理Fragment的添加、删除、替换等操作。
             * 3. `replace(R.id.container, Camera2BasicFragment.newInstance())`：将指定的布局文件或View替换为一个Fragment。
             *     其中，`R.id.container`是用来放置Fragment的视图容器ID，`Camera2BasicFragment.newInstance()`则是创建一个新的Fragment实例。
             * 4. `commit()`：提交事务，使之生效。
             * 综上所述，该代码的作用是将一个名为Camera2BasicFragment的Fragment添加到指定的视图容器中。
             */
            getSupportFragmentManager().beginTransaction().replace(R.id.container, Camera2BasicFragment.newInstance()).commit();
        }
    }
}
