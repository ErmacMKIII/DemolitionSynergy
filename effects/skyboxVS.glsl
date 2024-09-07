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

uniform vec4 modelColor0;
uniform vec4 modelColor1;

uniform float gameTime;

struct LightSource {
	vec3 pos;
	vec3 color;
	float intensity;
};

const float AMBIENT_LIGHT = 0.15;

const vec3[6] lightDirX = vec3[](
	vec3(1.0, 0.0, 0.0), vec3(-1.0, 0.0, 0.0),
	vec3(0.0, 1.0, 0.0), vec3(0.0, -1.0, 0.0),	
	vec3(0.0, 0.0, 1.0), vec3(0.0, 0.0, -1.0)
);

uniform LightSource[256] modelLights;
uniform int modelLightNumber;

float attenuation(vec3 lightSrc, vec3 target) {
    float distance = length(lightSrc - target);
    float attenuation = 1.0 / (1.0 - 0.07 * distance + 1.8 * distance * distance);     
    attenuation = clamp(attenuation, 0.0, 1.0);
    return attenuation;
}

float diffuseLight(vec3 lightDir, vec3 normal) {    
    return max(dot(normal, lightDir), 0.0);
}

float specularLight(vec3 lightDir, vec3 normal) {    
    vec3 reflectDir = reflect(-lightDir, normal);  
    float specularLight = pow(max(dot(lightDir, reflectDir), 0.0), 32.0);
    return specularLight;
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
    varGLPos = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);
    varModelPos = (modelMatrix * vec4(pos, 1.0)).xyz;  
    gl_Position = varGLPos;        
    varNormal = normal;
    varUV = uv;    
		
	varColor = fog(cameraPos, varModelPos.xyz, modelColor0, FOG_COLOR, FOG_DENS);	
	
	float gradient = clamp(1.75 * mod(gameTime, 12.0) + varModelPos.y, 0.0, 1.0);
	varColor.rgb *= gradient;
	
	vec3 lightColor = vec3(AMBIENT_LIGHT);
	for (int i = 0; i < modelLightNumber; i++) {
		LightSource modelLight = modelLights[i];
		vec3 lightDir = normalize(modelLight.pos - varModelPos);
		float intensity = modelLight.intensity * attenuation(modelLight.pos, varModelPos);
		float diffuseSpecular = 0.0;
		for (int j = 0; j < 6; j++) {
			vec3 lightDirX = normalize(lightDir * lightDirX[j]);
			diffuseSpecular += diffuseLight(lightDirX, varNormal) + specularLight(lightDirX, varNormal);
		}
		lightColor += intensity * diffuseSpecular * modelLight.color;
	}

	varLightColor = vec4(lightColor, 1.0);    
}