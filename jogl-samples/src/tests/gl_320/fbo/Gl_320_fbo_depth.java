/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_320.fbo;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2.GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._2.Vec2;
import glm.vec._3.Vec3;
import framework.BufferUtils;
import framework.Profile;
import framework.Semantic;
import framework.Test;
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
public class Gl_320_fbo_depth extends Test {

    public static void main(String[] args) {
        Gl_320_fbo_depth gl_320_fbo_depth = new Gl_320_fbo_depth();
    }

    public Gl_320_fbo_depth() {
        super("Gl-320-fbo-depth", Profile.CORE, 3, 2, new Vec2(0.0f, -Math.PI * 0.48f));
    }

    private final String SHADERS_SOURCE_TEXTURE = "texture-2d";
    private final String SHADERS_SOURCE_SPLASH = "fbo-depth";
    private final String SHADERS_ROOT_TEXTURE = "src/data/gl_320/texture";
    private final String SHADERS_ROOT_SPLASH = "src/data/gl_320/fbo";
    private final String TEXTURE_DIFFUSE = "kueken7_rgb_dxt1_srgb.dds";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * glf.Vertex_v2fv2f.SIZE;
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

        public static final int DIFFUSE = 0;
        public static final int RENDERBUFFER = 1;
        public static final int MAX = 2;
    }

    private class Program {

        public static final int TEXTURE = 0;
        public static final int SPLASH = 1;
        public static final int MAX = 2;
    }

    private int[] programName = new int[Program.MAX];
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(Program.MAX),
            bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            textureName = GLBuffers.newDirectIntBuffer(Texture.MAX), framebufferName = GLBuffers.newDirectIntBuffer(1);
    private int uniformTransform;

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
        if (validated) {
            validated = initTexture(gl3);
        }
        if (validated) {
            validated = initFramebuffer(gl3);
        }

        return validated && checkError(gl3, "begin");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        // Create program
        if (validated) {

            ShaderCode vertexShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT_TEXTURE,
                    null, SHADERS_SOURCE_TEXTURE, "vert", null, true);
            ShaderCode fragmentShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT_TEXTURE,
                    null, SHADERS_SOURCE_TEXTURE, "frag", null, true);

            ShaderProgram program = new ShaderProgram();
            program.add(vertexShader);
            program.add(fragmentShader);
            program.init(gl3);

            programName[Program.TEXTURE] = program.program();

            gl3.glBindAttribLocation(programName[Program.TEXTURE], Semantic.Attr.POSITION, "position");
            gl3.glBindAttribLocation(programName[Program.TEXTURE], Semantic.Attr.TEXCOORD, "texCoord");
            gl3.glBindFragDataLocation(programName[Program.TEXTURE], Semantic.Frag.COLOR, "color");

            program.link(gl3, System.out);
        }
        if (validated) {

            ShaderCode vertexShader = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT_SPLASH,
                    null, SHADERS_SOURCE_SPLASH, "vert", null, true);
            ShaderCode fragmentShader = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT_SPLASH,
                    null, SHADERS_SOURCE_SPLASH, "frag", null, true);

            ShaderProgram program = new ShaderProgram();
            program.add(vertexShader);
            program.add(fragmentShader);
            program.init(gl3);

            programName[Program.SPLASH] = program.program();

            gl3.glBindFragDataLocation(programName[Program.SPLASH], Semantic.Frag.COLOR, "color");

            program.link(gl3, System.out);
        }
        if (validated) {

            uniformTransform = gl3.glGetUniformBlockIndex(programName[Program.TEXTURE], "Transform");
        }
        return validated & checkError(gl3, "initProgram");
    }

    private boolean initBuffer(GL3 gl3) {

        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl3.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);
        int uniformBlockSize = Math.max(Mat4.SIZE, uniformBufferOffset.get(0));

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl3.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_DYNAMIC_DRAW);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(elementBuffer);
        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(uniformBufferOffset);

        return true;
    }

    private boolean initTexture(GL3 gl3) {

        boolean validated = true;

        try {

            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));

            if (texture.empty()) {
                throw new Error("texture empty");
            }

            gl3.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            gl3.glGenTextures(Texture.MAX, textureName);

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.DIFFUSE));
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, texture.levels() - 1);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            for (int level = 0; level < texture.levels(); level++) {
                gl3.glCompressedTexImage2D(GL_TEXTURE_2D, level,
                        GL_COMPRESSED_SRGB_ALPHA_S3TC_DXT1,
                        texture.dimensions(level)[0], texture.dimensions(level)[1],
                        0,
                        texture.size(level),
                        texture.data(level));
            }

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.RENDERBUFFER));
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            gl3.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, windowSize.x, windowSize.y,
                    0, GL_DEPTH_COMPONENT, GL_FLOAT, null);

            gl3.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        } catch (IOException ex) {
            Logger.getLogger(Gl_320_fbo_depth.class.getName()).log(Level.SEVERE, null, ex);
        }
        return validated;
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(Program.MAX, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(Program.TEXTURE));
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, glf.Vertex_v2fv2f.SIZE, 0);
            gl3.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, glf.Vertex_v2fv2f.SIZE, Vec2.SIZE);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl3.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);

            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl3.glBindVertexArray(0);

        gl3.glBindVertexArray(vertexArrayName.get(Program.SPLASH));
        gl3.glBindVertexArray(0);

        return true;
    }

    private boolean initFramebuffer(GL3 gl3) {

        gl3.glGenFramebuffers(1, framebufferName);
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        gl3.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, textureName.get(Texture.RENDERBUFFER), 0);
        gl3.glDrawBuffer(GL_NONE);

        if (!isFramebufferComplete(gl3, framebufferName.get(0))) {
            return false;
        }

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return true;
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        {
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            ByteBuffer pointer = gl3.glMapBufferRange(
                    GL_UNIFORM_BUFFER, 0, Mat4.SIZE,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, 4.0f / 3.0f, 0.5f, 8.0f);
            Mat4 model = new Mat4(1.0f).scale(new Vec3(5.0f));
            pointer.asFloatBuffer().put(projection.mul(viewMat4()).mul(model).toFa_());

            // Make sure the uniform buffer is uploaded
            gl3.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        gl3.glEnable(GL_DEPTH_TEST);
        gl3.glDepthFunc(GL_LESS);

        gl3.glViewport(0, 0, windowSize.x, windowSize.y);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        float[] depth = {1.0f};
        gl3.glClearBufferfv(GL_DEPTH, 0, depth, 0);

        // Bind rendering objects
        gl3.glUseProgram(programName[Program.TEXTURE]);
        gl3.glUniformBlockBinding(programName[Program.TEXTURE], uniformTransform, Semantic.Uniform.TRANSFORM0);

        gl3.glActiveTexture(GL_TEXTURE0);
        gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.DIFFUSE));
        gl3.glBindVertexArray(vertexArrayName.get(Program.TEXTURE));
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));

        gl3.glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0);

        gl3.glDisable(GL_DEPTH_TEST);

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glUseProgram(programName[Program.SPLASH]);

        gl3.glActiveTexture(GL_TEXTURE0);
        gl3.glBindVertexArray(vertexArrayName.get(Program.SPLASH));
        gl3.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.RENDERBUFFER));

        gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, 3, 1);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteProgram(programName[Program.SPLASH]);
        gl3.glDeleteProgram(programName[Program.TEXTURE]);

        gl3.glDeleteFramebuffers(1, framebufferName);
        gl3.glDeleteBuffers(Buffer.MAX, bufferName);
        gl3.glDeleteTextures(Texture.MAX, textureName);
        gl3.glDeleteVertexArrays(Program.MAX, vertexArrayName);

        BufferUtils.destroyDirectBuffer(framebufferName);
        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(textureName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);

        return checkError(gl3, "end");
    }
}
