#define HIGHP

uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_resolution;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

float hash(vec2 p){
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p){
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float heatNoise(vec2 uv){
    float n = 0.0;
    n += noise(uv) * 0.5;
    n += noise(uv * 2.0) * 0.25;
    n += noise(uv * 4.0) * 0.125;  // 新增：第三层噪声
    return n;
}

void main(){
    float mask = 1.0 - smoothstep(0.0, 0.5, length(v_texCoord - 0.5));
    mask = pow(mask, 1.3);

    const float flowSpeed = 2.0;
    const float noiseScale = 12.0;
    const float pixelStrength = 8.0;  // 从 3.0 增强到 8.0

    vec2 localUv = v_texCoord * noiseScale + vec2(0.0, u_time * flowSpeed);
    float nx = heatNoise(localUv + vec2(u_time * 1.1, 0.0));
    float ny = heatNoise(localUv + vec2(0.0, u_time * 0.9));

    vec2 pixelOffset = vec2(nx, ny * 1.6) * pixelStrength;
    vec2 finalOffset = (pixelOffset / max(u_resolution, vec2(1.0))) * mask;
    vec2 sampleUv = clamp(v_screenUv + finalOffset, vec2(0.0), vec2(1.0));

    vec4 background = texture2D(u_texture, sampleUv);
    gl_FragColor = vec4(background.rgb, mask) * v_color * u_color;
}