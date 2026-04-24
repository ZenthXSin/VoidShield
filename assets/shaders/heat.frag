uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
// 核心：用相机尺寸修正缩放
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

// 噪声函数
float hash(vec2 p){
    return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);
}
float noise(vec2 p){
    vec2 i=floor(p);
    vec2 f=fract(p);
    f=f*f*(3.-2.*f);
    float a=hash(i);
    float b=hash(i+vec2(1,0));
    float c=hash(i+vec2(0,1));
    float d=hash(i+vec2(1,1));
    return mix(mix(a,b,f.x),mix(c,d,f.x),f.y);
}
float heatNoise(vec2 uv) {
    float n = 0.0;
    n += noise(uv)       * 0.5;
    n += noise(uv * 2.0) * 0.25;
    return n;
}

void main() {
    // 圆形遮罩（本地坐标，无缩放影响）
    float mask = 1.0 - smoothstep(0.0, 0.5, length(v_texCoord - 0.5));
    mask = pow(mask, 1.3);

    // ===================== 核心修正：u_camsize 适配缩放 =====================
    // 固定参数（强度/速度锁死）
    const float flowSpeed = 2.0;     // 流动速度（恒定）
    const float noiseScale = 12.0;   // 热浪细密程度（恒定）
    const float pixelStrength = 3.0; // 扭曲像素值（核心！固定屏幕像素）

    // 1. 纯本地坐标计算扰动（无缩放影响）
    vec2 localUV = v_texCoord * noiseScale + vec2(0.0, u_time * flowSpeed);
    float nx = heatNoise(localUV + vec2(u_time * 1.1, 0.0));
    float ny = heatNoise(localUV + vec2(0.0, u_time * 0.9));

    // 2. ✅ 关键：u_camsize 归一化 → 强制扭曲为【固定屏幕像素】
    // 无论相机怎么缩放，扭曲的像素大小永远不变
    vec2 pixelOffset = vec2(nx, ny * 1.6) * pixelStrength;
    vec2 finalOffset = pixelOffset / u_camsize;

    // 叠加遮罩
    finalOffset *= mask;
    // =====================================================================

    // 采样背景（像素级恒定扭曲）
    vec4 background = texture2D(u_texture, v_screenUv + finalOffset);

    // 最终输出
    gl_FragColor = vec4(background.rgb, mask) * v_color * u_color;
}