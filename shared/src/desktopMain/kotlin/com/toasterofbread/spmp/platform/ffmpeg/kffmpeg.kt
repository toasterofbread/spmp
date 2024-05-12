package com.toasterofbread.spmp.platform.ffmpeg

// Thanks to timo-drick
// https://github.com/timo-drick/cfd_video/blob/eb702eb260c68aba685b6e12e7e331681314622e/Compose%20for%20Desktop%20Video%20Player/playerFFMPEG/src/main/kotlin/de/appsonair/cppffmpeg/kffmpeg.kt

import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import org.bytedeco.ffmpeg.avcodec.AVCodecContext
import org.bytedeco.ffmpeg.avcodec.AVCodecHWConfig
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters
import org.bytedeco.ffmpeg.avcodec.AVPacket
import org.bytedeco.ffmpeg.avformat.AVFormatContext
import org.bytedeco.ffmpeg.avutil.AVBufferRef
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.ffmpeg.global.avformat
import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.ffmpeg.global.swscale
import org.bytedeco.javacpp.BytePointer
import org.bytedeco.javacpp.DoublePointer
import org.bytedeco.javacpp.PointerPointer
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.nio.charset.Charset
import kotlin.math.absoluteValue
import kotlin.math.min

enum class KAVMediaType(val id: Int) {
    UNKNOWN(avutil.AVMEDIA_TYPE_UNKNOWN),
    VIDEO(avutil.AVMEDIA_TYPE_VIDEO),
    AUDIO(avutil.AVMEDIA_TYPE_AUDIO),
    DATA(avutil.AVMEDIA_TYPE_DATA),
    SUBTITLE(avutil.AVMEDIA_TYPE_SUBTITLE),
    ATTACHMENT(avutil.AVMEDIA_TYPE_ATTACHMENT),
    NB(avutil.AVMEDIA_TYPE_NB);
    companion object {
        private val idMap = values().associateBy { it.id }
        fun fromId(id: Int) = idMap[id] ?: UNKNOWN
    }
}

data class KAVCodec(
    val id: Int,
    val name: String,
    val hwDecoder: List<KHWDecoder>
)
data class KHWDecoder(
    val type: Int,
    val name: String
)

sealed interface KAVStream {
    val id: Int
}
data class KVideoStream(
    override val id: Int,
    val width: Int,
    val height: Int,
    val durationMillis: Long,
    val fps: Float,
    val bitrate: Long,
    val codec: String
): KAVStream
data class KAudioStream(
    override val id: Int
): KAVStream

class KAVFormatContext {
    val fmtCtx = AVFormatContext(null)

    fun openInput(filePath: String) {
        if (avformat.avformat_open_input(fmtCtx, filePath, null, null) < 0)
            throw IllegalStateException("Open file $filePath failed!")
        if (avformat.avformat_find_stream_info(fmtCtx, null as PointerPointer<*>?) < 0)
            throw IllegalStateException("Unable to open stream_info!")
    }

    fun closeInput() {
        avformat.avformat_close_input(fmtCtx)
    }

    fun findMetadata(): Map<String, String> {
        val metadataMap = mutableMapOf<String, String>()
        var entry: AVDictionaryEntry? = null
        do {
            entry = avutil.av_dict_iterate(fmtCtx.metadata(), entry)
            if (entry != null) {
                val key = entry.key().getString(Charset.defaultCharset())
                val value = entry.value().getString(Charset.defaultCharset())
                metadataMap[key] = value
            }
        } while (entry != null)
        return metadataMap
    }

    fun findVideoStreams(): List<KVideoStream> =
        (0 until fmtCtx.nb_streams()).mapNotNull { i ->
            val type = KAVMediaType.fromId(fmtCtx.streams(i).codecpar().codec_type())
            if (type == KAVMediaType.VIDEO) {
                val stream = fmtCtx.streams(i)
                val width = stream.codecpar().width()
                val height = stream.codecpar().height()
                val duration = stream.duration() * 1000L * stream.time_base().num().toLong() / stream.time_base().den().toLong()
                val fps = stream.avg_frame_rate().num().toFloat() / stream.avg_frame_rate().den().toFloat()
                val bitrate = stream.codecpar().bit_rate()
                val codecName = avcodec.avcodec_get_name(stream.codecpar().codec_id()).getString(Charset.defaultCharset())
                KVideoStream(
                    id = i,
                    width = width,
                    height = height,
                    durationMillis = duration,
                    fps = fps,
                    bitrate = bitrate,
                    codec = codecName
                )
            } else {
                null
            }
        }

