# 在 Android 平台使用 Camera API 进行视频的采集，分别使用 SurfaceView、TextureView 来预览 Camera 数据，取到 NV21 的数据回调


这个任务稍微有点过时了，目前主流的摄像头  API 应该为 Camera2，而且其主要的视频裸数据回调应为 `YUV420_888`

所以本任务的实现改为采用 Camera2 API 进行视频采集，并获取 `YUV420_888` 数据回调。

##  Camera2 API 的使用

Camera2 API 采用了一套全新的架构组织，是通过 `Session` 和底层的摄像头设备进行通信，并通过 `Request` 传达拍照或者摄像的具体操作。

Camera2 API 使用的具体步骤如下：


1. 准备好预览界面(`SurfaceView/TextureView`)
2. 构建 `HandlerThread` 子线程
1. 获取 `CameraManager`
4. 通过`CameraCharacteristic`从`CameraManager.cameraIdList` 中获取对应的摄像头 ID
5. 通过摄像头 ID 获取其`CameraCharacteristic`
6. 从`CameraCharacteristic`中获取 `StreamConfigMap`
7. 从 `StreamConfigMap` 中获取可用的输出尺寸，并依此确定录制视频的尺寸和预览的尺寸
8. 从 `CameraCharacteristic` 中获取可用的 FPS 值和对应的摄像头传感器旋转值
9. 根据 7 中获取到的录制视频的尺寸构建 `ImageReader` 实例
10. 使用 `manager.openCamera()` 发送打开摄像头请求，并在回调中获取 `CameraDevice` 实例
11. 使用 `CameraDevice` 实例构建 `CaptureRequest`
12. 调用 `CameraDevice.createCaptrueSession()` 开启会话
13. 在 `CaptrueSession` 中设置之前的 `CaptrueRequest` 即可实现拍照或者录像

`CaptureRequest` 的构建需要传入视频数据展示的 `Surface` 实例，通过 `CaptureRequest.addTarget()` 可以增加采集到的视频画面的输出地。

上面构建的 `ImageReader` 实例含有一个 `Surface`，将该 `Surface` 作为 `CaptureRequest` 的 `Target` 就可以读取摄像头捕获到的帧数据。

另外，无论是开启会话还是构建请求，都需要传入一个 `Handler` 对象，这个 `Handler` 实际上也就指定了会话或者请求回调的线程；这也是为什么我们需要构建一个全新的 `HandlerThread` 的原因。

## ImageReader 读取帧数据

通过 `ImageReader` 的 `setOnImageAvailableListener()` 可以设置当摄像头捕获到的帧数据传递到 `ImageReader` 的 `Surface` 时的回调。

在这个回调方法中，我们就可以获取每一帧的数据。

每一帧的数据都会被包含在一个 `Image` 类中，通过回调传入的 `ImageReader` 对象的 `acquireLatestImage()` 方法可以获取到。

注意一个 `Image` 类会很大，在使用完一个 `Image` 对象之后，记得尽快调用 `close()` 方法释放资源。

`Image` 类中，有一个 `Plane` 数组，其中，一个 `Plane` 就是一个通道平面，至于是什么平面，这和 `ImageReader` 的构建参数 `ImageFormat` 有关。

假如采用本任务中的 `YUV420_888` 格式，那么，第一个 `Plane` 为 Y 平面，第二个为 U 平面， 第三个为 V 平面。

一个 `Plane` 含有一个包含数据的 `ByteBuffer`，`rowStride` 和 `pixelStride` 组成。

其中，要注意的是 `pixelStride`，如果当前数据的下标为 `i`，那么下一个 **有效数据** 的下标为 `i + pixelStride`。

如果采用 `YUV420_888` 格式，那么 UV 平面的 `pixelStride` 都是 `2`！

这点要特别注意。

## 预览界面的注意事项

录制视频显然我们需要一个预览界面，但是，问题在于摄像头的最大的可用分辨率很可能会大大高于我们的屏幕分辨率。

就荣耀 8x 来说，它的摄像头最高可以录制 4k 视频，但是屏幕的分辨率也就只有 2380 * 1080 。

这样子就造成了我们的预览和录制的分辨率不同，因此需要获取可用的输出尺寸，并选择一个和我们预览界面大小最为贴近的。

但是，摄像头的输出尺寸是有限的，它只有有限个宽高比，但是我们的预览界面的宽高比是可以由我们自己去设定的；

假如我们预览界面的宽高比和所选择的输出尺寸的宽高比不匹配，就会造成预览界面的图像变形。

