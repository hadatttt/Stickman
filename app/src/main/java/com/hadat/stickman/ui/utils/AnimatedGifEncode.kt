package com.hadat.stickman.utils

import android.graphics.Bitmap
import java.io.IOException
import java.io.OutputStream

class AnimatedGifEncoder {
    private var width: Int = 0
    private var height: Int = 0
    private var delay: Int = 0
    private var repeat: Int = 0
    private var started: Boolean = false
    private var out: OutputStream? = null
    private var pixels: ByteArray? = null
    private var indexedPixels: ByteArray? = null
    private var colorTab: ByteArray? = null
    private var usedEntry = BooleanArray(256)
    private var palSize: Int = 7
    private var colorDepth: Int = 0
    private var closeStream: Boolean = false

    fun start(outStream: OutputStream): Boolean {
        out = outStream
        started = true
        try {
            writeString("GIF89a")
        } catch (e: IOException) {
            return false
        }
        return started
    }

    fun setDelay(ms: Int) {
        delay = Math.round(ms.toFloat() / 10.0f)
    }

    fun setRepeat(repeat: Int) {
        if (repeat >= 0) {
            this.repeat = repeat
        }
    }

    fun addFrame(image: Bitmap): Boolean {
        if (!started || image == null) return false
        var result = true
        try {
            if (width == 0) {
                width = image.width
                height = image.height
                writeLSD()
                writePalette()
                if (repeat >= 0) writeNetscapeExt()
            }
            writeGraphicCtrlExt()
            writeImageDesc()
            writePalette()
            writePixels(image)
        } catch (e: IOException) {
            result = false
        }
        return result
    }

    fun finish(): Boolean {
        if (!started) return false
        var result = true
        try {
            out?.write(0x3b) // GIF trailer
            out?.flush()
            if (closeStream) out?.close()
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
        for (i in s.indices) {
            out?.write(s[i].toInt())
        }
    }

    private fun writeLSD() {
        writeShort(width)
        writeShort(height)
        out?.write((0x80 or 0x70 or 0x00 or palSize) and 0xff)
        out?.write(0) // Background color index
        out?.write(0) // Pixel aspect ratio
    }

    private fun writePalette() {
        val tab = ByteArray(768)
        out?.write(tab, 0, tab.size)
        val n = (3 * 256) - tab.size
        for (i in 0 until n) out?.write(0)
    }

    private fun writeNetscapeExt() {
        out?.write(0x21) // Extension introducer
        out?.write(0xff) // App extension label
        out?.write(11) // Block size
        writeString("NETSCAPE2.0")
        out?.write(3) // Block size
        out?.write(1) // Loop sub-block ID
        writeShort(repeat) // Loop count (0 = infinite)
        out?.write(0) // Block terminator
    }

    private fun writeGraphicCtrlExt() {
        out?.write(0x21) // Extension introducer
        out?.write(0xf9) // Graphic control label
        out?.write(4) // Block size
        out?.write(0 or 0) // Packed fields
        writeShort(delay) // Delay (1/100th of a second)
        out?.write(0) // Transparent color index
        out?.write(0) // Block terminator
    }

    private fun writeImageDesc() {
        out?.write(0x2c) // Image separator
        writeShort(0) // Image left position
        writeShort(0) // Image top position
        writeShort(width)
        writeShort(height)
        out?.write(0) // Packed fields
    }

    private fun writePixels(image: Bitmap) {
        pixels = ByteArray(width * height * 3)
        indexedPixels = ByteArray(width * height)
        colorTab = ByteArray(256 * 3)
        usedEntry.fill(false)
        colorDepth = 8
        palSize = 7

        val pixels = IntArray(width * height)
        image.getPixels(pixels, 0, width, 0, 0, width, height)

        var k = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                this.pixels!![k++] = (pixel shr 16 and 0xff).toByte() // R
                this.pixels!![k++] = (pixel shr 8 and 0xff).toByte() // G
                this.pixels!![k++] = (pixel and 0xff).toByte() // B
            }
        }

