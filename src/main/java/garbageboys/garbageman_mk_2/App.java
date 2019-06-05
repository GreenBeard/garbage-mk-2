package garbageboys.garbageman_mk_2;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class App {

	// The window handle
	private long window;

	public void run() {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		game_loop_start();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		Configuration.DEBUG.set(true);

		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

		// Create the window
		window = glfwCreateWindow(1600, 900, "Hello World!", NULL, NULL);
		if (window == NULL) {
			throw new RuntimeException("Failed to create the GLFW window");
		}

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			}
		});

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
				window,
				(vidmode.width() - pWidth.get(0)) / 2,
				(vidmode.height() - pHeight.get(0)) / 2
			);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(window);
	}
	
	int create_gl_shader(String source, int type) {
		int shader_id = glCreateShader(type);
		MemoryStack stack = stackPush();
		glShaderSource(shader_id, source);
		glCompileShader(shader_id);
		
		IntBuffer result = stack.mallocInt(1);
		glGetShaderiv(shader_id, GL_COMPILE_STATUS, result);
		
		if (result.get(0) == GL_FALSE) {
			String log = glGetShaderInfoLog(shader_id);
			System.out.println("Error compiling shader: " + log);
			
			// Technically bad style
			System.exit(1);
		}
		stack.pop();
		
		return shader_id;
	}
	
	int create_gl_program(String vertex_src, String fragment_src) {
		int program_id = glCreateProgram();
		
		int vertex_id = create_gl_shader(vertex_src, GL_VERTEX_SHADER);
		int fragment_id = create_gl_shader(fragment_src, GL_FRAGMENT_SHADER);
		
		glAttachShader(program_id, vertex_id);
		glAttachShader(program_id, fragment_id);
		glLinkProgram(program_id);
		glValidateProgram(program_id);
		
		MemoryStack stack = stackPush();
		
		IntBuffer validate_status = stack.mallocInt(1);
		glGetProgramiv(program_id, GL_VALIDATE_STATUS, validate_status);
		if (validate_status.get(0) == GL_FALSE) {
			String log = glGetProgramInfoLog(program_id);
			System.out.println("Error validating program: " + log);
			
			// Technically bad style
			System.exit(1);
		}
		
		// Mark for deletion when no longer in use
		glDeleteShader(vertex_id);
		glDeleteShader(fragment_id);
		
		stack.pop();
		
		return program_id;
	}

	void check_gl_errors() {
		int error;
		while ((error = glGetError()) != GL_NO_ERROR) {
			System.out.println("OpenGL error code: " + error);
			new Exception().printStackTrace();
		}
	}

	private void game_loop_start() {
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		System.out.println("OpenGL version: " + glGetString(GL_VERSION));
		System.out.println("       device: " + glGetString(GL_RENDERER));

		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.4f, 0.0f);
		
		// Enable depth test
		glEnable(GL_DEPTH_TEST);
		// Accept fragment if it closer to the camera than the former one
		glDepthFunc(GL_LESS);

		String vertex_src = ResourceLoader.LoadShader("/shaders/vertex_shader.glsl");
		String fragment_src = ResourceLoader.LoadShader("/shaders/fragment_shader.glsl");
		int program_id = create_gl_program(vertex_src, fragment_src);
		
		MemoryStack stack = stackPush();

		float[] raw_triangle_data = {
			/* Triangle one */
			-0.5f, -0.5f, -0.8f,
			0.5f, -0.5f, -0.8f,
			0.5f, 0.5f, -0.8f,
			/* Triangle two */
			-0.5f, -0.5f, 0.0f,
			0.5f, 0.5f, 0.0f,
			-0.5f, 0.5f, 0.0f
		};
		
		IntBuffer vao = stack.mallocInt(1);
		glGenVertexArrays(vao);
		glBindVertexArray(vao.get(0));
		
		IntBuffer vbo = stack.mallocInt(1);
		glGenBuffers(vbo);
		glEnableVertexAttribArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, vbo.get(0));
		
		/*FloatBuffer triangle_data = BufferUtils.createFloatBuffer(3 * 3);
		triangle_data.put(raw_triangle_data);
		// Will not work without following line
		triangle_data.rewind();
		glBufferData(GL_ARRAY_BUFFER, triangle_data, GL_STATIC_DRAW);*/

		glBufferData(GL_ARRAY_BUFFER, raw_triangle_data, GL_STATIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		
		IntBuffer width = stack.mallocInt(1);
		IntBuffer height = stack.mallocInt(1);
		IntBuffer channels = stack.mallocInt(1);
		ByteBuffer img_buffer = ResourceLoader.LoadTexture("/assets/Buttons/play.png", width, height, channels);
		
		/* http://wiki.lwjgl.org/images/5/51/Coordinates.png */
		float[] raw_uv_coordinates = {
		  0.0f, 1.0f,
		  1.0f, 1.0f,
		  1.0f, 0.0f,
		  0.0f, 1.0f,
		  1.0f, 0.0f,
		  0.0f, 0.0f
		};
		
		IntBuffer texture_id = stack.mallocInt(1);
		glGenTextures(texture_id);
		glBindTexture(GL_TEXTURE_2D, texture_id.get(0));
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, img_buffer);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, texture_id.get(0));
		
		IntBuffer vbo_uv = stack.mallocInt(1);
		glGenBuffers(vbo_uv);
		glEnableVertexAttribArray(1);
		glBindBuffer(GL_ARRAY_BUFFER, vbo_uv.get(0));
		glBufferData(GL_ARRAY_BUFFER, raw_uv_coordinates, GL_STATIC_DRAW);
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		
		int texture_location = glGetUniformLocation(program_id, "text");

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
			
			glUniform1i(texture_location, 0);

			glUseProgram(program_id);
			glDrawArrays(GL_TRIANGLES, 0, 6);
			
			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}

		glDisableVertexAttribArray(0);
		
		stack.pop();
	}

	public static void main(String[] args) {
		new App().run();
	}

}
