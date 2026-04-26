uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
// 核心：用相机尺寸修正缩放
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

// 基础噪声函数保持不变
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

void main() {
    // 1. 基础空间坐标计算（以中心为基准的径向坐标）
    vec2 center = vec2(0.5);
    vec2 delta = v_texCoord - center;
    float dist = length(delta);       // 距中心的距离
    vec2 dir = dist > 0.0 ? normalize(delta) : vec2(0.0); // 辐射方向

    // 圆形遮罩（边缘平滑衰减，向中心聚集）
    float mask = 1.0 - smoothstep(0.0, 0.5, dist);
    mask = pow(mask, 1.5); // 让中心区域的影响力更集中

    // ===================== 空间跃迁核心算法 =====================
    // 跃迁参数配置
    const float shockwaveSpeed = 15.0; // 冲击波扩散/收缩速度（快！）
    const float shockwaveFreq = 25.0;  // 空间波纹的密集程度
    const float spatialTear = 4.0;     // 空间撕裂的噪点大小
    const float pixelStrength = 12.0;  // 扭曲的最大屏幕像素值（比热浪要大）

    // A. 径向波纹（模拟引力波冲击波向外震荡）
    float wave = sin(dist * shockwaveFreq - u_time * shockwaveSpeed);

    // B. 螺旋空间撕裂（使用极坐标计算噪声，产生类似虫洞漩涡的感觉）
    float angle = atan(delta.y, delta.x);
    // 极坐标UV：角度 + 距离流动
    vec2 polarUv = vec2(angle * spatialTear, dist * spatialTear - u_time * 5.0);
    float tearNoise = noise(polarUv) * 2.0 - 1.0; // 将噪声映射到 -1 ~ 1

    // 综合扭曲力度：高频波纹 + 撕裂噪点
    float warpIntensity = (wave * 0.4 + tearNoise * 0.6);

    // 计算实际偏移量：沿着辐射方向（dir）推拉，或者加入一点切线方向（旋转）
    // vec2(-dir.y, dir.x) 产生切线旋涡力
    vec2 warpDir = normalize(dir + vec2(-dir.y, dir.x) * 0.5);

    // 2. ✅ 关键：u_camsize 归一化 → 强制扭曲为【固定屏幕像素】
    vec2 pixelOffset = warpDir * warpIntensity * pixelStrength;
    vec2 finalOffset = (pixelOffset / u_camsize) * mask;
    // ==========================================================

    // ===================== 科幻色散 (Chromatic Aberration) ====
    // 空间折叠会导致光线RGB分离，这里让RGB按照不同力度进行偏移采样
    const float rgbSplit = 0; // 色散偏移像素
    vec2 splitOffset = dir * (rgbSplit / u_camsize) * mask; // 沿着辐射方向色散

    // 分离采样 R, G, B
    float r = texture2D(u_texture, v_screenUv + finalOffset + splitOffset).r;
    float g = texture2D(u_texture, v_screenUv + finalOffset).g;
    float b = texture2D(u_texture, v_screenUv + finalOffset - splitOffset).b;
    vec4 background = vec4(r, g, b, 1.0);
    // ==========================================================

    // 可选：空间跃迁中心通常伴随高能发光，给中心加一点点亮度脉冲
    float energyGlow = exp(-dist * 8.0) * (sin(u_time * 20.0) * 0.5 + 0.5) * 0.2 * mask;

    // 最终输出：叠加色散背景、发光，并乘上原有的色彩
    gl_FragColor = (background + vec4(energyGlow)) * v_color * u_color;
    // 如果不需要中心发光，可以用下面这句替换上一句：
    // gl_FragColor = background * v_color * u_color;
}