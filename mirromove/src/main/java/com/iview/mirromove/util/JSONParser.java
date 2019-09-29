package com.iview.mirromove.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONParser {
    private final static String TAG = "JSONParser";

    private String mMessage;
    private JSONObject mJsonObject;

    public JSONParser(String msg) {
        mMessage = msg;
        try {
            mJsonObject = new JSONObject(mMessage);
        } catch (JSONException e) {
            Log.e(TAG, "Get message extra JSON error!");
            mJsonObject = null;
        }
    }

    public String getAction() {
        String action = null;
        if (mJsonObject != null) {
            action = mJsonObject.optString("action");
        }

        return action;
    }

    public String getType() {
        String action = null;
        if (mJsonObject != null) {
            action = mJsonObject.optString("type");
        }

        return action;
    }

    public int getAngle() {
        int angle = 0;

        JSONObject jsonObject = null;
        if (mJsonObject != null) {
            try {
                jsonObject = mJsonObject .getJSONObject("arg");
                angle = jsonObject.optInt("angle");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return angle;
    }

    public int getRotation() {
        int rotation = 0;
        JSONObject jsonObject = null;
        if (mJsonObject != null) {
            try {
                jsonObject = mJsonObject .getJSONObject("arg");
                rotation =  jsonObject.optInt("rotation");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return rotation;
    }

    public String getUrl() {
        String url = null;
        JSONObject jsonObject = null;
        if (mJsonObject != null) {
            try {
                jsonObject = mJsonObject .getJSONObject("arg");
                url = jsonObject.optString("url");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return url;
    }

    public int getShowTime() {
        int showTime = 0;
        JSONObject jsonObject = null;
        if (mJsonObject != null) {
            try {
                jsonObject = mJsonObject .getJSONObject("arg");
                showTime = jsonObject.optInt("imgTime");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return showTime;
    }

    public int getKeystone() {
        int keystone = 0;
        JSONObject jsonObject = null;
        if (mJsonObject != null) {
            try {
                jsonObject = mJsonObject .getJSONObject("arg");
                keystone = jsonObject.optInt("keystone", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return keystone;
    }

}