        analyzePixels()
        writeLZW()
    }

    private fun analyzePixels() {
        val len = pixels!!.size
        val nPix = len / 3
        indexedPixels = ByteArray(nPix)
        val nq = NeuQuant(pixels!!, len, 10)
        colorTab = nq.process()
        for (i in 0 until nPix) {
            val index = nq.map(
                pixels!![i * 3].toInt() and 0xff,
                pixels!![i * 3 + 1].toInt() and 0xff,
                pixels!![i * 3 + 2].toInt() and 0xff
            )
            indexedPixels!![i] = index.toByte()
            usedEntry[index] = true
        }
    }

    private fun writeLZW() {
        val lzw = LZWEncoder(width, height, indexedPixels, colorDepth)
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
        private val radPower = DoubleArray(32)
        private val alpha = 1.0 / sample
        private val beta = 1.0 / 1024.0
        private val netSize = 256

        init {
            for (i in 0 until netSize) {
                network[i][0] = (i shl 8) / netSize.toDouble()
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
            var i = 0
            while (i < length) {
                val r = pixels[i++].toInt() and 0xff
                val g = pixels[i++].toInt() and 0xff
                val b = pixels[i++].toInt() and 0xff
                alterneigh(30, 0, r, g, b)
                altersingle(alpha, 0, r, g, b)
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
            for (i in 0 until netSize) {
                colorMap[i * 3] = network[i][0].toInt().toByte()
                colorMap[i * 3 + 1] = network[i][1].toInt().toByte()
                colorMap[i * 3 + 2] = network[i][2].toInt().toByte()
            }
            return colorMap
        }

        fun map(r: Int, g: Int, b: Int): Int {
            var bestd = 1000
            var best = -1
            for (i in 0 until netSize) {
                val n = network[i]
                val dist = Math.abs(n[0] - r) + Math.abs(n[1] - g) + Math.abs(n[2] - b)
                if (dist < bestd) {
                    bestd = dist.toInt()
                    best = i
                }
            }
            return best
        }

        private fun alterneigh(radius: Int, i: Int, r: Int, g: Int, b: Int) {
            val lo = i - radius
            val hi = i + radius
            for (j in lo + 1 until hi) {
                if (j < 0 || j >= netSize) continue
                val n = network[j]
                n[0] -= (alpha * (n[0] - r)).toInt().toDouble()
                n[1] -= (alpha * (n[1] - g)).toInt().toDouble()
                n[2] -= (alpha * (n[2] - b)).toInt().toDouble()
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
        private val width: Int,
        private val height: Int,
        private val pixels: ByteArray?,
        private val colorDepth: Int
    ) {
        private val maxbits = 12
        private val maxmaxcode = 1 shl maxbits
        private val htab = IntArray(5003)
        private val codetab = IntArray(5003)
        private var hsize = 5003
        private var freeEnt = 0
        private var clearFlag = false
        private var gInitBits = 0
        private var clearCode = 0
        private var EOFCode = 0
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
            gInitBits = colorDepth + 1
            clearCode = 1 shl colorDepth
            EOFCode = clearCode + 1
            freeEnt = clearCode + 2
            clearFlag = false
            aCount = 0
            var ent = pixels!![0].toInt() and 0xff
            var hshift = 0
            var fcode: Int
            for (i in 0 until hsize) htab[i] = -1
            output(clearCode, os)
            var i = 0
            while (i < width * height) {
                val c = pixels[i++].toInt() and 0xff
                fcode = (c shl maxbits) + ent
                hshift = c xor ent
                var disp = if (hshift == 0) 1 else hsize - hshift
                var j = hshift % hsize
                if (j < 0) j += hsize
                while (true) {
                    if (htab[j] == fcode) {
                        ent = codetab[j]
                        break
                    }
                    if (htab[j] < 0) {
                        output(ent, os)
                        ent = c
                        if (freeEnt < maxmaxcode) {
                            codetab[j] = freeEnt++
                            htab[j] = fcode
                        } else {
                            clHash(hsize)
                            freeEnt = clearCode + 2
                            clearFlag = true
                            output(clearCode, os)
                        }
                        break
                    }
                    j = (j + disp) % hsize
                    if (j < 0) j += hsize
                }
            }
            output(ent, os)
            output(EOFCode, os)
        }

        private fun clHash(hsize: Int) {
            for (i in 0 until hsize) htab[i] = -1
        }

        private fun output(code: Int, os: OutputStream?) {
            curAccum = curAccum and masks[curBits]
            if (curBits > 0) curAccum = curAccum or (code shl curBits)
            else curAccum = code
            curBits += gInitBits
            while (curBits >= 8) {
                charOut((curAccum and 0xff).toByte(), os)
                curAccum = curAccum shr 8
                curBits -= 8
            }
            if (freeEnt > maxmaxcode || clearFlag) {
                if (clearFlag) {
                    gInitBits = colorDepth + 1
                    clearFlag = false
                } else {
                    gInitBits++
                }
                if (gInitBits > maxbits) {
                    gInitBits--
                }
            }
            if (code == EOFCode) {
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