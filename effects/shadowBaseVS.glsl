#version 330 core

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

out vec4 varGLPos;

void main() {    
	varGLPos = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0); 
    gl_Position = varGLPos;
}