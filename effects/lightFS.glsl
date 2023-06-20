#version 330 core

in vec2 varUV;

in vec4 varGLPos;
in vec3 varModelPos;
in vec4 varLightColor;

uniform sampler2D ifcTexture;

void main() {	
	gl_FragColor = varLightColor;	 
}