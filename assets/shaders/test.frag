uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

#define iterations 17
#define formuparam 0.53
#define volsteps 20
#define stepsize 0.1
#define zoom   0.800
#define tile   0.850
#define speed  0.010
#define brightness 0.0015
#define darkmatter 0.300
#define distfading 0.730
#define saturation 0.850

void main() {
      // 从 v_texCoord 构建 uv（适配你的坐标系）
      vec2 uv = v_texCoord - 0.5;
      // 保持纵横比（如果需要的话，根据 u_camsize 调整）
      // uv.y *= u_camsize.y / u_camsize.x;

      vec3 dir = vec3(uv * zoom, 1.0);
      float time = u_time * speed + 0.25;

      // 取消鼠标旋转，改用自动旋转
      float a1 = 0.5 + time * 0.2;
      float a2 = 0.8 + time * 0.15;
      mat2 rot1 = mat2(cos(a1), sin(a1), -sin(a1), cos(a1));
      mat2 rot2 = mat2(cos(a2), sin(a2), -sin(a2), cos(a2));
      dir.xz *= rot1;
      dir.xy *= rot2;

      vec3 from = vec3(1.0, 0.5, 0.5);
      from += vec3(time * 2.0, time, -2.0);
      from.xz *= rot1;
      from.xy *= rot2;

      // volumetric rendering
      float s = 0.1, fade = 1.0;
      vec3 v = vec3(0.0);
      for (int r = 0; r < volsteps; r++) {
            vec3 p = from + s * dir * 0.5;
            p = abs(vec3(tile) - mod(p, vec3(tile * 2.0))); // tiling fold
            float pa, a = pa = 0.0;
      for (int i = 0; i < iterations; i++) {
      p = abs(p) / dot(p, p) - formuparam; // the magic formula
      a += abs(length(p) - pa); // absolute sum of average change
      pa = length(p);
      }
      float dm = max(0.0, darkmatter - a * a * 0.001); // dark matter
      a *= a * a; // add contrast
      if (r > 6) fade *= 1.0 - dm; // dark matter, don't render near
      v += fade;
      v += vec3(s, s * s, s * s * s * s) * a * brightness * fade; // coloring based on distance
      fade *= distfading; // distance fading
      s += stepsize;
      }
      v = mix(vec3(length(v)), v, saturation); // color adjust

      vec3 starColor = v * 0.01;

      // 混合原图
      vec4 tex = texture2D(u_texture, v_screenUv);
      vec3 finalColor = mix(tex.rgb, starColor, 0.85);

      gl_FragColor = vec4(finalColor, 1.0) * v_color * u_color;
}