#version 330 core

const vec4 FOG_COLOR = vec4(0.25, 0.5, 0.75, 0.05);
const float FOG_DENS = 3.19E-5;

in vec3 varNormal;
in vec2 varUV;

in vec4 varGLPos;
in vec3 varModelPos;
in vec4 varColor;
in vec4 varLightColor;

uniform vec4 modelColor0;
uniform vec4 modelColor1;

uniform vec3 cameraPos;
uniform vec3 cameraFront;

uniform sampler2D modelTexture0; // this is primary texture
uniform sampler2D modelTexture1; // this is water texture

uniform float gameTime;

vec4 fog(vec3 posPoint, vec3 posOrigin, vec4 color, vec4 fogColor, float fogDensity) {
	float distance = distance(posPoint, posOrigin);
	float dmf = distance * fogDensity;
	float fogFactor = 1.0 - exp(-pow(dmf, 2.0));
	fogFactor = clamp(fogFactor, 0.0, 1.0);
	vec4 resultColor = mix(color, fogColor, fogFactor);
	return resultColor;
}

void main() {    
	vec4 texColor = texture2D(modelTexture0, varUV);	
	vec4 primColor = varColor * texColor;		
	gl_FragColor = varLightColor * fog(cameraPos, varModelPos.xyz, primColor, FOG_COLOR, FOG_DENS);	
	float gradient = clamp(1.75 * mod(gameTime, 12.0) + varModelPos.y, 0.0, 1.0);
	gl_FragColor.rgb *= gradient;	
}