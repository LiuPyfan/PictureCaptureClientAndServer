package com.example.administrator.picturecapture;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.kevin.crop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TakeActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int GALLERY_REQUEST_CODE = 0;    // 相册选图标记
    private static final int CAMERA_REQUEST_CODE = 1;    // 相机拍照标记
    private ImageView picture;

    private String mTempPhotoPath;// 拍照临时图片
    private Uri mDestinationUri;// 剪切后图像文件
    protected static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;
    protected static final int REQUEST_STORAGE_WRITE_ACCESS_PERMISSION = 102;
    private static final String TAG = "TakeActivity";
    private OnPictureSelectedListener mOnPictureSelectedListener;

    private static final MediaType MEDIA_TYPE = MediaType.parse("image/*");// 上传类型
    private final OkHttpClient client = new OkHttpClient();
    private String mImagePath;
    private EditText mUsername;
    private EditText mImagename;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button takeButton = (Button) findViewById(R.id.take_photo);
        Button chooseFromAlbum = (Button) findViewById(R.id.choose_from_album);
        Button uploadBtn = (Button) findViewById(R.id.upload);
        mUsername = (EditText) findViewById(R.id.username_et);
        mImagename = (EditText) findViewById(R.id.imagename_et);
        picture = (ImageView) findViewById(R.id.picture);
        takeButton.setOnClickListener(this);
        chooseFromAlbum.setOnClickListener(this);


        initUri();


        // 设置裁剪图片结果监听
        setOnPictureSelectedListener(new OnPictureSelectedListener() {
            @Override
            public void onPictureSelected(Uri fileUri, Bitmap bitmap) {
                picture.setImageBitmap(bitmap);
                String filePath = fileUri.getEncodedPath();
                mImagePath = Uri.decode(filePath);



            }
        });
uploadBtn.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        if (mImagePath != null){
            uploadImage(mImagePath);
        }else
            Toast.makeText(TakeActivity.this, "请先拍照或选择上传", Toast.LENGTH_SHORT).show();
    }
});


    }

//  上传逻辑
//    private void uploadImage(String imagePath) {
////        String requestUrl = String.format("%s/%s","主机地址","action地址");
////        File file = new File(imagePath,"cropImage.jpeg");
//        File file = new File(imagePath,"photo.jpg");

////        if (!file.exists()) {
////            Log.d(TAG, file.getAbsoluteFile() + "not exist!");
////            Toast.makeText(this, "not exist!", Toast.LENGTH_SHORT).show();
////            return;
////        }
//        RequestBody body = RequestBody.create(MediaType.parse("application/octet-stream"),file);
//
//        Request.Builder  builder = new Request.Builder();
//        Request request = null;
//        try {
//            request = builder.url("http://192.168.1.22:8080/okhttp/postFile").removeHeader("User-Agent")
//                    .addHeader("User-Agent", "HLFROBOT" )
//                    .addHeader("Host", "localhost:8080"  )
//                    .addHeader("Connection", "Keep-Alive" )
//                    .addHeader("Accept-Encoding", "gzip")
//                    .addHeader("Content-Length", String.valueOf(body.contentLength()))
//                    .post(body)
//                    .build();
//
////            request = builder.url("http://192.168.1.22:8080/okhttp/postFile")
////                    .post(body)
////                    .build();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//        //发送请求获取响应
//        try {
//            Response response = client.newCall(request).execute();
//            Log.d("getResponse", "response.code():" + response.code());
//            //判断请求是否成功
//            if (response.isSuccessful()) {
//                //打印服务端返回结果
//                Log.e("getResponse", response.code() + "postJson: 1");
//
//            }
//        } catch (IOException e) {
//
//            e.printStackTrace();
//        }
//
//    }
//// TODO: 2017/7/19  cropimage
    private void initUri() {
        mDestinationUri = Uri.fromFile(new File(this.getCacheDir(), "cropImage.jpeg"));
        mTempPhotoPath = Environment.getExternalStorageDirectory() + File.separator + "photo.jpeg";
    }

