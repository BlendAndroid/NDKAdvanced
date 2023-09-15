// 两个图层的纹素进行混合，并且上面的那层随着时间的推移，会逐渐放大且不透明度逐渐降低
precision mediump float;
varying highp vec2 aCoord;
uniform sampler2D  vTexture;// 纹理采样器
// 缩放
uniform highp float scalePercent;
// 透明度
uniform lowp float mixturePercent;
void main() {
    // 纹理坐标系中心点的坐标
    highp vec2 center=vec2(0.5, 0.5);

    //将顶点坐标对应的纹理坐标的x/y值到中心点的距离，缩小一定的比例，仅仅只是改变了纹理坐标，而保持顶点坐标不变，从而达到拉伸效果

    // 临时变量,传入纹理原来的坐标
    highp vec2 textureCoordinateToUse = aCoord;
    // 减去中心点的坐标
    // 如:[0.6,0.6] - [0.5, 0.5] = [0.1, 0.1]
    textureCoordinateToUse-=center;

    // [0.1, 0.1] / 1.1 = [0.09, 0.09]
    textureCoordinateToUse=textureCoordinateToUse/scalePercent;

    // 采样点一定比需要渲染的坐标点要小
    // [0.09, 0.09] + [0.5,0.5] =[0.59,0.59]
    // 本来是[0.6,0.6],现在是[0.59,0.59]
    // 为什么会放大呢?
    // 可以想象下，未放大时，窗口刚好和纹理坐标系大小重叠，当需要放大画面时，将窗口映射缩小到纹理坐标系的不同位置上，
    // 但是整个窗口是不变的,那么缩小的部分就会显示到整个窗口上,即可实现局部画面放大到整个窗口上。
    textureCoordinateToUse+=center;

    // 原来绘制颜色
    lowp vec4 textureColor = texture2D(vTexture, aCoord);
    // 放大后的绘制颜色
    lowp vec4 textureColor2= texture2D(vTexture, textureCoordinateToUse);

    // 将两者混合,再加上透明度的变化
    //    gl_FragColor= mix(textureColor, textureColor2, mixturePercent);

    // 加权混合(原来的颜色从无到有,放大后的颜色从有到无)
    gl_FragColor = textureColor * (1.0 - mixturePercent) + textureColor2 * mixturePercent;

    // 放大效果
    // gl_FragColor = textureColor2;

}