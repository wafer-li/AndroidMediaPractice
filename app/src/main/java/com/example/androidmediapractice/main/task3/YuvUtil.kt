package com.example.androidmediapractice.main.task3

import kotlin.math.max
import kotlin.math.min

object YuvUtil {
    private val Y1 = LongArray(256)
    private val Y2 = LongArray(256)
    private val U = LongArray(256)
    private val V = LongArray(256)

    private var isInitialized = false

    private fun initTable() {
        var i = 0
        if (isInitialized) {
            return
        }
        // Initialize table
        while (i < 256) {
            V[i] = (15938 * i - 2221300).toLong()
            U[i] = (20238 * i - 2771300).toLong()
            Y1[i] = (11644 * i).toLong()
            Y2[i] = (19837 * i - 311710).toLong()
            i++
        }
        isInitialized = true
    }

    fun convertYuv420ToARGB(yuv420Bytes: ByteArray, width: Int, height: Int): ByteArray {
        // upscaling to yuv444
        val yuv444Bytes = yuv420ToYuv444(yuv420Bytes, width, height)
        val argb8888Bytes = ByteArray(yuv444Bytes.size + (width * height))

        initTable()

        for (i in 0 until width * height) {
            val py = i
            val pu = i + width * height
            val pv = i + 2 * width * height

            val y = yuv444Bytes[py].toInt() and 0xff
            val u = yuv444Bytes[pu].toInt() and 0xff
            val v = yuv444Bytes[pv].toInt() and 0xff

            val r = max(
                0,
                min(
                    255,
                    (V[v] + Y1[y]) / 10000
                )
            )
            val b = max(
                0,
                min(
                    255,
                    (U[u] + Y1[y]) / 10000
                )
            )
            val g =
                max(0, min(255, (Y2[y] - 5094 * r - 1942 * b) / 10000))

            argb8888Bytes[4 * i] = r.toByte()
            argb8888Bytes[4 * i + 1] = g.toByte()
            argb8888Bytes[4 * i + 2] = b.toByte()
            argb8888Bytes[4 * i + 3] = 255.toByte()
        }
        return argb8888Bytes
    }

    fun yuv420ToYuv444(yuv420Bytes: ByteArray, width: Int, height: Int): ByteArray {
        val size = width * height
        val yuv444Bytes = ByteArray(width * height * 3)

        for (y in 0 until size) {
            yuv444Bytes[y] = yuv420Bytes[y]
        }

        val originUIndex = size
        val originVIndex = originUIndex + width * height / 4
        val desUIndex = size
        val desVIndex = desUIndex + size
        for (h in 0 until height step 2) {
            for (w in 0 until width step 2) {
                val originU = yuv420Bytes[originUIndex + h / 2 * width / 2 + w / 2]
                yuv444Bytes[desUIndex + h * width + w] = originU
                yuv444Bytes[desUIndex + h * width + w + 1] = originU
                yuv444Bytes[desUIndex + (h + 1) * width + w] = originU
                yuv444Bytes[desUIndex + (h + 1) * width + w + 1] = originU

                val originV = yuv420Bytes[originVIndex + h / 2 * width / 2 + w / 2]
                yuv444Bytes[desVIndex + h * width + w] = originV
                yuv444Bytes[desVIndex + h * width + w + 1] = originV
                yuv444Bytes[desVIndex + (h + 1) * width + w] = originV
                yuv444Bytes[desVIndex + (h + 1) * width + w + 1] = originV
            }
        }

        return yuv444Bytes
    }
}