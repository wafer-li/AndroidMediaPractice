# 学习 MediaCodec API，完成视频 H.264 的硬编、硬解

本任务主要学习 MediaCodec 相关 API 和 H.264 相关封装格式

## MediaCodec 的裸数据和封装数据

MediaCodec 对于裸数据和对封装数据的传入要求是不一样的，对于裸数据而言，它输入可以一股脑输入，但是压缩数据就必须按照封装格式**一帧一帧**输入，除非设置 `BUFFER_FLAG_PARTIAL_FRAME`，否则不能出现不完整的帧。

## H264 的封装格式

H264 为了实现网络亲和性，把标准分成了两个部分：VCL 和 NAL，前者为视频编码层，后者为网络抽象层。

VCL 生成的数据会被包裹进 NAL 进行传输，因此，H264 最基本的传输单位为 NAL 单元，即 **NALU**

在解码时，我们就需要将数据拆分成一个个的 NALU，将其传入 MediaCodec 中。

一个 NALU 中的数据可能有如下类型：

- SPS
- PPS
- IDR 帧
- 非 IDR 帧

这些类型之后会详细描述。

NALU 也有不同的分割格式，这里展示的是使用 `startcode` 进行分割的格式。

![H.264 Bitstream](https://user-images.githubusercontent.com/12459199/65832773-eef13f80-e2fa-11e9-91b3-23d9de86ebcb.png)


### SPS 和 PPS

SPS 是序列参数集，PPS 是图像参数集，它们包含了图像的各种信息，是解码器初始化的重要参数。

一个完整的 H264 码流的第一个 NALU 是 SPS，第二个 NALU 是 PPS。

因此，我们可以直接通过这两个 NALU 向 MediaCodec 传入配置。


### IDR 帧

IDR 帧是 H264 中的一种特殊的 I 帧，称为『瞬间解码刷新』帧，当解码器接收到 IDR 帧时会刷新帧缓冲区，并将其设置为参照帧，在 MediaCodec 中，将其作为 `BUFFER_FLAG_KEY_FRAME`。

关于 IPB 帧，请看[这里](https://wafer.li/MediaDev/%E9%9F%B3%E8%A7%86%E9%A2%91%E5%BC%80%E5%8F%91%E5%9F%BA%E7%A1%80%E7%9F%A5%E8%AF%86/#53-ipb-%E5%B8%A7)。


### NALU 和帧

注意，一个 NALU 并不严格对应一帧，如下图：

![NAL](https://user-images.githubusercontent.com/12459199/65832988-d3d3ff00-e2fd-11e9-9034-d61a3c2583e5.png)

从图中可以看到，一个帧也可以包含多个 NALU，此时称这个帧为多 slice 帧。

### startcode

startcode 用于分割不同的 NALU，有 `0x00_00_00_01`(4 字节) 和 `0x00_00_01`(3 字节) 两种格式。

其中，SPS、PPS、和 Access Unit 的首个 NALU 为 4 字节，其余为 3 字节。

例如上图，第一个 I 帧的第一个 NALU 的 startcode 为 4 字节，第二个为 3 字节。

### AVCC 和 Annex-B

上面说到，NALU 具有不同的分割格式，这是因为 H264 具有不同的码流格式，主要就是小标题中的这两种，其中：

AVCC 也叫 AVC1，是 MPEG-4 格式，字节对齐，也叫 Byte-Stream Format 格式；

Annex-B 也叫 MPEG-2 TS 格式，或者 ElementaryStream 格式，用于 TS 流或者 hls 格式中。

**Android MediaCodec 硬解只支持 Annex-B 格式，iOS VideoToolBox 只支持 AVCC 格式**

因此，在 Android 平台硬解播放 mp4 需要将 AVCC 转为 Annex-B；

在 iOS 播放 ts 需要将 Annex-B 转为 AVCC。

两者的区别：

- AVCC 格式使用 NALU 长度（固定字节，字节数由 extradata 中的信息给定）进行分割，在封装文件或者直播流的头部包含 extradata 信息（非 NALU），extradata 中包含 NALU 长度的字节数以及 SPS/PPS 信息

- Annex-B格式使用 startcode 进行分割，startcode 为 `0x000001`或 `0x00000001`，SPS/PPS 作为一般 NALU 单元以 startcode 作为分隔符的方式放在文件或者直播流的头部

## MediaCodec 编码 H264 注意事项

[MediaFormat 文档](https://developer.android.com/reference/android/media/MediaFormat) 列出了视频格式需要的各种属性。

其中，作为编码器，必须传入的参数有：

- `KEY_COLOR_FORMAT`
  > 颜色格式，为 `MediaCodecInfo.CodecCapabilities.COLOR_Format*`
- `KEY_FRAME_RATE`
  > 帧率
- `KEY_CAPTURE_RATE`
  > 摄像机录制的帧率
- `KEY_I_FRAME_INTERNAL`
  > 两个 I 帧之间间隔的**秒数**，决定了 GOP 的大小，这里为两个 IDR 帧之间的间隔秒数。


这里必须要注意的是，在 API 23 之后 `MediaCodecInfo.CodecCapabilities.COLOR_Format*` 中的大多数已经被废弃了；

但是，由于本 Demo 中采用的 YUV 文件存储方法为 420 Planer，因此我在这里直接指定了 `MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar` 这样编码出来的颜色才能保证正确。

但是不是所有手机都支持 `MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar` 这一颜色格式的；

因此，最好采用推荐的 `Flexible` 选项，然后在 `configure()` 之后，获取 `inputFormat`，从中读取到真正支持的颜色格式，随后再进行颜色格式的转换即可。

最后，编码的产物是裸 H264 **码流**，一般的播放器不能直接播放，需要使用之前任务中使用过的 `MediaMuxer` 封装成 MP4 等格式才能被播放器播放。

## MediaCodec 解码注意事项

相比编码而言，解码有几个比较隐藏的坑需要注意一下。

### 传入 Codec Specific Data

在上一节中，我们提到解码器需要配置 CSD 数据，对于 H264 而言需要配置 `csd-0` 和 `csd-1`，分别为 SPS 和 PPS 数据。

但是，这两个数据通常比较多，所以，我们可以不用在 `configure` 之前配置；

而是在传入 `inputBuffer` 时设置 `BUFFER_FLAG_CODEC_CONFIG`，就可以配置对应的 CSD 数据。

从上面我们知道，SPS 和 PPS 分别为 H264 码流的前两个 NALU，因此直接对头两个 NALU 设置 `BUFFER_FLAG_CODEC_CONFIG` 即可；

或者我们可以通过判断当前 NALU 是否为 SPS 或者 PPS 来更准确的处理这个情况。


### 输出的分辨率问题

对于解码 1080p 数据，即 1920 * 1080 的数据，这里有一个很隐藏的坑需要注意；

上面说到，MediaCodec 只支持 Annex-B 格式的码流，该码流是 MPEG-2 格式，MPEG-2 要求每一行列都必须能 **被 16 整除**；

因此，最后生成的总分辨率为 **1920 * 1088**，最后的 8 个像素是填充使用，实际上是**不显示**的。

如果你天真的以为输出的数据是 1080 的，那么用播放器播放的时候画面只有第一帧是相对正常的（顶部有色带），然后之后都是花屏。

那么怎么处理呢？我们可以使用 5.0 新增的 `getOutputImage()` 可以获取这一帧的 `Image` 对象，然后我们就可以对 YUV 平面进行分别处理，丢弃最后的 8 个像素来保证我们的分辨率。