//    public interface OnSelectedListener {
//        void OnSelected(View v, int position);
//    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.take_photo:
                takePhoto();
                break;
            case R.id.choose_from_album:
                pickFromGallery();
                break;
            default:
                break;
        }
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_STORAGE_READ_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    pickFromGallery();
                }
                break;
            case REQUEST_STORAGE_WRITE_ACCESS_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void takePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN // Permission was added in API Level 16
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    "选择图片时需要读取权限",
                    REQUEST_STORAGE_WRITE_ACCESS_PERMISSION);
        } else {

            Intent takeIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //下面这句指定调用相机拍照后的照片存储的路径
            takeIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(mTempPhotoPath)));
            startActivityForResult(takeIntent, CAMERA_REQUEST_CODE);
        }
    }

    private void pickFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN // Permission was added in API Level 16
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    "拍照时需要存储权限",
                    REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {

            Intent pickIntent = new Intent(Intent.ACTION_PICK, null);
            // 如果限制上传到服务器的图片类型时可以直接写如："image/jpeg 、 image/png等的类型"
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            startActivityForResult(pickIntent, GALLERY_REQUEST_CODE);
        }
    }

    /**
     * 请求权限
     * <p>
     * 如果权限被拒绝过，则提示用户需要权限
     */
    protected void requestPermission(final String permission, String rationale, final int requestCode) {
        if (shouldShowRequestPermissionRationale(permission)) {
            Toast.makeText(this, "稍后申请权限", Toast.LENGTH_SHORT).show();
//            showAlertDialog(getString(R.string.permission_title_rationale), rationale,
//                    new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            requestPermissions(new String[]{permission}, requestCode);
//                        }
//                    }, getString(R.string.label_ok), null, getString(R.string.label_cancel));
        } else {
            requestPermissions(new String[]{permission}, requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_REQUEST_CODE:   // 调用相机拍照
                    File temp = new File(mTempPhotoPath);
                    startCropActivity(Uri.fromFile(temp));
                    break;
                case GALLERY_REQUEST_CODE:  // 直接从相册获取
                    startCropActivity(data.getData());
                    break;
                case UCrop.REQUEST_CROP:    // 裁剪图片结果
                    handleCropResult(data);
                    break;
                case UCrop.RESULT_ERROR:    // 裁剪图片错误
                    handleCropError(data);
                    break;
            }
        }
    }

    /**
     * 裁剪图片方法实现
     *
     * @param uri
     */
    public void startCropActivity(Uri uri) {
        UCrop.of(uri, mDestinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(512, 512)
                .withTargetActivity(CropActivity.class)
                .start(TakeActivity.this);
    }

    /**
     * 处理剪切成功的返回值
     *
     * @param result
     */
    private void handleCropResult(Intent result) {
        deleteTempPhotoFile();
        final Uri resultUri = UCrop.getOutput(result);
        if (null != resultUri && null != mOnPictureSelectedListener) {
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(TakeActivity.this.getContentResolver(), resultUri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mOnPictureSelectedListener.onPictureSelected(resultUri, bitmap);
        } else {
            Toast.makeText(this, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 处理剪切失败的返回值
     *
     * @param result
     */
    private void handleCropError(Intent result) {
        deleteTempPhotoFile();
        final Throwable cropError = UCrop.getError(result);
        if (cropError != null) {
            Log.e(TAG, "handleCropError: ", cropError);
            Toast.makeText(this, cropError.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "无法剪切选择图片", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 删除拍照临时文件
     */
    private void deleteTempPhotoFile() {
        File tempFile = new File(mTempPhotoPath);
        if (tempFile.exists() && tempFile.isFile()) {
            tempFile.delete();
        }
    }

    /**
     * 设置图片选择的回调监听
     *
     * @param l
     */
    public void setOnPictureSelectedListener(OnPictureSelectedListener l) {
        this.mOnPictureSelectedListener = l;
    }

    /**
     * 图片选择的回调接口
     */
    public interface OnPictureSelectedListener {
        /**
         * 图片选择的监听回调
         *
         * @param fileUri
         * @param bitmap
         */
        void onPictureSelected(Uri fileUri, Bitmap bitmap);
    }


    private String doPost(String imagePath) {
        OkHttpClient mOkHttpClient = new OkHttpClient();

        String result = "error";
//        单纯传文件
//        File file = new File(imagePath);
//        if (!file.exists()) {
//            Log.d(TAG, "doPost: " + file.getAbsolutePath() + " not exist");
//        }
//        RequestBody requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), file);

        // TODO: 2017/7/19
//       MultipartBody.Builder builder = new MultipartBody.Builder();
//        // 这里演示添加用户ID
//        builder.addFormDataPart("userId", "20160519142605");
//        builder.addFormDataPart("image", imagePath,
//                RequestBody.create(MediaType.parse("image/jpeg"), new File(imagePath)));
//        RequestBody requestBody = builder.build();
        // TODO: 2017/7/19
        // TODO: 2017/7/19  upload

        File file = new File(imagePath);
        if (!file.exists()) {
            Log.d(TAG, "doPost: " + file.getAbsolutePath() + " not exist");
        }


        MultipartBody.Builder builder = new MultipartBody.Builder();
        RequestBody requestBody;
        if (mUsername != null && mImagename != null && mImagePath != null){

            requestBody = builder.setType(MultipartBody.FORM)
                    .addFormDataPart("username",mUsername.getText().toString())
                    .addFormDataPart("mPhoto",mImagename.getText().toString() +".jpeg", RequestBody.create(MediaType.parse("application/octet-stream"), file))
                    .build();
        }else {
            requestBody = builder.setType(MultipartBody.FORM)
                    .addFormDataPart("username","默认的用户名")
                    .addFormDataPart("mPhoto","默认上传的图片名.jpeg", RequestBody.create(MediaType.parse("application/octet-stream"), file))
                    .build();
        }

        String contentLength = null;

        try {
            // 单纯传文件
            //"http://192.168.1.22:8080/okhttp/postFile"
            if (requestBody != null){
                contentLength = String.valueOf(requestBody.contentLength());
            }
            Request.Builder reqBuilder = new Request.Builder();
            Request request = reqBuilder
                    .url("http://192.168.1.22:8080/okhttp/uploadInfo")
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", "HLFROBOT" )
                    .addHeader("Host", "localhost:8080"  )
                    .addHeader("Connection", "Keep-Alive" )
                    .addHeader("Accept-Encoding", "gzip")
                    .addHeader("Content-Length", contentLength)
                    .post(requestBody)
                    .build();
            Response response = mOkHttpClient.newCall(request).execute();
            Log.d(TAG, "响应码 " + response.code());
            if (response.isSuccessful()) {
                String resultValue = response.body().string();
                Log.d(TAG, "响应体 " + resultValue);
                return resultValue;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 上传图片
     *
     * @param imagePath
     */
    private void uploadImage(String imagePath) {
        new NetworkTask().execute(imagePath);
    }

    /**
     * 访问网络AsyncTask,访问网络在子线程进行并返回主线程通知访问的结果
     */
    class NetworkTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            return doPost(params[0]);
        }


    }

}




