#version 330 core

in vec3 varNormal;
in vec2 varUV;
in vec3 varColor;

uniform vec3 modelColor0;
uniform float modelAlpha;
uniform sampler2D modelTexture0; // this is primary texture

void main() {            
	vec3 modelColor0 = varColor;
    vec3 finalColor = modelColor0 * texture(modelTexture0, varUV).rgb;                    
    float alpha = modelAlpha * texture(modelTexture0, varUV).a;    
    
    gl_FragColor = vec4(finalColor, alpha);    
}