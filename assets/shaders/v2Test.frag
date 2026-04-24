uniform sampler2D u_texture;
uniform vec2 u_resolution;
uniform float u_time;

varying vec2 v_texCoords;

void main() {
    vec2 uv = v_texCoords;

    // === 1. 热浪扭曲 ===
    float waveX = sin(uv.y * 20.0 + u_time * 6.0) * 0.025;
    float waveY = cos(uv.x * 15.0 + u_time * 4.0) * 0.025;
    uv += vec2(waveX, waveY);

    vec4 color = texture2D(u_texture, uv);

    // === 2. 彩虹色循环叠加 ===
    float t = u_time * 3.0;
    vec3 rainbow = vec3(
    sin(t) * 0.5 + 0.5,
    sin(t + 2.094) * 0.5 + 0.5,
    sin(t + 4.189) * 0.5 + 0.5
    );
    color.rgb = mix(color.rgb, rainbow, 0.5);

    // === 3. 脉冲亮度闪烁 ===
    float flash = 0.7 + 0.3 * sin(u_time * 8.0);
    color.rgb *= flash;

    // === 4. 扫描线 ===
    float scanline = sin(uv.y * 40.0 + u_time * 10.0) * 0.08;
    color.rgb += scanline;

    gl_FragColor = color;
}