attribute vec4 vPosition;
attribute vec2 vCoordinate;
uniform mat4 vMatrix;

varying vec2 aCoordinate;
varying vec4 aPos;
varying vec4 gPosition;

void main(){
    // 是一个四维的坐标，将世界坐标系的值经过矩阵变换，转为裁剪坐标系
    gl_Position=vMatrix*vPosition;
    // 将下面的三个值传给片元坐标系
    aPos=vPosition;
    aCoordinate=vCoordinate;
    gPosition=vMatrix*vPosition;
}