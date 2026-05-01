/*
Main render.

Temporal AA for a smooth image. Temporal accumulation is disabled while moving the view to prevent ghosting.
*/

#define ITERATIONS 200          //Increase for less grainy result
#define TEMPORAL_AA


const vec3 MainColor = vec3(1.0);

//noise code by iq
float noise( in vec3 x )
{
    vec3 p = floor(x);
    vec3 f = fract(x);
    f = f*f*(3.0-2.0*f);
    vec2 uv = (p.xy+vec2(37.0,17.0)*p.z) + f.xy;
    vec2 rg = textureLod( iChannel0, (uv+ 0.5)/256.0, 0.0 ).yx;
    return -1.0+2.0*mix( rg.x, rg.y, f.z );
}

float saturate(float x)
{
    return clamp(x, 0.0, 1.0);
}

vec3 saturate(vec3 x)
{
    return clamp(x, vec3(0.0), vec3(1.0));
}

float rand(vec2 coord)
{
    return saturate(fract(sin(dot(coord, vec2(12.9898, 78.223))) * 43758.5453));
}

float pcurve( float x, float a, float b )
{
    float k = pow(a+b,a+b) / (pow(a,a)*pow(b,b));
    return k * pow( x, a ) * pow( 1.0-x, b );
}

const float pi = 3.14159265;

float atan2(float y, float x)
{
    if (x > 0.0)
    {
        return atan(y / x);
    }
    else if (x == 0.0)
    {
        if (y > 0.0)
        {
            return pi / 2.0;
        }
        else if (y < 0.0)
        {
            return -(pi / 2.0);
        }
        else
        {
            return 0.0;
        }
    }
    else //(x < 0.0)
    {
        if (y >= 0.0)
        {
            return atan(y / x) + pi;
        }
        else
        {
            return atan(y / x) - pi;
        }
    }
}

float sdTorus(vec3 p, vec2 t)
{
    vec2 q = vec2(length(p.xz) - t.x, p.y);
    return length(q)-t.y;
}

float sdSphere(vec3 p, float r)
{
    return length(p)-r;
}

void Haze(inout vec3 color, vec3 pos, float alpha)
{
    vec2 t = vec2(1.0, 0.01);

    float torusDist = length(sdTorus(pos + vec3(0.0, -0.05, 0.0), t));

    float bloomDisc = 1.0 / (pow(torusDist, 2.0) + 0.001);
    vec3 col = MainColor;
    bloomDisc *= length(pos) < 0.5 ? 0.0 : 1.0;

    color += col * bloomDisc * (2.9 / float(ITERATIONS)) * (1.0 - alpha * 1.0);
}

