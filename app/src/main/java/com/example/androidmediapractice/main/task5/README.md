# 学习 Android 平台 OpenGL ES API，了解 OpenGL 开发的基本流程，使用 OpenGL 绘制一个三角形

本任务主要目的为学习 OpenGL ES 绘制的基本流程，并兼具移动设备的特点。

## OpenGL ES 学习版本

在这里采用 OpenGL ES 2.0 作为主要的学习版本，原因在于：

1. Android 对 OpenGL 2.0 的支持度仍有 20% 左右，且高 API 版本设备未必就支持高版本的 OpenGL ES
2. OpenGL ES 2.0 之后的主要渲染流程未发生改变，之后的版本也都能向后兼容到 2.0

## OpenGL 的渲染流程和相关概念


### 基本概念

OpenGL 最主要的部分就是它的**渲染管线(Render Pipeline)**，经过一系列步骤，将数据变成视图，并输出到显示器上。

渲染管线主要分为两部分：

1. 3D 坐标转换为 2D 坐标
2. 2D 坐标转换为有颜色的像素

**着色器(Shader)**：渲染管线中处理数据的小程序，OpenGL 的着色器使用 GLSL（一种 DSL）写成

**顶点数据**：图形坐标的集合，顶点使用**顶点属性**表示，包括顶点的 3D 坐标等其他属性

**图元**：指示顶点数据构成的东西是什么，即指定基本渲染类型，OpenGL 中有三种基本渲染类型：

- 点
- 线
- 三角形

### 基本渲染流程

