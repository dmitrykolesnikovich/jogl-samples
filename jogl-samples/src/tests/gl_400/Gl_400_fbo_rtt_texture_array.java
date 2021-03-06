/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_400;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import framework.Profile;
import framework.Test;
import java.nio.IntBuffer;
import jglm.Vec2i;
import jglm.Vec4;

/**
 *
 * @author GBarbieri
 */
public class Gl_400_fbo_rtt_texture_array extends Test {

    public static void main(String[] args) {
        Gl_400_fbo_rtt_texture_array gl_400_fbo_rtt_texture_array = new Gl_400_fbo_rtt_texture_array();
    }

    public Gl_400_fbo_rtt_texture_array() {
        super("gl-400-fbo-rtt-texture-array", Profile.CORE, 4, 0);
    }

    private final String SHADERS_SOURCE = "rtt-array";
    private final String SHADERS_ROOT = "src/data/gl_400";

    private final int FRAMEBUFFER_SIZE = 2;
    private int vertexCount = 3;

    private class Texture {

        public static final int R = 0;
        public static final int G = 1;
        public static final int B = 2;
        public static final int MAX = 3;
    };

    private IntBuffer framebufferName = GLBuffers.newDirectIntBuffer(1),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1), textureName = GLBuffers.newDirectIntBuffer(1);
    private int programName, uniformDiffuse, uniformLayer;
    private Vec4[] viewport = new Vec4[Texture.MAX];

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        Vec2i framebufferSize = new Vec2i(windowSize.x / FRAMEBUFFER_SIZE, windowSize.y / FRAMEBUFFER_SIZE);

        viewport[Texture.R] = new Vec4(windowSize.x >> 1, 0, framebufferSize.x, framebufferSize.y);
        viewport[Texture.G] = new Vec4(windowSize.x >> 1, windowSize.y >> 1, framebufferSize.x, framebufferSize.y);
        viewport[Texture.B] = new Vec4(0, windowSize.y >> 1, framebufferSize.x, framebufferSize.y);

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }
        if (validated) {
            validated = initTexture(gl4);
        }
        if (validated) {
            validated = initFramebuffer(gl4);
        }

        return validated && checkError(gl4, "begin");
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        // Create program
        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE, "frag", null, true);

            shaderProgram.init(gl4);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            programName = shaderProgram.program();

            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            uniformDiffuse = gl4.glGetUniformLocation(programName, "diffuse");
            uniformLayer = gl4.glGetUniformLocation(programName, "layer");
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initTexture(GL4 gl4) {

        gl4.glActiveTexture(GL_TEXTURE0);
        gl4.glGenTextures(1, textureName);

        gl4.glBindTexture(GL_TEXTURE_2D_ARRAY, textureName.get(0));
        gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BASE_LEVEL, 0);
        gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAX_LEVEL, 0);
        gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_SWIZZLE_R, GL_RED);
        gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_SWIZZLE_G, GL_GREEN);
        gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_SWIZZLE_B, GL_BLUE);
        gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_SWIZZLE_A, GL_ALPHA);
        gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl4.glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        Vec2i framebufferSize = new Vec2i(windowSize.x / FRAMEBUFFER_SIZE, windowSize.y / FRAMEBUFFER_SIZE);

        gl4.glTexImage3D(
                GL_TEXTURE_2D_ARRAY,
                0,
                GL_RGBA8,
                framebufferSize.x, framebufferSize.y, 3, //depth
                0,
                GL_RGB, GL_UNSIGNED_BYTE,
                null);

        return checkError(gl4, "initTexture");
    }

    private boolean initFramebuffer(GL4 gl4) {

        gl4.glGenFramebuffers(1, framebufferName);
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        gl4.glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureName.get(0), 0, 0);
        gl4.glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, textureName.get(0), 0, 1);
        gl4.glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, textureName.get(0), 0, 2);
        IntBuffer drawBuffers = GLBuffers.newDirectIntBuffer(new int[]{GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1,
            GL_COLOR_ATTACHMENT2});
        gl4.glDrawBuffers(3, drawBuffers);
        if (!isFramebufferComplete(gl4, framebufferName.get(0))) {
            return false;
        }

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl4.glDrawBuffer(GL_BACK);
        if (!isFramebufferComplete(gl4, framebufferName.get(0))) {
            return false;
        }

        return checkError(gl4, "initFramebuffer");
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glBindVertexArray(0);

        return checkError(gl4, "initVertexArray");
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        Vec2i framebufferSize = new Vec2i(windowSize.x / FRAMEBUFFER_SIZE, windowSize.y / FRAMEBUFFER_SIZE);

        // Pass 1
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
        gl4.glViewport(0, 0, framebufferSize.x, framebufferSize.y);
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1).put(1, 0).put(2, 0).put(3, 1));
        gl4.glClearBufferfv(GL_COLOR, 1, clearColor.put(0, 0).put(1, 1).put(2, 0).put(3, 1));
        gl4.glClearBufferfv(GL_COLOR, 2, clearColor.put(0, 0).put(1, 0).put(2, 1).put(3, 1));
        gl4.glClearBufferfv(GL_COLOR, 3, clearColor.put(0, 1).put(1, 1).put(2, 0).put(3, 1));

        // Pass 2
        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl4.glViewport(0, 0, windowSize.x, windowSize.y);
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1).put(1, .5f).put(2, 0).put(3, 1));

        gl4.glUseProgram(programName);
        gl4.glUniform1i(uniformDiffuse, 0);

        gl4.glActiveTexture(GL_TEXTURE0);
        gl4.glBindTexture(GL_TEXTURE_2D_ARRAY, textureName.get(0));
        gl4.glBindVertexArray(vertexArrayName.get(0));

        for (int i = 0; i < Texture.MAX; ++i) {
            gl4.glViewport((int) viewport[i].x, (int) viewport[i].y, (int) viewport[i].z, (int) viewport[i].w);
            gl4.glUniform1i(uniformLayer, i);

            gl4.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 1);
        }

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteProgram(programName);
        gl4.glDeleteTextures(1, textureName);
        gl4.glDeleteFramebuffers(1, framebufferName);
        gl4.glDeleteVertexArrays(1, vertexArrayName);

        BufferUtils.destroyDirectBuffer(textureName);
        BufferUtils.destroyDirectBuffer(framebufferName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);

        return true;
    }
}
