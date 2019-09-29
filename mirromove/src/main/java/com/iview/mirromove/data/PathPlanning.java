package com.iview.mirromove.data;

import org.json.JSONException;
import org.json.JSONObject;

public class PathPlanning {
    private String action;
    private int angle;  // 马达移动方向，顺时针
    private int rotateAngle; //视频或图片旋转角度
    private String url;
    private int imgDisplayTime;
    private int keystone;

    public int getKeystone() {
        return keystone;
    }

    public void setKeystone(int keystone) {
        this.keystone = keystone;
    }

    public int getImgDisplayTime() {
        return imgDisplayTime;
    }

    public void setImgDisplayTime(int imgDisplayTime) {
        this.imgDisplayTime = imgDisplayTime;
    }

    public PathPlanning(String action, int angle, int rotateAngle, String url, int imgDisplayTime, int keystone) {
        this.action = action;
        this.angle = angle;
        this.rotateAngle = rotateAngle;
        this.url = url;
        this.imgDisplayTime = imgDisplayTime;
        this.keystone = keystone;
    }

    public PathPlanning(JSONObject object) {
        action = object.optString("action", "");
        angle = object.optInt("angle", 0);
        rotateAngle = object.optInt("rotateAngle",  0);
        url = object.optString("url", "");
        imgDisplayTime = object.optInt("imgDisplayTime", 0);
        keystone = object.optInt("keystone", 0);
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getRotateAngle() {
        return rotateAngle;
    }

    public void setRotateAngle(int rotateAngle) {
        this.rotateAngle = rotateAngle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JSONObject toJSON() {
        JSONObject object = new JSONObject();
        try {
            object.put("action", action);
            object.put("angle", angle);
            object.put("rotateAngle", rotateAngle);
            object.put("url", url);
            object.put("imgDisplayTime", imgDisplayTime);
            object.put("keystone", keystone);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }
}
