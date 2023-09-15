precision mediump float;
varying vec2 aCoord;//待绘制的纹理坐标,左上角是0,0
uniform sampler2D vTexture;
void main() {
    // 纹理坐标系的y坐标
    float y = aCoord.y;

    if (y<0.5)
    {
        y+=0.25;
    } else {
        y -= 0.25;
    }
    gl_FragColor= texture2D(vTexture, vec2(aCoord.x, y));

}