#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec3 uSpherePos;
uniform vec3 uSpherePosWorld;
uniform float uScale;
uniform mat4 uInvProj;
uniform mat4 uInvView;
uniform mat4 uPlanetRotation;
uniform sampler2D PlanetTexture;
uniform sampler2D CloudTexture;
uniform float uTime;
uniform vec3 uSunDir;
uniform float uHasClouds;
uniform float uCloudSpeed;

uniform float uAtmosphereRadius;
uniform vec3  uRayleighCoeff;
uniform float uMieCoeff;
uniform float uAtmosphereStrength;

uniform float uIsStar;
uniform vec3  uStarColor;
uniform float uPlasmaTime;

uniform float uPlasmaSpeed;
uniform float uPlasmaScale;
uniform float uCoronalIntensity;

// ===================== УТИЛІТИ =====================

bool sphereIntersect(vec3 offset, vec3 dir, float radius, out float t1, out float t2) {
    float A = dot(dir, dir);
    float B = dot(offset, dir);
    float C = dot(offset, offset) - radius * radius;
    float disc = B*B - A*C;
    if (disc < 0.0) return false;
    float sq = sqrt(disc);
    t1 = (-B - sq) / A;
    t2 = (-B + sq) / A;
    return true;
}

bool sphereHit(vec3 offset, vec3 dir, float radius, out float dist) {
    float t1, t2;
    if (!sphereIntersect(offset, dir, radius, t1, t2)) return false;
    if (t1 > 0.0) { dist = t1; return true; }
    if (t2 > 0.0) { dist = t2; return true; }
    return false;
}

// ===================== ШУМ =====================

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash(i), hash(i + vec2(1,0)), f.x),
        mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), f.x),
        f.y
    );
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.1 + vec2(1.7, 9.2);
        a *= 0.5;
    }
    return v;
}

float noise3D(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash(i.xy + i.z * 17.0);
    float n100 = hash(i.xy + vec2(1,0) + i.z * 17.0);
    float n010 = hash(i.xy + vec2(0,1) + i.z * 17.0);
    float n110 = hash(i.xy + vec2(1,1) + i.z * 17.0);
    float n001 = hash(i.xy + (i.z+1.0) * 17.0);
    float n101 = hash(i.xy + vec2(1,0) + (i.z+1.0) * 17.0);
    float n011 = hash(i.xy + vec2(0,1) + (i.z+1.0) * 17.0);
    float n111 = hash(i.xy + vec2(1,1) + (i.z+1.0) * 17.0);

    return mix(
        mix(mix(n000, n100, f.x), mix(n010, n110, f.x), f.y),
        mix(mix(n001, n101, f.x), mix(n011, n111, f.x), f.y),
        f.z
    );
}

float fbm3D(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise3D(p);
        p = p * 2.1 + vec3(1.7, 9.2, 3.5);
        a *= 0.5;
    }
    return v;
}

// ===================== GLOW =====================

vec4 starGlow(vec3 dir) {
    vec3 toSun = normalize(-uSpherePos);
    float cosA = dot(dir, toSun);
    float angle = acos(clamp(cosA, -1.0, 1.0));
    float sunAngularRadius = atan(uScale / length(uSpherePos));
    float edgeDist = max(0.0, angle - sunAngularRadius);
    float normalizedDist = edgeDist / sunAngularRadius;

    vec3 dirWorld = normalize((uInvView * vec4(dir, 0.0)).xyz);
    vec3 camPosWorld = (uInvView * vec4(0.0, 0.0, 0.0, 1.0)).xyz;

    float noiseRadius = uScale * 1.1;
    float t1, t2;
    vec3 noisePos;

    if (sphereIntersect(camPosWorld - uSpherePosWorld, dirWorld, noiseRadius, t1, t2)) {
        noisePos = (camPosWorld + dirWorld * t1) - uSpherePosWorld;
    } else {
        noisePos = dirWorld * noiseRadius;
    }

    vec3 p = noisePos * (2.5 / max(uScale, 0.001));
    vec3 movement = vec3(uPlasmaTime * uPlasmaSpeed * 0.5,
                         uPlasmaTime * uPlasmaSpeed * 0.2,
                         uPlasmaTime * uPlasmaSpeed * 0.3);

    float plasma = fbm3D(p + movement) * 0.6;
    plasma += fbm3D(p * 1.7 - movement * 0.4) * 0.3;

    float deformEffect = plasma * 0.45 * exp(-normalizedDist * 4.0);
    float deformed = normalizedDist - deformEffect;

    float glow1 = exp(-deformed * 35.0) * 0.3;
    float glow2 = exp(-deformed * 5.0) * 0.7;
    float glow3 = exp(-deformed * 1.5) * 0.25;

    float totalGlow = (glow1 + glow2 + glow3) * uCoronalIntensity;

    vec3 coreColor   = uStarColor * 2.5;
    vec3 orangeColor = uStarColor * vec3(1.0, 0.55, 0.1);
    vec3 yellowColor = uStarColor * vec3(1.0, 0.85, 0.4);

    vec3 finalGlowColor = mix(yellowColor, orangeColor, clamp(glow2, 0.0, 1.0));
    finalGlowColor = mix(finalGlowColor, coreColor, clamp(glow1 * 2.0, 0.0, 1.0));

    float prominence = exp(-deformed * 6.0) * plasma * 1.8;
    vec3 prominenceColor = vec3(1.0, 0.5, 0.1) * prominence;

    finalGlowColor += prominenceColor;

    return vec4(finalGlowColor * totalGlow, clamp(totalGlow, 0.0, 1.0));
}

