#version 420 core

#define FRAG_COLOR		0

precision highp float;
precision highp int;
layout(std140, column_major) uniform;

struct Vertex
{
    vec4 color;
};

layout(location = 0) in Vertex stIn;

in Block
{
    vec4 color;
} blIn; 

layout(location = FRAG_COLOR, index = 0) out vec4 color;

void main()
{
    color = stIn.color + blIn.color;
}
