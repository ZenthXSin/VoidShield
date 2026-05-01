uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

mat2 makem2(float theta) {
    float c = cos(theta);
    float s = sin(theta);
    return mat2(c, -s, s, c);
}

float fbm(vec2 p) {
    float z = 2.0;
    float rz = 0.0;
    for (float i = 1.0; i < 6.0; i++) {
        rz += abs((noise(p) - 0.5) * 2.0) / z;
        z = z * 2.0;
        p = p * 2.0;
    }
    return rz;
}

float dualfbm(vec2 p) {
    float t = u_time * 0.15;
    vec2 p2 = p * 0.7;
    vec2 basis = vec2(fbm(p2 - t * 1.6), fbm(p2 + t * 1.7));
    basis = (basis - 0.5) * 0.2;
    p += basis;

    return fbm(p * makem2(t * 0.2));
}

float circ(vec2 p) {
    float r = length(p);
    r = log(sqrt(r));
    return abs(mod(r * 4.0, 6.2831853) - 3.14159) * 3.0 + 0.2;
}

void main() {
      vec2 p = v_texCoord - 0.5;
      p.x *= u_camsize.x / u_camsize.y;

      float dist = length(p); // 到中心距离

      p *= 4.0;

      float rz = dualfbm(p);

      float t = u_time * 0.15;
      vec2 p_ring = p / exp(mod(t * 10.0, 3.14159));
      rz *= pow(abs(0.1 - circ(p_ring)), 0.9);

      vec3 col = vec3(0.2, 0.1, 0.4) / rz;
      col = pow(abs(col), vec3(0.99));

      // ===== 关键：边缘渐隐 =====
      float fade = smoothstep(0.5, 0.35, dist);
      // 外0.5开始透明，0.35完全不透明（可以调）

      gl_FragColor = vec4(col, fade) * v_color * u_color;
}
