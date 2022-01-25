package com.linkedin.android.litr.io

import android.media.MediaCodec
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Implementation of [MediaTarget] that writes a single audio track to WAV file
 */
class WavMediaTarget(
    private val targetPath: String
) : MediaTarget {

    private val tracks = mutableListOf<MediaFormat>()
    private val outputStream: OutputStream

    init {
        outputStream = FileOutputStream(File(targetPath))
    }

    override fun addTrack(mediaFormat: MediaFormat, targetTrack: Int): Int {
        if (mediaFormat.containsKey(MediaFormat.KEY_MIME) &&
            mediaFormat.getString(MediaFormat.KEY_MIME) == "audio/raw" &&
            mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) &&
            mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            tracks.add(mediaFormat)
            writeWavHeader(
                outputStream,
                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).toShort(),
                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                16
            )
        }
        if (tracks.size > 1){
            throw IllegalStateException("Cannot add more than one track")
        }
        return 0
    }

    override fun writeSampleData(targetTrack: Int, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
//        val writeBuffer: ByteBuffer = ByteBuffer.allocate(buffer.capacity()).order(ByteOrder.LITTLE_ENDIAN)
//        writeBuffer.asShortBuffer().put(buffer.asShortBuffer())
        outputStream.write(buffer.array(), info.offset, info.size)
    }

    override fun release() {
        outputStream.close()
        updateWavHeader()
    }

    override fun getOutputFilePath(): String {
        return targetPath
    }

    // https://gist.github.com/kmark/d8b1b01fb0d2febf5770#file-audiorecordactivity-java-L288
    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param out        The stream to write the header to
     * @param channels   The number of channels
     * @param sampleRate The sample rate in hertz
     * @param bitDepth   The bit depth
     */
    private fun writeWavHeader(out: OutputStream, channels: Short, sampleRate: Int, bitDepth: Short) {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        val littleBytes = ByteBuffer
            .allocate(14)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort(channels)
            .putInt(sampleRate)
            .putInt(sampleRate * channels * (bitDepth / 8))
            .putShort((channels * (bitDepth / 8)).toShort())
            .putShort(bitDepth)
            .array()

        // Not necessarily the best, but it's very easy to visualize this way
        out.write(byteArrayOf( // RIFF header
            'R'.toByte(), 'I'.toByte(), 'F'.toByte(), 'F'.toByte(),  // ChunkID
            0, 0, 0, 0,  // ChunkSize (must be updated later)
            'W'.toByte(), 'A'.toByte(), 'V'.toByte(), 'E'.toByte(),  // Format
            // fmt subchunk
            'f'.toByte(), 'm'.toByte(), 't'.toByte(), ' '.toByte(),  // Subchunk1ID
            16, 0, 0, 0,  // Subchunk1 Size
            1, 0,  // AudioFormat
            littleBytes[0], littleBytes[1],  // NumChannels
            littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5],  // SampleRate
            littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9],  // ByteRate
            littleBytes[10], littleBytes[11],  // BlockAlign
            littleBytes[12], littleBytes[13],  // BitsPerSample
            // data subchunk
            'd'.toByte(), 'a'.toByte(), 't'.toByte(), 'a'.toByte(),  // Subchunk2 ID
            0, 0, 0, 0))
    }

    /**
     * Updates the given wav file's header to include the final chunk sizes
     *
     * @param wav The wav file to update
     * @throws IOException
     */
    private fun updateWavHeader() {
        val wav = File(targetPath)
        val sizes = ByteBuffer
            .allocate(8)
            .order(ByteOrder.LITTLE_ENDIAN) // There are probably a bunch of different/better ways to calculate
            // these two given your circumstances. Cast should be safe since if the WAV is
            // > 4 GB we've already made a terrible mistake.
            .putInt((wav.length() - 8).toInt()) // ChunkSize
            .putInt((wav.length() - 44).toInt()) // Subchunk2Size
            .array()
        var accessWave: RandomAccessFile? = null
        try {
            accessWave = RandomAccessFile(wav, "rw")
            // ChunkSize
            accessWave.seek(4)
            accessWave.write(sizes, 0, 4)

            // Subchunk2Size
            accessWave.seek(40)
            accessWave.write(sizes, 4, 4)
        } catch (ex: IOException) {
            // Rethrow but we still close accessWave in our finally
            throw ex
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close()
                } catch (ex: IOException) {
                    //
                }
            }
        }
    }

}
