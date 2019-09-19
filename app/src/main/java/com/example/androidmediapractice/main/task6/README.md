# 学习 Android 平台 OpenGL ES API，学习纹理绘制，能够使用 OpenGL 显示一张图片

本任务目的为纹理绘制，但是在纹理绘制之前，我们首先要绘制一个矩形；

这里采用 VBO 和 EBO 方式来进行绘制，减少所需要绘制的顶点数量。


> OpenGL ES 2.0 中没有 VAO

## VBO 和 EBO 的使用步骤

### 1. `glGenBuffers(count, intArray, offset)` 生成 VBO 和 EBO

VBO 和 EBO 都是缓存，因此我们可以使用这个方法统一生成

调用之后，`intArray` 中会含有两个值，我们采用第一个值为 `vbo`，第二个值为 `ebo`

### 2. 绑定 VBO

1. 调用 `glBindBuffer(GL_ARRAY_BUFFER, vbo)` 将 VBO 绑定到 `GL_ARRAY_BUFFER` 上。

2. 创建顶点 `Buffer`
   > 注意使用 `position(0)` 重置指针
3. `glBufferData(GL_ARRAY_BUFFER, size, buffer, mode)` 绑定数据
   - `size` 的单位为 byte
   - `mode` 是 OpenGL 的绘制模式，有 `GL_STATIC_DRAW` 等，根据模式的不同，会将数据放到不同的缓存区域
4. `glVertexAttribPointer` 设置顶点数据指针，注意最后一个参数为 `offset` 的 `int` 值，指示该 `attribute` 数据从数组的什么地方开始读取
   > 由于没有指定数据，因此该方法会读取之前绑定在 `GL_ARRAY_BUFFER` 中的数据

### 3. 绑定 EBO

与绑定 VBO 类似，将绑定 VBO 中的 `GL_ARRAY_BUFFER` 改为 `GL_ELEMENT_ARRAY_BUFFER` 即可。

最后在绘制的时候，采用 `glDrawElements()` 代替 `glDrawArrays()` 即可。

对于索引的数据，如果采用 `IntArray`，那么 `glDrawElements()` 就需要传入 `GL_UNSIGNED_INT` 作为参数。


## 纹理绘制

所谓纹理(Texture)就是一张张图片，在进行纹理绘制之前，我们需要知道纹理与我们的图形的对应关系，这样才能将纹理附着在图形上。

因此，OpenGL 引入了纹理坐标。

### 纹理坐标

纹理坐标指代了纹理单元在图片上的位置，图片的纹理坐标范围为 `[0-1]`，以 **左下角** 为坐标原点，如下图所示：


