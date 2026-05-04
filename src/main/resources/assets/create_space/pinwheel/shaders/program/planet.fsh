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

bool sphere(vec3 offset, vec3 dir, float radius, out float dist) {
    float A = dot(dir, dir);
    float B = dot(offset, dir);
    float C = dot(offset, offset) - radius * radius;
    float disc = B*B - A*C;

    if (disc < 0.0) return false;

    float sqrtDisc = sqrt(disc);
    float t1 = (-B - sqrtDisc) / A;
    float t2 = (-B + sqrtDisc) / A;

    if (t1 > 0.0) {
        dist = t1;
        return true;
    } else if (t2 > 0.0) {
        dist = t2;
        return true;
    }
    return false;
}

void main() {
    vec4 rayClip = vec4(texCoord.x, texCoord.y, -1.0, 1.0);
    vec4 rayView = uInvProj * rayClip;
    vec3 dir = normalize(rayView.xyz);

    float dist;
    if (!sphere(uSpherePos, dir, uScale, dist)) {
        discard;
    }

    vec3 hitPointView = dir * dist;

    mat4 proj = inverse(uInvProj);
    vec4 clipPos = proj * vec4(hitPointView, 1.0);
    gl_FragDepth = (clipPos.z / clipPos.w) * 0.5 + 0.5;

    vec3 hitPointWorld = (uInvView * vec4(hitPointView, 1.0)).xyz;
    vec3 normal = normalize(hitPointWorld - uSpherePosWorld);
    vec3 rotatedNormal = normalize((uPlanetRotation * vec4(normal, 0.0)).xyz);

    float u = fract(atan(rotatedNormal.z, rotatedNormal.x) / (2.0 * 3.14159));
    float v = acos(clamp(rotatedNormal.y, -1.0, 1.0)) / 3.14159;

    vec3 color = texture(PlanetTexture, vec2(u, v)).rgb;

    if (uHasClouds > 0.5) {
        float cloudU = fract(u + uTime * uCloudSpeed);
        vec4 cloud = texture(CloudTexture, vec2(cloudU, v));
        color = mix(color, cloud.rgb, cloud.a);
    }

    float light = max(0.15, dot(normal, normalize(uSunDir)));
    color *= light;

    fragColor = vec4(color, 1.0);
}