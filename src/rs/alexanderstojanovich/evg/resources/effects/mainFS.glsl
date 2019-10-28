#version 330 core

const float ambientLight = 0.25;

in vec3 normalOut;
in vec2 uvOut;

in vec4 glPosOut;
in vec3 modelPosOut;

uniform vec4 modelColor0;
uniform vec4 modelColor1;
uniform vec4 modelColor2;
uniform vec3 modelLight;
uniform sampler2D modelTexture0; // this is primary texture
uniform sampler2D modelTexture1; // this is editor overlay texture
uniform sampler2D modelTexture2; // this is reflective texture
uniform vec3 cameraPos;
uniform vec3 cameraFront;

float attenuation(vec3 lightSrc, vec3 target){
    float distance = length(lightSrc - target);
    float attenuation = 1.0 / (pow(distance, 2.0) - 4.0 * distance + 4.0);        
    attenuation = clamp(attenuation, 0.0, 1.0);
    return attenuation;
}

float diffuseLight(vec3 lightSrc, vec3 target, vec3 normal){
    vec3 lightDir = normalize(lightSrc - target); 
    return max(dot(normal, lightDir), 0.0);
}

float specularLight(vec3 lightSrc, vec3 target, vec3 normal){
    vec3 lightDir = normalize(lightSrc - target);
    vec3 reflectDir = reflect(-lightDir, normal);  
    float specularLight = pow(max(dot(lightDir, reflectDir), 0.0), 32);
    return specularLight;
}

vec2 reflUV() {
    vec2 ndc = (glPosOut.xy / glPosOut.w) / 2.0 + 0.5;
    return vec2(ndc.x, -ndc.y);
}

void main() {
    vec3 lightDir = normalize(modelLight - modelPosOut);    
    float theta = dot(lightDir, -cameraFront);
    float brightness;
    if (theta >= 0){                                              
        brightness = max((ambientLight + diffuseLight(modelLight, modelPosOut, normalOut) 
            + specularLight(modelLight, modelPosOut, normalOut)) * attenuation(modelLight, modelPosOut), ambientLight);
    } else {
        brightness = ambientLight;
    }
    
    float fresnel = dot(normalize(cameraPos), normalOut);

    vec3 finalColor = brightness * (modelColor0.rgb * texture(modelTexture0, uvOut).rgb 
                    + modelColor1.rgb * texture(modelTexture1, uvOut).rgb
                    + fresnel * modelColor2.rgb * texture(modelTexture2, reflUV()).rgb);
    float alpha = modelColor0.a * texture(modelTexture0, uvOut).a;    
    
    gl_FragColor = vec4(finalColor.rgb, alpha);    
}