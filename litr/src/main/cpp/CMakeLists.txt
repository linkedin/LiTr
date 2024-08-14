cmake_minimum_required(VERSION 3.10.2)

project("LiTrNative")

add_library(litr-jni SHARED
        audio-processor.cpp)

add_subdirectory(oboe_resampler)

find_library(log-lib log)

target_link_libraries(litr-jni
        ${log-lib}
        oboe-resampler)

#[[To support compiling 16 KB-aligned shared libraries with Android NDK version r26 or lower]]
set_target_properties(litr-jni PROPERTIES
        LINK_FLAGS "-Wl,-z,max-page-size=16384"
)
