# 在 Android 平台绘制一张图片，使用至少 3 种不同的 API，ImageView，SurfaceView，自定义 View


使用 `ImageView` 绘制图片，这个大家都已经轻车熟路了；

这个任务的重点还是在掌握 `SurfaceView` 的使用，因为后面的音视频播放主要还是使用 `SurfaceView` 进行的。

## SurfaceView

`SurfaceView` 是 Android 视图体系中比较特殊的一个 View，要理解 `SurfaceView`，我们首先要理解什么是 `Surface`。

实际上，Android 视图的所有绘制，都是在 `DecorView` 的同一个 `Surface` 中进行的，当视图变换的时候，就不停刷新 `Surface` 上的内容，在达到视图变换的效果。

那么 `SurfaceView` 的特殊之处，就在于它自己拥有一个 **独立的 `Surface`**，并且这个 `Surface` 是位于原 `Surface` 的后方；

在 `SurfaceView` 的 `onDraw()` 方法中，它将自己绘制为透明，这就相当于在原 `Surface` 上打了一个洞，将其后面的，也就是 `SurfaceView` 自己的 `Surface` 显示了出来。

这样，我们就可以看到 `SurfaceView` 自己的 `Surface` 绘制的各种效果了。

同时，由于内容的绘制是在 `SurfaceView` 自己的 `Surface` 上进行，与原先的 `Surface` 无关，因此我们可以在 **非主线程** 实现内容的绘制，这个特点也就完美契合了视频播放的需求，因为视频内容的绘制不能和 UI 的处理相冲突。

## SurfaceView 绘图步骤

1. 获取 `SurfaceHolder`
2. 通过 `SurfaceHolder.addCallback()` 监听 `Surface` 状态，当 `Surface` 构建完毕时方可绘制图像
3. 通过 `holder.lockCanvas()` 获取绘图的 `Canvas` 对象
4. 通过 3 中获取到的 `Canvas` 进行绘图
5. 通过 `holder.unlockCanvasAndPost()` 将绘制的图像进行显示

## 双缓冲

`SurfaceView` 采用双缓冲机制，也就是说你获取到的 `canvas` 实际上保存着 **前前一帧** 的内容，如果需要局部绘制就需要注意这一点。
