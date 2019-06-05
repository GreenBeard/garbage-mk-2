#version 330 core

uniform sampler2D text;

in vec2 texture_coord;

out vec4 frag_color;

void main() {
  frag_color = texture(text, texture_coord);
}
