package com.rainy.yupdate;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * time:2018-12-26 12:18
 * description:
 *
 * @author yueleilei
 */
public class YUpdate {
    private String mUrl;
    private int mVersionCode;
    private String mRequestMethod;
    private Context mContext;

    private YUpdate(Builder builder) {
        this.mUrl = builder.url;
        this.mVersionCode = builder.versionCode;
        this.mRequestMethod = builder.requestMethod;
        this.mContext = builder.context;
    }

    public static class Builder {
        private String url;
        private int versionCode;
        private String requestMethod = "GET";
        private Context context;

        public Builder(Context context) {
            this.context = context;
            this.versionCode = getVersionCode(context);
        }

        private int getVersionCode(Context context) {
            int versionCode = 0;
            try {
                PackageManager packageManager = context.getPackageManager();
                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), PackageManager.GET_UNINSTALLED_PACKAGES);
                versionCode = packageInfo.versionCode;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return versionCode;
        }

        public Builder get() {
            this.requestMethod = "GET";
            return this;
        }

        public Builder post() {
            this.requestMethod = "POST";
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public YUpdate create() {
            return new YUpdate(this);
        }
    }

    public void checkNewApp() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(mUrl);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod(mRequestMethod);
                    con.setRequestProperty("Charset", "UTF-8");
                    con.setConnectTimeout(15_000);
                    con.setReadTimeout(15_000);
                    con.connect();
                    int responseCode = con.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        final String result = is2String(con.getInputStream());
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                processResult(result);
                            }
                        });
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processResult(String result) {
        try {
            JSONObject jsonObject = new JSONObject(result);
            JSONObject data = jsonObject.optJSONObject("result_info");
            int versionCode = data.optInt("versionCode");
            String versionName = data.optString("versionName");
            boolean isForceUpdate = data.optBoolean("isForceUpdate");
            String describe = data.optString("describe");
            String title = data.optString("updateTitle");
            String downloadUrl = data.optString("url");
            if (this.mVersionCode < versionCode) {//发现新版本
                Intent intent = new Intent(mContext, UpdateActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("versionCode", versionCode);
                intent.putExtra("versionName", versionName);
                intent.putExtra("isForceUpdate", isForceUpdate);
                intent.putExtra("describe", describe);
                intent.putExtra("title", title);
                intent.putExtra("downloadUrl", downloadUrl);
                mContext.startActivity(intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String is2String(InputStream is) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int len;
        try {
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeStream(bos);
            closeStream(is);
        }
        return "";
    }

    private void closeStream(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
