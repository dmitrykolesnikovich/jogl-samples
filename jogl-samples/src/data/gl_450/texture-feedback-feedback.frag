#version 440 core

#define POSITION	0
#define COLOR		3
#define FRAG_COLOR	0

precision highp float;
precision highp int;
layout(std140, column_major) uniform;
//layout(std430, column_major) buffer; AMD bug

in Block
{
    vec4 color;
} inBlock;

layout(location = FRAG_COLOR, index = 0) out vec4 color;

void main()
{
    color = inBlock.color;
}

