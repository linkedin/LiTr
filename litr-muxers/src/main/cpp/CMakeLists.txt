cmake_minimum_required(VERSION 3.10.2)

# Enable C++11 features.
set(CMAKE_CXX_STANDARD 11)

project(LiTrMuxers C CXX)

# Additional flags needed for "arm64-v8a" from NDK 23.1.7779620 and above.
if(${ANDROID_ABI} MATCHES "arm64-v8a")
    set(CMAKE_CXX_FLAGS "-Wl,-Bsymbolic")
endif()

# If locally built ffmpeg binaries are available, we will prioritise these. This is preferable as
# ensures they are built to match exactly what the consumer requires. However, we will allow the
# build to fallback to those bundled with the repository.
if(EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg")
    set(ffmpeg_location "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg")
    set(ffmpeg_binaries "${ffmpeg_location}/android-libs/${ANDROID_ABI}")
else()
    set(ffmpeg_location "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_bundled")
    set(ffmpeg_binaries "${ffmpeg_location}/android-libs/${ANDROID_ABI}")
endif()

foreach(ffmpeg_lib avutil avcodec avformat)
    set(ffmpeg_lib_filename lib${ffmpeg_lib}.so)
    set(ffmpeg_lib_file_path ${ffmpeg_binaries}/${ffmpeg_lib_filename})
    add_library(
            ${ffmpeg_lib}
            SHARED
            IMPORTED)
    set_target_properties(
            ${ffmpeg_lib} PROPERTIES
            IMPORTED_LOCATION
            ${ffmpeg_lib_file_path})
endforeach()

include_directories(${ffmpeg_location})
find_library(log-lib log)

add_library(litr-muxers SHARED
        FFmpeg.h
        Logging.h
        )

target_link_libraries(litr-muxers
        PRIVATE android
        PRIVATE avcodec
        PRIVATE avutil
        PRIVATE avformat
        PRIVATE ${log-lib})

set_property(TARGET litr-muxers PROPERTY LINKER_LANGUAGE CXX)
