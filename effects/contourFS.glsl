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

uniform vec3 modelColor0;
uniform vec3 modelColor1;

uniform vec3 cameraPos;
uniform vec3 cameraFront;

uniform sampler2D modelTexture0; // this is primary texture
uniform sampler2D modelTexture1; // this is water texture
uniform sampler2D modelTexture2; // this is water texture

uniform float modelAlpha;
uniform float gameTime;
uniform float unit;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float cvet(vec2 texCoords) {	
	float v = (texCoords.x - unit) / (1.0 - unit);
	v += 1.0 - 0.5 * mod(1.75 * gameTime, 24.0);
	if(v > 1.0) {
		v = 2.0 - v;
	} else if(v < 0.0) {
		v = -v;
	}	

	v = v * 0.60 - 0.30;
	
	v = clamp(v, 0.0, 1.0);
	
	return v;
}
/*
float check(vec2 texCoords, float offset) {
	float chk = (
		texture2D(modelTexture0, vec2(texCoords.x + offset, texCoords.y)).a +
		texture2D(modelTexture0, vec2(texCoords.x, texCoords.y - offset)).a +
		texture2D(modelTexture0, vec2(texCoords.x - offset, texCoords.y)).a +
		texture2D(modelTexture0, vec2(texCoords.x, texCoords.y + offset)).a + 
		
		texture2D(modelTexture0, vec2(texCoords.x + offset, texCoords.y + offset)).a +
		texture2D(modelTexture0, vec2(texCoords.x + offset, texCoords.y - offset)).a +
		texture2D(modelTexture0, vec2(texCoords.x - offset, texCoords.y + offset)).a +
		texture2D(modelTexture0, vec2(texCoords.x - offset, texCoords.y - offset)).a
	) / 8.0;
	
	return chk;
}*/

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
	vec4 texColor = texture2D(modelTexture0, varUV);
	vec4 primColor = varColor * texColor;			
	
	float cvet = 2.0 * cvet(varUV);		
	
	vec4 texColor2 = texture(modelTexture2, shadowUV());	
	vec4 shadowColor = varShadowColor * texColor2;
		
	gl_FragColor =  vec4(varLightColor.rgb - shadowColor.rgb, 1.0) * fog(cameraPos, varModelPos, primColor, FOG_COLOR, FOG_DENS);	
}