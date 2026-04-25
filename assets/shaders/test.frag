uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

// --- 极致噪声库 ---
float hash(vec2 p) {
      p = fract(p * vec2(123.34, 456.21));
      p += dot(p, p + 45.32);
      return fract(p.x * p.y);
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

// 增强版fBm，增加迭代次数以提升细节密度
float fBm(vec2 p) {
      float v = 0.0;
      float a = 0.5;
      for (int i = 0; i < 8; ++i) { // 增加到8层，细节拉满
                                    v += a * noise(p);
                                    p = p * 2.0 + vec2(10.0);
                                    a *= 0.5;
      }
      return v;
}

// 【核心】领域扭曲 (Domain Warping) - 创造浓稠流体感的秘诀
// 用噪声去偏移噪声，产生像大理石或浓稠星云的纹理
float warp(vec2 p) {
      vec2 q = vec2(fBm(p + vec2(0.0)), fBm(p + vec2(5.2, 1.3)));
      vec2 r = vec2(fBm(p + 4.0 * q + vec2(1.7, 9.2)), fBm(p + 4.0 * q + vec2(8.3, 2.8)));
      return fBm(p + 4.0 * r);
}

// 模拟爆裂闪电
float lightning(vec2 p, float t) {
      float n = fBm(p * 3.0 + t);
      float line = abs(sin(p.x * 2.0 + n * 5.0));
      return smoothstep(0.9, 1.0, 1.0 - line) * smoothstep(0.0, 0.1, n);
}

void main() {
      // 1. 坐标系构建
      vec2 uv = v_texCoord - 0.5;
      float dist = length(uv);
      float angle = atan(uv.y, uv.x);

      // 强力漩涡扭曲：距离中心越近，旋转越剧烈
      float twist = 5.0 * (1.0 - dist * 2.0) * u_time * 0.5;
      float spiralAngle = angle + twist + dist * 4.0;
      vec2 spiralUv = vec2(spiralAngle, dist * 3.0);

      // ===================== 视觉堆料开始 =====================

      // --- 第一层：深渊底色 (Deep Void) ---
      vec3 finalColor = vec3(0.01, 0.0, 0.03);

      // --- 第二层：浓稠亚空间星云 (Thick Subspace Nebula) ---
      // 使用 Domain Warping 创造极度复杂的流体感
      float n1 = warp(spiralUv + u_time * 0.1);
      float n2 = warp(spiralUv * 1.5 - u_time * 0.2);

      vec3 colorA = vec3(0.3, 0.0, 0.6); // 深紫
      vec3 colorB = vec3(0.0, 0.7, 0.9); // 电青
      vec3 colorC = vec3(0.8, 0.2, 0.9); // 亮品红

      vec3 nebula = mix(colorA, colorB, n1);
      nebula = mix(nebula, colorC, n2);

      // 增加对比度和饱和度，让星云看起来“浓稠”
      nebula *= pow(n1 * n2, 0.5) * 3.0;
      nebula *= smoothstep(0.6, 0.1, dist); // 边缘衰减

      finalColor += nebula;

      // --- 第三层：中心爆裂闪电 (Bursting Lightning) ---
      // 闪电需要极高频的闪烁和不规则的放射状分布
      float flicker = sin(u_time * 30.0) * 0.5 + 0.5;
      float bolt = 0.0;
      for(int i=0; i<4; i++) {
            float rot = float(i) * 1.57;
            vec2 boltUv = vec2(spiralAngle + rot, dist * 5.0 - u_time * 2.0);
            bolt += lightning(boltUv, u_time * 10.0);
      }

      vec3 boltColor = vec3(0.9, 0.9, 1.0) * bolt * 5.0 * flicker;
      boltColor *= exp(-dist * 3.0); // 闪电从中心爆出，向外迅速衰减

      finalColor += boltColor;

      // --- 第四层：奇点光晕 (Singularity Glow) ---
      float core = exp(-dist * 15.0);
      float corePulse = 0.8 + 0.2 * sin(u_time * 20.0);
      vec3 coreColor = vec3(1.0, 1.0, 1.0) * core * corePulse * 10.0;

      finalColor += coreColor;

      // --- 第五层：空间撕裂噪点 (Spatial Grain) ---
      float grain = hash(v_texCoord + u_time) * 0.1;
      finalColor += grain;

      // ===================== 后处理：震撼增强 =====================

      // 1. 强力对比度提升 (Contrast)
      finalColor = smoothstep(0.0, 1.1, finalColor);

      // 2. 模拟 Bloom 效果 (让亮部溢出)
      float brightness = dot(finalColor, vec3(0.299, 0.587, 0.114));
      finalColor += finalColor * brightness * 0.5;

      // 3. 结合原图：原图被彻底撕碎并吸入
      vec2 distortUv = v_screenUv + (uv * dist * 2.0) + (normalize(uv) * 0.1 * sin(dist * 50.0 - u_time * 20.0));
      vec4 texSample = texture2D(u_texture, distortUv);
      finalColor += texSample.rgb * 0.4 * (1.0 - core);

      // 最终输出
      gl_FragColor = vec4(finalColor, 1.0) * v_color * u_color;
}