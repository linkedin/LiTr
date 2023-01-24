cmake_minimum_required(VERSION 3.18.1)

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
    # We need to link against the source code that matches our bundled bindaries. We will therefore
    # download the original snapshot locally.
    set(ffmpeg_bundled_source_archive "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_bundled/ffmpeg-4.2.tar.gz")
    if (NOT EXISTS "${ffmpeg_bundled_source_archive}")
        file(DOWNLOAD
                https://git.ffmpeg.org/gitweb/ffmpeg.git/snapshot/e228a0cccd31c2466ea968f34be4ec0da50bd792.tar.gz
                "${ffmpeg_bundled_source_archive}"
                EXPECTED_MD5 a08c1bdffbbb1e801c06fd62721af008
                STATUS DOWNLOAD_STATUS)

        # Separate the returned status code, and error message.
        list(GET DOWNLOAD_STATUS 0 STATUS_CODE)
        list(GET DOWNLOAD_STATUS 1 ERROR_MESSAGE)

        # If the download (or hash verification) failed, let's clean up the old downloaded file.
        if(${STATUS_CODE} EQUAL 1)
            file(REMOVE "${ffmpeg_bundled_source_archive}")
            message(FATAL_ERROR "Error occurred during download: ${ERROR_MESSAGE}")
        endif()
    endif()

    # Once we have the snapshot, we can uncompress it in a location that we'll link too.
    set(ffmpeg_bundled_source "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_bundled/ffmpeg-e228a0c")
    if (NOT EXISTS "${ffmpeg_bundled_source}")
        file(ARCHIVE_EXTRACT
                INPUT "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_bundled/ffmpeg-4.2.tar.gz"
                DESTINATION "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_bundled")
    endif()

    set(ffmpeg_location "${ffmpeg_bundled_source}")
    set(ffmpeg_binaries "${CMAKE_CURRENT_SOURCE_DIR}/ffmpeg_bundled/android-libs/${ANDROID_ABI}")
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
