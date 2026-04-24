attribute vec2 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

uniform mat4 u_proj;
uniform mat4 u_trns;

void main() {
    vec4 world = u_trns * vec4(a_position, 0.0, 1.0);
    vec4 clip = u_proj * world;

    v_color = a_color;
    v_texCoord = a_texCoord0;

    // clip space (-1~1) -> screen uv (0~1)
    v_screenUv = clip.xy / clip.w * 0.5 + 0.5;

    gl_Position = clip;
}
