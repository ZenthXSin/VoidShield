uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

// ---------- 噪声 ----------
float hash(vec2 p){
    return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453);
}
float noise(vec2 p){
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f*f*(3.0-2.0*f);
    float a = hash(i);
    float b = hash(i + vec2(1.0,0.0));
    float c = hash(i + vec2(0.0,1.0));
    float d = hash(i + vec2(1.0,1.0));
    return mix(mix(a,b,f.x), mix(c,d,f.x), f.y);
}
float fbm(vec2 p){
    float v = 0.0;
    float a = 0.5;
    for(int i=0;i<4;i++){
        v += noise(p) * a;
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

void main(){
    vec2 local = v_texCoord - 0.5;
    float r = length(local);
    float ang = atan(local.y, local.x);

    // 区域遮罩
    float regionMask = 1.0 - smoothstep(0.45, 0.5, r);

    // ---- 黑洞参数 ----
    const float eventHorizon = 0.25;
    const float maxDistortion = 0.60;

    // ===================== 核心修正：固定屏幕像素强度 =====================
    // ✅ 固定像素偏移（恒定屏幕像素值，不随相机缩放改变）
    const float basePixelOffset = 56.0; // 80 * 0.7 = 56，锁定最佳效果

    // 计算向中心拉的像素偏移（本地坐标决定强度分布）
    vec2 dir = (r > 1e-4) ? normalize(local) : vec2(0.0);

    // 强度从中心到边缘递减（纯本地坐标，无缩放影响）
    float distortionStrength = 1.0 - smoothstep(0.0, maxDistortion, r);
    distortionStrength = pow(distortionStrength, 0.7);

    // 像素偏移量（恒定屏幕像素）
    float pixelOffset = basePixelOffset * distortionStrength;

    // ✅ 关键：除以 u_camsize 转换为 UV 空间，抵消相机缩放
    vec2 lensOffset = (-dir * pixelOffset) / u_camsize;
    // =====================================================================

    // ---- 采样背景 ----
    vec2 uv = v_screenUv + lensOffset;
    vec4 bg = texture2D(u_texture, uv);

    // ---- 中心黑盘 ----
    float core = 1.0 - smoothstep(eventHorizon * 0.7, eventHorizon, r);

    vec3 col = bg.rgb;
    col = mix(col, vec3(0.0), core);
    col = mix(texture2D(u_texture, v_screenUv).rgb, col, regionMask);

    float outA = regionMask * v_color.a * u_color.a;
    gl_FragColor = vec4(col, outA);
}