uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

void main() {
    // 计算时间影响的波动因子
    float distortion = sin(u_time * 10.0) * 0.1;  // 使用正弦波进行动态扭曲
    vec2 distortedUv = v_screenUv + vec2(sin(v_screenUv.y * 10.0 + u_time) * distortion, cos(v_screenUv.x * 10.0 + u_time) * distortion);

    // 获取被扭曲的纹理值
    vec4 tex = texture2D(u_texture, distortedUv);

    // 计算最终的颜色
    gl_FragColor = tex * v_color * u_color;
}
