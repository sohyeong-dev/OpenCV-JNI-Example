package com.example.opencvjniexample;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import org.opencv.android.OpenCVLoader;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    private static final String TAG = "OpenCV JNI Example";
    private static final int REQ_CODE_SELECT_IMAGE = 200;

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");

        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "static initializer: OpenCVLoader successfully loaded!");
        } else {
            Log.d(TAG, "static initializer: Error: OpenCVLoader load failed!");
        }

        System.loadLibrary("opencv_java4");
    }

    private ImageView imageView;
    private Bitmap bitmap;

    ArrayList<Mat> cropedList;
    ArrayList<Mat> albumimages;
    ArrayList<Mat> ocrimages;

    @Override
    protected void onDestroy() {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        imageView = (ImageView) findViewById(R.id.imageView);

        cropedList = new ArrayList<Mat>();
        albumimages = new ArrayList<Mat>();
        ocrimages = new ArrayList<Mat>();
    }

    private boolean hasPermissions(String[] permissions) {
        int result;

        for (String permission: permissions) {
            result = ContextCompat.checkSelfPermission(this, permission);

            if (result == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native Mat[] detectPlaylistJNI(String imagePath, long dstMat);   // long addrMat

    public void onButtonClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQ_CODE_SELECT_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_SELECT_IMAGE) {
            if (resultCode == RESULT_OK) {
                try {
                    String path = getImagePathFromURI(data.getData());
                    Log.d(TAG, "onActivityResult: " + path);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 4;
                    bitmap = BitmapFactory.decodeFile(path, options);

                    if (bitmap != null) {
                        detectPlaylist(path);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void detectPlaylist(String path) {
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);
        Mat gray = new Mat();
        Mat[] tempMatArray = detectPlaylistJNI(path, gray.getNativeObjAddr());    // src.getNativeObjAddr()
        Log.d(TAG, "detectPlaylist: " + tempMatArray.length);
        for (Mat tempMat: tempMatArray) {
            cropedList.add(tempMat);
        }
        Log.d(TAG, "detectPlaylist: 곡의 개수는? " + cropedList.size() / 2);
        for (int i = 0; i < cropedList.size(); i += 2) {
            albumimages.add(cropedList.get(i));
            ocrimages.add(cropedList.get(i + 1));
        }
//        Utils.matToBitmap(gray, bitmap);
//        imageView.setImageBitmap(bitmap);

        // --- OCR ---

        ArrayList<Bitmap> ocrBitmapImages = new ArrayList<Bitmap>();
        // Mat to Bitmap
        Bitmap bmp = null;
        for (Mat ocrimage: ocrimages) {
            bmp = Bitmap.createBitmap(ocrimage.cols(), ocrimage.rows(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(ocrimage, bmp);
            ocrBitmapImages.add(bmp);
        }

        OcrApi ocrApi = new OcrApi(this, ocrBitmapImages);

        ArrayList<String> ocrResult = ocrApi.recognizing();
        Log.d(TAG, "detectPlaylist: " + ocrResult.size());
        for (String keyword: ocrResult) {
            Log.d(TAG, "detectPlaylist: " + keyword);
        }
    }

    private String getImagePathFromURI(Uri data) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(data, proj, null, null, null);
        if (cursor == null) {
            return data.getPath();
        } else {
            int idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String imgPath = cursor.getString(idx);
            cursor.close();
            return imgPath;
        }
    }
}
