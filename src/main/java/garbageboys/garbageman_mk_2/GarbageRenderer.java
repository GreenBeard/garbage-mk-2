package garbageboys.garbageman_mk_2;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class GarbageRenderer implements Render2D {

	// The window handle
	private long window;
	
	private int program_id;
	
	private class GarbageImage {
		public int texture_id;
		public int texture_pos;
		public int width;
		public int height;
		float[] raw_triangle_data;
		
		public GarbageImage(int texture_id, int texture_pos, int width, int height) {
			this.texture_id = texture_id;
			this.texture_pos = texture_pos;
			this.raw_triangle_data = null;
			this.width = width;
			this.height = height;
		}
	}

	private HashMap<String, GarbageImage> images;
	
	private void resizeCallback(long window, int width, int height) {
		glViewport(0, 0, width, height);
	}

	@Override
	public void initialize() {
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

		// Get the thread stack and push a new frame
		MemoryStack stack = stackPush();
		IntBuffer pWidth = stack.mallocInt(1);
		IntBuffer pHeight = stack.mallocInt(1);

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
	
		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);
	
		// Make the window visible
		glfwShowWindow(window);
		
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		IntBuffer fWidth = stack.mallocInt(1);
		IntBuffer fHeight = stack.mallocInt(1);
		glfwGetFramebufferSize(window, fWidth, fHeight);
		resizeCallback(window, fWidth.get(0), fHeight.get(0));

		glfwSetWindowSizeCallback(window, new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				resizeCallback(window, width, height);
			}
		});

		System.out.println("OpenGL version: " + glGetString(GL_VERSION));
		System.out.println("       device: " + glGetString(GL_RENDERER));

		// Enable depth test
		glEnable(GL_DEPTH_TEST);
		// Accept fragment if it closer to the camera than the former one
		glDepthFunc(GL_LESS);

		String vertex_src = ResourceLoader.LoadShader("/shaders/vertex_shader.glsl");
		String fragment_src = ResourceLoader.LoadShader("/shaders/fragment_shader.glsl");
		program_id = create_gl_program(vertex_src, fragment_src);
		
		IntBuffer vao = stack.mallocInt(1);
		glGenVertexArrays(vao);
		check_gl_errors();
		glBindVertexArray(vao.get(0));
		check_gl_errors();
		
		images = new HashMap<>();
		
		stack.pop();
	}

	@Override
	public void cleanup() {
		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	@Override
	public long getWindowID() {
		return window;
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

	/*
	 * Terrible, but effective algorithm.
	 */
	private int next_texture_id() {
		int max_textures = glGetInteger(GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
		boolean[] used = new boolean[max_textures];
		Arrays.fill(used, false);
		if (images.size() >= max_textures) {
			throw new RuntimeException("Out of memory");
		}
		for (Entry<String, GarbageImage> image : images.entrySet()) {
			int pos = image.getValue().texture_pos;
			used[pos] = true;
		}
		for (int i = 0; i < max_textures; ++i) {
			if (!used[i]) {
				return i;
			}
		}
		assert(false);
		return -1;
	}

	@Override
	public boolean loadImage(String resource) {
		MemoryStack stack = stackPush();

		IntBuffer width = stack.mallocInt(1);
		IntBuffer height = stack.mallocInt(1);
		IntBuffer channels = stack.mallocInt(1);
		ByteBuffer img_buffer = ResourceLoader.LoadTexture(resource, width, height, channels);

		int texture_pos = next_texture_id();
		glActiveTexture(GL_TEXTURE0 + texture_pos);
		IntBuffer texture_id = stack.mallocInt(1);
		glGenTextures(texture_id);
		glBindTexture(GL_TEXTURE_2D, texture_id.get(0));
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, img_buffer);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

		GarbageImage image = new GarbageImage(texture_id.get(0), texture_pos, width.get(0), height.get(0));
		images.put(resource, image);

		stack.pop();
		return true;
	}

	@Override
	public void refreshImages() {
		
	}

	@Override
	public boolean unloadImage(String resource) {
		GarbageImage image = images.get(resource);
		glDeleteTextures(image.texture_id);
		images.remove(resource);
		return true;
	}

	@Override
	public void renderBatchStart() {
		// TODO improve
    for(Entry<String, GarbageImage> image : images.entrySet()) {
    	assert(image.getValue().raw_triangle_data.length == 3 * 6);
    	image.getValue().raw_triangle_data = new float[] {
    		0.0f, 0.0f, 0.0f,
    		0.0f, 0.0f, 0.0f,
    		0.0f, 0.0f, 0.0f,
    		0.0f, 0.0f, 0.0f,
    		0.0f, 0.0f, 0.0f,
    		0.0f, 0.0f, 0.0f
    	};
    }
	}

	@Override
	public void renderBatchEnd() {
		// Set the clear color
		glClearColor(0.0f, 0.0f, 0.4f, 0.0f);
		
		/* Clear the color, and z-depth buffers */
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		MemoryStack stack = stackPush();

		/*int img_count = images.size();

		FloatBuffer triangle_data = BufferUtils.createFloatBuffer(3 * 6 * img_count);
    for(Entry<String, GarbageImage> image : images.entrySet()) {
    	assert(image.getValue().raw_triangle_data.length == 3 * 6);
    	triangle_data.put(image.getValue().raw_triangle_data);
    }
		triangle_data.rewind();*/
		
		IntBuffer vbo = stack.mallocInt(1);
		glGenBuffers(vbo);
		check_gl_errors();
		glEnableVertexAttribArray(0);
		check_gl_errors();
		glBindBuffer(GL_ARRAY_BUFFER, vbo.get(0));
		check_gl_errors();
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		check_gl_errors();

		/* http://wiki.lwjgl.org/images/5/51/Coordinates.png */
		float[] raw_uv_coordinates = {
		  0.0f, 1.0f,
		  1.0f, 1.0f,
		  1.0f, 0.0f,
		  0.0f, 1.0f,
		  1.0f, 0.0f,
		  0.0f, 0.0f
		};
		/*FloatBuffer uv_coordinates = BufferUtils.createFloatBuffer(raw_uv_coordinates.length * img_count);
		for (int i = 0; i < img_count; ++i) {
			uv_coordinates.put(raw_uv_coordinates);
		}
		uv_coordinates.rewind();*/

		IntBuffer vbo_uv = stack.mallocInt(1);
		glGenBuffers(vbo_uv);
		check_gl_errors();
		glEnableVertexAttribArray(1);
		check_gl_errors();
		glBindBuffer(GL_ARRAY_BUFFER, vbo_uv.get(0));
		check_gl_errors();
		glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		check_gl_errors();
		glBufferData(GL_ARRAY_BUFFER, raw_uv_coordinates, GL_STATIC_DRAW);
		check_gl_errors();

		int texture_location = glGetUniformLocation(program_id, "text");

		for(Entry<String, GarbageImage> image : images.entrySet()) {
    	assert(image.getValue().raw_triangle_data.length == 3 * 6);

  		glBindBuffer(GL_ARRAY_BUFFER, vbo.get(0));
  		check_gl_errors();
  		glBufferData(GL_ARRAY_BUFFER, image.getValue().raw_triangle_data, GL_STATIC_DRAW);
  		check_gl_errors();

  		glUseProgram(program_id);
  		check_gl_errors();
  		glUniform1i(texture_location, image.getValue().texture_pos);
  		check_gl_errors();
  		glDrawArrays(GL_TRIANGLES, 0, 6);
  		check_gl_errors();
    }

		glfwSwapBuffers(window);

		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDeleteBuffers(vbo);
		glDeleteBuffers(vbo_uv);

		stack.pop();
	}

	@Override
	public void batchImage(String resource, int layer, int x, int y) {
		MemoryStack stack = MemoryStack.stackPush();

		GarbageImage image = images.get(resource);
		IntBuffer raw_width = stack.mallocInt(1);
		IntBuffer raw_height = stack.mallocInt(1);
		glfwGetWindowSize(window, raw_width, raw_height);
		float screen_width = raw_width.get(0);
		float screen_height = raw_height.get(0);
		batchImageScreenScaled(resource, layer, x / screen_width, y / screen_height,
				image.width / screen_width, image.height / screen_height);

		stack.pop();
	}

	@Override
	public void batchImageScaled(String resource, int layer, int x, int y, int width, int height) {
		MemoryStack stack = MemoryStack.stackPush();

		IntBuffer raw_width = stack.mallocInt(1);
		IntBuffer raw_height = stack.mallocInt(1);
		glfwGetWindowSize(window, raw_width, raw_height);
		float screen_width = raw_width.get(0);
		float screen_height = raw_height.get(0);
		batchImageScreenScaled(resource, layer, x / screen_width, y / screen_height,
				width / screen_width, height / screen_height);

		stack.pop();
	}

	@Override
	public void batchImageScreenScaled(String resource, int layer, float x, float y, float width, float height) {
		GarbageImage image = images.get(resource);
		float fixed_x = 2 * x - 1;
		float fixed_y = 2 * x - 1;
		float fixed_width = 2 * width;
		float fixed_height = 2 * height;
		batchImageRaw(image, layer, fixed_x, fixed_y, fixed_width, fixed_height);
	}
	
	private void batchImageRaw(GarbageImage image, int layer, float x, float y, float width, float height) {
		image.raw_triangle_data = new float[] {
				/* Triangle one */
				x, y, -layer / 1000f,
				x + width, y, -layer / 1000f,
				x + width, y + height, -layer / 1000f,
				/* Triangle two */
				x, y, -layer / 1000f,
				x + width, y + height, -layer / 1000f,
				x, y + height, -layer / 1000f
		};
	}

}
