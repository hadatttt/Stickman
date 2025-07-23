package com.hadat.stickman.utils

import android.graphics.Bitmap
import java.io.IOException
import java.io.OutputStream

class ImprovedAnimatedGifEncoder {
    private var width = 0
    private var height = 0
    private var frameRate = 10f
    private var quality = 10
    private var repeat = 0
    private var started = false
    private var out: OutputStream? = null
    private var pixels: ByteArray? = null
    private var indexedPixels: ByteArray? = null
    private var colorTab: ByteArray? = null
    private var usedEntry = BooleanArray(256)
    private var palSize = 7
    private var colorDepth = 8
    private var transparent: Int? = null
    private var dispose = -1
    private var firstFrame = true

    fun setQuality(quality: Int) {
        this.quality = if (quality < 1) 1 else quality
    }

    fun setSize(width: Int, height: Int) {
        if (!started) {
            this.width = width
            this.height = height
        }
    }

    fun setFrameRate(fps: Float) {
        this.frameRate = fps
    }

    fun setRepeat(repeat: Int) {
        if (repeat >= 0) {
            this.repeat = repeat
        }
    }

    fun setTransparent(color: Int) {
        this.transparent = color
    }

    fun start(outStream: OutputStream): Boolean {
        if (outStream == null) return false
        out = outStream
        started = true
        try {
            writeString("GIF89a")
            writeLogicalScreenDescriptor()
            if (repeat >= 0) writeNetscapeExt()
        } catch (e: IOException) {
            started = false
            return false
        }
        return true
    }

    fun addFrame(image: Bitmap): Boolean {
        if (!started || image == null) return false
        try {
            if (firstFrame) {
                if (width == 0 || height == 0) {
                    width = image.width
                    height = image.height
                    writeLogicalScreenDescriptor()
                }
                firstFrame = false
            }
            processFrame(image)
            return true
        } catch (e: IOException) {
            return false
        }
    }

    fun finish(): Boolean {
        if (!started) return false
        var result = true
        try {
            out?.write(0x3b) // GIF trailer
            out?.flush()
        } catch (e: IOException) {
            result = false
        }
        started = false
        out = null
        pixels = null
        indexedPixels = null
        colorTab = null
        return result
    }

    private fun writeString(s: String) {
        out?.write(s.toByteArray())
    }

    private fun writeLogicalScreenDescriptor() {
        writeShort(width)
        writeShort(height)
        out?.write(0x80 or 0x70 or palSize) // Global color table flag + color resolution + sort flag
        out?.write(0) // Background color index
        out?.write(0) // Pixel aspect ratio
    }

    private fun writeNetscapeExt() {
        out?.write(0x21) // Extension introducer
        out?.write(0xff) // Application extension label
        out?.write(11) // Block size
        writeString("NETSCAPE2.0")
        out?.write(3) // Sub-block size
        out?.write(1) // Loop sub-block ID
        writeShort(repeat) // Loop count (0 = infinite)
        out?.write(0) // Block terminator
    }

    private fun writeGraphicControlExt() {
        out?.write(0x21) // Extension introducer
        out?.write(0xf9) // Graphic control label
        out?.write(4) // Block size
        val packed = if (transparent != null) 0x01 else 0x00 // Transparency flag
        out?.write(packed or (dispose shl 2))
        writeShort((1000 / frameRate).toInt()) // Delay in 1/100th of a second
        out?.write(transparent?.and(0xff) ?: 0) // Transparent color index
        out?.write(0) // Block terminator
    }

    private fun writeImageDescriptor() {
        out?.write(0x2c) // Image separator
        writeShort(0) // Image left
        writeShort(0) // Image top
        writeShort(width)
        writeShort(height)
        out?.write(0x80 or palSize) // Local color table flag + palette size
    }

    private fun processFrame(image: Bitmap) {
        pixels = ByteArray(width * height * 3)
        indexedPixels = ByteArray(width * height)
        colorTab = ByteArray(256 * 3)
        usedEntry.fill(false)

        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)

        var k = 0
        pixels.forEach { pixel ->
            this.pixels!![k++] = (pixel shr 16 and 0xff).toByte()
            this.pixels!![k++] = (pixel shr 8 and 0xff).toByte()
            this.pixels!![k++] = (pixel and 0xff).toByte()
        }

