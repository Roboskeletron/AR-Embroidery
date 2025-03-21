#version 300 es

precision mediump float;

in vec2 v_TexCoord;

uniform sampler2D u_Texture;

out vec4 o_FragColor;

void main() {
    o_FragColor = texture(u_Texture, v_TexCoord);
}