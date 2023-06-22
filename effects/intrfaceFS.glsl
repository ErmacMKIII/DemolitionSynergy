#version 330 core

in vec2 varUV;

uniform vec4 color;
uniform sampler2D ifcTexture;

void main() {    
	vec4 texColor = texture2D(ifcTexture, varUV);
    gl_FragColor = color * texColor;  
}