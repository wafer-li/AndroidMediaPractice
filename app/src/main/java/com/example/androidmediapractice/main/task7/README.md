# 学习 MediaCodec API，完成音频 AAC 硬编、硬解

本任务重点学习 AAC 相关的编解码知识，结合 MediaCodec 对 AAC 实现编码和解码。

## AAC 介绍

在进行正式的编解码工作之前，首先来介绍一下 AAC 文件。

AAC 是一种有损压缩音频格式，是 MP3 的后继者，在相同的码率之下能达到比 MP3 更好的音频质量，通常用于视频文件的音频轨道。

AAC 是 MPEG-2 和 MPEG-4 标准的一部分，通常的文件后缀有三种：

1. `.aac`，使用 MPEG-2 的 ADTS 封装
2. `.mp4`，使用 MPEG-4 的 ADTS 封装，如果作为音频轨道，可以没有 ADTS 头
3. `.m4a`，只包含音频的 MP4 文件

AAC 具有不同的规格，按照容器和规格可以组成不同的类型——

容器： MPEG-2，MPEG-4

规格： LC，MAIN，SSR，LTP，LD，HE

AAC 文件是由一个一个的 ADTS 帧组成的，一个 ADTS 帧由 ADTS 头部和数据部组成，ADTS 头部的具体参数说明见[这里](https://wiki.multimedia.cx/index.php/ADTS)

我们之后编码得到的 AAC 流需要加上 ADTS 头才能作为 AAC 文件播放，否则不能被播放器播放。

## MediaCodec 使用

之前在任务四的时候简单介绍了 MediaCodec 的编解码架构，这里就不多赘述

MediaCodec 作为媒体格式的编解码工具，主要有以下的步骤：

1. 创建
2. 配置开启
3. 输入输出
4. 关闭释放

### 创建

MediaCodec 主要通过静态工厂使用 MIME 进行创建：

1. 通过 `MediaCodec.createEncoderByType(mime)` 创建编码器
1. 通过 `MediaCodec.createDecoderByType(mime)` 创建解码器

### 配置开启

MediaCodec 的配置主要通过 `MediaCodec.configure(mediaFormat, surface, crypto, flag)` 这个函数进行，其中：

- `mediaFormat`: 指明了需要编解码的媒体格式的相关信息
- `surface`: MediaCodec 解码视频支持直接渲染，这是渲染的目标 Surface
- `crypto`：MediaCodec 支持编解码加密格式
- `flag`: 主要用来指明是编码器还是解码器

其中，与 AAC 编解码相关的是 `mediaFormat` 和 `flag` 这两个参数；

`surface` 和 `crypto` 在这里我们都设置为 `null`;

当 `surface` 设置为 `null` 时，它的输出是通过 `ouputBuffer` 输出，如果设置了 `surface`，那么就可以通过 `releaseOuputBuffer(outputBufferIndex, true)` 渲染到 Surface 上。

`flag` 通过指定为 `MediaCodec.CONFIGURE_FLAG_ENCODE` 指示该 MediaCodec 作为编码器。

`mediaFormat` 的配置倒是有点复杂，编码器和解码器需要分别做配置。

#### 编码器配置

首先，我们通过 `MediaFormat.createAudioFormat(mime, sampleRate, channelCount)` 创建对应的音频 `MediaFormat`

随后要设置三个参数，包括 `KEY_BIT_RATE`，`KEY_MAX_INPUT_SIZE`, `KEY_AAC_PROFILE`

其中，比特率指的是 **输出** 的比特率，而不是输入的比特率；

`KEY_MAX_INPUT_SIZE` 指明获取的 `inputBuffer` 的最大容量

`KEY_AAC_PROFILE` 指的是 AAC 中对应的 `AudioObjectType` 的值。

#### 解码器配置

解码器的配置需要在编码器的配置的基础上增加一些必要的参数，否则不能够成功解码，包括 `KEY_IS_ADTS` 和 `csd-0`

首先是 `KEY_IS_ADTS`，它的值是 0 或者 1，表示输入是否包含 ADTS 头。

`csd-0` 是一个 **C**odec **S**pecific **D**ata，类型是一个 `byte` 数组，对于每一种媒体格式都有不同的值。

对于 AAC，我们需要提供一个这样的 `csd-0`，一共两个 `byte`：

```java
// AAC CSD_0 Format
oooo offf fccc c000
o - audio object type
f - sample frequency
c - channel configuration
```

使用 `setByteBuffer("csd-0", buffer)` 设置 `csd-0` 数据。

#### 开启

使用 `start()` 开启 MediaCodec

### 输入输出

MediaCodec 输入输出主要通过 `inputBuffer` 和 `outputBuffer` 的相关功能完成。

#### 输入

输入步骤为：

1. `dequeueInputBuffer(timeoutUs)` 获取 `inputBufferIndex`
2. `getInputBuffer(inputBufferIndex)` 获取 `inputBuffer`
3. 给 `inputBuffer` 填入数据
4. 使用 `queueInputBuffer(inputBufferIndex, offset, size, presentationTimeUs, flags)` 将 `inputBuffer` 传入 MediaCodec 中进行处理

注意事项：

首先，`inputBuffer` 是一个 `DirectByteBuffer`，因此需要注意**字节序**问题。

`inputBuffer` 是小端序存取的，如果输入的 PCM 文件是大端序的，就需要进行字节序转换，否则编码出来的 AAC 文件就是一堆噪声。

其次，`dequeInputBuffer()` 方法在 `inputBuffer` 没有准备好时，会返回 `-1`，因此需要检查 `inputBufferIndex` 之后，才能传入 `getInputBuffer()` 之中。

第三，在文件读取到 EOF 时，`read()` 方法会返回 `-1`，但是 `queueInputBuffer()` 的 `size` 参数不能小于 `0`，因此需要对这个情况特殊处理，如果已经 EOF，那么需要给 `flag` 设置 `MediaCodec.BUFFER_FLAG_END_OF_STREAM` 指示读取完毕。

最后，在解码时，需要 **一帧一帧** 喂给 `inputBuffer`，而不能一股脑把 `inputBuffer` 塞满；

假如 `inputBuffer` 中含有不完整的帧，那么就会导致解码错误从而程序崩溃。

#### 输出

输出步骤为：

1. `dequeueOutputBuffer(bufferInfo, timeoutUs)` 获取 `outputBufferIndex`
2. `getOutputBuffer(outputBufferIndex)` 获取 `outputBuffer`
3. 从 `outputBuffer` 中取出数据
3. `releaseOuputBuffer(outputBufferIndex, render)` 将 `outputBuffer` 回收

注意事项：

`bufferInfo` 是一个输出参数，在调用方法之后，它会包含与该 `outputBuffer` 相关的一些信息，包括 `offset`、`size` 和 `flag`

因此，我们在取出 `outputBuffer` 之后，需要根据 `offset` 和 `size` 调整它对应的 `position` 和 `limit` 来正确读取数据。

同时，`dequeueOutputBuffer()` 也会返回负数值，表明 `outputBuffer` 没准备好或者转换，此时不能传入 `getOutputBuffer()`，否则会出错。

在 `bufferInfo.size <= 0 || bufferInfo.flag == END_OF_STREAM` 时，表明编解码已经完毕，需要关闭和释放 MediaCodec。

对于编码器，我们在获取到对应的每一个 AAC 数据流之后，需要给它们附上对应的 ADTS 头，否则得到的 AAC 文件将无法播放。

### 关闭和释放

调用 `MediaCodec.stop()` 关闭 MediaCodec，`MediaCodec.release()` 释放。

在释放之后，不能再调用 `start()` 否则会出错。

## 造成 IllegalStateException 的不完全原因

在使用 MediaCodec 时，有很多种原因会造成各种地方的 `IllegalStateException`，这里做一个不完全梳理。

1. 未调用 `start()`
2. `stop()` 之后继续调用 `inputBuffer` 和 `outputBuffer` 相关函数
3. `inputBufferIndex` 和 `outputBufferIndex` 的数值非法
4. 解码器未设置 `csd-0`
5. 解码器的 `inputBuffer` 包含不完整的 ADTS 帧
6. `END_OF_STREAM` 之后继续调用 `inputBuffer` 和 `outputBuffer` 相关函数

## 需要注意的位操作技巧

ADTS 头的设置需要一些位操作技巧，这里进行相关的一些总结

### 拼接操作

```
AA0000 | 00BB00 | 0000CC  = AABBCC
```

### 获取对应位操作

```
AAXXYY & 110000 = AA0000
XXBBYY & 001100 = 00BB00
CC0000 >> 4 = 0000CC
```

### 十六进制转二进制问题

十六进制中，一个 `F` **四位**二进制，`0xFF` 为一个 `byte`

### 超出视界问题

在进行左移操作时，需要考虑『超出视界』问题。

所谓『超出视界』指的是，进行位操作的数值的实际位数要大于我们以为的位数，例如数值 `7` 只需要 3 位二进制即可表示，但是实际进行左移操作的数值 `7` 是整形，拥有 32 位。

下面是这个问题的形象表达：

```
                    -----------------------
0000 0000 0000 0000 | 1111 0000 1111 0000 |
                    -----------------------
                             视界
                    -----------------------
0000 0000 0000 0001 | 1110 0001 1110 0000 |
                  ^ -----------------------
```

可以看到，下面的数经过一个左移操作之后，有一个 `1` 超出了我们的视界，此时我们要意识到有东西超出了我们的视界，需要进行一个位与操作保证视界以外的数值都为 `0`

例如上面这个例子位与一个 `0xFFF` 即可。