// ===================== АТМОСФЕРА =====================

float getDensity(vec3 pos) {
    float dist = length(pos - uSpherePosWorld);
    float h = max(0.0, dist - uScale);
    float thickness = max(uAtmosphereRadius - uScale, 0.001);
    return exp(-(h / thickness) * 8.0);
}

float opticalDepth(vec3 rayPos, vec3 rayDir, float rayLen) {
    int steps = 8;
    float stepSize = rayLen / float(steps);
    // FIX: Використовуємо нормалізований крок для глибини
    float thickness = max(uAtmosphereRadius - uScale, 0.001);
    float normalizedStep = stepSize / thickness;

    float depth = 0.0;
    vec3 pos = rayPos + rayDir * stepSize * 0.5;

    for (int i = 0; i < steps; i++) {
        depth += getDensity(pos) * normalizedStep;
        pos += rayDir * stepSize;
    }
    return depth;
}

vec3 rayleighScatter(vec3 rayOrigin, vec3 rayDir, float rayLen, vec3 hitPoint) {
    int steps = 16;
    float stepSize = rayLen / float(steps);

    float thickness = max(uAtmosphereRadius - uScale, 0.001);
    // Нормалізований крок для розрахунків
    float normalizedStep = stepSize / thickness;

    vec3 scatteredLight = vec3(0.0);
    float viewDepth = 0.0;
    vec3 pos = rayOrigin + rayDir * stepSize * 0.5;
    vec3 sunDir = normalize(uSunDir);

    for (int i = 0; i < steps; i++) {
        float density = getDensity(pos);

        float shadowT1, shadowT2;
        bool inShadow = sphereIntersect(pos - uSpherePosWorld, sunDir, uScale, shadowT1, shadowT2) && shadowT2 > 0.0;

        if (!inShadow) {
            float sunAngle = dot(normalize(pos - uSpherePosWorld), sunDir);
            float sunVisibility = smoothstep(-0.15, 0.2, sunAngle); // Трохи м'якший термінатор

            float t1, t2;
            float sunRayLen = 0.0;
            if (sphereIntersect(pos - uSpherePosWorld, sunDir, uAtmosphereRadius, t1, t2)) {
                sunRayLen = max(0.0, t2);
            }
            float sunDepth = opticalDepth(pos, sunDir, sunRayLen);

            viewDepth += density * normalizedStep;

            // transmittance — це те, скільки світла ВТРАЧАЄТЬСЯ.
            // Щоб атмосфера була помітнішою, ми робимо поглинання менш агресивним
            vec3 transmittance = exp(-(uRayleighCoeff * (viewDepth + sunDepth) * 0.5));

            // Збільшуємо внесок кожного кроку (множимо на 2.0 для густини)
            scatteredLight += density * transmittance * uRayleighCoeff * normalizedStep * sunVisibility * 2.0;
        }
        pos += rayDir * stepSize;
    }

    float cosAngle = dot(rayDir, sunDir);
    // Фазова функція (робимо розсіювання вперед трохи сильнішим для ефекту серпанку)
    float phase = 0.75 * (1.0 + cosAngle * cosAngle);

    // Підсилюємо фінальний результат, щоб повернути яскравість до правік
    return scatteredLight * phase * uAtmosphereStrength * 1.5;
}

// ===================== MAIN =====================

