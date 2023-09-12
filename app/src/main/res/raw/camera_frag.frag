#extension GL_OES_EGL_image_external : require
//所有float类型数据的精度是lowp
precision mediump float;
varying vec2 aCoord;    //顶点着色器传递过来的纹理坐标
//采样器  uniform static
uniform samplerExternalOES vTexture;
void main(){
    vec4 rgba = texture2D(vTexture, aCoord);
    gl_FragColor=vec4(rgba.r, rgba.g, rgba.b, rgba.a);
    // 也能直接赋值
//    gl_FragColor = rgba;
}