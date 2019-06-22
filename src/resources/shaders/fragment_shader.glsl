#version 330 core

uniform sampler2D text;

in vec2 texture_coord;

out vec4 frag_color;

void main() {
  vec4 color = texture(text, texture_coord);
  if (color.w < 1.0) {
    discard;
  } else {
    frag_color = color;
  }
}