void main() {
    vec4 rayClip = vec4(texCoord.x, texCoord.y, -1.0, 1.0);
    vec4 rayView  = uInvProj * rayClip;
    vec3 dir = normalize(rayView.xyz);

    float atmDist;
    bool hitsAtm = sphereHit(uSpherePos, dir, uAtmosphereRadius, atmDist);

    float planetDist;
    bool hitsPlanet = sphereHit(uSpherePos, dir, uScale, planetDist);

    // ---- Зірка ----
    if (uIsStar > 0.5) {
        if (!hitsPlanet && !hitsAtm) discard;

        if (hitsPlanet) {
            vec3 hitPointView = dir * planetDist;
            mat4 proj = inverse(uInvProj);
            vec4 clipPos = proj * vec4(hitPointView, 1.0);
            gl_FragDepth = (clipPos.z / clipPos.w) * 0.5 + 0.5;

            vec3 hitPointWorld = (uInvView * vec4(hitPointView, 1.0)).xyz;
            vec3 normal = normalize(hitPointWorld - uSpherePosWorld);
            vec3 rotatedNormal = normalize((uPlanetRotation * vec4(normal, 0.0)).xyz);
            float u = fract(atan(rotatedNormal.z, rotatedNormal.x) / (2.0 * 3.14159));
            float v = acos(clamp(rotatedNormal.y, -1.0, 1.0)) / 3.14159;
            vec3 starSurface = texture(PlanetTexture, vec2(u, v)).rgb * uStarColor * 2.0;
            vec4 glow = starGlow(dir);
            float distFromCenter = length(dir.xy - normalize(-uSpherePos).xy);
            float centerBrightness = 1.0 - exp(-distFromCenter * 3.0) * 0.7;

            fragColor = vec4(starSurface * centerBrightness + glow.rgb * 0.01, 1.0);
        } else {
            gl_FragDepth = 1.0;
            vec4 glow = starGlow(dir);
            if (glow.a < 0.001) discard;
            fragColor = glow;
        }
        return;
    }

    if (!hitsAtm && !hitsPlanet) discard;

    // ---- Планета ----
    vec3 planetColor = vec3(0.0);
    if (hitsPlanet) {
        vec3 hitPointView  = dir * planetDist;

        mat4 proj = inverse(uInvProj);
        vec4 clipPos = proj * vec4(hitPointView, 1.0);
        gl_FragDepth = (clipPos.z / clipPos.w) * 0.5 + 0.5;

        vec3 hitPointWorld = (uInvView * vec4(hitPointView, 1.0)).xyz;
        vec3 normal = normalize(hitPointWorld - uSpherePosWorld);
        vec3 rotatedNormal = normalize((uPlanetRotation * vec4(normal, 0.0)).xyz);

        float u = fract(atan(rotatedNormal.z, rotatedNormal.x) / (2.0 * 3.14159));
        float v = acos(clamp(rotatedNormal.y, -1.0, 1.0)) / 3.14159;

        planetColor = texture(PlanetTexture, vec2(u, v)).rgb;

        if (uHasClouds > 0.5) {
            float cloudU = fract(u - uTime * uCloudSpeed);
            vec4 cloud = texture(CloudTexture, vec2(cloudU, v));
            planetColor = mix(planetColor, cloud.rgb, cloud.a);
        }

        float light = max(0.05, dot(normal, normalize(uSunDir)));
        planetColor *= light;
    } else {
        gl_FragDepth = 1.0;
    }

    // ---- Атмосфера ----
    vec3 atmColor = vec3(0.0);
    if (hitsAtm) {
        float t1, t2;
        sphereIntersect(uSpherePos, dir, uAtmosphereRadius, t1, t2);

        float atmStart = max(0.0, t1);
        float atmEnd   = hitsPlanet ? planetDist : max(0.0, t2);
        float atmLen   = max(0.0, atmEnd - atmStart);

        if (atmLen > 0.0) {
            vec3 startWorld = (uInvView * vec4(dir * atmStart, 1.0)).xyz;
            vec3 dirWorld   = normalize((uInvView * vec4(dir, 0.0)).xyz);
            atmColor = rayleighScatter(startWorld, dirWorld, atmLen, startWorld);
        }
    }

    float atmAlpha = clamp(length(atmColor), 0.0, 1.0);
    vec3 finalColor = planetColor + atmColor;

    if (!hitsPlanet && hitsAtm) {
        fragColor = vec4(atmColor, atmAlpha);
    } else {
        fragColor = vec4(finalColor, 1.0);
    }
}
