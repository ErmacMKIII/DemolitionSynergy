#version 330 core

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;
layout (location = 3) in vec4 color;

uniform mat4 lightViewMatrix;
uniform mat4 lightProjectionMatrix;
uniform mat4 modelMatrix;

out vec4 varGLPos;

void main() {    
	varGLPos = lightProjectionMatrix * lightViewMatrix * modelMatrix * vec4(pos, 1.0); 
    gl_Position = varGLPos;
}