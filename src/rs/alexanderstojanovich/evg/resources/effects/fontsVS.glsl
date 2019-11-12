#version 330 core

layout (location = 0) in vec2 pos;
layout (location = 1) in vec2 uv;
layout (location = 2) in vec2 offset;
out vec2 uvOut;

uniform vec2 trans;
uniform float width;
uniform float height;
uniform float scale;

void main() {					
	vec2 constrPos = scale * vec2(width * (pos.x + offset.x), height * (pos.y - offset.y));
	constrPos += trans;
    gl_Position = vec4(constrPos, 0.0, 1.0);                        
    uvOut = uv;    
}