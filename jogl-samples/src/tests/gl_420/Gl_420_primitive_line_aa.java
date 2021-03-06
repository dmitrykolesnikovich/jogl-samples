/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_420;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.vec._2.i.Vec2i;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glm.vec._2.Vec2;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_420_primitive_line_aa extends Test {

    public static void main(String[] args) {
        Gl_420_primitive_line_aa gl_420_primitive_line_aa = new Gl_420_primitive_line_aa();
    }

    public Gl_420_primitive_line_aa() {
        super("gl-420-primitive-line-aa", Profile.CORE, 4, 2);
    }

    private final String SHADERS_SOURCE_AA = "primitive-line-aa";
    private final String SHADERS_SOURCE_SPLASH = "primitive-line-splash";
    private final String SHADERS_ROOT = "src/data/gl_420";

    private final Vec2i FRAMEBUFFER_SIZE = new Vec2i(80, 60);
    private Vec2[] vertexData;

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int TRANSFORM = 1;
        public static final int MAX = 2;
    }

    private class Texture {

        public static final int MULTISAMPLE = 0;
        public static final int COLOR = 1;
        public static final int MAX = 2;
    }

    private class Framebuffer {

        public static final int MULTISAMPLE = 0;
        public static final int COLOR = 1;
        public static final int MAX = 2;
    }

    private class Pipeline {

        public static final int MULTISAMPLE = 0;
        public static final int SPLASH = 1;
        public static final int MAX = 2;
    }

    private IntBuffer pipelineName = GLBuffers.newDirectIntBuffer(Pipeline.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(Pipeline.MAX),
            framebufferName = GLBuffers.newDirectIntBuffer(Framebuffer.MAX),
            bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    private int[] programName = new int[Pipeline.MAX];

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;

        if (validated) {
            validated = initState(gl4);
        }
        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initTexture(gl4);
        }
        if (validated) {
            validated = initFramebuffer(gl4);
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

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE_AA, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE_AA, "frag", null, true);

            shaderProgram.init(gl4);
            programName[Pipeline.MULTISAMPLE] = shaderProgram.program();

            gl4.glProgramParameteri(programName[Pipeline.MULTISAMPLE], GL_PROGRAM_SEPARABLE, GL_TRUE);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);
            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE_SPLASH, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE_SPLASH, "frag", null, true);

            shaderProgram.init(gl4);
            programName[Pipeline.SPLASH] = shaderProgram.program();

            gl4.glProgramParameteri(programName[Pipeline.SPLASH], GL_PROGRAM_SEPARABLE, GL_TRUE);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);
            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            gl4.glGenProgramPipelines(Pipeline.MAX, pipelineName);
            gl4.glUseProgramStages(pipelineName.get(Pipeline.MULTISAMPLE), GL_VERTEX_SHADER_BIT | GL_FRAGMENT_SHADER_BIT,
                    programName[Pipeline.MULTISAMPLE]);
            gl4.glUseProgramStages(pipelineName.get(Pipeline.SPLASH), GL_VERTEX_SHADER_BIT | GL_FRAGMENT_SHADER_BIT,
                    programName[Pipeline.SPLASH]);
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initBuffer(GL4 gl4) {

        int count = 1000;
        float step = (float) (Math.PI * 2.0f / count);

        vertexData = new Vec2[count];

        ByteBuffer vertexBuffer = GLBuffers.newDirectByteBuffer(vertexData.length * Vec2.SIZE);
        IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);

        for (int i = 0; i < count; ++i) {


            //VertexData[i] = glm::vec2(glm::fastSin(glm::radians(Step * float(i))), glm::fastCos(glm::radians(Step * float(i))));
            /*
			VertexData[i] = glm::vec2(
				glm::fastSin(glm::mod(Step * float(i), glm::pi<float>())),
				glm::fastCos(glm::mod(Step * float(i), glm::pi<float>())));
             */ /*	VertexData[i] = glm::vec2(
				glm::sin(Step * float(i)),
				glm::fastCos(glm::mod(Step * float(i), glm::pi<float>())));
             */
            vertexData[i] = new Vec2(
                    Math.sin(step * i),
                    Math.cos(step * i));
        }
        gl4.glGenBuffers(Buffer.MAX, bufferName);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        for (int i = 0; i < vertexData.length; i++) {
            vertexData[i].toDbb(vertexBuffer, i * Vec2.SIZE);
        }
        gl4.glBufferData(GL_ARRAY_BUFFER, vertexData.length * Vec2.SIZE, vertexBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl4.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);
        int uniformBlockSize = Math.max(Mat4.SIZE, uniformBufferOffset.get(0));

        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl4.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_DYNAMIC_DRAW);
        gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(uniformBufferOffset);

        return true;
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(Pipeline.MAX, vertexArrayName);

        gl4.glBindVertexArray(vertexArrayName.get(Pipeline.MULTISAMPLE));
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        }
        gl4.glBindVertexArray(0);

        gl4.glBindVertexArray(vertexArrayName.get(Pipeline.SPLASH));
        gl4.glBindVertexArray(0);

        return true;
    }

    private boolean initFramebuffer(GL4 gl4) {

        gl4.glGenFramebuffers(Framebuffer.MAX, framebufferName);

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.MULTISAMPLE));
        gl4.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureName.get(Texture.MULTISAMPLE), 0);
        if (!isFramebufferComplete(gl4, framebufferName.get(Framebuffer.MULTISAMPLE))) {
            return false;
        }
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.COLOR));
        gl4.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureName.get(Texture.COLOR), 0);
        if (!isFramebufferComplete(gl4, framebufferName.get(Framebuffer.COLOR))) {
            return false;
        }
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        return true;
    }

    private boolean initTexture(GL4 gl4) {

        gl4.glGenTextures(Texture.MAX, textureName);

        {
            gl4.glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, textureName.get(Texture.MULTISAMPLE));
            gl4.glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, 8, GL_RGBA8, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y,
                    true);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl4.glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, 0);
        }

        {
            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.COLOR));
            gl4.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, null);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl4.glBindTexture(GL_TEXTURE_2D, 0);
        }

        return true;
    }

    private boolean initState(GL4 gl4) {

        gl4.glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

        return true;
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            ByteBuffer pointer = gl4.glMapBufferRange(
                    GL_UNIFORM_BUFFER, 0, Mat4.SIZE,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            Mat4 projection = glm.perspectiveFov_((float) Math.PI * 0.25f, windowSize.x, windowSize.y, 0.1f, 100.0f);
            Mat4 model = new Mat4(1.0f);

            projection.mul(viewMat4()).mul(model).toDbb(pointer);

            // Make sure the uniform buffer is uploaded
            gl4.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        //////////////////////////////
        // Render multisampled texture
        //glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.MULTISAMPLE));
        //glEnable(GL_MULTISAMPLE);
        gl4.glViewportIndexedf(0, 0, 0, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y);
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1).put(1, 0.5f).put(2, 0).put(3, 1));

        gl4.glBindProgramPipeline(pipelineName.get(Pipeline.MULTISAMPLE));
        gl4.glBindVertexArray(vertexArrayName.get(Pipeline.MULTISAMPLE));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));

        gl4.glDrawArraysInstancedBaseInstance(GL_LINE_LOOP, 0, vertexData.length, 3, 0);

        //////////////////////////
        // Resolving multisampling
        gl4.glBindFramebuffer(GL_READ_FRAMEBUFFER, framebufferName.get(Framebuffer.MULTISAMPLE));
        gl4.glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebufferName.get(Framebuffer.COLOR));
        gl4.glBlitFramebuffer(
                0, 0, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y,
                0, 0, FRAMEBUFFER_SIZE.x, FRAMEBUFFER_SIZE.y,
                GL_COLOR_BUFFER_BIT, GL_NEAREST);
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);

        //////////////////////////////////////
        // Render resolved multisample texture
        gl4.glViewportIndexedf(0, 0, 0, windowSize.x, windowSize.y);
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        //glDisable(GL_MULTISAMPLE);
        gl4.glActiveTexture(GL_TEXTURE0);
        //glBindTexture(GL_TEXTURE_2D_MULTISAMPLE, TextureName[texture::MULTISAMPLE]);
        gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.COLOR));
        gl4.glBindVertexArray(vertexArrayName.get(Pipeline.SPLASH));
        gl4.glBindProgramPipeline(pipelineName.get(Pipeline.SPLASH));

        gl4.glDrawArraysInstancedBaseInstance(GL_TRIANGLES, 0, 6, 1, 0);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteBuffers(Texture.MAX, textureName);
        gl4.glDeleteProgramPipelines(Pipeline.MAX, pipelineName);
        gl4.glDeleteProgram(programName[Pipeline.MULTISAMPLE]);
        gl4.glDeleteProgram(programName[Pipeline.SPLASH]);
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        gl4.glDeleteVertexArrays(Pipeline.MAX, vertexArrayName);
        gl4.glDeleteFramebuffers(Framebuffer.MAX, framebufferName);

        BufferUtils.destroyDirectBuffer(textureName);
        BufferUtils.destroyDirectBuffer(pipelineName);
        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);
        BufferUtils.destroyDirectBuffer(framebufferName);

        return true;
    }
}