void GasDisc(inout vec3 color, inout float alpha, vec3 pos)
{
    float discRadius = 3.2;
    float discWidth = 5.3;
    float discInner = discRadius - discWidth * 0.5;
    float discOuter = discRadius + discWidth * 0.5;

    vec3 origin = vec3(0.0, 0.0, 0.0);
    float mouseZ = iMouse.y / iResolution.y;
    vec3 discNormal = normalize(vec3(0.0, 1.0, 0.0));
    float discThickness = 0.1;

    float distFromCenter = distance(pos, origin);
    float distFromDisc = dot(discNormal, pos - origin);

    float radialGradient = 1.0 - saturate((distFromCenter - discInner) / discWidth * 0.5);

    float coverage = pcurve(radialGradient, 4.0, 0.9);

    discThickness *= radialGradient;
    coverage *= saturate(1.0 - abs(distFromDisc) / discThickness);

    vec3 dustColorLit = MainColor;
    vec3 dustColorDark = vec3(0.0, 0.0, 0.0);

    float dustGlow = 1.0 / (pow(1.0 - radialGradient, 2.0) * 290.0 + 0.002);
    vec3 dustColor = dustColorLit * dustGlow * 8.2;

    coverage = saturate(coverage * 0.7);


    float fade = pow((abs(distFromCenter - discInner) + 0.4), 4.0) * 0.04;
    float bloomFactor = 1.0 / (pow(distFromDisc, 2.0) * 40.0 + fade + 0.00002);
    vec3 b = dustColorLit * pow(bloomFactor, 1.5);

    b *= mix(vec3(1.7, 1.1, 1.0), vec3(0.5, 0.6, 1.0), vec3(pow(radialGradient, 2.0)));
    b *= mix(vec3(1.7, 0.5, 0.1), vec3(1.0), vec3(pow(radialGradient, 0.5)));

    dustColor = mix(dustColor, b * 150.0, saturate(1.0 - coverage * 1.0));
    coverage = saturate(coverage + bloomFactor * bloomFactor * 0.1);

    if (coverage < 0.01)
    {
        return;
    }


    vec3 radialCoords;
    radialCoords.x = distFromCenter * 1.5 + 0.55;
    radialCoords.y = atan2(-pos.x, -pos.z) * 1.5;
    radialCoords.z = distFromDisc * 1.5;

    radialCoords *= 0.95;

    float speed = 0.06;

    float noise1 = 1.0;
    vec3 rc = radialCoords + 0.0;               rc.y += iTime * speed;
    noise1 *= noise(rc * 3.0) * 0.5 + 0.5;      rc.y -= iTime * speed;
    noise1 *= noise(rc * 6.0) * 0.5 + 0.5;      rc.y += iTime * speed;
    noise1 *= noise(rc * 12.0) * 0.5 + 0.5;     rc.y -= iTime * speed;
    noise1 *= noise(rc * 24.0) * 0.5 + 0.5;     rc.y += iTime * speed;

    float noise2 = 2.0;
    rc = radialCoords + 30.0;
    noise2 *= noise(rc * 3.0) * 0.5 + 0.5;      rc.y += iTime * speed;
    noise2 *= noise(rc * 6.0) * 0.5 + 0.5;      rc.y -= iTime * speed;
    noise2 *= noise(rc * 12.0) * 0.5 + 0.5;     rc.y += iTime * speed;
    noise2 *= noise(rc * 24.0) * 0.5 + 0.5;     rc.y -= iTime * speed;
    noise2 *= noise(rc * 48.0) * 0.5 + 0.5;     rc.y += iTime * speed;
    noise2 *= noise(rc * 92.0) * 0.5 + 0.5;     rc.y -= iTime * speed;

    dustColor *= noise1 * 0.998 + 0.002;
    coverage *= noise2;

    radialCoords.y += iTime * speed * 0.5;

    dustColor *= pow(texture(iChannel1, radialCoords.yx * vec2(0.15, 0.27)).rgb, vec3(2.0)) * 4.0;

    coverage = saturate(coverage * 1200.0 / float(ITERATIONS));
    dustColor = max(vec3(0.0), dustColor);

    coverage *= pcurve(radialGradient, 4.0, 0.9);

    color = (1.0 - alpha) * dustColor * coverage + color;

    alpha = (1.0 - alpha) * coverage + alpha;
}



vec3 rotate(vec3 p, float x, float y, float z)
{
    mat3 matx = mat3(1.0, 0.0, 0.0,
    0.0, cos(x), sin(x),
    0.0, -sin(x), cos(x));

    mat3 maty = mat3(cos(y), 0.0, -sin(y),
    0.0, 1.0, 0.0,
    sin(y), 0.0, cos(y));

    mat3 matz = mat3(cos(z), sin(z), 0.0,
    -sin(z), cos(z), 0.0,
    0.0, 0.0, 1.0);

    p = matx * p;
    p = matz * p;
    p = maty * p;

    return p;
}

void RotateCamera(inout vec3 eyevec, inout vec3 eyepos)
{
    float mousePosY = iMouse.y / iResolution.y;
    float mousePosX = iMouse.x / iResolution.x;

    vec3 angle = vec3(mousePosY * 0.05 + 0.05, 1.0 + mousePosX * 1.0, -0.45);

    eyevec = rotate(eyevec, angle.x, angle.y, angle.z);
    eyepos = rotate(eyepos, angle.x, angle.y, angle.z);
}

