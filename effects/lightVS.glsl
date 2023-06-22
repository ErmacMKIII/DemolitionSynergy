#version 330 core

layout (location = 0) in vec2 pos;
layout (location = 1) in vec2 uv;

out vec2 varUV;

out vec4 varGLPos;
out vec3 varModelPos;
out vec4 varLightColor;

uniform vec3 cameraPos;
uniform vec3 cameraFront;
uniform mat4 modelMatrix;

uniform vec4 color;

struct LightSource {
	vec3 pos;
	vec3 color;
	float intensity;
};

const float AMBIENT_LIGHT = 0.15;

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

float specularLight(vec3 lightSrc, vec3 lightDir, vec3 normal) {    
    vec3 reflectDir = reflect(-lightDir, normal);  
    float specularLight = pow(max(dot(lightDir, reflectDir), 0.0), 32.0);
    return specularLight;
}

void main() {      
    varGLPos = modelMatrix * vec4(pos, 0.0, 1.0); 
	varModelPos = (modelMatrix * vec4(pos, 0.0, 1.0)).xyz; 	
	gl_Position = varGLPos;          
    varUV = uv;    		
	vec3 lightColor = vec3(AMBIENT_LIGHT);	

	float light;	
	int i;

	// starting from 1; ignore player light - causing problems
	for (i = 1; i < modelLightNumber; i++) {
		LightSource modelLight = modelLights[i];		
		vec3 lightDir = normalize(modelLight.pos - cameraPos);		
		light = diffuseLight(lightDir, cameraFront) 
			+ specularLight(modelLight.pos, lightDir, cameraFront);													
		light *= modelLight.intensity;
		light *= 1.0 - exp(-attenuation(modelLight.pos, cameraPos)); // slightly with exp
		lightColor += light * modelLight.color;							
    }
	
	varLightColor = vec4(lightColor, 0.3);
}