package com.iview.mirromove.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public final static String TAG = "FileUtils";
    public static List<String> getFilePaths(String dirPath, boolean bAbsPath) {
        List<String> fileList = new ArrayList<>();

        File file = new File(dirPath);
        File[] files = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                fileList.add(files[i].getName());
            }
        }
        return  fileList;
    }

    public static boolean copyFile(File srcFile, File dstFile) {

        if (srcFile.exists()) {

            int byteSum = 0;
            int byteRead = 0;

            try {
                InputStream inputStream = new FileInputStream(srcFile);
                FileOutputStream outputStream = new FileOutputStream(dstFile);
                byte [] buffer = new byte[1444];
                int len;
                while ((byteRead = inputStream.read(buffer)) != -1) {
                    byteSum += byteRead;
                    outputStream.write(buffer, 0, byteRead);
                }

                inputStream.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.e(TAG, "copy file error");
        return false;
    }

}
