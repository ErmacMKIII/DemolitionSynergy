#version 330 core

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;
layout (location = 4) in vec4 column0;
layout (location = 5) in vec4 column1;
layout (location = 6) in vec4 column2;
layout (location = 7) in vec4 column3;

out vec3 normalOut;
out vec2 uvOut;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

uniform float waterHeight;

void main() {  
	mat4 modelMatrix = mat4(column0, column1, column2, column3);
    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);       
    vec4 plane = vec4(0.0, 1.0, 0.0, -waterHeight);
    gl_ClipDistance[0] = dot(plane, modelMatrix * vec4(pos, 1.0));    
    normalOut = normal;
    uvOut = uv;    	
}