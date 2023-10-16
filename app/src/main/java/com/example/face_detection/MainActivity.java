package com.example.face_detection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.Manifest;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    JavaCameraView javaCameraView;
    File caseFile;
    CascadeClassifier faceDetector;
    private Mat mRgba, mGrey;
    private final int PERMISSIONS_READ_CAMERA=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        if(OpenCVLoader.initDebug()) Log.d("LOADED", "success");
//        else Log.d("LOADED", "err");



        javaCameraView = (JavaCameraView)findViewById(R.id.javaCamView);
        javaCameraView.setCameraIndex(0);

        if(!OpenCVLoader.initDebug())
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseCallBack);
        }
        else
        {
            try {
                baseCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.CAMERA)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSIONS_READ_CAMERA);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            Log.d("LOADED", "PERMISSIOns granted");
            javaCameraView.setCameraPermissionGranted();
            // Permission has already been granted
        }

        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mGrey = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGrey.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGrey = inputFrame.gray();

        MatOfRect faceDetections = new MatOfRect();
        faceDetector.detectMultiScale(mRgba, faceDetections);

        for(Rect rect: faceDetections.toArray())
        {
            Imgproc.rectangle(mRgba, new Point(rect.x, rect.y),
                    new Point(rect.x + rect.width, rect.y + rect.height),
                    new Scalar(255, 0, 0));
        }
        return mRgba;
    }

    private BaseLoaderCallback baseCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) throws IOException {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                {
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    caseFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");
                    FileOutputStream fos = new FileOutputStream(caseFile);

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while((bytesRead = is.read(buffer))!=-1)
                    {
                        fos.write(buffer, 0, bytesRead);
                    }

                    is.close();
                    fos.close();

                    faceDetector = new CascadeClassifier(caseFile.getAbsolutePath());

                    if(faceDetector.empty())
                    {
                        faceDetector = null;
                    }
                    else
                        cascadeDir.delete();

                    javaCameraView.enableView();

                }
                break;
                default:
                {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // Ensure that this result is for the camera permission request
        if (requestCode == PERMISSIONS_READ_CAMERA) {
            // Check if the request was granted or denied
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // The request was granted -> tell the camera view
                javaCameraView.setCameraPermissionGranted();
            } else {
                // The request was denied -> tell the user and exit the application
                Toast.makeText(this, "Camera permission required.",
                        Toast.LENGTH_LONG).show();
                this.finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}