![基本渲染流程](https://user-images.githubusercontent.com/12459199/65027300-adf94280-d96c-11e9-8952-4179a16afba2.png)

上图表示了 OpenGL 的基本渲染历程，下面对每个阶段进行讲解：

1. 顶点着色器：进行坐标转换，将顶点数据的 3D 坐标转换为另一个 3D 坐标
2. 图元装配：确定渲染的图元
3. 几何着色器：产生新顶点，构造图元，生成其他形状
4. 光栅化：将图元生成一个一个像素，生成供片段着色器使用的片段
5. 裁切：丢弃超过视图以外的像素（在光栅化和片段着色器之间发生）
6. 片段着色器：计算像素的颜色
7. Alpha 测试和混合：检测片段对应的深度，抛弃看不见的片段，计算 Alpha 值，并进行混合

经过上述过程后，最后输出的数据被写入显示缓冲区，依此完成一次 VSync 过程，显示器双缓冲区切换，逐行读取缓冲区内部数据，最终图像得以显示。

必备的着色器：**顶点着色器** 和 **片段着色器**

> 后面我们会定义并使用这两个着色器

### OpenGL 的屏幕坐标

OpenGL 采用和手机屏幕坐标系不一样的坐标空间，它的坐标值为 `[-1,1]`，坐标原点位于 **屏幕中点**：

![OpenGL 坐标空间](https://user-images.githubusercontent.com/12459199/65028885-5dcfaf80-d96f-11e9-9e74-9d79593f808c.png)

可以看到，OpenGL 假定屏幕是正方形的，但是如果不是正方形的会怎么样呢？

它会保持 `[-1,1]` 的限制不变，同时拉伸比例，如下图所示：

![OpenGL 坐标空间变形](https://user-images.githubusercontent.com/12459199/65029850-07fc0700-d971-11e9-9402-b1782d317944.png)


在其中的顶点，都使用浮点数来表示其坐标。

同时，颜色值也采用 `[0.0, 1.0]` 的浮点数表示

## OpenGL 绘制的主要流程

### 准备环境

1. 使用 `<use-feature>` 声明使用的 OpenGL 版本
2. 声明使用的材质压缩格式
3. 创建 `GLSurfaceView`
4. 创建 `GLSurfaceView.Renderer`
5. 给 `GLSurfaceView` 设置 OpenGL 版本和对应的 renderer
6. 处理 `GLSurfaceView` 的生命周期

创建 `renderer` 时，需要在 `onSurfaceChanged()` 方法中调用 `glViewPort()` 方法指定屏幕绘制空间。

### OpenGL ES 的具体绘图步骤

这里采用构建一个三角形类，并提供 `draw()` 方法进行绘制来实现代码封装

三角形在 `onSurfaceCreated()` 创建，并在 `onDrawFrame()` 调用 `draw()` 方法来进行绘制

1. 定义顶点坐标，坐标为 3D 坐标，每一个维度数据采用一个 `float(32bit)` 表示
2. 构建顶点 `Buffer`，注意采用 `nativeOrder()`
3. 构建颜色，使用一个 4 长度的 `float` 数组表示一个颜色，顺序为 RGBA
4. 构建顶点和片段着色器
   1. 使用 `glCreateShader(type)` 构建着色器，返回着色器的 id
   2. 使用 `glShaderSource(id, shaderCode)` 导入着色器代码
   3. 使用 `glCompileShader(id)` 编译着色器
5. 构建、链接着色器程序
   1. 使用 `glCreateProgram()` 构建着色器程序，返回程序 id
   2. 使用 `glAttachShader(programId, shaderId)` 将着色器附加到程序上
   3. 使用 `glLinkProgram(programId)` 链接着色器程序
   4. 链接完毕后，使用 `glDeleteShader(shaderId)` 删除附加过的着色器，释放空间
6. 使用 `glUseProgram(programId)` 使用着色器程序
7. 使用 `glGetAttributeLocation()` 和 `glGetUniformLocation()` 获取顶点位置和颜色句柄
8. 使用 `glVertexAttribPointer(indx, size, type, normalized, stride, ptr)` 设置顶点属性指针
   - index: 顶点属性的 Location，即上面获取的句柄
   - size: 顶点属性的大小（一个顶点有多少属性）
   - type: 顶点属性的数据类型，本任务中为 float
   - normalized：若标准化，则所有数据都会被映射到 `[0-1]` 之间
   - stride: 步长，连续顶点属性组之间的间隔，两个相同属性之间的间隔，单位为 **byte**
   - ptr: Java 层，该参数为一个 `Buffer` 引用；native 层为数据的指针
9. 使用 `glEnableVertexAttrbArray(vertexLocation)` 启用顶点属性
9. 使用 `glUniform4fv(location, count, value, offset)` 设置颜色值
   - location: 颜色值的句柄
   - count: 如果颜色值不是数组，那么为 1，如果是数组则为需要设置的数量
   - value: Java 层为一个 `float` 数组，native 层为一个指针，为需要传入的数据
   - offset: 从 offset 个偏移量开始读取传入数据
10. 使用 `glDrawArrays(mode, first, count)` 绘制图形
    - mode: 指代需要绘制的基本图形：点、线、三角形
    - first：要绘制的顶点的起始位置，由于本任务中只画一次，所以从 0 开始
    - count: 要绘制的顶点数量
11. 使用 `glDisableVertexAttribArray()` 关掉顶点属性
    > 因为本任务中没有使用 VAO，因此在绘制完毕之后需要关掉顶点属性，以免绑定到无效指针引起崩溃

## 调整宽高比

上面说过，OpenGL 采用归一坐标空间，在不是正方形的屏幕空间会造成图像变形问题；

因此，我们需要对这个屏幕空间进行调整，防止图像变形。

在解决这个问题之前，我们先来了解一下 OpenGL 的坐标空间。

### OpenGL 坐标空间

顶点数据在经过顶点着色器之后，最终都会转变为屏幕空间坐标，但是转变步骤并不是一次性完成的，而是经历了不同的坐标转换，最后转换到了屏幕空间坐标，下面是它经历的坐标空间：

1. 局部空间：物体中心位于`(0,0,0)`的坐标空间
2. 世界空间：处在更大的空间范围内，物体的坐标是相对于世界的原点进行摆放
3. 观察空间：从摄像机的角度进行观察，所得到的物体坐标形成的坐标空间
4. 裁剪空间：这个即为我们熟悉的归一化坐标空间，原点位于屏幕中点，坐标范围 `[-1,1]`，同时会裁减掉不在这个坐标范围内的顶点，由观察空间投影而成
5. 屏幕空间：裁剪空间经过视口转换为 `glViewPort()` 定义的坐标空间，最终转换得到的坐标会传给光栅器，并转换为片段

要进行坐标空间转换就要使用转换矩阵，主要有以下三个：

1. 模型矩阵(Model Matrix)
2. 观察矩阵(View Matrix)
3. 投影矩阵(Projection Matrix)

具体的转换过程如下图所示：

![坐标空间转换过程](https://user-images.githubusercontent.com/12459199/65034882-7b564680-d97a-11e9-8a24-c3e9c6f06f5b.png)

可以看到，局部坐标在依次经过上面三个矩阵的映射，就转换为了屏幕空间坐标；

我们知道，进行坐标转换，具体来说就是进行矩阵的 **前乘**，因此，我们可以得出，最后的坐标为 ：

```
gl_Position = projection_mat * (view_mat * (model_mat * vPosition))
```

经过上述的矩阵变换之后，就得到了最终的坐标

### 调整宽高比的步骤


#### 构造模型矩阵

在这里，我们就要依次构造上述的几个矩阵，实现坐标的转换。

首先是模型矩阵，由于我们没有绘制其他物体，因此我们可以将模型矩阵设置为单位矩阵。


#### 构造观察矩阵

其次是观察矩阵，Android 有一个很方便的方法用于生成观察矩阵：

```java
Matrix.setLookAtM(viewMatrix, offset,
  eyeX, eyeY, eyeZ,
  centerX, centerY, centerZ,
  upX, upY, upZ
  )
```

这个方法的参数由一个数组，一个 `offset` 还有三个坐标组成，其中：

1. eye 坐标为摄像机坐标
2. center 坐标为被观察物体坐标
3. up 坐标为指向摄像机上方的**向量**

用一个很形象的图来表示一下什么是 UP 向量：

![eye center up 向量](https://user-images.githubusercontent.com/12459199/65036843-f7eb2400-d97e-11e9-8a37-d3501751c025.png)


#### 构造投影矩阵

最后是投影矩阵，由于我们的图像是被拉伸了，因此是主要是观察坐标投影到裁剪坐标发生了问题，假如我们将投影矩阵响应做一些调整，那么就能解决这个图像变形的问题。

由于屏幕的宽高比只会在 `onSurfaceChanged()` 时才会发生改变，因此我们只需要在这个方法中计算出投影矩阵即可。

Android 已经提供了一个 `Matrix.frustumM(projectionMatrix, offset, left, right, top, bottom, near, far)` 来生成对应的投影矩阵，它的具体作用就是将裁剪空间限制在如下坐标范围之内：

1. `[left, right]`
2. `[top, bottom]`
3. `[-near, -far]`

这里为什么 `near` 和 `far` 是负的呢？主要是因为 OpenGL 的摄像机坐标、物体坐标等都是**右手系**的，而归一化坐标是 **左手系** 的；

因此，对传入的参数中，`near` 和 `far` 都要求为**正数**，而且 `far > near`

这里 Android 系统为我们处理了这一个问题，但是如果直接使用 `glFrustumM` 就需要注意这个问题。

> 左手系和右手系的主要区别就在于 `z` 轴的方向是反的，如图所示：
> ![左手系和右手系](https://user-images.githubusercontent.com/12459199/65037209-be66e880-d97f-11e9-930c-27cd655328b8.png)


那么如何调整宽高比呢？我们可以对屏幕空间进行相应的拉伸，让小的一方为 `[-1,1]` 然后让大的一方按比例调整为 `[-ratio, ratio]` 即可。

例如，若 `width > height`，那么就应该是采用如下的矩阵：

```java
Matrix.frustumM(projectionMatrix, offset,
  -ratio, ratio,
  -1f, 1f,
  near, far
  )
```

若 `height > width`，则应该使用如下的矩阵

```java
Matrix.frustumM(projectionMatrix, offset,
  -1f, 1f,
  -ratio, ratio,
  near, far
  )
```

最后，代入公式，我们就得出了顶点着色器最终的转换矩阵，Android 也很贴心的给我们提供了一个矩阵乘法的方法：

```java
Matrix.multiplyMM(vPMatirx, 0, projectionMatrix, 0, viewMatrix, 0)
```

#### 将矩阵导入着色器中

得到转换矩阵之后，我们要将矩阵导入顶点着色器中，因此，我们的顶点着色器代码为：

```c
uniform mat4 uMvpMatrix;
attribute vec4 vPosition;
void main() {
  gl_Position = uMvpMatrix * vPosition;
}
```

导入的代码为：

```kotlin
glGetUniformLocation(program, "uMvpMatrix").also { uMvpMatrixHandle ->
                    glUniformMatrix4fv(uMvpMatrixHandle, 1, false, vPMatrix, 0)
                  }
```

由于我们的模型矩阵为单位矩阵，因此直接传入 `vPMatrix` 即可
