#version 330 core

const vec4 FOG_COLOR = vec4(0.25, 0.5, 0.75, 0.05);
const float FOG_DENS = 1.22E-6;

in vec3 varNormal;
in vec2 varUV;

in vec4 varGLPos;
in vec3 varModelPos;
in vec4 varShadowGLPos;
in vec4 varColor;
in vec4 varLightColor;
in vec4 varShadowColor;

uniform vec4 modelColor0;
uniform vec4 modelColor1;

uniform vec3 cameraPos;
uniform vec3 cameraFront;

uniform sampler2D modelTexture0; // this is primary texture
uniform sampler2D modelTexture1; // this is water texture
uniform sampler2D modelTexture2; // this is shadow texture

vec2 shadowUV() {
	vec2 ndc = (varShadowGLPos.xy / varShadowGLPos.w) / 2.0 + 0.5;
    return vec2(ndc.x, -ndc.y);
}

vec4 fog(vec3 posPoint, vec3 posOrigin, vec4 color, vec4 fogColor, float fogDensity) {
	float distance = distance(posPoint, posOrigin);
	float dmf = distance * fogDensity;
	float fogFactor = 1.0 - exp(-pow(dmf, 2.0));
	fogFactor = clamp(fogFactor, 0.0, 1.0);
	vec4 resultColor = mix(color, fogColor, fogFactor);
	return resultColor;
}

void main() {	
	vec4 texColor0 = texture2D(modelTexture0, varUV);
	vec4 primColor = varColor * texColor0;
	//vec4 texColor1 = texture2D(modelTexture1, reflUV());
	//vec4 reflColor = varWaterColor * texColor1;	
	vec4 texColor2 = texture(modelTexture2, shadowUV());	
	vec4 shadowColor = varShadowColor * vec4(vec3(1.0 - texColor2.r), 1.0);
	
	gl_FragColor = vec4(varLightColor.rgb - shadowColor.rgb, 1.0) * fog(cameraPos, varModelPos, primColor, FOG_COLOR, FOG_DENS);					
}