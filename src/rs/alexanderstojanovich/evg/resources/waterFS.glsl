#version 330 core

in vec3 normalOut;
in vec2 uvOut;

in vec4 glPosOut;
in vec3 modelPosOut;

uniform vec4 modelColor0;
uniform vec3 modelLight;
uniform sampler2D modelTexture0; // this is primary texture
uniform vec3 cameraPos;
uniform vec3 cameraFront;

vec2 reflUV(){
    vec2 ndc = (glPosOut.xy/glPosOut.w) /2.0 + 0.5;
    return vec2(ndc.x, -ndc.y);
}

void main(){            
    vec3 finalColor = modelColor0.rgb * texture(modelTexture0, uvOut).rgb;                    
    float alpha = modelColor0.a * texture(modelTexture0, uvOut).a;    
    
    gl_FragColor = vec4(finalColor, alpha);    
}