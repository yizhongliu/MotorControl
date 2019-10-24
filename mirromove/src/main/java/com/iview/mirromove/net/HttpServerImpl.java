package com.iview.mirromove.net;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;

import com.iview.mirromove.ControlService;
import com.iview.mirromove.MirroApplication;
import com.iview.mirromove.util.CommandListProvider;
import com.iview.mirromove.util.FileUtils;
import com.iview.mirromove.util.MediaFileUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

import static android.media.MediaMetadataRetriever.METADATA_KEY_DURATION;
import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT;
import static android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH;
import static android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC;

/**
 *
 * @author lixm
 *
 */
public class HttpServerImpl extends NanoHTTPD {

    //  http://172.22.158.31:8080/getFileList?dirPath=/sdcard
    //  http://172.22.158.31:8080/getFile?fileName=/sdcard/FaceFingerMatch_AD

    public static final int DEFAULT_SERVER_PORT = 8093;
    public static final String TAG = "HttpServerImpl";

    private static final String REQUEST_ROOT = "/";
    private static final String REQUEST_TEST = "/test";
    private static final String REQUEST_ACTION_GET_FILE = "/getFile";
    private static final String REQUEST_ACTION_GET_FILE_LIST = "/getFileList";
    private static final String REQUEST_ACTION_SAMPLE = "/sample";

    private static final String GET_PATH_LIST = "/getPathlist";
    private static final String DEL_PATH_LIST = "/delPathlist";
    private static final String GET_DEV_STATE = "/getDeviceState";

    private String mBasePath;
    private Context mContext;

