// 把顶点坐标给这个变量， 确定要画画的形状
//自己定义的  4个元素的矩阵，接收来自CPU的传值
attribute vec4 vPosition;//0
//cpu
//接收纹理坐标，接收采样器采样图片的坐标  camera
attribute vec4 vCoord;

//   oepngl坐标系和camera坐标系做转换
uniform mat4 vMatrix;

//经过varying修饰的话,传给片元着色器 像素点,和片元着色器里面的定义是一样的
varying vec2 aCoord;
void main(){
    //    gpu  需要渲染的 什么图像   形状
    // 顶点位置坐标
    gl_Position=vPosition;
    //
    aCoord= (vMatrix * vCoord).xy;
}
