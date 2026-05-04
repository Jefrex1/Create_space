#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SkyboxTexture;
uniform mat4 uInvProj;
uniform mat4 uViewMatrix; // Це наша матриця обертання
uniform float uRotation;

void main() {
    // 1. Створюємо промінь у просторі екрана
    vec4 rayClip = vec4(texCoord.x, texCoord.y, -1.0, 1.0);

    // 2. Переводимо в простір виду
    vec4 rayView = uInvProj * rayClip;

    // 3. Множимо на матрицю обертання камери (uViewMatrix)
    // Важливо: множимо як vec4 з w=0.0, щоб ігнорувати позицію
    vec3 dir = normalize((uViewMatrix * vec4(rayView.xyz, 0.0)).xyz);

    // 4. Повільне обертання самого космосу (опціонально)
    //float s = sin(uRotation);
    //float c = cos(uRotation);
    //dir.xz *= mat2(c, -s, s, c);

    // 5. Проекція на сферу
    float u = fract(atan(dir.z, dir.x) / (2.0 * 3.14159));
    float v = acos(clamp(dir.y, -1.0, 1.0)) / 3.14159;

    vec3 color = texture(SkyboxTexture, vec2(u, v)).rgb;
    fragColor = vec4(color, 1.0);
}