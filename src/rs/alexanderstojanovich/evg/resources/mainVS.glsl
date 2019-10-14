#version 330 core

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;

out vec3 normalOut;
out vec2 uvOut;

out vec4 glPosOut;
out vec3 modelPosOut;

uniform mat4 modelMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

uniform float waterHeight;

void main(){      
    glPosOut = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);
    modelPosOut = (modelMatrix * vec4(pos, 1.0)).xyz;  
    gl_Position = glPosOut;        
    normalOut = normal;
    uvOut = uv;    
}

