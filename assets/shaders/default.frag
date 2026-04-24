uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_camsize;

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

void main() {
    vec4 tex = texture2D(u_texture, v_screenUv);
    gl_FragColor = tex * v_color * u_color;
}
