// 把顶点坐标(也就是世界坐标)给这个变量， 确定要画画的形状
// 自己定义的4个分量的矩阵，接收来自CPU的传值,但是只有前两个值是有效的
attribute vec4 vPosition;

// 接收纹理坐标，接收采样器采样图片的坐标
attribute vec4 vCoord;

// oepngl坐标系和camera坐标系做转换
uniform mat4 vMatrix;

//经过varying修饰的话,传给片元着色器 像素点,和片元着色器里面的定义是一样的
varying vec2 aCoord;
void main(){
    // gpu需要渲染的图像形状
    // 顶点位置坐标
    gl_Position=vPosition;
    aCoord= (vMatrix * vCoord).xy;
}
