#version 330 core

const vec4 FOG_COLOR = vec4(0.25, 0.5, 0.75, 0.05);
const float FOG_DENS = 1.22E-6;

in vec3 varNormal;
in vec2 varUV;

in vec4 varGLPos;
in vec3 varModelPos;
in vec4 varColor;
in vec4 varLightColor;

uniform vec3 modelColor0;
uniform vec3 modelColor1;

uniform vec3 cameraPos;
uniform vec3 cameraFront;

uniform sampler2D modelTexture0; // this is primary texture
uniform sampler2D modelTexture1; // this is water texture

uniform float modelAlpha;
uniform float gameTime;
uniform float unit;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float cvet(vec2 texCoords) {	
	float v = (texCoords.x - unit) / (1.0 - unit);
	v += 1.0 - 0.5 * mod(1.75 * gameTime, 2.0);
	if(v > 1.0) {
		v = 2.0 - v;
	} else if(v < 0.0) {
		v = -v;
	}	

	v = v * 0.60 - 0.30;
	
	return v;
}

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
}

void main() {    
	vec4 texColor = texture2D(modelTexture0, varUV);
	vec4 primColor = varColor * texColor;	
	float check = check(varUV, unit);	
	
	float cvet = cvet(varUV);
	
	if (texColor.a == 1.0) {
		gl_FragColor.rgb = 4.0 * varLightColor.rgb * primColor.rgb * cvet;
		gl_FragColor.a = 1.0;
	} else if (texColor.a < 1.0 && check > 0.0) {
		gl_FragColor.rgb = 4.0 * varLightColor.rgb * varColor.rgb * cvet;
		gl_FragColor.a = 1.0;
	} else {
		gl_FragColor = vec4(0.0);	
	}
}