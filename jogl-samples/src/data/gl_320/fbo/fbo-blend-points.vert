#version 150 core

precision highp float;
precision highp int;
layout(std140, column_major) uniform;

uniform Transform
{
    mat4 mvp;
} transform;

in vec4 position;
in vec4 color;

out Block
{
    vec4 color;
} outBlock;

void main()
{
    outBlock.color = color;
    gl_PointSize = 256.0;
    gl_Position = transform.mvp * position;
}
