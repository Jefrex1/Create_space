#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SkyboxTexture;
uniform mat4 uInvProj;
uniform mat4 uInvView;
uniform float uRotation;

void main() {
    vec4 rayClip = vec4(texCoord.x, texCoord.y, -1.0, 1.0);
    vec4 rayView = uInvProj * rayClip;
    vec3 dir = normalize((uInvView * vec4(rayView.xyz, 0.0)).xyz);

    float s = sin(uRotation);
    float c = cos(uRotation);
    dir.xz *= mat2(c, -s, s, c);

    float u = fract(atan(dir.z, dir.x) / (2.0 * 3.14159));
    float v = acos(clamp(dir.y, -1.0, 1.0)) / 3.14159;

    vec3 color = texture(SkyboxTexture, vec2(u, v)).rgb;
    fragColor = vec4(color, 1.0);
}