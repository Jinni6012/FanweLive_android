package com.fanwe.shortvideo.activity;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fanwe.hybrid.activity.BaseActivity;
import com.fanwe.hybrid.http.AppRequestCallback;
import com.fanwe.hybrid.model.BaseActModel;
import com.fanwe.library.adapter.http.model.SDResponse;
import com.fanwe.live.R;
import com.fanwe.live.common.CommonInterface;
import com.fanwe.live.utils.GlideUtil;
import com.fanwe.shortvideo.common.utils.FileUtils;
import com.fanwe.shortvideo.common.utils.TCConstants;
import com.fanwe.shortvideo.editor.TCVideoPreprocessActivity;
import com.fanwe.shortvideo.model.SignModel;
import com.fanwe.shortvideo.videoupload.TXUGCPublish;
import com.fanwe.shortvideo.videoupload.TXUGCPublishTypeDef;
import com.fanwei.jubaosdk.common.util.ToastUtil;

import org.xutils.view.annotation.ViewInject;

import java.io.File;


public class UpLoadVideoActivity extends BaseActivity implements View.OnClickListener {
    private static String TAG = "UpLoadVideoActivity";

    @ViewInject(R.id.img_bg)
    private ImageView img_bg;
    @ViewInject(R.id.img_back)
    private ImageView img_back;
    @ViewInject(R.id.img_edit)
    private ImageView img_edit;
    @ViewInject(R.id.edit_video_comment)
    private EditText edit_video_comment;
    @ViewInject(R.id.img_video_cover)
    private ImageView img_video_cover;
    @ViewInject(R.id.tv_save_local)
    private TextView tv_save_local;
    @ViewInject(R.id.tv_upload_video)
    private TextView tv_upload_video;
    private int record_type;
    private String videoPath;
    private String coverPath;
    private String resolution;
    private long duration;

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.img_back:
                clickBack();
                break;
            case R.id.img_edit:
                startEditVideo();
                break;
            case R.id.tv_save_local:
                downloadRecord();
                break;
            case R.id.tv_upload_video:
                requestSignData();
                break;
        }
    }

    private void clickBack() {
        new AlertDialog.Builder(this).setMessage("删除并重新录制？")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                }).setPositiveButton("确定", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteVideo();
            }
        }).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_upload_video);
        getIntentData();
        initView();
        initListener();
