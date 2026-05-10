#define HIGHP

#define ALPHA 0.5
#define step 2.0

uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec2 u_invsize;
uniform float u_time;
uniform float u_dp;
uniform vec2 u_offset;

varying vec2 v_texCoords;

// ----- 程序化噪声函数 -----
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
               f.y);
}

float fbm(vec2 p) {
    float z = 2.0;
    float rz = 0.0;
    for (int i = 0; i < 5; i++) {
        rz += abs(noise(p) - 0.5) * 2.0 / z;
        z *= 2.0;
        p *= 2.0;
    }
    return rz;
}

float dualfbm(vec2 p, float time) {
    vec2 p2 = p * 0.7;
    vec2 basis = vec2(fbm(p2 - time * 1.6), fbm(p2 + time * 1.7));
    basis = (basis - 0.5) * 0.2;
    p += basis;
    return fbm(p);
}
// -----------------------

void main() {
    vec2 T = v_texCoords.xy;
    vec2 coords = (T * u_texsize) + u_offset;

    // 位置扰动
    T += vec2(sin(coords.y / 3.0 + u_time / 20.0), sin(coords.x / 3.0 + u_time / 20.0)) / u_texsize;

    vec4 color = texture2D(u_texture, T);
    vec2 v = u_invsize;

    vec4 maxed = max(
        max(
            max(texture2D(u_texture, T + vec2(0, step) * v),
                texture2D(u_texture, T + vec2(0, -step) * v)),
            texture2D(u_texture, T + vec2(step, 0) * v)
        ),
        texture2D(u_texture, T + vec2(-step, 0) * v)
    );

    // 边缘高亮（保持不变）
    if (texture2D(u_texture, T).a < 0.9 && maxed.a > 0.9) {
        gl_FragColor = vec4(maxed.rgb, maxed.a * 100.0);
    } else {
        if (color.a > 0.0) {
            // ====== 内部护盾特效 + 泛光层 ======
            float time = u_time * 0.009;
            vec2 p = coords * 0.004;             // 固定缩放，不受相机影响

            // 核心电光（已去除旋转与中心波扩散）
            float rz = dualfbm(p, time);
            vec3 col = vec3(0.2, 0.1, 0.4) / rz;
            col = pow(abs(col), vec3(0.99));

            // ---- 新增泛光层 ----
            // 用低频噪声生成一层柔和的光晕，叠加在电光之上
            float glowNoise = fbm(p * 0.3 - time * 0.5);       // 更平滑的噪声
            vec3 glowColor = vec3(0.5, 0.25, 0.9) * glowNoise; // 泛光基础色
            float glowStrength = 1.0;                           // 泛光强度，可按需调整
            col += glowColor * glowStrength;                    // 加法叠加形成光晕
            // -------------------

            gl_FragColor = vec4(col, ALPHA);
            // ==================================
        } else {
            gl_FragColor = color;
        }
    }
}