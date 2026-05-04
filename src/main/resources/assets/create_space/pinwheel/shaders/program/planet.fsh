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

bool sphere(vec3 offset, vec3 dir, float radius, out float dist) {
    float A = dot(dir, dir);
    float B = dot(offset, dir);
    float C = dot(offset, offset) - radius * radius;
    float disc = B*B - A*C;
    if (disc < 0.0) return false;
    dist = (-B - sqrt(disc)) / A;
    return dist > 0.0;
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
    vec3 hitPointWorld = (uInvView * vec4(hitPointView, 1.0)).xyz;
    vec3 normal = normalize(hitPointWorld - uSpherePosWorld);
    vec3 rotatedNormal = normalize((uPlanetRotation * vec4(normal, 0.0)).xyz);

    float u = fract(atan(rotatedNormal.z, rotatedNormal.x) / (2.0 * 3.14159));
    float v = acos(clamp(rotatedNormal.y, -1.0, 1.0)) / 3.14159;

    vec3 color = texture(PlanetTexture, vec2(u, v)).rgb;

    // хмари крутяться трохи повільніше ніж планета
    float cloudU = fract(u + uTime * 0.06);
    vec4 cloud = texture(CloudTexture, vec2(cloudU, v));
    color = mix(color, cloud.rgb, cloud.a);

    vec3 sunDir = normalize(vec3(1.0, 0.5, 0.3));
    float light = max(0.15, dot(normal, sunDir));
    color *= light;

    fragColor = vec4(color, 1.0);
}