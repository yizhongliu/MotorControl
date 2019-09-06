# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# 设置include文件夹的地址
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)

#设置ffmpeg的动态库
add_library(libavcodec-58 SHARED IMPORTED)
set_target_properties(libavcodec-58 PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavcodec-58.so)

add_library(libavdevice-58 SHARED IMPORTED)
set_target_properties(libavdevice-58 PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavdevice-58.so)

add_library(libavfilter-7 SHARED IMPORTED)
set_target_properties(libavfilter-7 PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavfilter-7.so)

add_library(libavformat-58 SHARED IMPORTED)
set_target_properties(libavformat-58 PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavformat-58.so)

add_library(libpostproc-55 SHARED IMPORTED)
set_target_properties(libpostproc-55 PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libpostproc-55.so)

add_library(libswresample-3 SHARED IMPORTED)
set_target_properties(libswresample-3 PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libswresample-3.so)

add_library(libswscale-5 SHARED IMPORTED)
set_target_properties(libswscale-5 PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libswscale-5.so)

add_library(libavutil-56 SHARED IMPORTED)
set_target_properties(libavutil-56 PROPERTIES IMPORTED_LOCATION
            ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libavutil-56.so)


add_library( # Sets the name of the library.
        player-jni

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/player_jni.cpp
        src/main/cpp/ffplayer.c
        src/main/cpp/ffrender.c
        src/main/cpp/pktqueue.c
        src/main/cpp/snapshot.c
        src/main/cpp/recorder.c
        src/main/cpp/adev-cmn.c
        src/main/cpp/vdev-cmn.c
        src/main/cpp/adev-android.cpp
        src/main/cpp/vdev-android.cpp
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )


target_link_libraries( # Specifies the target library.
        player-jni
        libavcodec-58
        libavdevice-58
        libavfilter-7
        libavformat-58
        libpostproc-55
        libswresample-3
        libswscale-5
        libavutil-56
        libandroid.so
        libz.so

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib} )