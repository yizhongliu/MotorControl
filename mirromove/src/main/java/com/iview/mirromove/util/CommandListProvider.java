package com.iview.mirromove.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.iview.mirromove.data.PathPlanning;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

public class CommandListProvider {
    public final static String TAG = "CommandListProvider";

    private final String mFilename = "pathlist.json";//"cmlist.json";

    private static CommandListProvider commandListProvider = null;
    private Context mContext;

    private CommandListProvider(Context context) {
        this.mContext = context;
    }

    public static CommandListProvider getInstance(Context context) {
        synchronized (CommandListProvider.class) {
            if (commandListProvider == null) {
                commandListProvider = new CommandListProvider(context.getApplicationContext());
            }
        }
        return commandListProvider;
    }


    public void saveCmdList(ArrayList<PathPlanning> mediaInfos, String pathName) throws JSONException,
            IOException {
        synchronized (commandListProvider) {
            JSONArray array = new JSONArray();
            for (PathPlanning c :  mediaInfos)
                array.put(c.toJSON());

            Writer writer = null;
            try {
                OutputStream out = mContext.openFileOutput(pathName+ ".json",
                        Context.MODE_PRIVATE);
                writer = new OutputStreamWriter(out);
                writer.write(array.toString());

                Log.d(TAG, "savePlayList :" + array.toString());
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

    }

    public ArrayList<PathPlanning> loadCmdList(String pathName) throws IOException, JSONException {
        Log.e(TAG, "Enter loadCmdList");
        synchronized (commandListProvider) {
            ArrayList<PathPlanning> mediaInfos = new ArrayList<PathPlanning>();
            BufferedReader reader = null;
            try {
                InputStream in = mContext.openFileInput(pathName+".json");
                reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder jsonString = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    jsonString.append(line);
                }
                JSONArray array = (JSONArray) new JSONTokener(jsonString.toString())
                        .nextValue();
                for (int i = 0; i < array.length(); i++) {
                    mediaInfos.add(new PathPlanning(array.getJSONObject(i)));
                }
            } catch (FileNotFoundException e) {

            } finally {
                if (reader != null) {
                    reader.close();
                }
            }

            Log.e(TAG, "loadCmdList size:" + mediaInfos.size());
            return mediaInfos;
        }
    }

    public boolean deleteCmdfile(String fileName) {
        String absPath = mContext.getFilesDir() + "/" + fileName;
        return FileUtils.delete(absPath);
    }

    public void savePathList(ArrayList<String> paths) throws JSONException,
            IOException {
        JSONObject jsonObject = new JSONObject();
        JSONArray array = new JSONArray();
        for (String c :  paths)
            array.put(c);

        jsonObject.put("pathlist", array);

        Writer writer = null;
        try {
            OutputStream out = mContext.openFileOutput(mFilename,
                    Context.MODE_PRIVATE);
            writer = new OutputStreamWriter(out);
            writer.write(jsonObject.toString());

            Log.d(TAG, "savePathlist :" + jsonObject.toString());
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public ArrayList<String> loadPathList() throws IOException, JSONException {
        ArrayList<String> paths = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            InputStream in = mContext.openFileInput(mFilename);
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder jsonString = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
         //   JSONArray array = (JSONArray) new JSONTokener(jsonString.toString())
         //           .nextValue();


            Log.e(TAG, "file string:" + jsonString.toString());

            JSONObject jSONObject = new JSONObject(jsonString.toString());
            Log.e(TAG, "pase json:" + jSONObject.toString());

            JSONArray array = jSONObject.getJSONArray("pathlist");

            for (int i = 0; i < array.length(); i++) {
                paths.add(array.get(i).toString());
            }
        } catch (FileNotFoundException e) {

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return paths;
    }
}
