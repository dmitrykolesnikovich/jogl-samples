/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_430;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL3ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._2.Vec2;
import framework.BufferUtils;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glf.Vertex_v2fv2f;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgli.Texture2d;

/**
 *
 * @author GBarbieri
 */
public class Gl_430_texture_view extends Test {

    public static void main(String[] args) {
        Gl_430_texture_view gl_430_texture_view = new Gl_430_texture_view();
    }

    public Gl_430_texture_view() {
        super("gl-430-texture-view", Profile.CORE, 4, 3);
    }

    private final String SHADERS_SOURCE = "texture-view";
    private final String SHADERS_ROOT = "src/data/gl_430";
    private final String TEXTURE_DIFFUSE = "kueken7_rgb8_unorm.dds";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * Vertex_v2fv2f.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,/**/ 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0};

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int TRANSFORM = 2;
        public static final int MAX = 3;
    }

    private class Texture {

        public static final int TEXTURE = 0;
        public static final int VIEW_A = 1;
        public static final int VIEW_B = 2;
        public static final int MAX = 3;
    }

    private IntBuffer pipelineName = GLBuffers.newDirectIntBuffer(1), vertexArrayName = GLBuffers.newDirectIntBuffer(1),
            bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX), textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    private int programName;

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;
        validated = validated && gl4.isExtensionAvailable("GL_ARB_texture_query_levels");
        validated = validated && gl4.isExtensionAvailable("GL_ARB_texture_view");
        validated = validated && gl4.isExtensionAvailable("GL_ARB_fragment_layer_viewport");

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }
        if (validated) {
            validated = initTexture(gl4);
        }

        return validated;
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        // Create program
        if (validated) {

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "vert", null, true);
            ShaderCode geomShaderCode = ShaderCode.create(gl4, GL_GEOMETRY_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "geom", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.init(gl4);

            programName = shaderProgram.program();

            gl4.glProgramParameteri(programName, GL_PROGRAM_SEPARABLE, GL_TRUE);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(geomShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            gl4.glGenProgramPipelines(1, pipelineName);
            gl4.glUseProgramStages(pipelineName.get(0), GL_VERTEX_SHADER_BIT | GL_GEOMETRY_SHADER_BIT
                    | GL_FRAGMENT_SHADER_BIT, programName);
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initBuffer(GL4 gl4) {

        gl4.glGenBuffers(Buffer.MAX, bufferName);

        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        gl4.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        BufferUtils.destroyDirectBuffer(elementBuffer);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        gl4.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        BufferUtils.destroyDirectBuffer(vertexBuffer);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        int[] uniformBufferOffset = {0};
        gl4.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset, 0);
        int uniformBlockSize = Math.max(Mat4.SIZE, uniformBufferOffset[0]);

        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl4.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_DYNAMIC_DRAW);
        gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        return true;
    }

    private boolean initTexture(GL4 gl4) {

        try {
            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));
            assert (!texture.empty());

            {
                jgli.Gl.Format format = jgli.Gl.translate(jgli.Format.FORMAT_RGB8_UNORM_PACK8);

                gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

                gl4.glGenTextures(Texture.MAX, textureName);
                gl4.glActiveTexture(GL_TEXTURE0);
                gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.TEXTURE));
                gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_RED);
                gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_GREEN);
                gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_BLUE);
                gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ALPHA);
                gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
                gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, texture.levels() - 1);

                gl4.glTexStorage2D(GL_TEXTURE_2D, texture.levels(),
                        format.internal.value,
                        texture.dimensions()[0], texture.dimensions()[1]);

                for (int level = 0; level < texture.levels(); ++level) {
                    gl4.glTexSubImage2D(GL_TEXTURE_2D, level,
                            0, 0,
                            texture.dimensions(level)[0], texture.dimensions(level)[1],
                            format.external.value, format.type.value,
                            texture.data(level));
                }

                gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);
            }

            {
                jgli.Gl.Format format = jgli.Gl.translate(texture.format());
                gl4.glTextureView(textureName.get(Texture.VIEW_A), GL_TEXTURE_2D_ARRAY, textureName.get(Texture.TEXTURE),
                        format.internal.value, 0, texture.levels(), 0, 1);
                gl4.glTextureView(textureName.get(Texture.VIEW_B), GL_TEXTURE_2D_ARRAY, textureName.get(Texture.TEXTURE),
                        format.internal.value, 0, 1, 0, 1);
            }

        } catch (IOException ex) {
            Logger.getLogger(Gl_430_texture_view.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, Vertex_v2fv2f.SIZE, 0);
            gl4.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, Vertex_v2fv2f.SIZE, Vec2.SIZE);
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl4.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);

            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl4.glBindVertexArray(0);

        return true;
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            ByteBuffer pointer = gl4.glMapBufferRange(GL_UNIFORM_BUFFER, 0, Mat4.SIZE,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            //glm::mat4 Projection = glm::perspectiveFov(glm::pi<float>() * 0.25f, 640.f, 480.f, 0.1f, 100.0f);
            Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, windowSize.x * 0.5f / windowSize.y, 0.1f, 100.0f);
            Mat4 model = new Mat4(1.0f);

            pointer.asFloatBuffer().put(projection.mul(viewMat4()).mul(model).toFa_());

            gl4.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        gl4.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 0.5f, 0.0f, 1.0f}, 0);

        gl4.glBindProgramPipeline(pipelineName.get(0));
        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));

        gl4.glViewportIndexedf(0, 0, 0, windowSize.x / 2.0f, windowSize.y);
        gl4.glViewportIndexedf(1, windowSize.x / 2.0f, 0, windowSize.x / 2.0f, windowSize.y);

        gl4.glActiveTexture(GL_TEXTURE0);
        gl4.glBindTexture(GL_TEXTURE_2D_ARRAY, textureName.get(Texture.VIEW_A));

        gl4.glActiveTexture(GL_TEXTURE1);
        gl4.glBindTexture(GL_TEXTURE_2D_ARRAY, textureName.get(Texture.VIEW_B));

        gl4.glDrawElementsInstancedBaseVertexBaseInstance(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 2, 0, 0);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteProgramPipelines(1, pipelineName);
        BufferUtils.destroyDirectBuffer(pipelineName);
        gl4.glDeleteProgram(programName);
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        BufferUtils.destroyDirectBuffer(bufferName);
        gl4.glDeleteTextures(Texture.MAX, textureName);
        BufferUtils.destroyDirectBuffer(textureName);
        gl4.glDeleteVertexArrays(1, vertexArrayName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);

        return true;
    }
}
