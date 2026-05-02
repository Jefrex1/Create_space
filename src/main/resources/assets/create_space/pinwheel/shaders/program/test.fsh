#version 150

in vec2 texCoord;
in vec3 worldPos;

uniform float VeilRenderTime;

out vec4 fragColor;

void main() {
    float hue = texCoord.x + texCoord.y * 0.5 + VeilRenderTime * 0.3;
    vec3 col;
    col.r = sin(hue * 6.28) * 0.5 + 0.5;
    col.g = sin(hue * 6.28 + 2.09) * 0.5 + 0.5;
    col.b = sin(hue * 6.28 + 4.18) * 0.5 + 0.5;

    float pulse = sin(VeilRenderTime * 3.0) * 0.5 + 0.5;
    float rim = pow(1.0 - abs(dot(normalize(worldPos), vec3(0.0, 0.0, 1.0))), 2.0);

    vec3 finalColor = mix(col, vec3(1.0), rim * pulse * 0.5);
    fragColor = vec4(finalColor, 0.9);
}