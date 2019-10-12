package com.iview.mirromove.view;

import android.content.Context;

/**
 * Created by llm on 18-12-15.
 */

public class DensityUtils {
    public static int dp2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dpValue * scale + 0.5f);
    }

    public static int px2dp(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return  (int)(pxValue/scale + 0.5f);
    }
}
