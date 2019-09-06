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
}
