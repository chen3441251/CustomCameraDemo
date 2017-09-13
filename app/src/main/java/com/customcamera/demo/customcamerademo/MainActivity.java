package com.customcamera.demo.customcamerademo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQUESTCODE_CAMERA = 1001;
    private static final int REQUESTCODE_CAMERA            = 101;
    private        Button           mBtn1;
    private        Button           mBtn2;
    private        Button           mBtn3;
    private        ImageView        mImag;
    private        File             mPhotoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }

    private void initView() {
        mBtn1 = (Button) findViewById(R.id.btn1);
        mBtn2 = (Button) findViewById(R.id.btn2);
        mBtn3 = (Button) findViewById(R.id.btn3);
        mImag = (ImageView) findViewById(R.id.iv_imag);
        mBtn1.setOnClickListener(this);
        mBtn2.setOnClickListener(this);
        mBtn3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1://拍照
                checkPermission();
                break;
            case R.id.btn2://相册
                break;
            case R.id.btn3://取消
                break;
        }
    }

    private void checkPermission() {
        //如果camera或者读取权限没有授权
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                //展示授权说明
                //                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUESTCODE_CAMERA);
                Toast.makeText(this, "不开启camera权限就无法使用拍照,请开启权限", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUESTCODE_CAMERA);
            }
        } else {
            //已经授权，直接调用相机
            openCamera();
        }
    }

    private void openCamera() {
        Toast.makeText(this, "openCamera", Toast.LENGTH_SHORT).show();
        File file = new File(Environment.getExternalStorageDirectory(), "/customCameraFile/");
        if (!file.exists()) {
            file.mkdirs();
        }
        mPhotoFile = new File(file, System.currentTimeMillis() + ".jpg");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uriFromFile = getUriFromFile(mPhotoFile);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFromFile);
        startActivityForResult(intent, REQUESTCODE_CAMERA);
    }

    private Uri getUriFromFile(File photoFile) {
        Uri photoUri = null;
        if (Build.VERSION.SDK_INT > M) {
            //android7.0
            photoUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photoFile);
            grantUriPermission(getPackageName(), photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            photoUri = Uri.fromFile(photoFile);
        }
        return photoUri;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUESTCODE_CAMERA) {

            if (grantResults.length >= 1 && grantResults[grantResults.length - 1] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "请开启读写权限", Toast.LENGTH_SHORT).show();

                } else if (grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "请开启相机权限", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(this, "请开启相机权限", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUESTCODE_CAMERA:
                //获取uri
                if (mPhotoFile != null) {
                    try {
                        Bitmap bitmapFormUri = getBitmapFormUri(this, mPhotoFile);
                        mImag.setImageBitmap(bitmapFormUri);
                        Log.d("xxx","路径=="+mPhotoFile.getPath());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    /**
     * 通过uri获取图片并进行压缩
     *
     * @param file
     */
    public Bitmap getBitmapFormUri(Activity ac, File file) throws FileNotFoundException, IOException {
        Uri uriFromFile = getUriFromFile(file);
        InputStream input = ac.getContentResolver().openInputStream(uriFromFile);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        onlyBoundsOptions.inSampleSize = calculateInSampleSize(onlyBoundsOptions, 480, 800);
        //设置缩放比例
        onlyBoundsOptions.inJustDecodeBounds = false;
        input = ac.getContentResolver().openInputStream(uriFromFile);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        return compressImage(bitmap, file);//再进行质量压缩
    }

    // 计算 BitmapFactpry 的 inSimpleSize的值的方法
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        if (reqWidth == 0 || reqHeight == 0) {
            return 1;
        }
        // 获取图片原生的宽和高
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        // 如果原生的宽高大于请求的宽高,那么将原生的宽和高都置为原来的一半
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // 主要计算逻辑
            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * 质量压缩方法
     *
     * @param image
     * @return
     */
    public Bitmap compressImage(Bitmap image, File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            options -= 10;//每次都减少10
            //第一个参数 ：图片格式 ，第二个参数： 图片质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(baos.toByteArray());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

               /* ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
                Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片*/
        int bitmapDegree = getBitmapDegree(file.getPath());
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        Bitmap rotatebitmap = rotateBitmapByDegree(bitmap, bitmapDegree);
        return rotatebitmap;
    }

   /* //保存压缩以后的bitmap
    private void saveBitmap(Bitmap bm, String path) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) // 判断是否可以对SDcard进行操作
        {    // 获取SDCard指定目录下
            String sdCardDir = Environment.getExternalStorageDirectory() + path;
            File dirFile = new File(sdCardDir);  //目录转化成文件夹
            if (!dirFile.exists()) {              //如果不存在，那就建立这个文件夹
                dirFile.mkdirs();
            }                          //文件夹有啦，就可以保存图片啦
            File file = new File(sdCardDir, "newPhoto.jpg");// 在SDcard的目录下创建图片文,以当前时间为其命名

            try {
                mFileOutputStream = new FileOutputStream(file);
                bm.compress(Bitmap.CompressFormat.JPEG, 60, mFileOutputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                mFileOutputStream.flush();
                mFileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }*/
    /**
     * 读取图片的旋转的角度
     *
     * @param path 图片绝对路径
     * @return 图片的旋转角度
     */
    public static int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
    /**
     * 将图片按照某个角度进行旋转
     *
     * @param bm     需要旋转的图片
     * @param degree 旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }
}
