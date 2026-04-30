uniform sampler2D u_texture;
uniform vec4 u_color;
uniform float u_time;
uniform vec2 u_camsize;
uniform vec2 u_resolution;  // 视口分辨率
uniform float u_blackhole_size; // 黑洞大小参数 (默认 0.3)

varying vec4 v_color;
varying vec2 v_texCoord;
varying vec2 v_screenUv;

#define AA 1
#define _Speed 3.0
#define _Steps 12.0
#define _Size 0.3

float hash(float x) { return fract(sin(x) * 152754.742); }
float hash(vec2 x) { return hash(x.x + hash(x.y)); }

float value(vec2 p, float f) {
    float bl = hash(floor(p * f + vec2(0., 0.)));
    float br = hash(floor(p * f + vec2(1., 0.)));
    float tl = hash(floor(p * f + vec2(0., 1.)));
    float tr = hash(floor(p * f + vec2(1., 1.)));
    vec2 fr = fract(p * f);
    fr = (3. - 2. * fr) * fr * fr;
    float b = mix(bl, br, fr.x);
    float t = mix(tl, tr, fr.x);
    return mix(b, t, fr.y);
}

// 从 u_texture 采样背景，应用引力透镜扭曲
vec4 sampleBackground(vec3 ray, vec2 screenUv, float distortionStrength) {
    // 计算透镜扭曲后的 UV
    vec2 distortedUv = screenUv;

    // 基于光线方向计算扭曲（模拟引力透镜）
    float distFromCenter = length(ray.xy);
    float lensEffect = distortionStrength / (distFromCenter * distFromCenter + 0.001);
    lensEffect = clamp(lensEffect, 0.0, 2.0);

    // 径向扭曲
    vec2 dir = normalize(ray.xy + vec2(0.001));
    distortedUv += dir * lensEffect * 0.05;

    // 环形扭曲（爱因斯坦环效果）
    float ring = smoothstep(0.15, 0.2, distFromCenter) * smoothstep(0.35, 0.25, distFromCenter);
    distortedUv += dir * ring * 0.08;

    // 采样纹理
    vec4 tex = texture2D(u_texture, distortedUv);
    return tex;
}

vec4 raymarchDisk(vec3 ray, vec3 zeroPos) {
    vec3 position = zeroPos;
    float lengthPos = length(position.xz);
    float dist = min(1., lengthPos * (1.0 / _Size) * 0.5) * _Size * 0.4 * (1.0 / _Steps) / (abs(ray.y) + 0.001);

    position += dist * _Steps * ray * 0.5;

    vec2 deltaPos;
    deltaPos.x = -zeroPos.z * 0.01 + zeroPos.x;
    deltaPos.y = zeroPos.x * 0.01 + zeroPos.z;
    deltaPos = normalize(deltaPos - zeroPos.xz);

    float parallel = dot(ray.xz, deltaPos);
    parallel /= sqrt(lengthPos);
    parallel *= 0.5;
    float redShift = parallel + 0.3;
    redShift *= redShift;
    redShift = clamp(redShift, 0., 1.);

    float disMix = clamp((lengthPos - _Size * 2.) * (1.0 / _Size) * 0.24, 0., 1.);
    vec3 insideCol = mix(vec3(1.0, 0.8, 0.0), vec3(0.5, 0.13, 0.02) * 0.2, disMix);
    insideCol *= mix(vec3(0.4, 0.2, 0.1), vec3(1.6, 2.4, 4.0), redShift);
    insideCol *= 1.25;
    redShift += 0.12;
    redShift *= redShift;

    vec4 o = vec4(0.);

    for (float i = 0.; i < _Steps; i++) {
        position -= dist * ray;

        float intensity = clamp(1. - abs((i - 0.8) * (1.0 / _Steps) * 2.), 0., 1.);
        float lengthPos = length(position.xz);
        float distMult = 1.;

        distMult *= clamp((lengthPos - _Size * 0.75) * (1.0 / _Size) * 1.5, 0., 1.);
        distMult *= clamp((_Size * 10. - lengthPos) * (1.0 / _Size) * 0.20, 0., 1.);
        distMult *= distMult;

        float u = lengthPos + u_time * _Size * 0.3 + intensity * _Size * 0.2;

        vec2 xy;
        float rot = mod(u_time * _Speed, 8192.);
        xy.x = -position.z * sin(rot) + position.x * cos(rot);
        xy.y = position.x * sin(rot) + position.z * cos(rot);

        float x = abs(xy.x / (xy.y + 0.001));
        float angle = 0.02 * atan(x);

        const float f = 70.;
        float noise = value(vec2(angle, u * (1.0 / _Size) * 0.05), f);
        noise = noise * 0.66 + 0.33 * value(vec2(angle, u * (1.0 / _Size) * 0.05), f * 2.);

        float extraWidth = noise * 1. * (1. - clamp(i * (1.0 / _Steps) * 2. - 1., 0., 1.));
        float alpha = clamp(noise * (intensity + extraWidth) * ((1.0 / _Size) * 10. + 0.01) * dist * distMult, 0., 1.);

        vec3 col = 2. * mix(vec3(0.3, 0.2, 0.15) * insideCol, insideCol, min(1., intensity * 2.));
        o = clamp(vec4(col * alpha + o.rgb * (1. - alpha), o.a * (1. - alpha) + alpha), vec4(0.), vec4(1.));

        lengthPos *= (1.0 / _Size);
        o.rgb += redShift * (intensity * 1. + 0.5) * (1.0 / _Steps) * 100. * distMult / (lengthPos * lengthPos + 0.001);
    }

    o.rgb = clamp(o.rgb - 0.005, 0., 1.);
    return o;
}

