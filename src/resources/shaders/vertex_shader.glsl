#version 330 core

layout (location = 0) in vec3 vertex;
layout (location = 1) in vec2 uv_point;

out vec2 texture_coord;

void main() {
  gl_Position = vec4(vertex, 1.0);
  texture_coord = uv_point;
}