    fun findCodec(videoStream: KVideoStream): KAVCodec {
        val stream = fmtCtx.streams(videoStream.id)
        val codecId = stream.codecpar().codec_id()
        val type = stream.codecpar().codec_type() // Normally this is 0 -> avutil.AV_HWDEVICE_TYPE_NONE
        val name = avcodec.avcodec_get_name(codecId).getString(Charset.defaultCharset())
        val hwDecoder = findHWDecoder(codecId) // search for hardware accelerated decoders
        return KAVCodec(
            id = codecId,
            name = name,
            hwDecoder = hwDecoder
        )
    }

    private fun findHWDecoder(codecId: Int): List<KHWDecoder> {
        val codec = avcodec.avcodec_find_decoder(codecId)
            ?: throw IllegalStateException("No codec found!")
        val codecType = codec.type()
        val supportHwTypes = mutableSetOf<Int>()
        var index = 0
        do {
            val config: AVCodecHWConfig? = avcodec.avcodec_get_hw_config(codec, index)
            if (config != null && (config.methods() and avcodec.AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX > 0)) {
                supportHwTypes.add(config.device_type())
            }
        } while (config != null && index++ < 10)

        val decoder = mutableListOf<KHWDecoder>()
        var hwType = avutil.av_hwdevice_iterate_types(codecType)

        index = 0
        while (hwType != avutil.AV_HWDEVICE_TYPE_NONE && index++ < 20) {
            val name = avutil.av_hwdevice_get_type_name(hwType).getString(Charset.defaultCharset())
            if (hwType in supportHwTypes)
                decoder.add(KHWDecoder(hwType, name))
            hwType = avutil.av_hwdevice_iterate_types(hwType)
        }
        return decoder
    }
}

class KAVCodecContext(codecParams: AVCodecParameters, hwDecoder: KHWDecoder? = null) {
    val codecCtx: AVCodecContext = avcodec.avcodec_alloc_context3(null)

    init {
        avcodec.avcodec_parameters_to_context(codecCtx, codecParams)
        val codec = avcodec.avcodec_find_decoder(codecParams.codec_id())
            ?: throw IllegalStateException("No codec found!")

        if (hwDecoder != null) {
            //val hwConfig = avcodec.avcodec_get_hw_config(codec, 0)
            // HWAccel
            val hwDeviceCtx = AVBufferRef()
            errCheck(
                avutil.av_hwdevice_ctx_create(hwDeviceCtx, hwDecoder.type, null as BytePointer?, null, 0),
                "Unable to create hw decoder ${hwDecoder.type}:${hwDecoder.name} for ${codec.name().getString(Charset.defaultCharset())}"
            )
            codecCtx.hw_device_ctx(avutil.av_buffer_ref(hwDeviceCtx))
        }

        //open
        if (avcodec.avcodec_open2(codecCtx, codec, null as PointerPointer<*>?) < 0)
            throw IllegalStateException("Can not open codec!")
    }

    fun close() {
        avcodec.avcodec_close(codecCtx)
        avcodec.avcodec_free_context(codecCtx)
    }
}

