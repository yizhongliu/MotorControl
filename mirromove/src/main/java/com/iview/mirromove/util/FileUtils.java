package com.iview.mirromove.util;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.iview.mirromove.MirroApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUtils {

    public final static String TAG = "FileUtils";


    private String SDPATH;

    public static final String FILE_INFO_NAME="FileName";
    public static final String FILE_INFO_TYPE="FileType";

    public FileUtils() {

    }

    public String getSDPATH() {
        return SDPATH;
    }

    public FileUtils(String SDPATH){
        //得到外部存储设备的目录（/SDCARD）
        SDPATH = Environment.getExternalStorageDirectory() + "/" ;
    }

    /**
     * 在SD卡上创建文件
     * @param fileName
     * @return
     * @throws java.io.IOException
     */
    public File createSDFile(String fileName) throws IOException {
        File file = new File(SDPATH + fileName);
        file.createNewFile();
        return file;
    }

    /**
     * 在SD卡上创建目录
     * @param dirName 目录名字
     * @return 文件目录
     */
    public File createDir(String dirName){
        File dir = new File(SDPATH + dirName);
        dir.mkdir();
        return dir;
    }

    /**
     * 判断文件是否存在
     * @param fileName
     * @return
     */
    public boolean isFileExist(String fileName){
        File file = new File(SDPATH + fileName);
        return file.exists();
    }

    public File write2SDFromInput(String path,String fileName,InputStream input){
        File file = null;
        OutputStream output = null;

        try {
            file = new File(path + "/" + fileName);
            file.createNewFile();
            output = new FileOutputStream(file);
            byte [] buffer = new byte[4 * 1024];
            int size;
            while((size = input.read(buffer)) != -1){
                output.write(buffer, 0, size);
            }
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    public List<Map<String, Object>> getFileDirFList(File path) {
        List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
        File[] files = path.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                Map<String, Object> fileInfo=new HashMap<String, Object>();
                if (files[i].isDirectory()) {

                } else {
                    fileInfo.put(FILE_INFO_NAME, files[i].getName());
                    fileInfo.put(FILE_INFO_TYPE, getFileType(files[i].getName()));
                    list.add(fileInfo);
                }
            }
        }

        return list;
    }

    /**
     * 根据后缀获取文件fileName的类型
     * @return String 文件的类型
     **/
    public String getFileType(String fileName){
        if(fileName!=""&&fileName.length()>3){
            int dot=fileName.lastIndexOf(".");
            if(dot>0){
                return fileName.substring(dot+1);
            }else{
                return "";
            }
        }
        return "";
    }

    /** 删除文件，可以是文件或文件夹
     * @param delFile 要删除的文件夹或文件名
     * @return 删除成功返回true，否则返回false
     */
    public static boolean delete(String delFile) {
        File file = new File(delFile);
        if (!file.exists()) {
            Toast.makeText(MirroApplication.getContext(), "删除文件失败:" + delFile + "不存在！", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            if (file.isFile())
                return deleteSingleFile(delFile);
            else
                return deleteDirectory(delFile);
        }
    }

    /** 删除单个文件
     * @param filePath$Name 要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteSingleFile(String filePath$Name) {
        File file = new File(filePath$Name);
        // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                Log.e("--Method--", "Copy_Delete.deleteSingleFile: 删除单个文件" + filePath$Name + "成功！");
                return true;
            } else {
                Toast.makeText(MirroApplication.getContext(), "删除单个文件" + filePath$Name + "失败！", Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            Toast.makeText(MirroApplication.getContext(), "删除单个文件失败：" + filePath$Name + "不存在！", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /** 删除目录及目录下的文件
     * @param filePath 要删除的目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String filePath) {
        // 如果dir不以文件分隔符结尾，自动添加文件分隔符
        if (!filePath.endsWith(File.separator))
            filePath = filePath + File.separator;
        File dirFile = new File(filePath);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
            Toast.makeText(MirroApplication.getContext(), "删除目录失败：" + filePath + "不存在！", Toast.LENGTH_SHORT).show();
            return false;
        }
        boolean flag = true;
        // 删除文件夹中的所有文件包括子目录
        File[] files = dirFile.listFiles();
        for (File file : files) {
            // 删除子文件
            if (file.isFile()) {
                flag = deleteSingleFile(file.getAbsolutePath());
                if (!flag)
                    break;
            }
            // 删除子目录
            else if (file.isDirectory()) {
                flag = deleteDirectory(file
                        .getAbsolutePath());
                if (!flag)
                    break;
            }
        }
        if (!flag) {
            Toast.makeText(MirroApplication.getContext(), "删除目录失败！", Toast.LENGTH_SHORT).show();
            return false;
        }
        // 删除当前目录
        if (dirFile.delete()) {
            Log.e("--Method--", "Copy_Delete.deleteDirectory: 删除目录" + filePath + "成功！");
            return true;
        } else {
            Toast.makeText(MirroApplication.getContext(), "删除目录：" + filePath + "失败！", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static boolean checkFileExist(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String ReadFile(String path) {
        File file = new File(path);
        BufferedReader reader = null;
        String laststr = "";
        try {
            // System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                System.out.println("line " + line + ": " + tempString);
                laststr = laststr + tempString;
                line++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return laststr;
    }

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
