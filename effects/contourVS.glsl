#version 330 core

const vec4 FOG_COLOR = vec4(0.25, 0.5, 0.75, 0.05);
const float FOG_DENS = 1.22E-6;

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;
layout (location = 3) in vec4 color;

out vec3 varNormal;
out vec2 varUV;

out vec4 varGLPos;
out vec3 varModelPos;
out vec4 varColor;
out vec4 varLightColor;
out vec4 varShadowColor;
out vec4 varShadowGLPos;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

uniform mat4 lightViewMatrix;
uniform mat4 lightProjectionMatrix;

uniform mat4 lightSpaceMatrix;

uniform vec3 cameraPos;
uniform vec3 cameraFront;

uniform vec4 lightColor;
uniform vec4 modelColor0;
uniform vec4 modelColor1;

uniform sampler2D modelTexture2; // this is shadow texture

const float SHADOW_FACTOR = 0.15;

vec4 fog(vec3 posPoint, vec3 posOrigin, vec4 color, vec4 fogColor, float fogDensity) {
	float distance = distance(posPoint, posOrigin);
	float dmf = distance * fogDensity;
	float fogFactor = 1.0 - exp(-pow(dmf, 2.0));
	fogFactor = clamp(fogFactor, 0.0, 1.0);
	vec4 resultColor = mix(color, fogColor, fogFactor);
	return resultColor;
}

vec2 shadowUV() {
	vec2 ndc = (varShadowGLPos.xy / varShadowGLPos.w) / 2.0 + 0.5;
    return vec2(ndc.x, -ndc.y);
}

void main() {      
    varGLPos = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);
    varModelPos = (modelMatrix * vec4(pos, 1.0)).xyz;  
	varShadowGLPos = lightProjectionMatrix * lightViewMatrix * modelMatrix * vec4(pos, 1.0);
    gl_Position = varGLPos;        
    varNormal = normal;
    varUV = uv;    
	
	varColor = fog(cameraPos, varModelPos.xyz, modelColor0, FOG_COLOR, FOG_DENS);	
	
	float testZ = texture2D(modelTexture2, shadowUV()).r;
	
	// darken pixel which is in shadow (fail depth-test)
	/*
	if (varGLPos.z <= testZ) {	
		varShadowColor.rgb *= SHADOW_FACTOR;
		varColor.rgb *= SHADOW_FACTOR;
	} 
	*/
	
	varLightColor = color;
}