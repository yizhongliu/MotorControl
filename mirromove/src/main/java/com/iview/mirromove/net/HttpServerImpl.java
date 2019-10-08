package com.iview.mirromove.net;

import android.util.Log;

import com.iview.mirromove.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

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

    private String mBasePath;

    public HttpServerImpl(String basePath) {
        super(DEFAULT_SERVER_PORT);

        mBasePath = basePath;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String strUri = session.getUri();
        String method = session.getMethod().name();
        Log.e(TAG,"Response serve uri = " + strUri + ", method = " + method);

        String absPath = mBasePath + strUri;
        File file = new File(absPath);

        if (file.exists()) {
            if (file.isDirectory()) {
                return responseFileList(session, absPath);
            } else {
                return responseFileStream(session,absPath);
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

    /**
     * 返回给调用端json字符串
     * @return
     */
    private Response responseJson(){
        return newFixedLengthResponse("调用成功");
    }
}

