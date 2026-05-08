#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform vec3  uSpherePos;     // позиція центру у view space
uniform float uScale;         // радіус сфери
uniform mat4  uInvProj;       // інверсна проекція
uniform mat4  uPlanetRotation;
uniform sampler2D PlanetTexture;
uniform sampler2D CloudTexture;
uniform float uTime;
uniform float uHasClouds;
uniform float uCloudSpeed;
uniform vec3  uSunDir;
uniform float uAlpha;

// атмосфера
uniform vec3  uAtmColor;      // колір атмосфери RGB
uniform float uAtmSize;       // множник радіусу атмосфери (1.0 = немає, 1.1 = тонка)
uniform float uAtmStrength;   // сила атмосфери

bool sphereHit(vec3 offset, vec3 dir, float radius, out float dist) {
    float B = dot(offset, dir);
    float C = dot(offset, offset) - radius * radius;
    float disc = B*B - C;
    if (disc < 0.0) return false;
    float sq = sqrt(disc);
    float t1 = -B - sq;
    float t2 = -B + sq;
    if (t1 > 0.0) { dist = t1; return true; }
    if (t2 > 0.0) { dist = t2; return true; }
    return false;
}

void main() {
    // Ray у view space
    vec4 rayClip = vec4(texCoord.x, texCoord.y, -1.0, 1.0);
    vec4 rayView = uInvProj * rayClip;
    vec3 dir = normalize(rayView.xyz);

    float atmRadius = uScale * uAtmSize;

    // Перевіряємо чи ray б'є в атмосферу
    float atmDist;
    bool hitsAtm = sphereHit(uSpherePos, dir, atmRadius, atmDist);

    float planetDist;
    bool hitsPlanet = sphereHit(uSpherePos, dir, uScale, planetDist);

    if (!hitsAtm && !hitsPlanet) discard;

    vec3 finalColor = vec3(0.0);
    float finalAlpha = 0.0;

    // ---- Планета ----
    if (hitsPlanet) {
        vec3 hitPoint = dir * planetDist;
        vec3 normal = normalize(hitPoint - uSpherePos);
        vec3 rotatedNormal = normalize((uPlanetRotation * vec4(normal, 0.0)).xyz);

        float u = fract(atan(rotatedNormal.z, rotatedNormal.x) / (2.0 * 3.14159));
        float v = acos(clamp(rotatedNormal.y, -1.0, 1.0)) / 3.14159;

        vec3 color = texture(PlanetTexture, vec2(u, v)).rgb;

        if (uHasClouds > 0.5) {
            float cloudU = fract(u - uTime * uCloudSpeed);
            vec4 cloud = texture(CloudTexture, vec2(cloudU, v));
            color = mix(color, cloud.rgb, cloud.a);
        }

        float light = max(0.05, dot(normal, normalize(uSunDir)));
        finalColor = color * light;
        finalAlpha = 1.0;
    }

    // ---- Атмосфера ----
    if (hitsAtm && uAtmSize > 1.001 && uAtmStrength > 0.0) {
        vec3 atmHit = dir * atmDist;
        vec3 atmNormal = normalize(atmHit - uSpherePos);

        float edgeFactor = 1.0 - abs(dot(dir, atmNormal));
        edgeFactor = pow(edgeFactor, 2.0);

        float sunFactor = max(0.0, dot(atmNormal, normalize(uSunDir)));
        float atmAlpha = edgeFactor * uAtmStrength * sunFactor * uAlpha;
        atmAlpha = clamp(atmAlpha, 0.0, 1.0);

        if (!hitsPlanet) {
            finalColor = uAtmColor * sunFactor;
            finalAlpha = atmAlpha;
        } else {
            finalColor = mix(finalColor, uAtmColor, edgeFactor * uAtmStrength * 0.3);
        }
    }

    if (finalAlpha < 0.001) discard;

    fragColor = vec4(finalColor, finalAlpha * uAlpha);
}