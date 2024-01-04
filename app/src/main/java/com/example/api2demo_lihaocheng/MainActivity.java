package com.example.api2demo_lihaocheng;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private static final int REQUEST_PERMISSION = 1;
    private static final String CAMERA_ID = "1";

    /**
     *Camera state: Showing camera Preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the foucs to be locked.
     */
    private static final int STATE_WAITING_LOCK =1;

    private int mPreviewWidth;
    private int mPreviewHeight;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;

    private File root;

    private MainTextureView mMainTextureView;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mCameraCaptureSession;
    private CaptureRequest mCaptureRequest;

    private int mState;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        root = Environment.getExternalStorageDirectory();
        Button pictureButton = findViewById(R.id.picture);
        pictureButton.setOnClickListener(mPictureButtonListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        mMainTextureView = this.findViewById(R.id.textureView);
        mMainTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        super.onPause();
    }

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
            mPreviewHeight = height;
            mPreviewWidth = width;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    public final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    private void startBackgroundThread(){
        mBackgroundThread = new HandlerThread("backgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread(){
        mBackgroundThread.quitSafely();
        try{
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        }catch (InterruptedException e){
            e.printStackTrace();
        }

    }

    public void openCamera(){
        try{
            CameraManager manager = (CameraManager) MainActivity.this.getSystemService(Context.CAMERA_SERVICE);
            ContextWrapper mContextWrapper = new ContextWrapper(MainActivity.this);
            if(mContextWrapper.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_PERMISSION);
                return;
            }
            manager.openCamera("0", mStateCallback, mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == REQUEST_PERMISSION){
            if(grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                Toast.makeText(MainActivity.this, "This sample needs camera permission" ,Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createCameraPreview(){
        try{
            SurfaceTexture texture = mMainTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);
            Surface surface = new Surface(texture);
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(surface);

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {

                    mCameraCaptureSession = session;
                    mCaptureRequest = mCaptureRequestBuilder.build();
                    try {
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, mCaptureCallback ,mBackgroundHandler);
                    }catch (CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {


                }
            }, null);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result){
            switch (mState){
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK:{
                    Integer afStatge = result.get(CaptureResult.CONTROL_AF_STATE);

                }

            }

        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            process(partialResult);

        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

        }
    };

    private View.OnClickListener mPictureButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            takePicture();
        }
    };

    private void takePicture(){
        try{
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_TRIGGER_START);
            mState = STATE_WAITING_LOCK;
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        }catch (CameraAccessException e){
            e.printStackTrace();
        }

    }



}
