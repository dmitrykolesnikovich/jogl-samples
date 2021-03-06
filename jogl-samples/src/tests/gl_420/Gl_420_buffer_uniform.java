/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_420;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL3ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import glm.vec._2.Vec2;
import glm.vec._3.Vec3;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_420_buffer_uniform extends Test {

    public static void main(String[] args) {
        Gl_420_buffer_uniform gl_420_buffer_uniform = new Gl_420_buffer_uniform();
    }

    public Gl_420_buffer_uniform() {
        super("gl-420-buffer-uniform", Profile.CORE, 4, 2, new Vec2(Math.PI * 0.3f));
    }

    private final String SHADERS_SOURCE = "buffer-uniform";
    private final String SHADERS_ROOT = "src/data/gl_420";

    private int vertexCount = 4;
    private int positionSize = vertexCount * Vec2.SIZE;
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

    private int instances = 2;

    private class Buffer {

        public static final int ELEMENT = 0;
        public static final int VERTEX = 1;
        public static final int TRANSFORM = 2;
        public static final int MATERIAL = 3;
        public static final int MAX = 4;
    }

    private IntBuffer pipelineName = GLBuffers.newDirectIntBuffer(1), vertexArrayName = GLBuffers.newDirectIntBuffer(1),
            bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private int programName, uniformInstance;

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;

        logImplementationDependentLimit(gl4, GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, "GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT");
        logImplementationDependentLimit(gl4, GL_MAX_UNIFORM_BUFFER_BINDINGS, "GL_MAX_UNIFORM_BUFFER_BINDINGS");
        logImplementationDependentLimit(gl4, GL_MAX_UNIFORM_BLOCK_SIZE, "GL_MAX_UNIFORM_BLOCK_SIZE");
        logImplementationDependentLimit(gl4, GL_MAX_VERTEX_UNIFORM_BLOCKS, "GL_MAX_VERTEX_UNIFORM_BLOCKS");
        logImplementationDependentLimit(gl4, GL_MAX_TESS_CONTROL_UNIFORM_BLOCKS, "GL_MAX_TESS_CONTROL_UNIFORM_BLOCKS");
        logImplementationDependentLimit(gl4, GL_MAX_TESS_EVALUATION_UNIFORM_BLOCKS, "GL_MAX_TESS_EVALUATION_UNIFORM_BLOCKS");
        logImplementationDependentLimit(gl4, GL_MAX_GEOMETRY_UNIFORM_BLOCKS, "GL_MAX_GEOMETRY_UNIFORM_BLOCKS");
        logImplementationDependentLimit(gl4, GL_MAX_FRAGMENT_UNIFORM_BLOCKS, "GL_MAX_FRAGMENT_UNIFORM_BLOCKS");

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }

        return validated;
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        // Create program
        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "frag", null, true);

            shaderProgram.init(gl4);
            programName = shaderProgram.program();

            gl4.glProgramParameteri(programName, GL_PROGRAM_SEPARABLE, GL_TRUE);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            gl4.glGenProgramPipelines(1, pipelineName);
            gl4.glUseProgramStages(pipelineName.get(0), GL_VERTEX_SHADER_BIT | GL_FRAGMENT_SHADER_BIT, programName);
        }

        if (validated) {

            uniformInstance = gl4.glGetUniformLocation(programName, "instance");
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initVertexArray(GL4 gl4) {

        boolean validated = true;

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0, 0);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        }
        gl4.glBindVertexArray(0);

        return validated;
    }

    private boolean initBuffer(GL4 gl4) {

        boolean validated = true;

        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        FloatBuffer positionBuffer = GLBuffers.newDirectFloatBuffer(positionData);
        FloatBuffer diffuseBuffer = GLBuffers.newDirectFloatBuffer(new float[]{1.0f, 0.5f, 0.0f, 1.0f});

        gl4.glGenBuffers(Buffer.MAX, bufferName);

        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl4.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl4.glBufferData(GL_ARRAY_BUFFER, positionSize, positionBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            gl4.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE * instances, null, GL_DYNAMIC_DRAW);
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MATERIAL));
            gl4.glBufferData(GL_UNIFORM_BUFFER, diffuseBuffer.capacity() * Float.BYTES, diffuseBuffer, GL_STATIC_DRAW);
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }

        BufferUtils.destroyDirectBuffer(elementBuffer);
        BufferUtils.destroyDirectBuffer(positionBuffer);
        BufferUtils.destroyDirectBuffer(diffuseBuffer);

        return validated;
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        int bufferSize = Mat4.SIZE * 2;

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));

            ByteBuffer pointer = gl4.glMapBufferRange(GL_UNIFORM_BUFFER,
                    0, bufferSize, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, 4.0f / 3.0f, 0.1f, 100.0f);

            {
                Mat4 model = new Mat4(1.0f).rotate((float) Math.PI * 0.25f, new Vec3(0.f, 1.f, 0.f));
                Mat4 mvp = projection.mul_(viewMat4()).mul(model);

                mvp.toDbb(pointer);
            }
            {
                Mat4 model = new Mat4(1.0f).rotate((float) (Math.PI * 0.50f + Math.PI * 0.25f), new Vec3(0.f, 1.f, 0.f));
                Mat4 mvp = projection.mul(viewMat4()).mul(model);

                mvp.toDbb(pointer, Mat4.SIZE);
            }

            // Make sure the uniform buffer is uploaded
            gl4.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        gl4.glViewportIndexedf(0, 0, 0, windowSize.x, windowSize.y);
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 1));

        gl4.glBindProgramPipeline(pipelineName.get(0));
        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl4.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM), 0,
                Mat4.SIZE * 2);
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL));

        for (int i = 0; i < 2; ++i) {
            gl4.glProgramUniform1i(programName, uniformInstance, i);
            gl4.glDrawElementsInstancedBaseVertexBaseInstance(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0, 0);
        }

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteProgramPipelines(1, pipelineName);
        gl4.glDeleteVertexArrays(1, vertexArrayName);
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        gl4.glDeleteProgram(programName);
        
        BufferUtils.destroyDirectBuffer(pipelineName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);
        BufferUtils.destroyDirectBuffer(bufferName);

        return true;
    }
}
