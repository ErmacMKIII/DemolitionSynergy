#version 330 core

in vec3 varNormal;
in vec2 varUV;
in vec4 varColor;

uniform vec4 modelColor0;
uniform sampler2D modelTexture0; // this is primary texture

void main() {            
	vec4 modelColor0 = varColor;
    vec4 texColor = modelColor0 * texture(modelTexture0, varUV);   
    vec4 finalColor = modelColor0 * texColor;
    gl_FragColor = finalColor;    
}