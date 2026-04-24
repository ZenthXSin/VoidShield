uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

// ========== 噪声函数 ==========
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

// 分形布朗运动 - 用于能量流动
float fbm(vec2 p) {
      float value = 0.0;
      float amplitude = 0.5;
      float frequency = 1.0;
      for(int i = 0; i < 5; i++) {
            value += amplitude * noise(p * frequency);
            amplitude *= 0.5;
            frequency *= 2.0;
      }
      return value;
}

// ========== 电弧生成 ==========
// 闪电状电弧，沿切线方向跳动
float arcLine(vec2 uv, vec2 center, float radius, float thickness, float intensity) {
      vec2 dir = uv - center;
      float dist = length(dir);
      float angle = atan(dir.y, dir.x);

      // 电弧沿圆周分布，带时间偏移
      float arcCount = 3.0 + floor(hash(vec2(radius)) * 5.0); // 3-8条电弧
      float arcPos = fract(angle / 6.28318 * arcCount + u_time * 2.0 + hash(vec2(radius)) * 10.0);

      // 电弧跳动
      float jump = sin(u_time * 8.0 + radius * 20.0) * 0.5 + 0.5;
      float arcWidth = thickness * (0.5 + jump * 0.5);

      // 径向距离检测（在球壳上）
      float shellDist = abs(dist - radius);
      float arcMask = 1.0 - smoothstep(0.0, arcWidth, shellDist);

      // 电弧亮度脉冲
      float pulse = sin(u_time * 10.0 + radius * 15.0) * 0.5 + 0.5;
      arcMask *= pulse * intensity;

      // 电弧只在特定角度出现（断续效果）
      float arcSegment = smoothstep(0.0, 0.1, arcPos) * (1.0 - smoothstep(0.15, 0.25, arcPos));
      arcMask *= arcSegment;

      return arcMask;
}

// 多层级电弧网络
float arcNetwork(vec2 uv, vec2 center) {
      float arcs = 0.0;

      // 内层电弧（快，细密）
      arcs += arcLine(uv, center, 0.15, 0.015, 1.2) * 0.8;
      arcs += arcLine(uv, center, 0.22, 0.012, 1.0) * 0.7;

      // 中层电弧（主电弧）
      arcs += arcLine(uv, center, 0.35, 0.02, 1.5) * 1.0;
      arcs += arcLine(uv, center, 0.42, 0.018, 1.3) * 0.9;

      // 外层电弧（慢，粗大）
      arcs += arcLine(uv, center, 0.48, 0.025, 1.0) * 0.6;

      // 连接电弧（径向闪电）
      float radialArc = 0.0;
      float radialAngle = u_time * 3.0;
      vec2 radialDir = vec2(cos(radialAngle), sin(radialAngle));
      float radialProj = dot(uv - center, radialDir);
      float radialDist = length((uv - center) - radialProj * radialDir);
      radialArc = (1.0 - smoothstep(0.0, 0.02, radialDist)) * smoothstep(0.0, 0.5, abs(radialProj));
      radialArc *= sin(u_time * 15.0) * 0.5 + 0.5;

      arcs += radialArc * 0.5;

      return arcs;
}

// ========== 虚空盾能量场 ==========
float voidShieldEnergy(vec2 uv, vec2 center) {
      vec2 dir = uv - center;
      float dist = length(dir);

      // 基础能量场（不均匀分布）
      float energy = fbm(uv * 8.0 + vec2(u_time * 0.5));
      energy += fbm(uv * 16.0 - vec2(u_time * 0.3)) * 0.5;

      // 径向衰减（中心强，边缘弱但存在）
      float radialFalloff = 1.0 - smoothstep(0.0, 0.5, dist);
      radialFalloff = pow(radialFalloff, 0.7); // 让衰减更平缓

      // 波动效果（呼吸感）
      float breathe = sin(u_time * 2.0 + dist * 10.0) * 0.3 + 0.7;

      // 边缘强化（护盾边界）
      float edge = 1.0 - smoothstep(0.45, 0.5, dist);
      edge = pow(edge, 2.0) * 1.5;

      return energy * radialFalloff * breathe + edge * 0.3;
}

void main() {
      vec2 center = vec2(0.5);
      vec2 dir = v_texCoord - center;
      float dist = length(dir);

      // ========== 遮罩（护盾形状） ==========
      // 主护盾遮罩（略微不规则边缘）
      float edgeNoise = noise(v_texCoord * 20.0 + u_time) * 0.02;
      float mask = 1.0 - smoothstep(0.0, 0.5 + edgeNoise, dist);
      mask = pow(mask, 0.9);

      // ========== 虚空盾核心效果 ==========

      // 1. 能量场计算
      float energy = voidShieldEnergy(v_texCoord, center);

      // 2. 电弧网络
      float arcs = arcNetwork(v_texCoord, center);

      // 3. 噪点闪烁（微观扰动）
      float flicker = noise(v_texCoord * 100.0 + u_time * 20.0);
      flicker = pow(flicker, 3.0) * 0.3;

      // ========== 颜色合成 ==========
      // 虚空紫色调
      vec3 voidPurple = vec3(0.4, 0.1, 0.8);  // 深紫基色
      vec3 arcColor = vec3(0.7, 0.3, 1.0);     // 电弧亮紫
      vec3 coreColor = vec3(0.2, 0.0, 0.5);    // 核心暗紫

      // 能量场颜色（不均匀分布）
      vec3 energyColor = mix(coreColor, voidPurple, energy);
      energyColor = mix(energyColor, vec3(0.9, 0.5, 1.0), flicker);

      // 叠加电弧（带发光效果）
      float arcGlow = arcs * (1.0 + flicker * 2.0);
      vec3 finalColor = mix(energyColor, arcColor, clamp(arcGlow, 0.0, 1.0));

      // 电弧高亮
      finalColor += arcColor * arcs * 0.5;

      // ========== 透明度控制 ==========
      // 基础透明度（不均匀）
      float baseAlpha = energy * 0.4 + 0.1;  // 0.1-0.5 基础透明
      baseAlpha += arcs * 0.3;               // 电弧处更亮
      baseAlpha += flicker * 0.1;            // 闪烁

      // 边缘淡出
      baseAlpha *= mask;

      // 整体透明度调制（呼吸效果）
      float globalBreathe = sin(u_time * 1.5) * 0.1 + 0.9;
      baseAlpha *= globalBreathe;

      // 限制最大透明度（保持透明感）
      baseAlpha = clamp(baseAlpha, 0.0, 0.7);

      // ========== 背景扭曲（可选，轻微） ==========
      // 虚空盾对背景的轻微扭曲（空间不稳定感）
      const float pixelStrength = 1.5;
      vec2 warpUV = v_texCoord * 15.0 + vec2(u_time);
      float warpNoise = noise(warpUV) + noise(warpUV * 2.0) * 0.5;
      vec2 warpOffset = vec2(
      noise(warpUV + vec2(u_time * 2.0, 0.0)),
      noise(warpUV + vec2(0.0, u_time * 1.5))
      ) * pixelStrength / u_camsize * mask * energy;

      vec4 background = texture2D(u_texture, v_screenUv + warpOffset);

      // ========== 最终合成 ==========
      // 混合护盾颜色和背景
      vec3 composite = mix(background.rgb, finalColor, baseAlpha);

      // 电弧高光（加法混合）
      composite += arcColor * arcs * 0.4 * mask;

      gl_FragColor = vec4(composite, 1.0) * v_color * u_color;
}