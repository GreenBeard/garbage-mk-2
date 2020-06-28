package garbageboys.garbageman_mk_2;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLXSGIVideoSync;
import org.lwjgl.system.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class GarbageRenderer implements Render2D {

	// The window handle
	private long window;
	
	private long render_wait_time;

	private long last_frame_end;

	private int program_id;
	
	private URL TitleTagsFile;

	private class GarbageHandle {
		public String file_name;
		/* (0,0) is the bottom left */
		public int x;
		public int y;
		public int full_width;
		@SuppressWarnings("unused")
		public int full_height;
	}

	private class GarbageImage {
		public int tmp_id;
		public int texture_id;
		public int texture_pos;
		/* Full image dimensions even if not fully used */
		public int width;
		public int height;
		public float[] raw_triangle_data;
		public float[] raw_uv_coordinates;

		public GarbageImage(int texture_id, int texture_pos, int width, int height) {
			this.texture_id = texture_id;
			this.texture_pos = texture_pos;
			this.raw_triangle_data = null;
			this.raw_uv_coordinates = null;
			this.width = width;
			this.height = height;
		}
	}

	private HashMap<GarbageHandle, GarbageImage> images;
	
	private void resizeCallback(long window, int width, int height) {
		glViewport(0, 0, width, height);
	}
	
	enum RenderMode {
		PLAIN,
		VSYNC,
		/* Benefits of VSYNC if above refresh rate
		 * frame tearing may occur otherwise
		 */
		VBLANK_SYNC
	}
	
	private RenderMode render_mode = RenderMode.PLAIN;

	public void setRenderMode(RenderMode render_mode) {
		if (render_mode == RenderMode.VBLANK_SYNC) {
			if (glfwExtensionSupported("GLX_SGI_video_sync")) {
				this.render_mode = render_mode;
			} else {
				System.out.println("WARNING: GLX_SGI_video_sync unsupported using default vsync");
				this.render_mode = RenderMode.VSYNC;
			}
		} else {
			this.render_mode = render_mode;
		}
		// Enable v-sync
		switch (this.render_mode) {
			case PLAIN:
			case VBLANK_SYNC:
				glfwSwapInterval(0);
				break;
			case VSYNC:
				glfwSwapInterval(1);
				break;
		}
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
		// A form of antialiasing
		glfwWindowHint(GLFW_SAMPLES, 4);

		// Create the window
		String titleText = ResourceLoader.getTitleText();
		window = glfwCreateWindow(1600, 900, titleText, NULL, NULL);
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

		// Make the window visible
		glfwShowWindow(window);
		
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		//glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(), 0, 0, vidmode.width(), vidmode.height(), GLFW_DONT_CARE);

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
		System.out.println("Monitor width: " + vidmode.width());
		System.out.println("       height: " + vidmode.height());

		// Enable depth test
		glEnable(GL_DEPTH_TEST);
		// Accept fragment if it closer to the camera than the former one
		glDepthFunc(GL_LESS);
		glEnable(GL_MULTISAMPLE);

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
		for (Entry<GarbageHandle, GarbageImage> image : images.entrySet()) {
			int pos = image.getValue().texture_pos;
			if (pos >= 0) {
				used[pos] = true;
			}
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
	public Object loadImage(String resource) {
		MemoryStack stack = stackPush();

		IntBuffer full_width = stack.mallocInt(1);
		IntBuffer full_height = stack.mallocInt(1);
		IntBuffer channels = stack.mallocInt(1);
		ResourceLoader.LoadTexture(resource, full_width, full_height, channels);

		GarbageImage image = new GarbageImage(0, -1, full_width.get(0), full_height.get(0));
		GarbageHandle handle = new GarbageHandle();
		handle.file_name = resource;
		handle.full_width = full_width.get(0);
		handle.full_height = full_height.get(0);
		handle.x = 0;
		handle.y = 0;
		images.put(handle, image);

		stack.pop();
		return handle;
	}

	@Override
	public Object loadImage(String resource, int x, int y, int width, int height) {
		MemoryStack stack = stackPush();

		IntBuffer full_width = stack.mallocInt(1);
		IntBuffer full_height = stack.mallocInt(1);
		IntBuffer channels = stack.mallocInt(1);
		ResourceLoader.LoadTexture(resource, full_width, full_height, channels);

		GarbageImage image = new GarbageImage(0, -1, width, height);
		GarbageHandle handle = new GarbageHandle();
		handle.file_name = resource;
		handle.full_width = full_width.get(0);
		handle.full_height = full_height.get(0);
		handle.x = x;
		handle.y = y;
		images.put(handle, image);

		stack.pop();
		return handle;
	}

	@SuppressWarnings("unchecked")
	public List<Object> loadImageSeries(String resource, int width, int height, int frame_count) {
		MemoryStack stack = stackPush();
	
		IntBuffer full_width = stack.mallocInt(1);
		IntBuffer full_height = stack.mallocInt(1);
		IntBuffer channels = stack.mallocInt(1);
		ResourceLoader.LoadTexture(resource, full_width, full_height, channels);

		List<GarbageHandle> handles = new ArrayList<GarbageHandle>();
		handle_loop:
		for (int j = 0; j <= full_height.get(0) - height; j += height) {
			for (int i = 0; i <= full_width.get(0) - width; i += width) {
				if (handles.size() == frame_count) {
					break handle_loop;
				}
				GarbageImage image = new GarbageImage(0, -1, width, height);
				GarbageHandle handle = new GarbageHandle();

				handle.file_name = resource;
				handle.full_width = full_width.get(0);
				handle.full_height = full_height.get(0);
				handle.x = i;
				handle.y = j;
				images.put(handle, image);
				handles.add(handle);
			}
		}

		stack.pop();
		/* Java is paranoid... */
		return (List<Object>) (Object) handles;
	}

	@Override
	public void refreshImages() {
		MemoryStack stack = stackPush();

		/* Choose largest supported image up to 2048 by 2048 to improve rendering
	   speed */
		int image_size = glGetInteger(GL_MAX_TEXTURE_SIZE);
		if (image_size > 2048) {
			image_size = 2048;
		}

		for (GarbageImage image : images.values()) {
			/* The specification says calling delete on currently unallocated images
			   is fine */
			glDeleteTextures(image.texture_id);
			/* 0 is always an invalid texture */
			image.texture_id = 0;
		}

		int border_size = 4;
		int id = 0;
		ArrayList<Rect> rects = new ArrayList<Rect>();
		for (GarbageImage image : images.values()) {
			image.tmp_id = id++;

			Rect rect = new Rect();
			rect.width = image.width + 2 * border_size;
			rect.height = image.height + 2 * border_size;
			rect.id = image.tmp_id;
			rects.add(rect);
		}
		while (true) {
			ArrayList<Rect> placed = new ArrayList<Rect>();
			RectPacker packer = new RectPacker();
			boolean complete = packer.pack(rects, placed, image_size, image_size, 1, false);
			ByteBuffer atlas_buffer = BufferUtils.createByteBuffer(image_size * image_size * 4);

			int texture_pos = next_texture_id();
			glActiveTexture(GL_TEXTURE0 + texture_pos);
			IntBuffer texture_id = stack.mallocInt(1);
			glGenTextures(texture_id);
			glBindTexture(GL_TEXTURE_2D, texture_id.get(0));

			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);
			IntBuffer channels = stack.mallocInt(1);
			for (Rect rect : placed) {
				remove_rect_by_id(rects, rect.id);
				Entry<GarbageHandle, GarbageImage> entry = find_entry_by_id(rect.id);
				ByteBuffer img_buffer = ResourceLoader.LoadTexture(entry.getKey().file_name, width, height, channels);

				ByteBuffer img_buffer_cropped = crop_image_buffer(img_buffer, entry.getKey(), entry.getValue());

				entry.getValue().texture_pos = texture_pos;
				entry.getValue().texture_id = texture_id.get(0);

				Rect img_rect = new Rect();
				img_rect.width = rect.width - 2 * border_size;
				img_rect.height = rect.height - 2 * border_size;
				img_rect.x = rect.x + border_size;
				img_rect.y = rect.y + border_size;
				img_rect.id = rect.id;
				if (entry.getValue().width == img_rect.width) {
					assert(entry.getValue().height == img_rect.height);
					place_image_rgba(entry.getValue(), atlas_buffer, image_size, image_size, img_buffer_cropped, img_rect, false);
				} else if (entry.getValue().width == img_rect.height) {
					assert(entry.getValue().height == img_rect.width);
					place_image_rgba(entry.getValue(), atlas_buffer, image_size, image_size, img_buffer_cropped, img_rect, true);
				} else {
					assert(false);
				}
			}
			atlas_buffer.rewind();
			print_atlas(atlas_buffer, image_size, image_size, texture_pos);
			atlas_buffer.rewind();

			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image_size, image_size, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlas_buffer);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

			if (complete) {
				break;
			}
		}

		stack.pop();
	}

	private ByteBuffer crop_image_buffer(ByteBuffer img_buffer, GarbageHandle key, GarbageImage value) {
		ByteBuffer buffer = BufferUtils.createByteBuffer(4 * value.width * value.height);
		for (int j = 0; j < value.height; ++j) {
			for (int i = 0; i < value.width; ++i) {
				img_buffer.position(4 * (key.x + i + key.full_width * (key.y + j)));
				/* 4 for RGBA */
				for (int k = 0; k < 4; ++k) {
					buffer.put(img_buffer.get());
				}
			}
		}
		buffer.rewind();
		return buffer;
	}

	private void remove_rect_by_id(ArrayList<Rect> rects, int id) {
		for (int i = 0; i < rects.size(); ++i) {
			if (rects.get(i).id == id) {
				rects.remove(i);
				return;
			}
		}
	}

	private Entry<GarbageHandle, GarbageImage> find_entry_by_id(int id) {
		for (Entry<GarbageHandle, GarbageImage> image : images.entrySet()) {
			if (image.getValue().tmp_id == id) {
				return image;
			}
		}
		return null;
	}

	public void print_atlas(ByteBuffer img, int width, int height, int texture_pos) {
		try {
			File file = new File("./atlas_image_" + texture_pos + ".ppm");
			BufferedOutputStream stream = new BufferedOutputStream(new  FileOutputStream(file));
			stream.write(("P6 " + width + " " + height + " 255\n").getBytes(StandardCharsets.US_ASCII));
			while (img.hasRemaining()) {
				stream.write(img.get());
				stream.write(img.get());
				stream.write(img.get());
				/* Discard alpha */
				img.get();
			}
			stream.flush();
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Places textures into an atlas
	 * if img_flipped is true the image goes from this:
	 * +----------+
	 * |          |
	 * |   .  .   |
	 * |    __    |
	 * |          |
	 * +----------+
	 * to looking like this:
	 * +----------+
	 * |          |
	 * |    | .   |
	 * |    | .   |
	 * |          |
	 * +----------+
	 */
	private void place_image_rgba(GarbageImage image,
			ByteBuffer atlas_buffer, int atlas_width, int atlas_height,
			ByteBuffer img_buffer, Rect img_rect, boolean img_flipped) {
		/* RGBA */
		int channel_count = 4;
		assert(atlas_buffer.capacity() == atlas_width * atlas_height * channel_count);
		assert(img_buffer.capacity() == img_rect.width * img_rect.height * channel_count);

		/* Image buffers in LWJGL have image origin in the top left
		 * (normally they have an origin of the bottom left).
		 * This is accounted for by the fact that it defines the
		 * (u, v) coordinate space to have the y access flipped
		 * (starting in the top left also).
		 * http://wiki.lwjgl.org/images/5/51/Coordinates.png
		 */
		for (int i = 0; i < img_rect.width; ++i) {
			for (int j = 0; j < img_rect.height; ++j) {
				atlas_buffer.position(channel_count * (img_rect.x + i + atlas_width * (img_rect.y + j)));
				if (!img_flipped) {
					img_buffer.position(channel_count * (i + img_rect.width * j));
				} else {
					img_buffer.position(channel_count * (j + img_rect.height * (img_rect.width - 1 - i)));
				}
				for (int k = 0; k < channel_count; ++k) {
					atlas_buffer.put(img_buffer.get());
				}
			}
		}

		/*
		 * The subtraction of 1 and addition of 0.5 are to account for the fact
		 * that uv coordinates are for the centers of the pixels. Therefore to
		 * render the corner pixel at (0, 0) you would use (0.5, 0.5) in *screen
		 * pixels*. Of course this must be then converted into the proper uv
		 * domain.
		 */
		float uv_width = (img_rect.width - 1.0f) / (float) atlas_width;
		float uv_height = (img_rect.height - 1.0f) / (float) atlas_height;
		/* top left */
		float uv_u = (img_rect.x + 0.5f) / (float) atlas_width;
		float uv_v = (img_rect.y + 0.5f) / (float) atlas_height;
		if (!img_flipped) {
			image.raw_uv_coordinates = new float[] {
				uv_u, uv_v + uv_height,
				uv_u + uv_width, uv_v + uv_height,
				uv_u + uv_width, uv_v,
				uv_u, uv_v + uv_height,
				uv_u + uv_width, uv_v,
			  uv_u, uv_v
			};
		} else {
			image.raw_uv_coordinates = new float[] {
					uv_u, uv_v,
					uv_u, uv_v + uv_height,
					uv_u + uv_width, uv_v + uv_height,
					uv_u, uv_v,
					uv_u + uv_width, uv_v + uv_height,
					uv_u + uv_width, uv_v
				};
		}
	}
	
	GarbageHandle find_image_handle(String file_name) {
		for (GarbageHandle handle : images.keySet()) {
			if (handle.file_name == file_name) {
				return handle;
			}
		}
		return null;
	}

	Entry<GarbageHandle, GarbageImage> find_image_entry(String file_name) {
		for (Entry<GarbageHandle, GarbageImage> image : images.entrySet()) {
			if (image.getKey().file_name == file_name) {
				return image;
			}
		}
		return null;
	}

	@Override
	public void unloadImage(Object handle) {
		images.remove(handle);
	}

	@Override
	public void renderBatchStart() {
		// TODO improve
    for(Entry<GarbageHandle, GarbageImage> image : images.entrySet()) {
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

		int texture_location = glGetUniformLocation(program_id, "text");

		ArrayList<GarbageImage> sorted_images = sort_garbage_images(images.values());
		if (sorted_images.size() > 0) {
			int texture_pos = sorted_images.get(0).texture_pos;
			int i = 0;
			ArrayList<Float> uv_coords = new ArrayList<Float>();
			ArrayList<Float> triangle_coords = new ArrayList<Float>();
			while (true) {
				if (i == sorted_images.size() || texture_pos != sorted_images.get(i).texture_pos) {
		  		FloatBuffer uv_buffer = BufferUtils.createFloatBuffer(uv_coords.size());
		  		for (Float f : uv_coords) {
		  			uv_buffer.put(f.floatValue());
		  		}
		  		uv_buffer.rewind();
		  		FloatBuffer triangle_buffer = BufferUtils.createFloatBuffer(triangle_coords.size());
		  		for (Float f : triangle_coords) {
		  			triangle_buffer.put(f.floatValue());
		  		}
		  		triangle_buffer.rewind();

					glBindBuffer(GL_ARRAY_BUFFER, vbo_uv.get(0));
		  		check_gl_errors();
		  		glBufferData(GL_ARRAY_BUFFER, uv_buffer, GL_STATIC_DRAW);
		  		check_gl_errors();
		  		glBindBuffer(GL_ARRAY_BUFFER, vbo.get(0));
		  		check_gl_errors();
		  		glBufferData(GL_ARRAY_BUFFER, triangle_buffer, GL_STATIC_DRAW);
		  		check_gl_errors();

		  		glUseProgram(program_id);
		  		check_gl_errors();
		  		glUniform1i(texture_location, texture_pos);
		  		check_gl_errors();
		  		glDrawArrays(GL_TRIANGLES, 0, triangle_coords.size());
		  		check_gl_errors();

		  		uv_coords.clear();
		  		triangle_coords.clear();
		  		if (i == sorted_images.size()) {
		  			break;
		  		} else {
		  			texture_pos = sorted_images.get(i).texture_pos;
		  		}
				} else {
					GarbageImage image = sorted_images.get(i);
		    	assert(image.raw_triangle_data.length == 3 * 6);
		    	assert(image.raw_uv_coordinates.length == 2 * 6);
					for (float f : image.raw_uv_coordinates) {
						uv_coords.add(f);
					}
					for (float f : image.raw_triangle_data) {
						triangle_coords.add(f);
					}
					++i;
				}
			}
		}
		/*for(Entry<GarbageHandle, GarbageImage> image : images.entrySet()) {

  		glBindBuffer(GL_ARRAY_BUFFER, vbo_uv.get(0));
  		check_gl_errors();
  		glBufferData(GL_ARRAY_BUFFER, image.getValue().raw_uv_coordinates, GL_STATIC_DRAW);
  		check_gl_errors();
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
    }*/

		long time_now = System.nanoTime();
		long frame_time = time_now - last_frame_end;
		long extra_frame_time = render_wait_time + 1000 * 1000 * 1000 / 60 - frame_time;

		glFinish();
		//long swap_start = System.nanoTime();
		glfwSwapBuffers(window);
		glFinish();
		IntBuffer count = stack.mallocInt(1);
		if (render_mode == RenderMode.VBLANK_SYNC) {
			GLXSGIVideoSync.glXWaitVideoSyncSGI(1, 0, count);
		}

		last_frame_end = System.nanoTime();
		//long swap_end = System.nanoTime();
		//long real_swap_extra = swap_end - swap_start + render_wait_time;
		setHintSleep((long) (0.8 * extra_frame_time));
		System.out.printf("Render frame time %6.2f ms Next Delay %6.2f ms\n",
				frame_time / (1000f * 1000f),
				render_wait_time / (1000f * 1000f));

		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDeleteBuffers(vbo);
		glDeleteBuffers(vbo_uv);

		stack.pop();
	}

	private ArrayList<GarbageImage> sort_garbage_images(Collection<GarbageImage> values) {
		ArrayList<GarbageImage> images = new ArrayList<GarbageImage>();
		images.addAll(values);
		images.sort(new Comparator<GarbageImage>() {
			@Override
			public int compare(GarbageImage a, GarbageImage b) {
				if (a.texture_pos == b.texture_pos) {
					return 0;
				} else if (a.texture_pos > b.texture_pos) {
					return 1;
				} else {
					return -1;
				}
			}
		});
		return images;
	}

	@Override
	public void batchImage(Object handle, int layer, int x, int y) {
		MemoryStack stack = MemoryStack.stackPush();

		GarbageImage image = images.get(handle);
		IntBuffer raw_width = stack.mallocInt(1);
		IntBuffer raw_height = stack.mallocInt(1);
		glfwGetWindowSize(window, raw_width, raw_height);
		float screen_width = raw_width.get(0);
		float screen_height = raw_height.get(0);
		batchImageScreenScaled(handle, layer, x / screen_width, y / screen_height,
				image.width / screen_width, image.height / screen_height);

		stack.pop();
	}

	@Override
	public void batchImageScaled(Object handle, int layer, int x, int y, int width, int height) {
		MemoryStack stack = MemoryStack.stackPush();

		IntBuffer raw_width = stack.mallocInt(1);
		IntBuffer raw_height = stack.mallocInt(1);
		glfwGetWindowSize(window, raw_width, raw_height);
		float screen_width = raw_width.get(0);
		float screen_height = raw_height.get(0);
		batchImageScreenScaled(handle, layer, x / screen_width, y / screen_height,
				width / screen_width, height / screen_height);

		stack.pop();
	}

	@Override
	public void batchImageScreenScaled(Object handle, int layer, float x, float y, float width, float height) {
		GarbageImage image = images.get(handle);
		float fixed_x = 2 * x - 1;
		float fixed_y = 2 * y - 1;
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

	private void setHintSleep(long wait_time) {
		if (render_mode != RenderMode.PLAIN) {
			long wait_cap = (long) (0.9f * 1 / 60f * 1000 * 1000 * 1000);
			if (wait_time > wait_cap) {
				render_wait_time = wait_cap;
			} else if (wait_time > 0) {
				render_wait_time = wait_time;
			} else {
				render_wait_time = 0;
			}
		} else {
			render_wait_time = 0;
		}
	}

	@Override
	public long getHintSleep() {
		return render_wait_time / 1000;
	}
	
	

}