//        requestData(mVideoIdList.get(mCurrentItem));

    }

    private void getIntentData() {
        record_type = getIntent().getIntExtra(TCConstants.VIDEO_RECORD_TYPE, 0);
        videoPath = getIntent().getStringExtra(TCConstants.VIDEO_EDITER_PATH);
        coverPath = getIntent().getStringExtra(TCConstants.VIDEO_RECORD_COVERPATH);
        resolution = getIntent().getStringExtra(TCConstants.VIDEO_RECORD_RESOLUTION);
        duration = getIntent().getLongExtra(TCConstants.VIDEO_RECORD_DURATION, 0);
    }

    private void initView() {
        File file = new File(coverPath);
        GlideUtil.load(file).into(img_video_cover);
        GlideUtil.load(file).into(img_bg);

    }

    private void initListener() {
        img_back.setOnClickListener(this);
        img_edit.setOnClickListener(this);
        tv_save_local.setOnClickListener(this);
        tv_upload_video.setOnClickListener(this);

    }

    private void deleteVideo() {
        //删除文件
        FileUtils.deleteFile(videoPath);
        FileUtils.deleteFile(coverPath);
        finish();
    }

    private void startEditVideo() {
        Intent intent = new Intent(this, TCVideoPreprocessActivity.class);
        intent.putExtra(TCConstants.VIDEO_RECORD_TYPE, TCConstants.VIDEO_RECORD_TYPE_EDIT);
        intent.putExtra(TCConstants.VIDEO_EDITER_PATH, videoPath);
        intent.putExtra(TCConstants.VIDEO_RECORD_COVERPATH, coverPath);
        intent.putExtra(TCConstants.VIDEO_RECORD_RESOLUTION, resolution);
        startActivity(intent);
        finish();
    }

    private void downloadRecord() {
        File file = new File(videoPath);
        if (file.exists()) {
            try {
                File newFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + File.separator + file.getName());
//                if (!newFile.exists()) {
//                    newFile = new File(getExternalFilesDir(Environment.DIRECTORY_DCIM).getPath() + File.separator + file.getName());
//                }

                file.renameTo(newFile);
                videoPath = newFile.getAbsolutePath();

                ContentValues values = initCommonContentValues(newFile);
                values.put(MediaStore.Video.VideoColumns.DATE_TAKEN, System.currentTimeMillis());
                values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                values.put(MediaStore.Video.VideoColumns.DURATION, duration);//时长
                this.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

                insertVideoThumb(newFile.getPath(), coverPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
            finish();
        }
    }

    /**
     * 插入视频缩略图
     *
     * @param videoPath
     * @param coverPath
     */
    private void insertVideoThumb(String videoPath, String coverPath) {
        //以下是查询上面插入的数据库Video的id（用于绑定缩略图）
        //根据路径查询
        Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Thumbnails._ID},//返回id列表
                String.format("%s = ?", MediaStore.Video.Thumbnails.DATA), //根据路径查询数据库
                new String[]{videoPath}, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                String videoId = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Thumbnails._ID));
                //查询到了Video的id
                ContentValues thumbValues = new ContentValues();
                thumbValues.put(MediaStore.Video.Thumbnails.DATA, coverPath);//缩略图路径
                thumbValues.put(MediaStore.Video.Thumbnails.VIDEO_ID, videoId);//video的id 用于绑定
                //Video的kind一般为1
                thumbValues.put(MediaStore.Video.Thumbnails.KIND,
                        MediaStore.Video.Thumbnails.MINI_KIND);
                //只返回图片大小信息，不返回图片具体内容
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                Bitmap bitmap = BitmapFactory.decodeFile(coverPath, options);
                if (bitmap != null) {
                    thumbValues.put(MediaStore.Video.Thumbnails.WIDTH, bitmap.getWidth());//缩略图宽度
                    thumbValues.put(MediaStore.Video.Thumbnails.HEIGHT, bitmap.getHeight());//缩略图高度
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
                this.getContentResolver().insert(
                        MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, //缩略图数据库
                        thumbValues);
            }
            cursor.close();
        }
    }

    private static ContentValues initCommonContentValues(File saveFile) {
        ContentValues values = new ContentValues();
        long currentTimeInSeconds = System.currentTimeMillis();
        values.put(MediaStore.MediaColumns.TITLE, saveFile.getName());
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, saveFile.getName());
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, currentTimeInSeconds);
        values.put(MediaStore.MediaColumns.DATE_ADDED, currentTimeInSeconds);
        values.put(MediaStore.MediaColumns.DATA, saveFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.SIZE, saveFile.length());

        return values;
    }

    private void publish(String sign) {
        TXUGCPublish txugcPublish = new TXUGCPublish(this.getApplicationContext(), "customID");
        txugcPublish.setListener(new TXUGCPublishTypeDef.ITXVideoPublishListener() {
            @Override
            public void onPublishProgress(long uploadBytes, long totalBytes) {

            }

            @Override
            public void onPublishComplete(TXUGCPublishTypeDef.TXPublishResult result) {
                requestData(result.videoURL,result.coverURL);

            }
        });

        TXUGCPublishTypeDef.TXPublishParam param = new TXUGCPublishTypeDef.TXPublishParam();
        // signature计算规则可参考 https://www.qcloud.com/document/product/266/9221
        param.signature = sign;
        param.videoPath = videoPath;
        param.coverPath = coverPath;
        txugcPublish.publishVideo(param);
    }

    protected void requestSignData() {
        CommonInterface.requestUpLoadSign(new AppRequestCallback<SignModel>() {
            @Override
            protected void onSuccess(SDResponse sdResponse) {
                if (actModel.isOk()) {
                    publish(actModel.sign);
                }
            }

            @Override
            protected void onFinish(SDResponse resp) {
                super.onFinish(resp);
            }
        });

    }

    protected void requestData(String videoPath,String coverPath) {
        CommonInterface.requestUpLoadVideo(videoPath, coverPath, edit_video_comment.getText().toString(), new AppRequestCallback<BaseActModel>() {
            @Override
            protected void onSuccess(SDResponse sdResponse) {
                if (actModel.isOk()) {
                    ToastUtil.showToast(UpLoadVideoActivity.this,"发布成功", Toast.LENGTH_LONG);
                    finish();
                }
            }

            @Override
            protected void onFinish(SDResponse resp) {
                super.onFinish(resp);
            }
        });

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            clickBack();
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}