所以，我们再获取到最佳的预览尺寸之后，需要按照它的宽高比，对我们的预览界面进行调整，让它们的宽高比一致，这样在预览界面中的图像就不会变形了。

之前本人还想过另一个思路，就是如果采集到的图像的宽高比和预览界面不匹配，那么我直接将采集到的图像拉伸到预览界面不就行了？

实际实践之后，发现如果在预览阶段对图像进行处理，会造成预览界面非常卡顿。

所以就视频采集而言，需要将视频 **原样采集**，在之后的播放或者编解码过程中，再对视频进行相应的处理。

所以，拍摄的预览界面最好使用 `TextureView` 实现。

> 虽然我这里使用了 `SurfaceView`，所以会有预览的图像变形问题。

## 视频播放前的特殊处理

将视频原样采集之后，得到 YUV 文件，那之后对它进行播放，需要经历以下几个步骤：

1. 将 YUV 数据转换为 ARGB 数据
2. 使用 ARGB 数据构建 `Bitmap` 对象
3. 通过 `Canvas` 绘制 `Bitmap`

### 将 YUV 数据转换为 RGB 数据

我们通过摄像头采集得到的数据为 `YUV420_888`，要转换为 `ARGB_8888` 才可以生成 `Bitmap` 对象。

但是只有  **YUV444** 才能和 RGB 数据进行相互转化；

所以，我们首先要将 `YUV420` 拉伸到 `YUV444` 平面，具体的拉伸方法为田字拉伸法，即将一个 U 数据填充到田字形的四个格子中，形成四个 U 数据：

![拉伸之前](https://user-images.githubusercontent.com/12459199/64677985-38443100-d4ab-11e9-9cdc-6420e97dd10b.png)


![拉伸之后](https://user-images.githubusercontent.com/12459199/64678026-4eea8800-d4ab-11e9-8203-c1787b4ca74d.png)


拉伸完毕之后，我们就可以实行 YUV 到 RGB 的转换了，但是在转换的过程中，我们还需要注意一个坑：

我们采集到的 YUV420 数据采用的是平面方式进行存储的，即先存储 Y 平面，再存储 U 平面，再存储 V 平面；

但是 `ARGB_8888` 格式的数据却是 **压缩存储的**，而且，它的顺序 **并不是** A R G B，而是 **R G B A**；

同时，如果采用 `libyuv` C++ 库进行转换，还要注意 C++ 和 Java 的字节序问题，C++ 默认采用小端序，所以得到的 RGB 数据顺序为 A B G R，当传入 Java 层的时候还要把它倒回来。

另外，无论是 YUV 数据还是 ARGB 数据，实际上都是 `byte` 数组，但是真正的 YUV 和 RGB 的取值范围都是 0-255，在 C++ 层面上，它们真正的类型是 `unsigned char`；

但是 Java 中没有无符号数，所以在进行转换的过程中，最好使用 **移位法** 进行计算，

### 旋转

得到正确的 `ARGB_8888` 数据之后，我们并不能将图像直接绘制出来。

这是因为一般的摄像传感器，相对于手机都是有一个旋转角度的，方向为逆时针，通常为 90 度，也有一些机型为 270 度。

因此，如果我们按照我们普通使用手机的竖直拍摄，所采集到的图像就会旋转一个角度，上面说到，我们要将图像原样采集，所以在播放阶段，就要将这个角度补偿掉，让我们拍摄到的图片正常显示。

以摄像头传感器逆时针 90 度，竖直拍摄为例，我们要对绘制的图像做一个顺时针 90 度的旋转操作。

这个操作通常由一个仿射变换矩阵 `Matrix` 对象来完成，关于 `Matrix` 对象这里不多做说明，有一个[非常好的网站](https://www.gcssloop.com/customview/Matrix_Basic)对 `Matrix` 的问题做了解释。

在做旋转的时候，我们也要注意，其实不是单单一个旋转就解决问题了，具体可以看下面的图示：


![只做一次旋转](https://user-images.githubusercontent.com/12459199/64681005-7d6b6180-d4b1-11e9-98f7-52293f9be8f9.png)

其中，白色是手机屏幕，橙色是采集到的原图像，红色是旋转之后的图像。


可以看到，如果只经过一个旋转操作，并不能让图像全部显示在屏幕之中，而需要再进行一个移动操作，如下图：

![旋转+移动](https://user-images.githubusercontent.com/12459199/64681474-5f523100-d4b2-11e9-9333-7059d2704a6e.png)

蓝色是经过旋转 + 移动过后的最终的图像，可以看到它全部显示在了屏幕之中。
