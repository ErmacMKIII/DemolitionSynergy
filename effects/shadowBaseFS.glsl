#version 330 core

in vec3 varNormal;
in vec2 varUV;

uniform vec4 modelColor0;
uniform sampler2D modelTexture0; // this is primary texture

void main() {            
    gl_FragColor = modelColor0 * texture(modelTexture0, varUV);                       
}