    public HttpServerImpl(String basePath, Context context) {
        super(DEFAULT_SERVER_PORT);

        mBasePath = basePath;

        mContext = context.getApplicationContext();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Response serve(IHTTPSession session) {
        String strUri = session.getUri();
        Method method = session.getMethod();

        Log.e(TAG,"Response serve uri = " + strUri + ", method = " + method.name());
        Log.e(TAG, "getInputStream:" + session.getInputStream().toString() + ", head:" + session.getHeaders().toString() + ", getParam" + session.getParms().toString() + ", queryString" + session.getQueryParameterString());

        String absPath = mBasePath + strUri;
        File file = new File(absPath);

        if (Method.POST.equals(method) || Method.PUT.equals(method)) {

            if (DEL_PATH_LIST.equals(strUri)) {
                return responseDelPathlist(session);
            } else {
                Log.e(TAG, "get post method");
                Map<String, String> files = new HashMap<>();
                try {
                    session.parseBody(files);
                } catch (IOException ioe) {
                    Log.e(TAG, "return io exception");
                    return newFixedLengthResponse("Internal Error IO Exception: " + ioe.getMessage());
                } catch (ResponseException re) {
                    Log.e(TAG, "return resopn Exception");
                    return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
                }

                Map<String, String> params = session.getParms();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    final String paramsKey = entry.getKey();
                    Log.e(TAG, "get paramKey :" + paramsKey + ",value" + entry.getValue());
                    if (paramsKey.contains("filename_1")) {
                        final String tmpFilePath = files.get(paramsKey);
                        Log.e(TAG, "tmpFilePath:" + tmpFilePath);
                        final String fileName = entry.getValue();
                        final File tmpFile = new File(tmpFilePath);
                        final File targetFile = new File(mBasePath + "/" + fileName);
                        Log.e(TAG, "copy file now, source file path: " + tmpFile.getAbsoluteFile() + ",target file path:" +  targetFile.getAbsoluteFile());
                        //a copy file methoed just what you like
                        FileUtils.copyFile(tmpFile, targetFile);

                        //maybe you should put the follow code out
                        return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", "Success");
                    }
                }
            }

        } else if (Method.GET.equals(method)){
            if (GET_PATH_LIST.equals(strUri)) {
                return responsePathlist(session);
            } else if (GET_DEV_STATE.equals(strUri)) {
                return responseGetDevState(session);
            } else {
                String resize = session.getHeaders().get("resize");
                if (resize != null) {
                    try {
                        String[] size = resize.split(",");
                        int width = Integer.parseInt(size[0]);
                        int height = Integer.parseInt(size[1]);

                        if (file.exists()) {
                            if (file.isDirectory()) {
                                return responseFileList(session, absPath);
                            } else {
                                return responseThumbnailFileStream(session, absPath, width, height);
                            }
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else {

                    if (file.exists()) {
                        if (file.isDirectory()) {
                            return responseFileList(session, absPath);
                        } else {
                            return responseFileStream(session,absPath);
                        }
                    }
                }
            }

        }




//        if(REQUEST_ROOT.equals(strUri)) {   // 根目录
//            return responseFileList(session, absPath);
//        }else if(REQUEST_TEST.equals(strUri)){    // 返回给调用端json串
//            return responseJson();
//        }else if(REQUEST_ACTION_GET_FILE_LIST.equals(strUri)){    // 获取文件列表
//            Map<String,String> params = session.getParms();
//
//            String dirPath = params.get("dirPath");
//            if(!TextUtils.isEmpty(dirPath)){
//                return responseFileList(session,dirPath);
//            }
//        }else if(REQUEST_ACTION_GET_FILE.equals(strUri)){ // 下载文件
//            Map<String,String> params = session.getParms();
//            // 下载的文件名称
//            String fileName = params.get("fileName");
//
//            absPath = mBasePath + "/" + fileName;
//            File file = new File(absPath);
//            Log.e(TAG, "getFile:" + absPath);
//            if(file.exists()){
//                if(file.isDirectory()){
//                    Log.e(TAG, "responseFileList");
//                    return responseFileList(session, absPath);
//                }else{
//                    Log.e(TAG, "responseFileStream");
//                    return responseFileStream(session, absPath);
//                }
//            }
//        }
        return response404(session);
    }

    private Response responseRootPage(IHTTPSession session) {

        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><body>");
        builder.append("这是lixm的测试! \n");
        builder.append("</body></html>\n");
        //return Response.newFixedLengthResponse(Status.OK, "application/octet-stream", builder.toString());
        return newFixedLengthResponse(builder.toString());
    }

    /**
     * 返回给调用端LOG日志文件
     * @param session
     * @return
     */
    private Response responseFileStream(IHTTPSession session,String filePath) {
        Log.e(TAG, "responseFileStream() ,fileName = " + filePath);
        try {
            FileInputStream fis = new FileInputStream(filePath);
            //application/octet-stream
            return newChunkedResponse(Response.Status.OK, "application/octet-stream", fis);
        }
        catch (FileNotFoundException e) {
            Log.d("lixm", "responseFileStream FileNotFoundException :" ,e);
            return response404(session);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Response responseThumbnailFileStream(IHTTPSession session, String filePath, int width, int height) {
        File file = new File(filePath);
        if (MediaFileUtil.isImageFileType(filePath)) {

            Bitmap oriBitmat = BitmapFactory.decodeFile(filePath);

            Bitmap thumbnailBitmap = ThumbnailUtils.extractThumbnail(oriBitmat, width, height);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            InputStream isBm = new ByteArrayInputStream(baos.toByteArray());

            return newChunkedResponse(Response.Status.OK, "application/octet-stream", isBm);

        } else if (MediaFileUtil.isVideoFileType(filePath)) {

            Size size = new Size(width, height);
            try {
                Bitmap videoThumbnailBitmap = ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND);

                if (videoThumbnailBitmap != null) {
                    Bitmap thumbnailBitmap = ThumbnailUtils.extractThumbnail(videoThumbnailBitmap, width, height);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    InputStream isBm = new ByteArrayInputStream(baos.toByteArray());

                    return newChunkedResponse(Response.Status.OK, "application/octet-stream", isBm);
                } else {
                    return response404(session);
                }


            } catch (Exception e) {
                return response404(session);
            }


        } else {
            return response404(session);
        }

    }

    /**
     *
     * @param session http请求
     * @param dirPath 文件夹路径名称
     * @return
     */
    private Response responseFileList(IHTTPSession session,String dirPath) {
        Log.d("lixm", "responseFileList() , dirPath = " + dirPath);
        List <String> fileList = FileUtils.getFilePaths(dirPath, false);
        StringBuilder sb = new StringBuilder();

        JSONObject jsonObject = new JSONObject();
        JSONArray fileArray = new JSONArray();
        for(String filePath : fileList){
       //     sb.append("<a href=" + REQUEST_ACTION_GET_FILE + "?fileName=" + filePath + ">" + filePath + "</a>" + "<br>");
            fileArray.put(filePath);
        }
        try {
            jsonObject.put("files", fileArray);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "responseFileList:" + jsonObject.toString());

        return newFixedLengthResponse(jsonObject.toString());
    }

    /**
     * 调用的路径出错
     * @param session
     * @return
     */
    private Response response404(IHTTPSession session) {
        String url = session.getUri();
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html><html><body>");
        builder.append("Sorry, Can't Found "+url + " !");
        builder.append("</body></html>\n");
        return newFixedLengthResponse(builder.toString());
    }

    private Response responsePathlist(IHTTPSession session) {

        Log.e(TAG, "responsePathlist");
        try {
            ArrayList<String> pathlist = CommandListProvider.getInstance(MirroApplication.getContext()).loadPathList();

            JSONObject jsonObject = new JSONObject();
            JSONArray fileArray = new JSONArray();
            for(String path : pathlist){
                //     sb.append("<a href=" + REQUEST_ACTION_GET_FILE + "?fileName=" + filePath + ">" + filePath + "</a>" + "<br>");
                fileArray.put(path);
            }

            jsonObject.put("pathlist", fileArray);

            Log.e(TAG, "response pathlist:" + jsonObject.toString());

            return newFixedLengthResponse(jsonObject.toString());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(NanoHTTPD.Response.Status.NO_CONTENT, "text/html", "Fail");
    }

    private Response responseDelPathlist(IHTTPSession session) {
        Map<String, String> files = new HashMap<String, String>();

        try {
            session.parseBody(files);
            Log.e(TAG, "files:" +files.toString() + ", ffiles.size:" + files.size());
            String body = files.get("postData");


            if (body != null) {
                ArrayList<String> pathlist = CommandListProvider.getInstance(MirroApplication.getContext()).loadPathList();
                ArrayList<String> paths = new ArrayList<String>();

                JSONObject jSONObject = new JSONObject(body);
                JSONArray jsonArray = jSONObject.getJSONArray("pathlist");

                for (int i = 0; i < jsonArray.length(); i++) {
                    paths.add(jsonArray.get(i).toString());
                }

                for (String path : paths) {
                    CommandListProvider.getInstance(MirroApplication.getContext()).deleteCmdfile(path + ".json");
                }

                pathlist.removeAll(paths);

                CommandListProvider.getInstance(MirroApplication.getContext()).savePathList(pathlist);

                return responsePathlist(session);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ResponseException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return response404(session);
    }

    private Response responseGetDevState(IHTTPSession session) {
        int runningState = ((MirroApplication) mContext).getRunningState();
        String state = null;
        switch (runningState) {
            case MirroApplication.CONTROL_STATE:
                state = "control";
                break;
            case MirroApplication.AUTO_RUNNING_STATE:
                state = "autorunning";
                break;
            case MirroApplication.PATH_PLANNING_STATE:
                state = "pathplanning";
                break;
        }

        String pathName = ((MirroApplication)mContext).getRunningPath();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("RunningState", state);
            jsonObject.put("PathName", pathName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", jsonObject.toString());
    }

    /**
     * 返回给调用端json字符串
     * @return
     */
    private Response responseJson(){
        return newFixedLengthResponse("调用成功");
    }

}