        val quant = NeuQuant(pixels = this.pixels!!, length = this.pixels!!.size, sample = quality)
        colorTab = quant.process()
        for (i in 0 until width * height) {
            val index = quant.map(
                this.pixels!![i * 3].toInt() and 0xff,
                this.pixels!![i * 3 + 1].toInt() and 0xff,
                this.pixels!![i * 3 + 2].toInt() and 0xff
            )
            indexedPixels!![i] = index.toByte()
            usedEntry[index] = true
        }

        writeGraphicControlExt()
        writeImageDescriptor()
        writePalette()
        writeLZW()
    }

    private fun writePalette() {
        out?.write(colorTab, 0, colorTab!!.size)
        val remaining = (3 * 256) - colorTab!!.size
        repeat(remaining) { out?.write(0) }
    }

    private fun writeLZW() {
        val lzw = LZWEncoder(width * height, indexedPixels, colorDepth)
        lzw.encode(out)
    }

    private fun writeShort(value: Int) {
        out?.write(value and 0xff)
        out?.write(value shr 8 and 0xff)
    }

    private class NeuQuant(
        private val pixels: ByteArray,
        private val length: Int,
        private val sample: Int
    ) {
        private val network = Array(256) { DoubleArray(3) }
        private val freq = DoubleArray(256)
        private val bias = DoubleArray(256)
        private val netSize = 256
        private val alpha = 1.0 / sample
        private val beta = 1.0 / 1024.0
        private val radiusBias = 1.0 / 1024.0
        private val initRadius = netSize / 8
        private val radiusDec = 30

        init {
            for (i in 0 until netSize) {
                network[i][0] = (i * (255.0 / netSize)).toInt().toDouble()
                network[i][1] = network[i][0]
                network[i][2] = network[i][0]
                freq[i] = 1.0 / netSize
                bias[i] = 0.0
            }
        }

        fun process(): ByteArray {
            learn()
            unbiasNet()
            return inxbuild()
        }

        private fun learn() {
            var radius = initRadius
            var rad = radius
            var step = if (length < 3 * netSize) 1 else length / (3 * netSize)
            var i = 0
            var j = 0

            while (i < length) {
                val r = pixels[i++].toInt() and 0xff
                val g = pixels[i++].toInt() and 0xff
                val b = pixels[i++].toInt() and 0xff
                val best = findClosest(r, g, b)
                alterneigh(rad, best, r, g, b)
                altersingle(alpha, best, r, g, b)
                if (++j >= radiusDec) {
                    j = 0
                    rad--
                    if (rad <= 0) rad = 1
                }
                i += step * 3
                if (i >= length) i = 0
            }
        }

        private fun unbiasNet() {
            for (i in 0 until netSize) {
                network[i][0] = network[i][0].toInt().toDouble()
                network[i][1] = network[i][1].toInt().toDouble()
                network[i][2] = network[i][2].toInt().toDouble()
            }
        }

        private fun inxbuild(): ByteArray {
            val colorMap = ByteArray(3 * netSize)
            val index = IntArray(netSize) { it }
            // Chuyển IntArray thành MutableList và sắp xếp bằng sortedBy
            val sortedIndices = index.toList().sortedBy { i ->
                val sum = network[i][0] + network[i][1] + network[i][2]
                -sum // Sắp xếp giảm dần
            }
            for (i in 0 until netSize) {
                colorMap[i * 3] = network[sortedIndices[i]][0].toInt().toByte()
                colorMap[i * 3 + 1] = network[sortedIndices[i]][1].toInt().toByte()
                colorMap[i * 3 + 2] = network[sortedIndices[i]][2].toInt().toByte()
            }
            return colorMap
        }

        fun map(r: Int, g: Int, b: Int): Int = findClosest(r, g, b)

        private fun findClosest(r: Int, g: Int, b: Int): Int {
            var bestd = Int.MAX_VALUE
            var best = -1
            for (i in 0 until netSize) {
                val n = network[i]
                val dist = ((n[0] - r).toInt() * (n[0] - r).toInt() +
                        (n[1] - g).toInt() * (n[1] - g).toInt() +
                        (n[2] - b).toInt() * (n[2] - b).toInt())
                if (dist < bestd) {
                    bestd = dist
                    best = i
                }
            }
            return best
        }

        private fun alterneigh(radius: Int, i: Int, r: Int, g: Int, b: Int) {
            val lo = maxOf(0, i - radius)
            val hi = minOf(netSize - 1, i + radius)
            for (j in lo + 1 until hi + 1) {
                val n = network[j]
                val factor = radiusBias * (radius * radius - (j - i) * (j - i))
                n[0] -= (factor * (n[0] - r)).toInt().toDouble()
                n[1] -= (factor * (n[1] - g)).toInt().toDouble()
                n[2] -= (factor * (n[2] - b)).toInt().toDouble()
            }
        }

        private fun altersingle(alpha: Double, i: Int, r: Int, g: Int, b: Int) {
            val n = network[i]
            n[0] -= (alpha * (n[0] - r)).toInt().toDouble()
            n[1] -= (alpha * (n[1] - g)).toInt().toDouble()
            n[2] -= (alpha * (n[2] - b)).toInt().toDouble()
        }
    }

    private class LZWEncoder(
        private val pixelCount: Int,
        private val pixels: ByteArray?,
        private val colorDepth: Int
    ) {
        private val maxBits = 12
        private val maxMaxCode = 1 shl maxBits
        private val htab = IntArray(5003)
        private val codetab = IntArray(5003)
        private var hSize = 5003
        private var freeEnt = 0
        private var clearFlag = false
        private var initBits = 0
        private var clearCode = 0
        private var eofCode = 0
        private var curAccum = 0
        private var curBits = 0
        private val masks = intArrayOf(
            0x0000, 0x0001, 0x0003, 0x0007, 0x000F,
            0x001F, 0x003F, 0x007F, 0x00FF,
            0x01FF, 0x03FF, 0x07FF, 0x0FFF,
            0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF
        )
        private var aCount = 0
        private val accum = ByteArray(256)

        fun encode(os: OutputStream?) {
            os?.write(colorDepth)
            initBits = colorDepth + 1
            clearCode = 1 shl colorDepth
            eofCode = clearCode + 1
            freeEnt = clearCode + 2
            clearFlag = false
            aCount = 0

            htab.fill(-1)
            output(clearCode, os)

            var ent = pixels!![0].toInt() and 0xff
            var i = 1
            while (i < pixelCount) {
                val c = pixels[i++].toInt() and 0xff
                val fcode = (c shl maxBits) + ent
                var h = (c xor ent) % hSize
                if (h < 0) h += hSize

                while (true) {
                    if (htab[h] == fcode) {
                        ent = codetab[h]
                        break
                    }
                    if (htab[h] < 0) {
                        output(ent, os)
                        ent = c
                        if (freeEnt < maxMaxCode) {
                            codetab[h] = freeEnt++
                            htab[h] = fcode
                        } else {
                            htab.fill(-1)
                            freeEnt = clearCode + 2
                            clearFlag = true
                            output(clearCode, os)
                        }
                        break
                    }
                    h = (h + 1) % hSize
                }
            }
            output(ent, os)
            output(eofCode, os)
        }

        private fun output(code: Int, os: OutputStream?) {
            curAccum = curAccum and masks[curBits]
            if (curBits > 0) curAccum = curAccum or (code shl curBits)
            else curAccum = code
            curBits += initBits

            while (curBits >= 8) {
                charOut((curAccum and 0xff).toByte(), os)
                curAccum = curAccum shr 8
                curBits -= 8
            }

            if (freeEnt > maxMaxCode || clearFlag) {
                if (clearFlag) {
                    initBits = colorDepth + 1
                    clearFlag = false
                } else {
                    initBits++
                    if (initBits > maxBits) initBits = maxBits
                }
            }

            if (code == eofCode) {
                while (curBits > 0) {
                    charOut((curAccum and 0xff).toByte(), os)
                    curAccum = curAccum shr 8
                    curBits -= 8
                }
                flushChar(os)
            }
        }

        private fun charOut(c: Byte, os: OutputStream?) {
            accum[aCount++] = c
            if (aCount >= 254) flushChar(os)
        }

        private fun flushChar(os: OutputStream?) {
            if (aCount > 0) {
                os?.write(aCount)
                os?.write(accum, 0, aCount)
                aCount = 0
            }
        }
    }
}