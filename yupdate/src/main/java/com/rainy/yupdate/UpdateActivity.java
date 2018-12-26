package com.rainy.yupdate;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class UpdateActivity extends Activity {

    private int versionCode;
    private String versionName;
    private boolean isForceUpdate;
    private String describe;
    private String title;
    private String downloadUrl;
    private Dialog dialog;
    private File apkFile;
    private boolean isFinish;
    private DownloadService.OnDownloadListener onDownloadListener = new DownloadService.OnDownloadListener() {
        @Override
        public void onProgress(float progress, long totalSize) {
            tvCancel.setVisibility(View.GONE);
            tvConfirm.setText(((int) (progress * 100 / totalSize)) + "%");
            tvConfirm.setEnabled(false);
        }

        @Override
        public void onFinish(File file) {
            tvConfirm.setEnabled(true);
            tvConfirm.setText("安装");
            isFinish = true;
            apkFile = file;
            installApk(file);
        }
    };
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof DownloadService.DownloadBind) {
                DownloadService.DownloadBind binder = (DownloadService.DownloadBind) service;
                binder.startDownload(downloadUrl, onDownloadListener);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private TextView tvConfirm;
    private TextView tvCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_update);
        versionCode = getIntent().getIntExtra("versionCode", 0);
        versionName = getIntent().getStringExtra("versionName");
        isForceUpdate = getIntent().getBooleanExtra("isForceUpdate", false);
        describe = getIntent().getStringExtra("describe");
        title = getIntent().getStringExtra("title");
        downloadUrl = getIntent().getStringExtra("downloadUrl");
        showDialog();
    }

    private void showDialog() {
        dialog = new Dialog(this, R.style.updateDialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        View view = View.inflate(this, R.layout.dialog, null);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDescribe = view.findViewById(R.id.tvDescribe);
        tvConfirm = view.findViewById(R.id.tvConfirm);
        tvCancel = view.findViewById(R.id.tvCancel);
        tvCancel.setVisibility(isForceUpdate ? View.GONE : View.VISIBLE);
        tvConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFinish && apkFile != null) {
                    installApk(apkFile);
                } else {
                    startDownload();
                }
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissDialog();
            }
        });
        tvTitle.setText(title);
        tvDescribe.setText(describe);
        dialog.setContentView(view);
        dialog.show();
    }

    private void startDownload() {
        Intent intent = new Intent(this, DownloadService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (conn != null) {
            unbindService(conn);
        }
        super.onDestroy();
    }

    /**
     * 兼容7.0
     */
    public void installApk(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // 由于没有在Activity环境下启动Activity,设置下面的标签
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 24) { //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        startActivity(intent);
    }
}
