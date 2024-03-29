package OpenGL;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import static com.jogamp.opengl.GL3.*;
import com.jogamp.opengl.GL4;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;


/**
 *
 * @author gbarbieri
 */
public class GLTest implements GLEventListener, KeyListener {

    public static GLWindow glWindow;
    public static Animator animator;

    public static void main(String[] args) {

        Display display = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(display, 0);
        GLProfile glProfile = GLProfile.get(GLProfile.GL4);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glWindow = GLWindow.create(screen, glCapabilities);

        glWindow.setSize(1024, 768);
        glWindow.setPosition(50, 50);
        glWindow.setUndecorated(false);
        glWindow.setAlwaysOnTop(false);
        glWindow.setFullscreen(false);
        glWindow.setPointerVisible(true);
        glWindow.confinePointer(false);
        glWindow.setTitle("Hello Triangle");
        glWindow.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        glWindow.setVisible(true);

        GLTest GLTest = new GLTest();
        glWindow.addGLEventListener(GLTest);
        glWindow.addKeyListener(GLTest);

        animator = new Animator(glWindow);
        animator.start();
    }

    private final String SHADERS_ROOT = "src/gl4/helloTriangle/shaders";
    private final String SHADERS_NAME = "hello-triangle";

    private int vertexCount = 3;
    private int vertexSize = vertexCount * 5 * Float.BYTES;
    private float[] vertexData = new float[]{
            -1, -1,/**/ 1, 0, 0,
            +0, +2,/**/ 0, 0, 1,
            +1, -1,/**/ 0, 1, 0
    };

    private int elementCount = 3;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = new short[]{
            0, 2, 1
    };

    private static class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int TRANSFORM = 2;
        public static final int MAX = 3;
    }

    /**
     * Use pools, you don't want to create and let them cleaned by the garbage
     * collector continuously in the display() method.
     */
    private float[] scale = new float[16], zRotazion = new float[16], modelToClip = new float[16];
    private long start, now;
    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);
    private int programName;
    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(new float[]{1.0f, 0.5f, 0.0f, 1.0f});
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(new float[]{1.0f});
    private ByteBuffer transformPointer;
    /**
     * https://jogamp.org/bugzilla/show_bug.cgi?id=1287
     */
    private boolean bug1287 = true;

    public GLTest() {
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        System.out.println("init");

        GL4 gl4 = drawable.getGL().getGL4();

        initBuffers(gl4);


        initProgram(gl4);

        // map the transform buffer and keep it mapped
        transformPointer = gl4.glMapNamedBufferRange(
                bufferName.get(Buffer.TRANSFORM), // buffer
                0, // offset
                16 * Float.BYTES, // size
                GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT | GL_MAP_INVALIDATE_BUFFER_BIT); // flags

        gl4.glEnable(GL_DEPTH_TEST);

        start = System.currentTimeMillis();
    }


    private void initBuffers(GL4 gl4) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl4.glCreateBuffers(Buffer.MAX, bufferName);

        if (!bug1287) {

            gl4.glNamedBufferStorage(bufferName.get(Buffer.VERTEX), vertexBuffer.capacity() * Float.BYTES, vertexBuffer,
                    GL_STATIC_DRAW);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.ELEMENT), elementBuffer.capacity() * Short.BYTES,
                    elementBuffer, GL_STATIC_DRAW);

            gl4.glNamedBufferStorage(bufferName.get(Buffer.TRANSFORM), 16 * Float.BYTES, null, GL_MAP_WRITE_BIT);

        } else {
            // vertices
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            {
                gl4.glBufferStorage(GL_ARRAY_BUFFER, vertexBuffer.capacity() * Float.BYTES, vertexBuffer, 0);
            }
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);
            // elements
            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
            {
                gl4.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.capacity() * Short.BYTES, elementBuffer, 0);
            }
            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
            // transform
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            {
                IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);
                gl4.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);
                int uniformBlockSize = Math.max(16 * Float.BYTES, uniformBufferOffset.get(0));

                gl4.glBufferStorage(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT
                        | GL_MAP_COHERENT_BIT);

            }
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);
        }
    }


    private void initProgram(GL4 gl4) {

        ShaderCode vertShader = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_NAME, "vert", null, true);
        ShaderCode fragShader = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_NAME, "frag", null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl4);

        programName = shaderProgram.program();

        shaderProgram.link(gl4, System.out);

        vertShader.destroy(gl4);
        fragShader.destroy(gl4);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        //        System.out.println("display");

        GL4 gl4 = drawable.getGL().getGL4();

        gl4.glClearBufferfv(GL_COLOR, 0, clearColor);
        gl4.glClearBufferfv(GL_DEPTH, 0, clearDepth);

        {
            // update matrix based on time
            now = System.currentTimeMillis();
            float diff = (float) (now - start) / 1000;
            /**
             * Here we build the matrix that will multiply our original vertex
             * positions. We scale, halving it, and rotate it.
             */
            scale = FloatUtil.makeScale(scale, true, 0.5f, 0.5f, 0.5f);
            zRotazion = FloatUtil.makeRotationEuler(zRotazion, 0, 0, 0, diff);
            modelToClip = FloatUtil.multMatrix(scale, zRotazion);

            transformPointer.asFloatBuffer().put(modelToClip);
        }
        gl4.glUseProgram(programName);
        gl4.glBindVertexArray(vertexArrayName.get(0));

        gl4.glDrawElements(
                GL_TRIANGLES, // primitive mode
                elementCount, // element count
                GL_UNSIGNED_SHORT, // element type
                0); // element offset
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("dispose");

        GL4 gl4 = drawable.getGL().getGL4();

        gl4.glUnmapNamedBuffer(bufferName.get(Buffer.TRANSFORM));

        gl4.glDeleteProgram(programName);
        gl4.glDeleteVertexArrays(1, vertexArrayName);
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);

        System.exit(0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.out.println("reshape");
        GL4 gl4 = drawable.getGL().getGL4();
        /**
         * Just the glViewport for this sample, normally here you update your
         * projection matrix.
         */
        gl4.glViewport(x, y, width, height);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            animator.remove(glWindow);
            glWindow.destroy();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}