void WarpSpace(inout vec3 eyevec, inout vec3 raypos)
{
    vec3 origin = vec3(0.0, 0.0, 0.0);

    float singularityDist = distance(raypos, origin);
    float warpFactor = 1.0 / (pow(singularityDist, 2.0) + 0.000001);

    vec3 singularityVector = normalize(origin - raypos);

    float warpAmount = 5.0;

    eyevec = normalize(eyevec + singularityVector * warpFactor * warpAmount / float(ITERATIONS));
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{

    vec2 uv = fragCoord.xy / iResolution.xy;

    float aspect = iResolution.x / iResolution.y;

    vec2 uveye = uv;

    #ifdef TEMPORAL_AA
    uveye.x += (rand(uv + sin(iTime * 1.0)) / iResolution.x) * (iMouse.z > 1.0 ? 0.0 : 1.0);
    uveye.y += (rand(uv + 1.0 + sin(iTime * 1.0)) / iResolution.y) * (iMouse.z > 1.0 ? 0.0 : 1.0);
    #endif

    vec3 eyevec = normalize(vec3((uveye * 2.0 - 1.0) * vec2(aspect, 1.0), 6.0));
    vec3 eyepos = vec3(0.0, -0.0, -10.0);

    vec2 mousepos = iMouse.xy / iResolution.xy;
    if (mousepos.x == 0.0)
    {
        mousepos.x = 0.35;
    }
    eyepos.x += mousepos.x * 3.0 - 1.5;

    const float far = 15.0;

    RotateCamera(eyevec, eyepos);

    vec3 color = vec3(0.0, 0.0, 0.0);

    float dither = rand(uv
#ifdef TEMPORAL_AA
                        + sin(iTime * 1.0) * (iMouse.z > 1.0 ? 0.0 : 1.0)
#endif
                       ) * 2.0;


float alpha = 0.0;
vec3 raypos = eyepos + eyevec * dither * far / float(ITERATIONS);
for (int i = 0; i < ITERATIONS; i++)
{
WarpSpace(eyevec, raypos);
raypos += eyevec * far / float(ITERATIONS);
GasDisc(color, alpha, raypos);
Haze(color, raypos, alpha);
}

color *= 0.0001;


#ifdef TEMPORAL_AA
    const float p = 1.0;
vec3 previous = pow(texture(iChannel2, uv).rgb, vec3(1.0 / p));

color = pow(color, vec3(1.0 / p));

float blendWeight = 0.9 * (iMouse.z > 1.0 ? 0.0 : 1.0);

color = mix(color, previous, blendWeight);

color = pow(color, vec3(p));
#endif

fragColor = vec4(saturate(color), 1.0);

}

//First bloom pass, mipmap tree thing

vec3 ColorFetch(vec2 coord)
{
    return texture(iChannel0, coord).rgb;
}

vec3 Grab1(vec2 coord, const float octave, const vec2 offset)
{
    float scale = exp2(octave);

    coord += offset;
    coord *= scale;

    if (coord.x < 0.0 || coord.x > 1.0 || coord.y < 0.0 || coord.y > 1.0)
    {
        return vec3(0.0);
    }

    vec3 color = ColorFetch(coord);

    return color;
}

vec3 Grab4(vec2 coord, const float octave, const vec2 offset)
{
    float scale = exp2(octave);

    coord += offset;
    coord *= scale;

    if (coord.x < 0.0 || coord.x > 1.0 || coord.y < 0.0 || coord.y > 1.0)
    {
        return vec3(0.0);
    }

    vec3 color = vec3(0.0);
    float weights = 0.0;

    const int oversampling = 4;

    for (int i = 0; i < oversampling; i++)
    {
        for (int j = 0; j < oversampling; j++)
        {
            vec2 off = (vec2(i, j) / iResolution.xy + vec2(0.0) / iResolution.xy) * scale / float(oversampling);
            color += ColorFetch(coord + off);


            weights += 1.0;
        }
    }

    color /= weights;

    return color;
}

vec3 Grab8(vec2 coord, const float octave, const vec2 offset)
{
    float scale = exp2(octave);

    coord += offset;
    coord *= scale;

    if (coord.x < 0.0 || coord.x > 1.0 || coord.y < 0.0 || coord.y > 1.0)
    {
        return vec3(0.0);
    }

    vec3 color = vec3(0.0);
    float weights = 0.0;

    const int oversampling = 8;

    for (int i = 0; i < oversampling; i++)
    {
        for (int j = 0; j < oversampling; j++)
        {
            vec2 off = (vec2(i, j) / iResolution.xy + vec2(0.0) / iResolution.xy) * scale / float(oversampling);
            color += ColorFetch(coord + off);


            weights += 1.0;
        }
    }

    color /= weights;

    return color;
}

vec3 Grab16(vec2 coord, const float octave, const vec2 offset)
{
    float scale = exp2(octave);

    coord += offset;
    coord *= scale;

    if (coord.x < 0.0 || coord.x > 1.0 || coord.y < 0.0 || coord.y > 1.0)
    {
        return vec3(0.0);
    }

    vec3 color = vec3(0.0);
    float weights = 0.0;

    const int oversampling = 16;

    for (int i = 0; i < oversampling; i++)
    {
        for (int j = 0; j < oversampling; j++)
        {
            vec2 off = (vec2(i, j) / iResolution.xy + vec2(0.0) / iResolution.xy) * scale / float(oversampling);
            color += ColorFetch(coord + off);


            weights += 1.0;
        }
    }

    color /= weights;

    return color;
}

vec2 CalcOffset(float octave)
{
    vec2 offset = vec2(0.0);

    vec2 padding = vec2(10.0) / iResolution.xy;

    offset.x = -min(1.0, floor(octave / 3.0)) * (0.25 + padding.x);

    offset.y = -(1.0 - (1.0 / exp2(octave))) - padding.y * octave;

    offset.y += min(1.0, floor(octave / 3.0)) * 0.35;

    return offset;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{
    vec2 uv = fragCoord.xy / iResolution.xy;


    vec3 color = vec3(0.0);

/*
    Create a mipmap tree thingy with padding to prevent leaking bloom

	Since there's no mipmaps for the previous buffer and the reduction process has to be done in one pass,
    oversampling is required for a proper result
	*/
    color += Grab1(uv, 1.0, vec2(0.0,  0.0)   );
    color += Grab4(uv, 2.0, vec2(CalcOffset(1.0))   );
    color += Grab8(uv, 3.0, vec2(CalcOffset(2.0))   );
    color += Grab16(uv, 4.0, vec2(CalcOffset(3.0))   );
    color += Grab16(uv, 5.0, vec2(CalcOffset(4.0))   );
    color += Grab16(uv, 6.0, vec2(CalcOffset(5.0))   );
    color += Grab16(uv, 7.0, vec2(CalcOffset(6.0))   );
    color += Grab16(uv, 8.0, vec2(CalcOffset(7.0))   );


    fragColor = vec4(color, 1.0);
}

//Horizontal gaussian blur leveraging hardware filtering for fewer texture lookups.

vec3 ColorFetch(vec2 coord)
{
    return texture(iChannel0, coord).rgb;
}

float weights[5];
float offsets[5];


void mainImage( out vec4 fragColor, in vec2 fragCoord )
{

    weights[0] = 0.19638062;
    weights[1] = 0.29675293;
    weights[2] = 0.09442139;
    weights[3] = 0.01037598;
    weights[4] = 0.00025940;

    offsets[0] = 0.00000000;
    offsets[1] = 1.41176471;
    offsets[2] = 3.29411765;
    offsets[3] = 5.17647059;
    offsets[4] = 7.05882353;

    vec2 uv = fragCoord.xy / iResolution.xy;

    vec3 color = vec3(0.0);
    float weightSum = 0.0;

    if (uv.x < 0.52)
    {
        color += ColorFetch(uv) * weights[0];
        weightSum += weights[0];

        for(int i = 1; i < 5; i++)
        {
            vec2 offset = vec2(offsets[i]) / iResolution.xy;
            color += ColorFetch(uv + offset * vec2(0.5, 0.0)) * weights[i];
            color += ColorFetch(uv - offset * vec2(0.5, 0.0)) * weights[i];
            weightSum += weights[i] * 2.0;
        }

        color /= weightSum;
    }

    fragColor = vec4(color,1.0);
}

//Vertical gaussian blur leveraging hardware filtering for fewer texture lookups.

vec3 ColorFetch(vec2 coord)
{
    return texture(iChannel0, coord).rgb;
}

float weights[5];
float offsets[5];


void mainImage( out vec4 fragColor, in vec2 fragCoord )
{

    weights[0] = 0.19638062;
    weights[1] = 0.29675293;
    weights[2] = 0.09442139;
    weights[3] = 0.01037598;
    weights[4] = 0.00025940;

    offsets[0] = 0.00000000;
    offsets[1] = 1.41176471;
    offsets[2] = 3.29411765;
    offsets[3] = 5.17647059;
    offsets[4] = 7.05882353;

    vec2 uv = fragCoord.xy / iResolution.xy;

    vec3 color = vec3(0.0);
    float weightSum = 0.0;

    if (uv.x < 0.52)
    {
        color += ColorFetch(uv) * weights[0];
        weightSum += weights[0];

        for(int i = 1; i < 5; i++)
        {
            vec2 offset = vec2(offsets[i]) / iResolution.xy;
            color += ColorFetch(uv + offset * vec2(0.0, 0.5)) * weights[i];
            color += ColorFetch(uv - offset * vec2(0.0, 0.5)) * weights[i];
            weightSum += weights[i] * 2.0;
        }

        color /= weightSum;
    }

    fragColor = vec4(color,1.0);
}

vec3 saturate(vec3 x)
{
    return clamp(x, vec3(0.0), vec3(1.0));
}

vec4 cubic(float x)
{
    float x2 = x * x;
    float x3 = x2 * x;
    vec4 w;
    w.x =   -x3 + 3.0*x2 - 3.0*x + 1.0;
    w.y =  3.0*x3 - 6.0*x2       + 4.0;
    w.z = -3.0*x3 + 3.0*x2 + 3.0*x + 1.0;
    w.w =  x3;
    return w / 6.0;
}

vec4 BicubicTexture(in sampler2D tex, in vec2 coord)
{
    vec2 resolution = iResolution.xy;

    coord *= resolution;

    float fx = fract(coord.x);
    float fy = fract(coord.y);
    coord.x -= fx;
    coord.y -= fy;

    fx -= 0.5;
    fy -= 0.5;

    vec4 xcubic = cubic(fx);
    vec4 ycubic = cubic(fy);

    vec4 c = vec4(coord.x - 0.5, coord.x + 1.5, coord.y - 0.5, coord.y + 1.5);
    vec4 s = vec4(xcubic.x + xcubic.y, xcubic.z + xcubic.w, ycubic.x + ycubic.y, ycubic.z + ycubic.w);
    vec4 offset = c + vec4(xcubic.y, xcubic.w, ycubic.y, ycubic.w) / s;

    vec4 sample0 = texture(tex, vec2(offset.x, offset.z) / resolution);
    vec4 sample1 = texture(tex, vec2(offset.y, offset.z) / resolution);
    vec4 sample2 = texture(tex, vec2(offset.x, offset.w) / resolution);
    vec4 sample3 = texture(tex, vec2(offset.y, offset.w) / resolution);

    float sx = s.x / (s.x + s.y);
    float sy = s.z / (s.z + s.w);

    return mix( mix(sample3, sample2, sx), mix(sample1, sample0, sx), sy);
}

vec3 ColorFetch(vec2 coord)
{
    return texture(iChannel0, coord).rgb;
}

vec3 BloomFetch(vec2 coord)
{
    return BicubicTexture(iChannel3, coord).rgb;
}

vec3 Grab(vec2 coord, const float octave, const vec2 offset)
{
    float scale = exp2(octave);

    coord /= scale;
    coord -= offset;

    return BloomFetch(coord);
}

vec2 CalcOffset(float octave)
{
    vec2 offset = vec2(0.0);

    vec2 padding = vec2(10.0) / iResolution.xy;

    offset.x = -min(1.0, floor(octave / 3.0)) * (0.25 + padding.x);

    offset.y = -(1.0 - (1.0 / exp2(octave))) - padding.y * octave;

    offset.y += min(1.0, floor(octave / 3.0)) * 0.35;

    return offset;
}

vec3 GetBloom(vec2 coord)
{
    vec3 bloom = vec3(0.0);

    //Reconstruct bloom from multiple blurred images
    bloom += Grab(coord, 1.0, vec2(CalcOffset(0.0))) * 1.0;
    bloom += Grab(coord, 2.0, vec2(CalcOffset(1.0))) * 1.5;
    bloom += Grab(coord, 3.0, vec2(CalcOffset(2.0))) * 1.0;
    bloom += Grab(coord, 4.0, vec2(CalcOffset(3.0))) * 1.5;
    bloom += Grab(coord, 5.0, vec2(CalcOffset(4.0))) * 1.8;
    bloom += Grab(coord, 6.0, vec2(CalcOffset(5.0))) * 1.0;
    bloom += Grab(coord, 7.0, vec2(CalcOffset(6.0))) * 1.0;
    bloom += Grab(coord, 8.0, vec2(CalcOffset(7.0))) * 1.0;

    return bloom;
}

void mainImage( out vec4 fragColor, in vec2 fragCoord )
{

    vec2 uv = fragCoord.xy / iResolution.xy;

    vec3 color = ColorFetch(uv);


    color += GetBloom(uv) * 0.08;

    color *= 200.0;


    //Tonemapping and color grading
    color = pow(color, vec3(1.5));
    color = color / (1.0 + color);
    color = pow(color, vec3(1.0 / 1.5));


    color = mix(color, color * color * (3.0 - 2.0 * color), vec3(1.0));
    color = pow(color, vec3(1.3, 1.20, 1.0));

    color = saturate(color * 1.01);

    color = pow(color, vec3(0.7 / 2.2));

    fragColor = vec4(color, 1.0);

}
