#version 330 core

const vec4 FOG_COLOR = vec4(0.25, 0.5, 0.75, 0.05);
const float FOG_DENS = 3.19E-5;

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;

out vec3 varNormal;
out vec2 varUV;

out vec4 varGLPos;
out vec3 varModelPos;
out vec4 varColor;
out vec4 varLightColor;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

uniform vec3 cameraPos;
uniform vec3 cameraFront;

uniform vec4 lightColor;
uniform vec4 modelColor0;
uniform vec4 modelColor1;

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
    varGLPos = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);
    varModelPos = (modelMatrix * vec4(pos, 1.0)).xyz;  
    gl_Position = varGLPos;        
    varNormal = normal;
    varUV = uv;    
		
	varColor = fog(cameraPos, varModelPos.xyz, modelColor0, FOG_COLOR, FOG_DENS);	
	
	float gradient = clamp(1.75 * mod(gameTime, 12.0) + varModelPos.y, 0.0, 1.0);
	varColor.rgb *= gradient;	

	varLightColor = lightColor;    
}