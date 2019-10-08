package com.iview.mirromove.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
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
}
