#define HIGHP

#define ALPHA 0.18
#define step 2.0

uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec2 u_invsize;
uniform float u_time;
uniform float u_dp;
uniform vec2 u_offset;

varying vec2 v_texCoord;

// ========== 3D 噪声函数（来自提供的热浪代码） ==========
float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

float noise(vec3 x) {
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);

    float n = p.x + p.y * 157.0 + 113.0 * p.z;
    return mix(mix(mix(hash(n +   0.0), hash(n +   1.0), f.x),
                   mix(hash(n + 157.0), hash(n + 158.0), f.x), f.y),
               mix(mix(hash(n + 113.0), hash(n + 114.0), f.x),
                   mix(hash(n + 270.0), hash(n + 271.0), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float sum = 0.0;
    float amp = 1.0;
    float freq = 1.0;

    for (int i = 0; i < 5; i++) {
        sum += noise(p * freq) * amp;
        amp *= 0.5;
        freq *= 2.0;
    }
    return sum;
}
// ====================================================

void main() {
    vec2 uv = v_texCoord;   // 直接使用纹理坐标（固定缩放，不受相机影响）

    // ===== 热浪扭曲参数（可自行调节） =====
    float heatStrength = 0.03;
    float heatSpeed = 2.0;
    float heatFreq = 5.0;

    // 多层噪声混合，产生流动感
    float noiseX = fbm(vec3(uv * heatFreq, u_time * heatSpeed));
    float noiseY = fbm(vec3(uv * heatFreq + vec2(43.21, 56.78), u_time * heatSpeed * 0.7 + 10.0));
    float noiseX2 = fbm(vec3(uv * heatFreq * 2.0 + vec2(123.45, 78.90), u_time * heatSpeed * 1.3));
    float noiseY2 = fbm(vec3(uv * heatFreq * 2.0 + vec2(87.65, 43.21), u_time * heatSpeed * 0.9 + 5.0));

    float finalNoiseX = mix(noiseX, noiseX2, 0.5);
    float finalNoiseY = mix(noiseY, noiseY2, 0.5);

    vec2 offset = vec2(
    (finalNoiseX * 2.0 - 1.0) * heatStrength,
    (finalNoiseY * 2.0 - 1.0) * heatStrength
    );

    vec2 distortedUV = clamp(uv + offset, 0.0, 1.0);
    vec4 distortedColor = texture2D(u_texture, distortedUV);

    // 保留原纹理的 alpha 判定，只在护盾像素上显示热浪效果
    vec4 originalColor = texture2D(u_texture, uv);
    if (originalColor.a > 0.0) {
        gl_FragColor = vec4(distortedColor.rgb, ALPHA);
    } else {
        gl_FragColor = originalColor;
    }
}