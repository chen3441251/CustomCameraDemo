package com.customcamera.demo.customcamerademo;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.database.Cursor;
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
import java.util.List;

import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQUESTCODE_CAMERA = 1001;
    private static final int REQUESTCODE_CAMERA            = 101;//拍照
    private static final int REQUESTCODE_CAMERA_CROP       = 102;//拍照并裁剪
    private static final int REQUEST_CROP                  = 103;//裁剪
    private static final int REQUEST_PICK_PHOTO            = 104;//图库选择图片
    private static final int PERMISSION_REQUESTCODE_SDCARD = 1002;
    private Button    mBtn1;
    private Button    mBtn2;
    private Button    mBtn3;
    private Button    mBtn4;
    private Button    mBtn5;
    private ImageView mImag;
    private File      mPhotoFile;
    private  boolean isCrop;

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
        mBtn4 = (Button) findViewById(R.id.btn4);
        mBtn5 = (Button) findViewById(R.id.btn5);
        mImag = (ImageView) findViewById(R.id.iv_imag);
        mBtn1.setOnClickListener(this);
        mBtn2.setOnClickListener(this);
        mBtn3.setOnClickListener(this);
        mBtn4.setOnClickListener(this);
        mBtn5.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1://拍照
                isCrop=false;
                checkPermission();
                break;
            case R.id.btn2://拍照裁剪
                isCrop=true;
                checkPermission();
                break;
            case R.id.btn3://相册
                isCrop=false;
                checkSdcardPermission();
                //                pickPhoto();
                break;
            case R.id.btn4://相册裁剪
                isCrop=true;
                checkSdcardPermission();
                break;
            case R.id.btn5://取消
                break;
        }
    }

    private void checkSdcardPermission() {
        //如果camera或者读取权限没有授权
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //展示授权说明
                Toast.makeText(this, "不开启sd读写权限就无法使用,请开启权限", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUESTCODE_SDCARD);
            }
        } else {
            //已经授权
            pickPhoto();
        }
    }



    private void checkPermission() {
        //如果camera或者读取权限没有授权
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                //展示授权说明
                Toast.makeText(this, "不开启camera权限就无法使用拍照,请开启权限", Toast.LENGTH_SHORT).show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUESTCODE_CAMERA);
            }
        } else {
            //已经授权，直接调用相机
            openCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUESTCODE_CAMERA:
                if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(this, "请开启读写权限", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "请开启相机权限", Toast.LENGTH_SHORT).show();
                    }

                }
                break;
            case PERMISSION_REQUESTCODE_SDCARD:
                if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickPhoto();
                } else {
                    Toast.makeText(this, "请开启SD读写权限", Toast.LENGTH_SHORT).show();

                }
                break;
        }

    }
    /**
     * 判断系统中是否存在可以启动的相机应用
     *
     * @return 存在返回true，不存在返回false
     */
    public boolean hasCamera() {
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    private void openCamera() {
        if (!hasCamera()) {
            Toast.makeText(this, "没有可用相机，请检查", Toast.LENGTH_SHORT).show();
            return;
        }
        File file = new File(Environment.getExternalStorageDirectory(), "/customCameraFile/");
        if (!file.exists()) {
            file.mkdirs();
        }
        mPhotoFile = new File(file, System.currentTimeMillis() + ".jpg");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uriFromFile = transCameraFile2Uri(mPhotoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFromFile);
        startActivityForResult(intent,isCrop?REQUESTCODE_CAMERA_CROP: REQUESTCODE_CAMERA);
    }
    private void pickPhoto() {
        //定义需要保存裁剪图片的路径
        File file = new File(Environment.getExternalStorageDirectory(), "/customCameraFile/");
        if (!file.exists()) {
            file.mkdirs();
        }
        mPhotoFile = new File(file, System.currentTimeMillis() + ".jpg");
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_PICK_PHOTO);
    }
    /**
     * 图库裁剪
     * */
    private void takeCrop(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 300);
        intent.putExtra("outputY", 300);
        intent.putExtra("scale", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true); // no face detection
        startActivityForResult(intent, REQUEST_CROP);

    }
    //拍照定义的File存储路径转换成uri
    private Uri transCameraFile2Uri(File photoFile) {
        Uri photoUri = null;
        if (Build.VERSION.SDK_INT > M) {
            //android7.0
            photoUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            grantUriPermission(getApplicationContext().getPackageName(), photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            photoUri = Uri.fromFile(photoFile);
        }
        return photoUri;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUESTCODE_CAMERA://仅拍照
                //获取uri
                if (mPhotoFile != null) {
                    try {
                        Bitmap bitmapFormUri = compressAndSaveBitmap(mPhotoFile);
                        mImag.setImageBitmap(bitmapFormUri);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case REQUESTCODE_CAMERA_CROP://拍照并裁剪
                //获取uri
                if (mPhotoFile != null) {
                    try {
                        compressAndSaveBitmap(mPhotoFile);
                        //裁剪照片
                        Uri uri = getImageContentUri(this, mPhotoFile);
                        takeCrop(uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                break;
            case REQUEST_CROP://裁剪返回
                if (mPhotoFile != null) {
                    //裁剪返回
                    Bitmap bitmapFormUri = BitmapFactory.decodeFile(mPhotoFile.getAbsolutePath());
                    mImag.setImageBitmap(bitmapFormUri);
                }
                break;
            case REQUEST_PICK_PHOTO://图库选择图片
                if (data != null) {
                    if(isCrop){
                        Uri uri = saveGalleryBitmap(data.getData(), mPhotoFile);
                        takeCrop(uri);
                    }else {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    getContentResolver(), data.getData());
                            FileOutputStream fileOutputStream = new FileOutputStream(mPhotoFile);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                            fileOutputStream.flush();
                            fileOutputStream.close();
                            Bitmap bitmapFormUri = compressAndSaveBitmap(mPhotoFile);
                            mImag.setImageBitmap(bitmapFormUri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
        }
    }

    /**
     * 将Bitmap写入SD卡中的一个文件中,并返回写入文件的Uri
     *
     * @param uri
     * @param file
     * @return
     */
    private Uri saveGalleryBitmap(Uri uri, File file) {
        InputStream is;
        try {
            is = getContentResolver().openInputStream(uri);
            //InputStream ----> Bitmap
            Bitmap bm = BitmapFactory.decodeStream(is);
            //关闭流
            is.close();
            //打开文件输出流
            FileOutputStream fos = new FileOutputStream(file);
            //将bitmap压缩后写入输出流(参数依次为图片格式、图片质量和输出流)
            bm.compress(Bitmap.CompressFormat.PNG, 85, fos);
            //刷新输出流
            fos.flush();
            //关闭输出流
            fos.close();
            //返回File类型的Uri
            return getImageContentUri(getApplicationContext(), file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    /**
     * 通过uri获取图片并进行压缩
     *
     * @param file
     */
    public Bitmap compressAndSaveBitmap(File file) throws FileNotFoundException, IOException {
        Uri uriFromFile = transCameraFile2Uri(file);
        InputStream input = getApplicationContext().getContentResolver().openInputStream(uriFromFile);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        onlyBoundsOptions.inSampleSize = calculateInSampleSize(onlyBoundsOptions, 480, 800);
        //设置缩放比例
        onlyBoundsOptions.inJustDecodeBounds = false;
        input = getApplicationContext().getContentResolver().openInputStream(uriFromFile);
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
        //读取图片角度，纠正角度（针对三星手机拍照会旋转）
        int bitmapDegree = getBitmapDegree(file.getPath());
        image = rotateBitmapByDegree(image, bitmapDegree);
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
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        return bitmap;
    }

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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Configuration o = newConfig;
            o.orientation = Configuration.ORIENTATION_PORTRAIT;
            newConfig.setTo(o);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
        }
        super.onConfigurationChanged(newConfig);
    }
}
