#version 330 core

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;

out vec3 normalOut;
out vec2 uvOut;

uniform mat4 modelMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

uniform float waterHeight;

void main() {      
    gl_Position = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);       
    vec4 plane = vec4(0.0, 1.0, 0.0, -waterHeight);
    gl_ClipDistance[0] = dot(plane, modelMatrix * vec4(pos, 1.0));    
    normalOut = normal;
    uvOut = uv;    
}