![纹理坐标](https://user-images.githubusercontent.com/12459199/65234064-7c1be380-db06-11e9-9881-551727b2810f.png)

### 着色器相关修改

由于我们要绘制纹理，所以我们的着色器和数据就需要进行相应的修改：

顶点数据改为一个五个值一个顶点，包括：物体三维坐标和纹理坐标
> 纹理坐标也可以是三维的，使用 stpq 来表示， 区别于 xyzw

顶点着色器加入一个 `attribute` 和一个 `varying` 数据用于表示纹理坐标
> `attribute` 是传入值，`varying` 是输出值，之后片段着色器会使用这个值

片段着色器加入一个 `uniform sampler2D ourTexture` 以及 `varying` 数据来接受纹理坐标的输入；

`sampler2D` 是纹理专用类型，同理还有 `sampler1D` 和 `sampler3D`，对外为整数值，通过 `glUnifrom1i()` 进行设定。

随后采用 `texture2D(ourTexture, texCoord)` 来生成片段颜色：


```glsl
precision mediump float;
uniform vec4 vColor;
uniform sampler2D ourTexture;
varying vec2 texCoord;
void main() {
  gl_FragColor = texture2D(ourTexture, texCoord);
}
```

`texture2D()` 是 GLSL 内建的接受 2D 纹理，生成颜色的函数。

>特别注意：OpenGL 3.0 以上该函数改为 `texture()`，但是 2.0 不具备 `texture()` 函数，调用 `texture()` 导致没有颜色而黑屏

### 纹理环绕方式

当我们指定的纹理坐标超出 `[0-1]` 的范围内时采用的，此时 OpenGL 默认会复制图片在对应区域，当然也可以设定不同的环绕方式，如下图所示：

![纹理环绕方式](https://user-images.githubusercontent.com/12459199/65236893-192d4b00-db0c-11e9-93df-c417e4d1e74b.png)

### 纹理过滤

OpenGL 的纹理生成实际上是通过纹理坐标，查询到对应的图片像素，随后对像素进行采样从而生成的，纹理过滤就是 OpenGL 的具体采样方式

当物体很大，但是纹理图片的分辨率很低的时候，纹理过滤就显得比较重要了，这里来介绍两种比较重要的纹理过滤方式：邻近过滤和线性过滤。

邻近过滤是 OpenGL 默认的过滤方式，它会选择离纹理坐标最近的像素颜色

![邻近过滤](https://user-images.githubusercontent.com/12459199/65237287-ee8fc200-db0c-11e9-800c-f84090192ba1.png)

线性过滤，会基于纹理坐标周围的像素，计算出一个插值，像素的中心距离纹理坐标越近，它对插值的贡献度也就越大，最后得出的是周围像素的一个混合色：


![线性过滤](https://user-images.githubusercontent.com/12459199/65237476-4e866880-db0d-11e9-87e6-bc3c09b60843.png)

两种纹理过滤方式的对比：

![两种纹理过滤方式的对比](https://user-images.githubusercontent.com/12459199/65237560-75449f00-db0d-11e9-9173-08516449cb2c.png)


### 纹理绘制步骤

1. 生成纹理 `glGenTextures(1, textures, 0)`
2. 载入图片
   > Android 中采用 `Bitmap` 载入图片，然后使用 `copyPixelsToBuffer()` 复制图片数据到缓冲区
3. 激活纹理 `glActivateTexture(GL_TEXTUREn)`
   > OpenGL 允许绑定十六种纹理，同时 `GL_TEXTURE0` 是默认激活的，如果只使用一种纹理，那可以省略这个的调用
4. 绑定纹理 `glBindTexture(target, texture)`
   - `target`:  `GL_TEXTURE_1D` `GL_TEXTURE_2D` `GL_TEXTURE_3D`
   - `texture`：上面生成的纹理的索引值
5. 设置纹理属性
   - 包括环绕方式和过滤方式
   - 环绕方式可以在几个轴分别设置
   - 过滤方式可以在放大和缩小的时候分别设置
6. 导入图片数据 `glTexImage2D(target, level, internalFormat, width, height, border, format, type, buffer)`
   - `target`:  `GL_TEXTURE_1D` `GL_TEXTURE_2D` `GL_TEXTURE_3D`
   - `level`: 多级渐远的等级
   - `internalformat`: 纹理的存储格式，这里采用 `GL_RGBA`
   - `border`: 总为 0
   - `format`：源图的存储格式，bitmap 使用 `GL_RGBA`
   - `type`：源图数据的数据类型，这里采用 `GL_UNSIGNED_BYTE`
   - `buffer`：图片数据的 `Buffer`
   - 记得导入完毕之后调用 `bitmap.recycle()` 回收资源
7. 设置 `uniform sampler2D ourTexture`
   > 使用 `glUnifrom1i()` 对 `ourTexture` 进行设定，其值应该和你绑定的 `GL_TEXTUREn` 的 `n` 相同
   >
   > 由于默认值为 0，为我们默认激活的 `GL_TEXTURE0`，因此如果只使用一种纹理，那么可以跳过这一步
8. 调用 `glDrawxxx()` 方法进行绘制，这里采用了 EBO 的 `glDrawElements()` 进行绘制


## 图片颠倒问题

按照上面的方法进行绘制，你可能发现图片是颠倒的，这是因为 OpenGL 纹理坐标和 Android 屏幕坐标不同，OpenGL 纹理坐标的原点位于图片左下角，向上和向右是正方向，而 Android 屏幕的原点位于左上角，向下和向右是正方向。

这就造成了两者坐标的 `y` 轴是相反的，因此 OpenGL 在读取图片数据的时候就会出现颠倒问题。

这个问题我们可以由多种解决方式：

1. 在加载图片时处理：将图片数据进行处理过后再传入 OpenGL 中
2. 在纹理坐标数据进行处理：人为反转纹理坐标，将 `y` 改为 `1.0-y`
3. 在顶点着色器进行处理：同上
4. 调整摄像机 `up` 向量，将其翻转

当然，最好的处理方式还是对图片数据进行处理，保证在源头数据就是正确的；

本任务中为了图方便，是在顶点着色器进行的处理，最好还是在图片数据处就进行处理。