void Rotate(inout vec3 vector, vec2 angle) {
    vector.yz = cos(angle.y) * vector.yz + sin(angle.y) * vec2(-1, 1) * vector.zy;
    vector.xz = cos(angle.x) * vector.xz + sin(angle.x) * vec2(-1, 1) * vector.zx;
}

void main() {
    vec4 colOut = vec4(0.);

    // 将 v_screenUv 转换为 [-0.5, 0.5] 的中心坐标系
    vec2 uv = v_screenUv - 0.5;
    uv.x *= u_resolution.x / u_resolution.y; // 修正宽高比

    // 旋转 UV（模拟相机倾斜）
    vec2 fragCoordRot;
    fragCoordRot.x = uv.x * 0.985 + uv.y * 0.174;
    fragCoordRot.y = uv.y * 0.985 - uv.x * 0.174;
    fragCoordRot += vec2(-0.06, 0.12);

    for (int j = 0; j < AA; j++)
    for (int i = 0; i < AA; i++) {
        // 设置相机/射线
        vec3 ray = normalize(vec3((fragCoordRot + vec2(float(i), float(j)) / float(AA)) * 2.0, 1.0));
        vec3 pos = vec3(0., 0.05, -3.0); // 相机位置
        vec2 angle = vec2(u_time * 0.1, 0.2);
        float dist = length(pos);
        Rotate(pos, angle);
        angle.xy -= min(.3 / dist, 3.14) * vec2(1, 0.5);
        Rotate(ray, angle);

        vec4 col = vec4(0.);
        vec4 glow = vec4(0.);
        vec4 outCol = vec4(100.);

        for (int disks = 0; disks < 20; disks++) {
            for (int h = 0; h < 6; h++) {
                float dotpos = dot(pos, pos);
                float invDist = inversesqrt(dotpos);
                float centDist = dotpos * invDist;
                float stepDist = 0.92 * abs(pos.y / (ray.y + 0.001));
                float farLimit = centDist * 0.5;
                float closeLimit = centDist * 0.1 + 0.05 * centDist * centDist * (1.0 / _Size);
                stepDist = min(stepDist, min(farLimit, closeLimit));

                float invDistSqr = invDist * invDist;
                float bendForce = stepDist * invDistSqr * _Size * 0.625;
                ray = normalize(ray - (bendForce * invDist) * pos);
                pos += stepDist * ray;

                glow += vec4(1.2, 1.1, 1.0, 1.0) * (0.01 * stepDist * invDistSqr * invDistSqr * clamp(centDist * 2. - 1.2, 0., 1.));
            }

            float dist2 = length(pos);

            if (dist2 < _Size * 0.1) {
                // 光线被吸入黑洞 - 纯黑事件视界
                outCol = vec4(col.rgb * col.a + glow.rgb * (1. - col.a), 1.);
                break;
            }
            else if (dist2 > _Size * 1000.) {
                // 光线逃逸 - 采样 u_texture 作为背景，应用引力透镜
                vec4 bg = sampleBackground(ray, v_screenUv, _Size * 2.0);
                outCol = vec4(col.rgb * col.a + bg.rgb * (1. - col.a) + glow.rgb * (1. - col.a), 1.);
                break;
            }
            else if (abs(pos.y) <= _Size * 0.002) {
                // 光线击中吸积盘
                vec4 diskCol = raymarchDisk(ray, pos);
                pos.y = 0.;
                pos += abs(_Size * 0.001 / (ray.y + 0.001)) * ray;
                col = vec4(diskCol.rgb * (1. - col.a) + col.rgb, col.a + diskCol.a * (1. - col.a));
            }
        }

        if (outCol.r == 100.)
        outCol = vec4(col.rgb + glow.rgb * (col.a + glow.a), 1.);

        col = outCol;
        col.rgb = pow(col.rgb, vec3(0.6));

        colOut += col / float(AA * AA);
    }

    // 应用顶点颜色和 uniform 颜色
    gl_FragColor = colOut * v_color * u_color;
}