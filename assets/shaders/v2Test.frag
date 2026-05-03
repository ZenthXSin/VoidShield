#define HIGHP

#define ALPHA 1.0
#define STEP_SIZE 2.0

uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec2 u_invsize;
uniform float u_time;
uniform float u_dp;
uniform vec2 u_offset;

varying vec2 v_texCoords;

#define PI  3.141592654
#define TAU (2.0*PI)

#define TIME (u_time*0.1)

#define LESS(a,b,c) mix(a,b,step(0.,c))

#define SABS(x,k)    LESS((.5/k)*x*x+k*.5,abs(x),abs(x)-k)

#define RESOLUTION   u_texsize

const float twirly = 2.5;

vec2 toPolar(vec2 p) {
  return vec2(length(p), atan(p.y, p.x));
}

vec2 toRect(vec2 p) {
  return p.x*vec2(cos(p.y), sin(p.y));
}

vec2 mod2(inout vec2 p, vec2 size) {
  vec2 c = floor((p + size*0.5)/size);
  p = mod(p + size*0.5,size) - size*0.5;
  return c;
}

float noise1(vec2 p) {
  p *= tanh(0.1*length(p));
  float tm = TIME;
  float a = cos(p.x);
  float b = cos(p.y);
  float c = cos(p.x*sqrt(3.5)+tm);
  float d = cos(p.y*sqrt(1.5)+tm);
  return a*b*c*d;
}

void rot(inout vec2 p, float a) {
  float c = cos(a);
  float s = sin(a);
  p = vec2(c*p.x + s*p.y, -s*p.x + c*p.y);
}

vec2 twirl(vec2 p, float a, float z) {
  vec2 pp = toPolar(p);
  pp.y += pp.x*twirly + a;
  p = toRect(pp);
  p *= z;
  return p;
}

float galaxyNoise(vec2 p, float a, float z) {
  p = twirl(p, a, z);
  return noise1(p);
}

