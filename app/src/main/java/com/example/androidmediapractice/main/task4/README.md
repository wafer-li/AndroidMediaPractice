# 学习 Android 平台的 MediaExtractor 和 MediaMuxer API，知道如何解析和封装 mp4 文件

本任务主要目标在于学习 `MediaExtractor` 和 `MediaMuxer` 的相关使用，学习使用 Android API 解析和封装 mp4 文件

## MediaExtractor 使用

`MediaExtractor` 顾名思义，就是解析视频文件的音视频轨道用的，将各个轨道『解压』出来。

这里主要通过使用 `MediaExtractor` 解析 mp4 文件，提取视频轨道，并使用 `MediaCodec` 进行解码播放，来学习 `MediaExtractor` 的相关使用。

具体的使用步骤如下：

1. 构建 `MediaExtractor` 实例
2. `MediaExtractor.setDataSource()` 设置数据源
3. 选取想要的视频、音频轨道
   > `trackCount` 可获取 mp4 文件的轨道数量，包括视频轨道和音频轨道
   >
   > `getTrackFormat(trackIndex)` 可获取对应轨道的 `MediaFormat`
   >
   > 从中可以获得轨道的很多信息，比如比特率、视频长宽之类的
   >
   > 这里我们需要提取视频轨道，那么获取轨道的 `MIME` 值，并筛选 `video/` 即可
   >
   > 后续可以用这个来构建 `MediaCodec` 对象
   >
   > 同时，我们还可以获取 `KEY_MAX_INPUT_BUFFER` 值来构建对应的缓冲区
4. `MediaExtractor.readSampleData(buffer, offset)` 获取音视频数据
5. `MediaExtractor.advance()` 前进到下一个 sample
6. 使用 `sampleTime` 获取当前 sample 的时间戳
7. 使用 `sampleFlags` 获取当前的播放状态

注意事项：

`setDataSource()` 不仅可以传入文件路径，还可以传入 URL，如果使用 URL，则需要声明网络权限

`readSampleData(buffer, offset)` 会返回读取的字节大小，如果已经 `END_OF_STREAM`
 那么就会返回 -1。
 
`sampleTime` 会返回当前 sample 的时间，但是如果已经 `END_OF_STREAM` 则会返回 -1。

## MediaMuxer 使用

`MediaMuxer` 是混合器，是将音视频轨道混合起来封装成一个视频文件的。

这里通过使用 `MediaExtractor` 拆分 `sample.mp4` 再使用 `MediaMuxer` 将其封装得到 `output.mp4` 来学习相关内容。

具体的使用步骤如下：

1. 构建 `MediaMuxer` 实例，构造参数有输出路径和对应的媒体格式
2. 使用 `MediaMuxer.addTrack(trackFormat)` 增加轨道
3. 使用 `MediaMuxer.setOrientationHint(degrees)` 设置旋转角度（**重要**）
4. 使用 `MediaMuxer.start()` 开始封装
5. 使用 `MediaMuxer.writeSampleData(trackIndex, buffer, bufferInfo)` 写入数据
6. 使用 `MediaMuxer.stop()` 结束封装
7. 使用 `MediaMuxer.release()` 释放资源

特别注意：

`setOrientationHint()` 和 `addTrack()` 必须在 `start()` 之前调用，否则会报 `Muxer is not initialized.`

`MediaMuxer` 会消去轨道自带的 `rotation-degrees`，需要重新使用 `setOrientationHint()` 来设置旋转角度，角度为顺时针。
这也是为什么上面专门标注重要的原因。

`MediaFormat` 中的 `KEY_ROTATION` 在 API 23 以上才可用，API 23 以下使用 `"rotation-degrees"` 代替。

在写入数据时，需要设置 `bufferInfo` 的相关值，其中包括：

- flags: `MediaCodec.BUFFER_FLAG_KEY_FRAME` 或 `MediaCodec.BUFFER_FLAG_END_OF_STREAM`，两者之一
- offset: 通常为 0
- size: 即 buffer 的大小，在这里为 `extractor.readSampleData()` 的返回值
- presentationTimeUs: 即 PTS，在这里为 `extractor.sampleTime`

同时，在写入时，必须注意这几个值的限制范围，具体的限制如下： 

```java
if (bufferInfo.size < 0 || bufferInfo.offset < 0
                || (bufferInfo.offset + bufferInfo.size) > byteBuf.capacity()
                || bufferInfo.presentationTimeUs < 0) {
            throw new IllegalArgumentException("bufferInfo must specify a" +
                    " valid buffer offset, size and presentation time");
        }
```

在这里就要注意 `sampleTime` 有可能为 `-1`，注意判断它的值来对 `presentationTimeUs` 进行 clamp 操作。

## MediaCodec 使用

虽然这个任务没有涉及这个东西，这里还是来讲一下具体它的架构和使用。

`MediaCodec` 是 Android SDK 中的音视频编解码类，即，它既可以当编码器，也可以当解码器。

这里，我们将其作为解码器来使用。

其内部拥有两个 buffer 序列—— inputBuffer 和 outputBuffer

外部客户使用 `queueInputBuffer()` 方法来向其输入数据，使用 `releaseOutputBuffer()` 来释放其输出数据。

![inputBuffer and outputBuffer](https://user-images.githubusercontent.com/12459199/64943275-e716b180-d89d-11e9-9175-16b04dab7076.png)

同时，其内部还有一个状态机流转过程：

![States](https://user-images.githubusercontent.com/12459199/64943426-47a5ee80-d89e-11e9-892f-4586508cae89.png)

使用步骤如下：

1. 准备用于播放的 `Surface`
1. 通过 `MIME` 构建 `MediaCodec` 实例
3. 使用 `MediaCodec.configure(mediaFormat, surface, 加密, 编解码器的 flag)` 
4. 使用 `start()` 开始编解码
5. `dequeueInputBuffer()` 获取队列中空闲的 buffer 的下标
6. `queueInputBuffer(bufferIndex, offset, size, pts, flag)` 将写好的 buffer 推入 `MediaCodec` 进行处理
    > `flag` 是一个位掩码，如果已经 `END_OF_STREAM`，就必须设置 `MediaCodec.BUFFER_FLAG_END_OF_STREAM` 作为位掩码
7. `dequeueOutputBuffer(bufferInfo, 等待时间)` 返回完成的 outBuffer 的下标
    > `bufferInfo` 包含了该下标对应的 buffer 的相关信息，具体信息见上
    >
    > 该方法也会返回一些异常值(负值)，此时说明数据没有准备好，跳过即可。
8. `releaseOutputBuffer(bufferIndex, isRender)` 来释放输出数据
    > 如果 `isRender == true` 那么就会将数据渲染到我们之前 `configure()` 传入的 `surface` 中



