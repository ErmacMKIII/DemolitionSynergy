#version 330 core

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;
layout (location = 3) in vec4 color;
layout (location = 4) in vec4 column0;
layout (location = 5) in vec4 column1;
layout (location = 6) in vec4 column2;
layout (location = 7) in vec4 column3;

out vec3 normalOut;
out vec2 uvOut;

out vec4 glPosOut;
out vec3 modelPosOut;
out vec4 colorOut;

flat out int instanceIdOut;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

void main() {      
	mat4 modelMatrix = mat4(column0, column1, column2, column3);
    glPosOut = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);
    modelPosOut = (modelMatrix * vec4(pos, 1.0)).xyz;  
    gl_Position = glPosOut;        
    normalOut = normal;
    uvOut = uv;  		
	colorOut = color;
	instanceIdOut = gl_InstanceID;
}