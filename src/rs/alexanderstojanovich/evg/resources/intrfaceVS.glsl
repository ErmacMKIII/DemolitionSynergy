#version 330 core

layout (location = 0) in vec2 pos;
layout (location = 1) in vec2 uv;

out vec2 uvOut;

// used for crosshair and fonts
uniform vec2 trans;
uniform float width;
uniform float height;
uniform float scale;

// used for fonts
uniform float xinc;
uniform float ydec;

void main() {					
	vec2 constrPos = scale * vec2(width * (pos.x + xinc), height * (pos.y - ydec));
	constrPos += trans;
    gl_Position = vec4(constrPos, 0.0, 1.0);                        
    uvOut = uv;    
}