class KFrameGrabber(
    val kStream: KVideoStream,
    kFmtCtx: KAVFormatContext,
    val hwDecoder: KHWDecoder? = null,
    targetSize: IntSize? = null
) {
    private val fmtCtx = kFmtCtx.fmtCtx
    private val stream = fmtCtx.streams(kStream.id)
    private val kCodecContext = KAVCodecContext(stream.codecpar(), hwDecoder)

    private val codecCtx = kCodecContext.codecCtx

    val targetWidth = targetSize?.width ?: codecCtx.width()
    val targetHeight = targetSize?.height ?: codecCtx.height()

    private val dstFormat = avutil.AV_PIX_FMT_BGRA // Format matches the internal skia image format
    private val skiaFormat = ImageInfo(targetWidth, targetHeight, ColorType.BGRA_8888, ColorAlphaType.OPAQUE) //N32 == BGRA_8888 Native format

    //private val dstFormat = avutil.AV_PIX_FMT_RGB565 // 2 Byte per pixel -> half memory size for one image
    //private val skiaFormat = ImageInfo(targetWidth, targetHeight, ColorType.RGB_565, ColorAlphaType.OPAQUE)

    private val timeBaseNum = stream.time_base().num().toLong()
    private val timeBaseDen = stream.time_base().den().toLong()

    //Prepare rgb image
    private val numBytes = avutil.av_image_get_buffer_size(dstFormat, targetWidth, targetHeight, 1)
    private val jArray = ByteArray(numBytes)

    private val frame = avutil.av_frame_alloc() //TODO maybe null pointer handling
    private val hwFrame = avutil.av_frame_alloc()

    private val pFrameRGB = avutil.av_frame_alloc().also { frame ->
        val buffer = BytePointer(avutil.av_malloc(numBytes.toLong()))
        avutil.av_image_fill_arrays(frame.data(), frame.linesize(), buffer, dstFormat, targetWidth, targetHeight, 1)
    }
    private val bufferBitmap = Bitmap().also {
        it.allocPixels(skiaFormat)
    }
    val composeImage = bufferBitmap.asComposeImageBitmap()

    fun close() {
        avutil.av_frame_free(frame)
        avutil.av_frame_free(hwFrame)
        kCodecContext.close()
    }

    private val pkt = AVPacket()
    var decodedFrameCounter = 0L
        private set
    var bitmapFrameCounter = 0L
        private set
    private var currentImageTS: Long = 0L
    fun grabNextFrame(ts: Long) {
        //TODO seek to ts when diff is to high or negative diff
        /*if (frameCounter == 0) {
            avformat.av_seek_frame(fmtCtx, stream, 120, avformat.AVSEEK_FLAG_BACKWARD)
        }*/
        val timeDiff = currentImageTS - ts
        //println("pts: ${hwFrame.pts()} td: $timeDiff")
        if (timeDiff.absoluteValue > 500) {
            val seekPts = ts * timeBaseDen / timeBaseNum / 1000L
            // println("Time diff: $timeDiff seek to $seekPts ${hwFrame.pts()}")
            avformat.av_seek_frame(fmtCtx, stream.index(), seekPts, avformat.AVSEEK_FLAG_BACKWARD)
        } else if(timeDiff in 11..499) {
            return
        }

        var repeatCounter = 0
        do {
            do {
                val rf = avformat.av_read_frame(fmtCtx, pkt)
                if (rf < 0) return
            } while (pkt.stream_index() != stream.index())
            val r1 = avcodec.avcodec_send_packet(codecCtx, pkt)
            avcodec.av_packet_unref(pkt)
            val r2 = avcodec.avcodec_receive_frame(codecCtx, hwFrame)
            decodedFrameCounter++
            // if (r2 < 0) {
            //     println("ret1 $r1 ret2 $r2")
            //     println("Frame skipped!")
            // }
            val frameTime = hwFrame.pts() * 1000L * timeBaseNum / timeBaseDen
            currentImageTS = frameTime

            val isPast = frameTime < ts
        } while (r2 < 0 || (isPast && repeatCounter++ < 10))
        val hwFormat = codecCtx.hwaccel()?.pix_fmt() ?: -1
        val srcFrame = if (hwFrame.format() == hwFormat) {
            //Transfer data from hardware accel device to memory
            errCheck(
                avutil.av_hwframe_transfer_data(frame, hwFrame, 0),
                "Error transferring data to system memory!"
            )
            frame
        } else {
            hwFrame
        }
        //val frameFormat = avutil.av_get_pix_fmt_name(hwFrame.format()).getString(Charset.defaultCharset())
        //val srcFormat = avutil.av_get_pix_fmt_name(srcFrame.format()).getString(Charset.defaultCharset())
        //println("hwFormat: $frameFormat -> $srcFormat")
        //println("Frame: ${hwFrame.best_effort_timestamp()} format: ${frame.format()} (hw:${hwFrame.format()})")

        val swsCtx = swscale.sws_getContext(
            codecCtx.width(), codecCtx.height(), srcFrame.format(), //Src size, format
            targetWidth, targetHeight, dstFormat, //Dst size, format
            swscale.SWS_BILINEAR, null, null, null as DoublePointer?
        )
        //println("Frame format: ${frame.format()}")
        errCheck(
            swscale.sws_scale(
                swsCtx, srcFrame.data(), srcFrame.linesize(),
                0, codecCtx.height(),
                pFrameRGB.data(),
                pFrameRGB.linesize()
            ),
            "sws_scale error!"
        )
        // Copy data to java array
        pFrameRGB.data(0).get(jArray)
        // Copy java array to bitmap
        bufferBitmap.installPixels(jArray)
        bitmapFrameCounter++
    }
}

fun errCheck(returnCode: Int, errorMessage: String? = null) {
    if (returnCode < 0) {
        val message = if (errorMessage != null)
            "$errorMessage - code $returnCode: ${av_err2str(returnCode)}"
        else
            "$returnCode: ${av_err2str(returnCode)}"
        throw IllegalStateException(message)
    }
}

fun av_err2str(errNum: Int): String {
    val charArray = ByteArray(avutil.AV_ERROR_MAX_STRING_SIZE)
    avutil.av_strerror(errNum, charArray, avutil.AV_ERROR_MAX_STRING_SIZE.toLong())
    val string = charArray.decodeToString()
    val endIndex = min(string.indexOf(Char(0)), avutil.AV_ERROR_MAX_STRING_SIZE)
    return string.subSequence(0, endIndex).toString()
}
