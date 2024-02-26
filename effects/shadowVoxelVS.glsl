#version 330 core

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;
layout (location = 3) in vec4 color;
layout (location = 4) in vec4 modelColor;
layout (location = 5) in vec4 column0;
layout (location = 6) in vec4 column1;
layout (location = 7) in vec4 column2;
layout (location = 8) in vec4 column3;

uniform mat4 lightViewMatrix;
uniform mat4 lightProjectionMatrix;

out vec4 varGLPos;

void main() {  
	mat4 modelMatrix = mat4(column0, column1, column2, column3);
    varGLPos = lightProjectionMatrix * lightViewMatrix * modelMatrix * vec4(pos, 1.0);
	gl_Position = varGLPos;
}