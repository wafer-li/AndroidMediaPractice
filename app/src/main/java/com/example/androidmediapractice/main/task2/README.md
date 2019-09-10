# 在 Android 平台使用 AudioRecord 和 AudioTrack API 完成音频 PCM 数据的采集和播放，并实现读写音频 wav 文件

这个任务重点是学习使用 `AudioRecord` 和 `AudioTrack` 的音频录制和播放流程，同时熟悉 WAV 文件格式。

## 音频录制的裸数据

在介绍录制之前，首先介绍一下音频录制的裸数据——PCM 格式。

我们知道，声音是一种波，话筒在录制音频时，实际上相当于构建了一个声波的模拟信号。

而将模拟信号数字化，我们需要进行一个重要的步骤——采样。

所谓采样，就是对一个连续的物理量，按照指定的时间间隔记录下它在当时的值

![](https://pic2.zhimg.com/80/v2-e7fe1c6b13bbb6be4deb0b900c435eda_hd.jpg)

采样有几个相关的参数：

- 采样频率
- 采样深度

采样频率指的是一秒钟采样多少次，单位为赫兹(Hz)
采样深度指的是，一个采样数值使用多少位的二进制进行表示，有 8、16、32 位等

从上图我们可以看出，采样频率和采样深度越高，那么声音的还原度就越好；按照采样定理，我们的采样频率需要至少在原声波频率的两倍以上才能完美重现原有声波信号。

因此，通常 CD 的采样频率为 41kHz，采样深度通常为 16bit。

## AudioRecord 音频录制步骤

1. 构建 `AudioRecord` 实例：`AudioRecord(音源，采样频率(kHz)，声道，采样深度，bufferSize)`
2. `AudioRecord.start()` 开始录音
3. `AudioRecord.read()` 拉取 PCM 数据
4. `AudioRecord.stop()` 停止录音
5. `AudioRecord.release()` 释放资源


## AudioTrack 音频播放步骤
1. 构建 `AudioTrack` 实例
2. 调用 `AudioTrack.play()` 开始播放音频
3. 调用 `AudioTrack.write()` 写入 PCM 数据
4. 调用 `AudioTrack.stop()` 停止音频播放
5. 调用 `AudioTrack.release()` 释放资源


## 特别注意

`AudioTrack` 和 `AudioRecord` 所使用的 `bufferSize` 可以通过 `getMinBufferSize()` 获取，所得到的是 **一秒钟的数据大小**


构建 `AudioTrack` 实例时，有 `MODE_STATIC` 和 `MODE_STREAM` 两个模式；

`STATIC` 模式是将音频数据一次性准备好，再开始播放，即先 `write()` 再 `play()`

`STREAM` 模式是在播放时不断写入音频数据，即先 `play()`，再 `write()`


由于目前的音频数据都相对较大，因此建议使用 `STREAM` 模式进行播放。

## WAV 文件格式

WAV 文件是由微软退出的波形音频文件，其实质为一个 40 bytes 的文件头 + PCM 音频数据。

具体的文件格式说明详见[这里](http://soundfile.sapp.org/doc/WaveFormat/)

## 字节序问题

这是一个潜在的坑，在本任务中，在音频播放和转换为 WAV 文件时会遇到。

大端序指的是：在读取的时候，优先读取**高位字节**；
小端序指的是：在读取的时候，优先读取**低位字节**。

Java 文件系统默认使用 **大端序**；
C++ 文件系统默认使用 **小端序**。

WAV 文件中的 PCM 数据为**小端序**；
`AudioTrack` 使用的 PCM 数据为 **大端序**。

如果字节序搞反了，那么播放出来的音频就是 **噪声**；
