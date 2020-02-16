package com.gordan.baselibrary.net;

import android.util.Log;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 网络请求的工具是否好用 在网络不好的情况下最能体现出来，
 * <p>
 * 网速快 没问题的时候基本看不出来优劣
 * <p>
 * APP装到自己手机上看没问题是基本 APP到底好不好装到他人手机上就看出优劣来了
 */

public class RequestManager {
    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");//mdiatype 这个需要和服务端保持一致
    public static final MediaType FORM_CONTENT_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("text/x-markdown; charset=utf-8");//mdiatype 这个需要和服务端保持一致
    private static final String TAG = RequestManager.class.getSimpleName();
    private static RequestManager mInstance;//单利引用
    public static final int TYPE_GET = 0;//get请求
    public static final int TYPE_POST_JSON = 1;//post请求参数为json
    public static final int TYPE_POST_FORM = 2;//post请求参数为表单
    private OkHttpClient mOkHttpClient = new OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS).build();

    private RequestManager() {
    }

    public static RequestManager getInstance() {
        if (mInstance == null) {
            synchronized (RequestManager.class) {
                mInstance = new RequestManager();
            }
        }
        return mInstance;
    }

    /**
     * 统一为请求添加头信息
     * <p>
     * 如在Header中添加版本控制信息或者Cookie身份验证信息等
     *
     * @return
     */
    private Request.Builder addHeaders() {
        Request.Builder builder = new Request.Builder()
                .addHeader("Connection", "keep-alive");
//                .addHeader("Cookie", cookie);
//                .addHeader("platform", "2")
//                .addHeader("phoneModel", Build.MODEL)
//                .addHeader("systemVersion", Build.VERSION.RELEASE)
//                .addHeader("appVersion", "3.2.0");
        return builder;
    }

    /**
     * okHttp异步请求统一入口
     *
     * @param actionUrl   接口地址
     * @param requestType 请求类型
     * @param paramsMap   请求参数
     * @param callBack    请求返回数据回调
     **/
    public Call requestAsyn(String actionUrl, int requestType, HashMap<String, Object> paramsMap, Callback callBack) {
        Call call;
        switch (requestType) {
            case TYPE_GET:
                call = requestGetByAsyn(actionUrl, paramsMap, callBack);
                break;
            case TYPE_POST_JSON:
                call = requestPostByAsyn(actionUrl, paramsMap, callBack);
                break;
            case TYPE_POST_FORM:
                call = requestPostByAsynWithForm(actionUrl, paramsMap, callBack);
                break;
            default:
                call = null;
                break;
        }
        return call;
    }

    /**
     * okHttp同步请求统一入口
     *
     * @param actionUrl   接口地址
     * @param requestType 请求类型
     * @param paramsMap   请求参数
     **/
    public String requestSyn(String actionUrl, int requestType, HashMap<String, String> paramsMap) {
        String response = null;
        switch (requestType) {
            case TYPE_GET:
                response = requestGetByAsyn(actionUrl, paramsMap);
                break;
            case TYPE_POST_JSON:
                response = requestPostByAsyn(actionUrl, paramsMap);
                break;
            case TYPE_POST_FORM:
                response = requestPostByAsynWithForm(actionUrl, paramsMap);
                break;
        }
        return response;
    }

    /**
     * okHttp get异步请求
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @return
     */
    private Call requestGetByAsyn(String actionUrl, HashMap<String, Object> paramsMap, Callback callBack) {
        StringBuilder tempParams = new StringBuilder();
        String requestUrl = "";
        try {
            if (paramsMap != null) {
                int pos = 0;
                for (String key : paramsMap.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key)+"", "utf-8")));
                    pos++;
                }
                requestUrl = String.format("%s?%s", actionUrl, tempParams.toString());
            } else {
                requestUrl = actionUrl;
            }
            Log.d(TAG, "=====requestGetByAsyn=======" + requestUrl);
            Request request = addHeaders().url(requestUrl).build();
            Call call = mOkHttpClient.newCall(request);
            call.enqueue(callBack);
            return call;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }


    /**
     * okHttp get同步请求
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @return
     */
    private String requestGetByAsyn(String actionUrl, HashMap<String, String> paramsMap) {
        StringBuilder tempParams = new StringBuilder();
        String requestUrl = "";
        try {
            if (paramsMap != null) {
                int pos = 0;
                for (String key : paramsMap.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                    pos++;
                }
                requestUrl = String.format("%s?%s", actionUrl, tempParams.toString());

            } else {
                requestUrl = actionUrl;
            }

            Log.d(TAG, "requesturl:" + requestUrl);
            final Request request = addHeaders().url(requestUrl).build();
            final Call call = mOkHttpClient.newCall(request);
            String result = call.execute().body().string();
            Log.e(TAG, result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }

    /**
     * okHttp post异步请求
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @return
     */
    private Call requestPostByAsyn(String actionUrl, HashMap<String, Object> paramsMap, Callback callBack) {
        try {
            Log.i(TAG, "======requestPostByAsyn=======" + actionUrl);
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key)+"", "utf-8")));
                pos++;
            }
            String params = tempParams.toString();
            Log.i(TAG, "===param===" + params);
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, params);
            Request request = addHeaders().url(actionUrl).post(body).build();
            Call call = mOkHttpClient.newCall(request);
            call.enqueue(callBack);
            return call;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * okHttp post同步请求
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @return
     */
    private String requestPostByAsyn(String actionUrl, HashMap<String, String> paramsMap) {
        try {
            StringBuilder tempParams = new StringBuilder();
            int pos = 0;
            for (String key : paramsMap.keySet()) {
                if (pos > 0) {
                    tempParams.append("&");
                }
                tempParams.append(String.format("%s=%s", key, URLEncoder.encode(paramsMap.get(key), "utf-8")));
                pos++;
            }
            String params = tempParams.toString();
            RequestBody body = RequestBody.create(MEDIA_TYPE_JSON, params);
            Request request = addHeaders().url(actionUrl).post(body).build();
            Call call = mOkHttpClient.newCall(request);
            Response response = call.execute();
            return response.body().string();
        } catch (Exception e) {
            int e1 = Log.e(TAG, e.toString());

            return null;
        }
    }

    /**
     * okHttp post异步请求表单提交
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @param callBack  请求返回数据回调
     * @return
     */
    private Call requestPostByAsynWithForm(String actionUrl, HashMap<String, Object> paramsMap, Callback callBack) {
        try {
            Log.i(TAG, "======requestPostByAsynWithForm======" + actionUrl);
            FormBody.Builder builder = new FormBody.Builder();
            for (String key : paramsMap.keySet()) {
                builder.add(key, paramsMap.get(key)+"");
                Log.i(TAG, key + "===param===" + paramsMap.get(key));
            }
            RequestBody formBody = builder.build();
            Request request = addHeaders().url(actionUrl).post(formBody).build();
            Call call = mOkHttpClient.newCall(request);
            call.enqueue(callBack);
            return call;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return null;
    }


    /**
     * okHttp post同步请求表单提交
     *
     * @param actionUrl 接口地址
     * @param paramsMap 请求参数
     * @return
     */
    private String requestPostByAsynWithForm(String actionUrl, HashMap<String, String> paramsMap) {
        try {
            FormBody.Builder builder = new FormBody.Builder();
            for (String key : paramsMap.keySet()) {
                builder.add(key, paramsMap.get(key));
            }
            RequestBody formBody = builder.build();
            Request request = addHeaders().url(actionUrl).post(formBody).build();
            Call call = mOkHttpClient.newCall(request);
            Response response = call.execute();
            return response.body().string();
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            return null;
        }
    }


}
