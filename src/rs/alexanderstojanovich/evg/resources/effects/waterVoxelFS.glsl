#version 330 core

in vec3 normalOut;
in vec2 uvOut;
in vec4 colorOut;

uniform sampler2D modelTexture0; // this is primary texture

void main() {            
	vec4 modelColor0 = colorOut;
    vec3 finalColor = modelColor0.rgb * texture(modelTexture0, uvOut).rgb;                    
    float alpha = modelColor0.a * texture(modelTexture0, uvOut).a;    
    
    gl_FragColor = vec4(finalColor, alpha);    
}