float rand(vec2 co){
  return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

vec2 raySphere(vec3 ro, vec3 rd, vec3 center, float radius) {
  vec3 m = ro - center.xyz;
  float b = dot(m, rd);
  float c = dot(m, m) - radius*radius;
  if(c > 0.0 && b > 0.0) return vec2(-1.0, -1.0);
  float discr = b * b - c;
  if(discr < 0.0) return vec2(-1.0);
  float s = sqrt(discr);
  float t0 = -b - s;
  float t1 = -b + s;
  return vec2(t0, t1);
}

vec3 stars(vec2 p) {
  float l = length(p);
  vec2 pp = toPolar(p);
  pp.x /= (1.0+length(pp.x))*0.5;
  p = toRect(pp);
  float sz = 0.0075;
  vec3 s = vec3(10000.0);
  for (int i = 0; i < 3; ++i) {
    rot(p, 0.5);
    vec2 ip = p;
    vec2 n = mod2(ip, vec2(sz));
    float r = rand(n);
    vec2 o = -1.0 + 2.0*vec2(r, fract(r*1000.0));
    s.x = min(s.x, length(ip-0.25*sz*o));
    s.yz = n*0.1;
  }
  return s;
}

float height(vec2 p) {
  float ang = atan(p.y, p.x);
  float l = length(p);
  float sp = mix(1.0, pow(0.75 + 0.25*sin(2.0*(ang + l*twirly)), 3.0), tanh(6.0*l));
  float s = 0.0;
  float a = 1.0;
  float f = 15.0;
  float d = 0.0;
  for (int i = 0; i < 11; ++i) {
    float g = a*galaxyNoise(p, TIME*(0.025*float(i)), f);
    s += g;
    a *= sqrt(0.45);
    f *= sqrt(2.0);
    d += a;
  }
  s *= sp;
  return SABS((-0.25+ s/d), 0.5)*exp(-5.5*l*l);
}

vec3 normal(vec2 p) {
  vec2 eps = vec2(0.000125, 0.0);
  vec3 n;
  n.x = height(p - eps.xy) - height(p + eps.xy);
  n.y = 2.0*eps.x;
  n.z = height(p - eps.yx) - height(p + eps.yx);
  return normalize(n);
}

const vec3 colDust = vec3(1.0, 0.9, 0.75);

vec3 galaxyRender(vec2 p, vec3 ro, vec3 rd, float d) {
  rot(p, 0.5*TIME);
  float h = height(p);
  vec3 s = stars(p);
  float th = tanh(h);
  vec3 n = normal(p);
  vec3 p3 = vec3(p.x, th, p.y);
  float lh = 0.5;
  vec3 lp1 = vec3(-0.0, lh, 0.0);
  vec3 ld1 = normalize(lp1 - p3);
  vec3 lp2 = vec3(0.0, lh, 0.0);
  vec3 ld2 = normalize(lp2 - p3);
  float l = length(p);
  float tl = tanh(l);
  float diff1 = max(dot(ld1, n), 0.0);
  float diff2 = max(dot(ld2, n), 0.0);
  vec3 col = vec3(0.0);
  col += vec3(0.5, 0.5, 0.75)*h;
  col += 0.25*pow(diff2, 4.0);
  col += pow(vec3(0.5)*h, n.y*1.75*(mix(vec3(0.5, 1.0, 1.5), vec3(0.5, 1.0, 1.5).zyx, 1.25*tl)));
  float sr = rand(s.yz);
  float si = pow(th*sr, 0.25)*0.001;
  vec3 scol = sr*5.0*exp(-2.5*l*l)*tanh(pow(si/(s.x), 2.5))*mix(vec3(0.5, 0.75, 1.0), vec3(1.0, 0.75, 0.5), sr*0.6);
  scol = clamp(scol, 0.0, 1.0);
  col += scol*smoothstep(0.0, 0.35, 1.0-n.y);
  float ddust = (h - ro.y)/rd.y;
  if (ddust < d) {
    float t = d - ddust;
    col += 0.7*colDust*(1.0-exp(-2.0*t));
  }
  return col;
}

vec3 renderGalaxy(vec3 ro, vec3 rd) {
  float dgalaxy = (0.0 - ro.y)/rd.y;
  vec3 col = vec3(0);
  if (dgalaxy > 0.0) {
    col = vec3(0.5);
    vec3 p = ro + dgalaxy*rd;
    col = galaxyRender(p.xz, ro, rd, dgalaxy);
  }
  vec2 cgalaxy = raySphere(ro, rd, vec3(0.0), 0.125);
  float t;
  if (dgalaxy > 0.0 && cgalaxy.x > 0.0) {
    float t0 = max(dgalaxy - cgalaxy.x, 0.0);
    float t1 = cgalaxy.y - cgalaxy.x;
    t = min(t0, t1);
  } else if (cgalaxy.x < cgalaxy.y){
    t = cgalaxy.y - cgalaxy.x;
  }
  col += 1.7*colDust*(1.0-exp(-1.0*t));
  return col;
}

vec3 postProcess(vec3 col, vec2 q) {
  col = pow(clamp(col,0.0,1.0),vec3(0.75));
  col = col*0.6+0.4*col*col*(3.0-2.0*col);
  col = mix(col, vec3(dot(col, vec3(0.33))), -0.4);
  col *= 0.5+0.5*pow(19.0*q.x*q.y*(1.0-q.x)*(1.0-q.y),0.7);
  return col;
}

void main(){
  vec2 T = v_texCoords.xy;
  vec2 coords = (T * u_texsize) + u_offset;
  vec2 v = u_invsize;

  // UV扰动（受相机影响，保留原特效行为）
  T += vec2(sin(coords.y / 3.0 + u_time / 20.0), sin(coords.x / 3.0 + u_time / 20.0)) / u_texsize;

  // 边缘检测（使用 STEP_SIZE 代替 step）
  vec4 maxed = max(max(max(texture2D(u_texture, T + vec2(0, STEP_SIZE) * v), texture2D(u_texture, T + vec2(0, -STEP_SIZE) * v)), texture2D(u_texture, T + vec2(STEP_SIZE, 0) * v)), texture2D(u_texture, T + vec2(-STEP_SIZE, 0) * v));

  vec4 color = texture2D(u_texture, T);

  if(texture2D(u_texture, T).a < 0.9 && maxed.a > 0.9){
    // 边缘高亮
    gl_FragColor = vec4(maxed.rgb, maxed.a * 100.0);
  } else {
    if(color.a > 0.0){
      color.a = ALPHA;
    }

    // 螺旋星系 —— 基于屏幕坐标，不受相机缩放/偏移影响
    // 直接用 v_texCoords 计算，不乘 u_texsize，不加 u_offset
    vec2 q = v_texCoords;  // 0~1 屏幕空间
    vec2 p = -1.0 + 2.0 * q;
    p.x *= RESOLUTION.x / RESOLUTION.y;

    vec3 ro = vec3(0.0, 0.7, 2.0) * 0.75;
    vec3 la = vec3(0.0, 0.0, 0.0);
    vec3 up = vec3(-0.5, 1.0, 0.0);
    vec3 ww = normalize(la - ro);
    vec3 uu = normalize(cross(up, ww));
    vec3 vv = normalize(cross(ww, uu));
    vec3 rd = normalize(p.x * uu + p.y * vv + 2.5 * ww);

    vec3 galaxyCol = renderGalaxy(ro, rd);
    galaxyCol = postProcess(galaxyCol, q);

    vec3 finalCol = mix(color.rgb, galaxyCol, 0.6);
    gl_FragColor = vec4(finalCol, color.a);
  }
}