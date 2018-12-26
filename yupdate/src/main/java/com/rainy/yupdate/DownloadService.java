package com.rainy.yupdate;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * time:2018-12-26 15:09
 * description:
 *
 * @author yueleilei
 */
public class DownloadService extends Service {
    private DownloadBind mBind = new DownloadBind();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) {
        return mBind;
    }

    private long totalLength;
    private float currentLength;
    private boolean isDownloading;

    public class DownloadBind extends Binder {
        public void startDownload(final String downloadUrl, final OnDownloadListener listener) {
            if (isDownloading) return;
            isDownloading = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL(downloadUrl);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");
                        con.setRequestProperty("Charset", "UTF-8");
                        con.setConnectTimeout(15_000);
                        con.setReadTimeout(15_000);
                        con.connect();
                        totalLength = con.getContentLength();
                        final File file = is2File(con.getInputStream(), listener);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onFinish(file);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        isDownloading = false;
                    }
                }
            }).start();
        }

        private File is2File(InputStream is, final OnDownloadListener listener) {
            File file = new File(DownloadService.this.getExternalCacheDir(), "update.apk");
            try {
                FileOutputStream fos = new FileOutputStream(file);
                if (file.exists()) file.delete();
                byte[] buffer = new byte[2048];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                    fos.flush();
                    currentLength += len;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onProgress(currentLength, totalLength);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return file;
        }
    }

    public interface OnDownloadListener {
        void onProgress(float progress, long totalSize);

        void onFinish(File file);
    }
}
