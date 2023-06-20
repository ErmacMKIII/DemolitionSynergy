#version 330 core

in vec2 uvOut;

uniform vec3 color;
uniform sampler2D ifcTexture;

void main() {    
	vec4 texColor = texture2D(ifcTexture, uvOut);
    gl_FragColor.rgb = color * texColor.rgb;  
	gl_FragColor.a = texColor.a;
}