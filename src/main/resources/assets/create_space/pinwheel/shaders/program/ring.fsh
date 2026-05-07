#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D RingTexture;
uniform float uInnerRadius;
uniform float uOuterRadius;
uniform float uOpacity;
uniform vec3 uSunDirView;    // Напрямок світла у View Space
uniform vec3 uSpherePos;     // Центр планети у View Space
uniform vec3 uRingNormalView;
uniform float uScale;
uniform mat4 uInvProj;

// Функція для перевірки, чи промінь світла впирається в планету
float getShadow(vec3 hitPoint, vec3 lightDir) {
    // Вектор від точки на кільці до центру планети
    vec3 L = uSpherePos - hitPoint;
    float tca = dot(L, lightDir);
    if (tca < 0.0) return 1.0; // Сонце з іншого боку, але ми рахуємо тінь тільки від сфери

    float d2 = dot(L, L) - tca * tca;
    float r2 = uScale * uScale;

    // Якщо промінь світла проходить крізь радіус сфери - це тінь
    if (d2 < r2) return 0.0;
    return 1.0;
}

void main() {
    vec4 rayClip = vec4(texCoord.x, texCoord.y, -1.0, 1.0);
    vec4 rayView = uInvProj * rayClip;
    vec3 dir = normalize(rayView.xyz);

    vec3 normal = normalize(uRingNormalView);
    float denom = dot(dir, normal);
    if (abs(denom) < 0.0001) discard;

    float t = dot(uSpherePos, normal) / denom;
    if (t < 0.0) discard;

    vec3 hitView = dir * t;
    vec3 hitRelative = hitView - uSpherePos;
    float dist = length(hitRelative);

    float innerR = uScale * uInnerRadius;
    float outerR = uScale * uOuterRadius;
    if (dist < innerR || dist > outerR) discard;

    float u = (dist - innerR) / (outerR - innerR);
    vec4 ringColor = texture(RingTexture, vec2(u, 0.5));
    if (ringColor.a < 0.01) discard;

    // --- ОСВІТЛЕННЯ ТА ТІНЬ ---
    vec3 lightDir = normalize(uSunDirView);

    // Тінь від планети
    float shadow = getShadow(hitView, lightDir);

    // Дифузне освітлення (враховуємо обидві сторони кільця)
    float diff = max(0.5, abs(dot(normal, lightDir)));

    // Фінальний колір з урахуванням тіні
    // 0.05 - це мінімальне ембієнт-освітлення, щоб кільця в тіні не були абсолютно чорними
    float lighting = max(0.1, diff * shadow);

    fragColor = vec4(ringColor.rgb * lighting, ringColor.a * uOpacity);

    mat4 proj = inverse(uInvProj);
    vec4 clipPos = proj * vec4(hitView, 1.0);
    gl_FragDepth = (clipPos.z / clipPos.w) * 0.5 + 0.5;
}