/*
 * Copyright 2021 LinkedIn Corporation
 * All Rights Reserved.
 *
 * Licensed under the BSD 2-Clause License (the "License").  See License in the project root for
 * license information.
 */
package com.linkedin.android.litr.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.annotation.Nullable;
import java.io.FileNotFoundException;

public class FileUtils {

    private FileUtils() {}

    /**
     * Open a raw file descriptor to access data under a URI.
     */
    public static ParcelFileDescriptor getParcelFileDescriptor(Context context, Uri outputFileUri) throws FileNotFoundException {
        return context.getContentResolver().openFileDescriptor(outputFileUri, "rwt");
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * Callers should check whether the path is local before assuming it represents a local file.
     */
    @Nullable
    public static String getFilePathFromUri(Context context, Uri uri){
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }



    /**
     * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The discussionChatListRepository.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    @Nullable
    private static String getDataColumn(Context context, Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(column));
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }
}
