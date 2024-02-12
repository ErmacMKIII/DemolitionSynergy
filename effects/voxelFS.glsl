#version 330 core

const vec4 FOG_COLOR = vec4(0.25, 0.5, 0.75, 0.05);
const float FOG_DENS = 1.22E-2;

in vec3 varNormal;
in vec2 varUV;

in vec4 varGLPos;
in vec3 varModelPos;
in vec4 varColor;
in vec4 varWaterColor;
in vec4 varShadowColor;
in vec4 varLightColor;
in vec2 varShadowUV;

uniform sampler2D modelTexture0; // this is primary texture
uniform sampler2D modelTexture1; // this is reflective texture
uniform sampler2D modelTexture2; // this is shadow texture

uniform vec3 cameraPos;
uniform vec3 cameraFront;

vec2 reflUV() {
    vec2 ndc = (varGLPos.xy / varGLPos.w) / 2.0 + 0.5;
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
	vec4 texColor1 = texture2D(modelTexture1, reflUV());
	vec4 reflColor = varWaterColor * texColor1;
	vec4 shadowColor = varShadowColor * texture(modelTexture2, varShadowUV);
	
	gl_FragColor = (varLightColor - shadowColor) * fog(cameraPos, varModelPos, primColor + (1.0 - primColor.a) * reflColor, FOG_COLOR, FOG_DENS);		
}