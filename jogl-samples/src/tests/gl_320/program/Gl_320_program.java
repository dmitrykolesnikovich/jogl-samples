/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_320.program;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_320_program extends Test {

    public static void main(String[] args) {
        Gl_320_program gl_320_program = new Gl_320_program();
    }

    public Gl_320_program() {
        super("gl-320-program", Profile.CORE, 3, 2);
    }

    private final String SHADERS_SOURCE = "program";
    private final String SHADERS_ROOT = "src/data/gl_320/program";

    private int vertexCount = 4;
    private int positionSize = vertexCount * 2 * Float.BYTES;
    private float[] positionData = {
        -1.0f, -1.0f,
        +1.0f, -1.0f,
        +1.0f, +1.0f,
        -1.0f, +1.0f};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0};

    private enum Buffer {
        VERTEX,
        ELEMENT,
        TRANSFORM,
        MATERIAL,
        MAX
    }

    private enum Program {
        USED,
        MAX
    }

    private int[] programName = new int[Program.MAX.ordinal()], bufferName = new int[Buffer.MAX.ordinal()],
            vertexArrayName = new int[1];
    private int uniformTransform, uniformMaterial;
    private float[] projection = new float[16], model = new float[16], mvp = new float[16];

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl3);
        }
        if (validated) {
            validated = initBuffer(gl3);
        }
        if (validated) {
            validated = initVertexArray(gl3);
        }

        return validated && checkError(gl3, "begin");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        if (validated) {

            ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.init(gl3);

            programName[Program.USED.ordinal()] = shaderProgram.program();

            gl3.glBindAttribLocation(programName[Program.USED.ordinal()], Semantic.Attr.POSITION, "position");
            gl3.glBindFragDataLocation(programName[Program.USED.ordinal()], Semantic.Frag.COLOR, "color");

            shaderProgram.link(gl3, System.out);
        }
        if (validated) {

            uniformMaterial = gl3.glGetUniformBlockIndex(programName[Program.USED.ordinal()], "Material");
            uniformTransform = gl3.glGetUniformBlockIndex(programName[Program.USED.ordinal()], "Transform");
        }

        int[] activeUniformBlocks = {0};
        gl3.glGetProgramiv(programName[Program.USED.ordinal()], GL_ACTIVE_UNIFORM_BLOCKS, activeUniformBlocks, 0);

        for (int i = 0; i < activeUniformBlocks[0]; ++i) {
            int[] length = {0};
            byte[] name = new byte[128];

            gl3.glGetActiveUniformBlockName(programName[Program.USED.ordinal()], i, name.length, length, 0, name, 0);

            String stringName = new String(name);
            //remove the empty padding spaces at the end
            stringName = stringName.trim();

            validated = validated && (stringName.equals("Material") || stringName.equals("Transform"));
        }

        int[] activeUniform = {0};
        gl3.glGetProgramiv(programName[Program.USED.ordinal()], GL_ACTIVE_UNIFORMS, activeUniform, 0);

        for (int i = 0; i < activeUniformBlocks[0]; ++i) {

            int[] length = {0};
            byte[] name = new byte[128];

            gl3.glGetActiveUniformName(programName[Program.USED.ordinal()], i, name.length, length, 0, name, 0);

            String stringName = new String(name);
            //remove the empty padding spaces at the end
            stringName = stringName.trim();

            validated = validated && (stringName.equals("Material.diffuse") || stringName.equals("Transform.mvp"));
        }

        return validated & checkError(gl3, "initProgram");
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName, 0);
        gl3.glBindVertexArray(vertexArrayName[0]);
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX.ordinal()]);
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);

            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT.ordinal()]);
        }
        gl3.glBindVertexArray(0);

        return checkError(gl3, "initVertexArray");
    }

    private boolean initBuffer(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX.ordinal(), bufferName, 0);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT.ordinal()]);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX.ordinal()]);
        FloatBuffer positionBuffer = GLBuffers.newDirectFloatBuffer(positionData);
        gl3.glBufferData(GL_ARRAY_BUFFER, positionSize, positionBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        int[] uniformBlockSize = {0};

        {
            gl3.glGetActiveUniformBlockiv(programName[Program.USED.ordinal()], uniformTransform,
                    GL_UNIFORM_BLOCK_DATA_SIZE, uniformBlockSize, 0);

            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM.ordinal()]);
            gl3.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize[0], null, GL_DYNAMIC_DRAW);
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        {
            float[] diffuse = {1.0f, 0.5f, 0.0f, 1.0f};

            gl3.glGetActiveUniformBlockiv(programName[Program.USED.ordinal()], uniformMaterial,
                    GL_UNIFORM_BLOCK_DATA_SIZE, uniformBlockSize, 0);

            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.MATERIAL.ordinal()]);
            FloatBuffer diffuseBuffer = GLBuffers.newDirectFloatBuffer(diffuse);
            gl3.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize[0], diffuseBuffer, GL_DYNAMIC_DRAW);
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        return checkError(gl3, "initBuffer");
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        {
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM.ordinal()]);
            ByteBuffer pointer = gl3.glMapBufferRange(
                    GL_UNIFORM_BUFFER, 0, 16 * Float.BYTES,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            FloatUtil.makePerspective(projection, 0, true, (float) Math.PI * 0.25f, 4.0f / 3.0f, 0.1f, 100.0f);
            FloatUtil.makeIdentity(model);
            FloatUtil.multMatrix(projection, view(), mvp);
            FloatUtil.multMatrix(mvp, model);

            pointer.asFloatBuffer().put(mvp).rewind();

            // Make sure the uniform buffer is uploaded
            gl3.glUnmapBuffer(GL_UNIFORM_BUFFER);
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        gl3.glViewport(0, 0, windowSize.x, windowSize.y);
        gl3.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, 0);

        gl3.glUseProgram(programName[Program.USED.ordinal()]);
        gl3.glUniformBlockBinding(programName[Program.USED.ordinal()], uniformTransform, Semantic.Uniform.TRANSFORM0);
        gl3.glUniformBlockBinding(programName[Program.USED.ordinal()], uniformMaterial, Semantic.Uniform.MATERIAL);

        // Attach the buffer to UBO binding point semantic::uniform::TRANSFORM0
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName[Buffer.TRANSFORM.ordinal()]);

        // Attach the buffer to UBO binding point semantic::uniform::MATERIAL
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName[Buffer.MATERIAL.ordinal()]);

        gl3.glBindVertexArray(vertexArrayName[0]);
        gl3.glDrawElementsInstanced(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteVertexArrays(1, vertexArrayName, 0);
        gl3.glDeleteBuffers(Buffer.MAX.ordinal(), bufferName, 0);
        gl3.glDeleteProgram(programName[Program.USED.ordinal()]);

        return checkError(gl3, "end");
    }
}
