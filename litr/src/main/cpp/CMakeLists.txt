cmake_minimum_required(VERSION 3.10.2)

project("LiTrNative")

add_library(litr-jni SHARED
        audio-processor.cpp)

add_subdirectory(oboe_resampler)

find_library(log-lib log)

target_link_libraries(litr-jni
        ${log-lib}
        oboe-resampler)