precision mediump float;
//当前要采集像素点的xy坐标
varying mediump vec2 aCoord;

//采样器
uniform sampler2D vTexture;

vec2 blurCoordinates[20];

//cpu传的值
uniform int width;
uniform int height;
void main(){
    // 步长
    vec2 singleStepOffset=vec2(1.0/float(width), 1.0/float(height));
    // 先用高斯算法进行模糊
    blurCoordinates[0] = aCoord.xy + singleStepOffset* vec2(0.0, -10.0);
    blurCoordinates[1] = aCoord.xy + singleStepOffset * vec2(0.0, 10.0);
    blurCoordinates[2] = aCoord.xy + singleStepOffset * vec2(-10.0, 0.0);
    blurCoordinates[3] = aCoord.xy + singleStepOffset * vec2(10.0, 0.0);
    blurCoordinates[4] = aCoord.xy + singleStepOffset * vec2(5.0, -8.0);
    blurCoordinates[5] = aCoord.xy + singleStepOffset * vec2(5.0, 8.0);
    blurCoordinates[6] = aCoord.xy + singleStepOffset * vec2(-5.0, 8.0);
    blurCoordinates[7] = aCoord.xy + singleStepOffset * vec2(-5.0, -8.0);
    blurCoordinates[8] = aCoord.xy + singleStepOffset * vec2(8.0, -5.0);
    blurCoordinates[9] = aCoord.xy + singleStepOffset * vec2(8.0, 5.0);
    blurCoordinates[10] = aCoord.xy + singleStepOffset * vec2(-8.0, 5.0);
    blurCoordinates[11] = aCoord.xy + singleStepOffset * vec2(-8.0, -5.0);
    blurCoordinates[12] = aCoord.xy + singleStepOffset * vec2(0.0, -6.0);
    blurCoordinates[13] = aCoord.xy + singleStepOffset * vec2(0.0, 6.0);
    blurCoordinates[14] = aCoord.xy + singleStepOffset * vec2(6.0, 0.0);
    blurCoordinates[15] = aCoord.xy + singleStepOffset * vec2(-6.0, 0.0);
    blurCoordinates[16] = aCoord.xy + singleStepOffset * vec2(-4.0, -4.0);
    blurCoordinates[17] = aCoord.xy + singleStepOffset * vec2(-4.0, 4.0);
    blurCoordinates[18] = aCoord.xy + singleStepOffset * vec2(4.0, -4.0);
    blurCoordinates[19] = aCoord.xy + singleStepOffset * vec2(4.0, 4.0);

    // 获取坐标点的颜色值
    vec4 currentColor=texture2D(vTexture, aCoord);
    vec3 rgb=currentColor.rgb;
    for (int i = 0; i < 20; i++) {
        // 将颜色值相加
        rgb+=texture2D(vTexture, blurCoordinates[i].xy).rgb;
    }
    // 取平均值
    vec4 blur = vec4(rgb*1.0/21.0, currentColor.a);

    // 高反差 = 一个完整的图片 - 差异部分
    // 高反差，保留图像的细节信息
    vec4 highPassColor = currentColor-blur;

    // 高反差结果进一步调优
    // float clamp(float x, float minVal, float maxVal);
    // 限制x的值在minVal和maxVal之间，即如果x小于minVal，返回minVal，如果x大于maxVal，返回maxVal，否则返回x
    highPassColor.r = clamp(2.0 * highPassColor.r * highPassColor.r * 24.0, 0.0, 1.0);
    highPassColor.g = clamp(2.0 * highPassColor.g * highPassColor.g * 24.0, 0.0, 1.0);
    highPassColor.b = clamp(2.0 * highPassColor.b * highPassColor.b * 24.0, 0.0, 1.0);

    vec4 highPassBlur=vec4(highPassColor.rgb, 1.0);

    // 取蓝色通道作为后面像素融合使用
    float b =min(currentColor.b, blur.b);
    float value = clamp((b - 0.2) * 5.0, 0.0, 1.0);
    // 取rgb的最大值
    float maxChannelColor = max(max(highPassBlur.r, highPassBlur.g), highPassBlur.b);
    // 磨皮程度
    float intensity = 1.0;// 0.0 - 1.0f 再大会很模糊

    float currentIntensity = (1.0 - maxChannelColor / (maxChannelColor + 0.2)) * value * intensity;
    vec3 r = mix(currentColor.rgb, blur.rgb, currentIntensity);
    gl_FragColor=vec4(r, 1.0);
}