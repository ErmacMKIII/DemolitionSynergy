#version 330 core

const vec4 FOG_COLOR = vec4(0.25, 0.5, 0.75, 0.05);
const float FOG_DENS = 1.22E-2;

layout (location = 0) in vec3 pos;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec2 uv;
layout (location = 3) in vec3 color;
layout (location = 4) in vec4 column0;
layout (location = 5) in vec4 column1;
layout (location = 6) in vec4 column2;
layout (location = 7) in vec4 column3;

out vec3 varNormal;
out vec2 varUV;

out vec4 varGLPos;
out vec3 varModelPos;
out vec4 varColor;
out vec4 varWaterColor; // water reflect color

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;

uniform vec3 cameraPos;
uniform vec3 cameraFront;

struct LightSource {
	vec3 pos;
	vec3 color;
	float intensity;
};

const float AMBIENT_LIGHT = 0.05;

const vec3[6] lightDirC = vec3[](
	vec3(1.0, 0.0, 0.0), vec3(-1.0, 0.0, 0.0),
	vec3(0.0, 1.0, 0.0), vec3(0.0, -1.0, 0.0),	
	vec3(0.0, 0.0, 1.0), vec3(0.0, 0.0, -1.0)
);

uniform LightSource[256] modelLights;
uniform int modelLightNumber;
uniform float modelAlpha;
uniform vec3 modelColor1;

float attenuation(vec3 lightSrc, vec3 target) {
    float distance = length(lightSrc - target);
    float attenuation = 1.0 / (1.0 - 0.07 * distance + 1.8 * distance * distance);     
    attenuation = clamp(attenuation, 0.0, 1.0);
    return attenuation;
}

float diffuseLight(vec3 lightSrc, vec3 lightDir, vec3 normal){    
    return max(dot(normal, lightDir), 0.0);
}

float specularLight(vec3 lightSrc, vec3 lightDir, vec3 normal){    
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

vec2 reflUV() {
    vec2 ndc = (varGLPos.xy / varGLPos.w) / 2.0 + 0.5;
    return vec2(ndc.x, -ndc.y);
}

void main() {      
	mat4 modelMatrix = mat4(column0, column1, column2, column3);
	vec3 modelColor0 = color;
	varGLPos = projectionMatrix * viewMatrix * modelMatrix * vec4(pos, 1.0);
    varModelPos = (modelMatrix * vec4(pos, 1.0)).xyz;  
    gl_Position = varGLPos;        
    varNormal = normal;
    varUV = uv;  			
		
	float fresnel = dot(normalize(cameraPos), varNormal);
	varWaterColor = fresnel * vec4(modelColor1, modelAlpha);
	varColor = fog(cameraPos, varModelPos.xyz, vec4(modelColor0, modelAlpha), FOG_COLOR, FOG_DENS);
	vec3 normalX = normalize(modelMatrix * (vec4(varNormal - varModelPos, 1.0))).xyz;
	
	vec3 lightColor = vec3(0.0);	
	int i, j;	
	for (i = 0; i < modelLightNumber; i++) {
		LightSource modelLight = modelLights[i]; 	
		vec3 lightDirK = normalize(modelLight.pos - varModelPos.xyz);	
		float light = 0.0;
	    for (j = 0; j < 6; j++) {
			vec3 lightDir = normalize(lightDirC[j] * lightDirK); 
			light += diffuseLight(modelLight.pos, lightDir, normalX) 			
				+ specularLight(modelLight.pos, lightDir, normalX) + AMBIENT_LIGHT;												
		}
		light *= modelLight.intensity;
		light *= attenuation(modelLight.pos, varModelPos);
		lightColor += light * modelLight.color;
    }
	
	varColor.rgb *= lightColor;
}