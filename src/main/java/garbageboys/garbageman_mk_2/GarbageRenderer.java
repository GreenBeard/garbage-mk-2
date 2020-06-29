package garbageboys.garbageman_mk_2;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLXSGIVideoSync;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

	/*
	 * GarbageImageID - A structure to quickly identify, and avoid duplication of textures in the atlas
	 * AtlasInfo - used to hold temporal texture atlas data for a GarbageImageID (used in rendering)
	 * GarbageHandle - used to render a specific copy of an image, and handle events (such as clicks)
	 *
	 * Code must maintain all GarbaeeImageID that are equal to have the same atlas_info in this class.
	 * Meaning that code must first check if it is in atlas_images.
	 */

	private class AtlasInfo {
		/* A texture is referred to through an ID and is bound to a texture unit.
		 * It seems redudant to me, but that is just how openGL was made. I wonder if a texture may be
		 * bound, and filled with data then rebound later to a texture unit if it wasn't deleted?
		 */
		public int texture_id;
		public int texture_unit;
		public float[] raw_uv_coordinates;
		/* reference count */
		public int ref_count;
	}

	private class GarbageImageID {
		public String file_name;
		/* (0,0) is the bottom left */
		public int x;
		public int y;
		/* Full file image dimensions even if not fully used (for example a series of images, or clipped) */
		public int full_width;
		public int full_height;
		/* Image dimensions of rendered section */
		public int width;
		public int height;
		
		public GarbageImageID(String file_name, int x, int y, int full_width, int full_height, int width, int height) {
			this.file_name = file_name;
			this.x = x;
			this.y = y;
			this.full_width = full_width;
			this.full_height = full_height;
			this.width = width;
			this.height = height;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + file_name.hashCode();
			result = prime * result + full_height;
			result = prime * result + full_width;
			result = prime * result + height;
			result = prime * result + width;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj instanceof GarbageImageID){
				GarbageImageID other = (GarbageImageID) obj;
				return other.x == this.x
						&& other.y == this.y
						&& other.full_width == this.full_width
						&& other.full_height == this.full_height
						&& other.width == this.width
						&& other.height == this.height
						&& other.file_name.equals(this.file_name);
			} else {
				return false;
			}
		}
	}

	private class GarbageHandle {
		/* Has events propagate downward until the object handles that event */
		@SuppressWarnings("unused")
		public boolean scrollable;
		public boolean clickable;

		public float[] raw_triangle_data;
		public GarbageImageID image;
	}

	/* image id, reference count - allows for rendering a single object multiple times, but only
	 * placing it in the texture atlas once.
	 */
	private HashMap<GarbageImageID, AtlasInfo> atlas_images;

	/* Allows for easily rendering atlas_images multiple times in a single frame. */
	/* Sorted in refreshImages, must be sorted before renderBatchEnd */
	private ArrayList<GarbageHandle> sorted_image_handles;

	private Lock unhandled_events_lock;
	private ArrayList<InteractEvents> unhandled_events;

	@Override
	public void fillEventList(List<InteractEvents> events) {
		unhandled_events_lock.lock();
		try {
			while (unhandled_events.size() > 0) {
				events.add(unhandled_events.remove(0));
			}
		} finally {
			unhandled_events_lock.unlock();
		}
	}

	private void resizeCallback(long window, int width, int height) {
		glViewport(0, 0, width, height);
	}

	private boolean inRectangle(GarbageHandle handle, int mouse_x, int mouse_y, int window_width, int window_height) {
		float mouse_x_f = (float) mouse_x / window_width;
		float mouse_y_f = (float) mouse_y / window_height;
		float x_lower = handle.raw_triangle_data[0];
		float y_lower = handle.raw_triangle_data[1];
		float x_upper = handle.raw_triangle_data[6];
		float y_upper = handle.raw_triangle_data[7];
		if (x_lower <= mouse_x_f && mouse_x_f < x_upper
				&& y_lower <= mouse_y_f && mouse_y_f < y_upper) {
			return true;
		} else {
			return false;
		}
	}

	protected void mouseCallback(long window, int button, int action, int mods) {
		InteractEventType type;
		switch (action) {
		case GLFW_PRESS:
			switch (button) {
			case GLFW_MOUSE_BUTTON_1:
				type = InteractEventType.LEFT_MOUSE_DOWN;
				break;
			case GLFW_MOUSE_BUTTON_2:
				type = InteractEventType.RIGHT_MOUSE_DOWN;
				break;
			default:
				return;
			}
			break;
		case GLFW_RELEASE:
			switch (button) {
			case GLFW_MOUSE_BUTTON_1:
				type = InteractEventType.LEFT_MOUSE_UP;
				break;
			case GLFW_MOUSE_BUTTON_2:
				type = InteractEventType.RIGHT_MOUSE_UP;
				break;
			default:
				return;
			}
			break;
		default:
			return;
		}
		MemoryStack stack = MemoryStack.stackPush();
		DoubleBuffer x_pos = stack.mallocDouble(1);
		DoubleBuffer y_pos = stack.mallocDouble(1);
		glfwGetCursorPos(window, x_pos, y_pos);
		IntBuffer window_width = stack.mallocInt(1);
		IntBuffer window_height = stack.mallocInt(1);
		glfwGetWindowSize(window, window_width, window_height);
		unhandled_events_lock.lock();
		try {
			InteractEvents event = new InteractEvents();
			event.handle = null;
			event.mouse_x = (int) x_pos.get(0);
			event.mouse_y = window_height.get(0) - (int) y_pos.get(0);
			event.type = type;
			/* Requires sorted_image_handles to be sorted */
			for (GarbageHandle handle : sorted_image_handles) {
				if (handle.clickable && inRectangle(handle, event.mouse_x, event.mouse_y, window_width.get(0), window_height.get(0))) {
					event.handle = handle;
					break;
				}
			}
			unhandled_events.add(event);
		} finally {
			unhandled_events_lock.unlock();
		}
		stack.pop();
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

		glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				mouseCallback(window, button, action, mods);
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
		/* TODO: call glDeleteVertexArrays */

		atlas_images = new HashMap<>();

		sorted_image_handles = new ArrayList<>();

		unhandled_events_lock = new ReentrantLock();
		unhandled_events = new ArrayList<>();

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

		GL.setCapabilities(null);
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
		for (AtlasInfo atlas_info : atlas_images.values()) {
			int pos = atlas_info.texture_unit;
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

	/* width = -1 for full_width (same for height) */
	private GarbageHandle genericLoadImage(String resource, int x, int y, int width, int height) {
		MemoryStack stack = stackPush();

		IntBuffer full_width = stack.mallocInt(1);
		IntBuffer full_height = stack.mallocInt(1);
		IntBuffer channels = stack.mallocInt(1);
		ByteBuffer texture = ResourceLoader.LoadTexture(resource, full_width, full_height, channels);
		STBImage.stbi_image_free(texture);

		if (width == -1) {
			width = full_width.get(0);
		}
		if (height == -1) {
			height = full_height.get(0);
		}

		GarbageImageID image_id = new GarbageImageID(resource, x, y, full_width.get(0), full_height.get(0), width, height);
		GarbageHandle handle = new GarbageHandle();
		handle.image = image_id;

		AtlasInfo atlas_info = atlas_images.get(image_id);
		if (atlas_info != null) {
			++atlas_info.ref_count;
		} else {
			atlas_info = new AtlasInfo();
			atlas_info.ref_count = 1;
			atlas_images.put(image_id, atlas_info);
			sorted_image_handles.add(handle);
		}

		stack.pop();
		return handle;
	}

	@Override
	public Object loadImage(String resource) {
		return genericLoadImage(resource, 0, 0, -1, -1);
	}

	@Override
	public Object loadImage(String resource, int x, int y, int width, int height) {
		return genericLoadImage(resource, x, y, width, height);
	}

	@SuppressWarnings("unchecked")
	public List<Object> loadImageSeries(String resource, int width, int height, int frame_count) {
		MemoryStack stack = stackPush();

		IntBuffer full_width = stack.mallocInt(1);
		IntBuffer full_height = stack.mallocInt(1);
		IntBuffer channels = stack.mallocInt(1);
		ByteBuffer texture = ResourceLoader.LoadTexture(resource, full_width, full_height, channels);
		STBImage.stbi_image_free(texture);

		List<GarbageHandle> handles = new ArrayList<GarbageHandle>();
		handle_loop:
		for (int j = 0; j <= full_height.get(0) - height; j += height) {
			for (int i = 0; i <= full_width.get(0) - width; i += width) {
				if (handles.size() == frame_count) {
					break handle_loop;
				}
				GarbageHandle handle = genericLoadImage(resource, i, j, width, height);
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

		for (AtlasInfo image : atlas_images.values()) {
			/* The specification says calling delete on currently unallocated images
			   is fine */
			glDeleteTextures(image.texture_id);
			/* 0 is always an invalid texture */
			image.texture_id = 0;
		}

		final int border_size = 4;
		ArrayList<Rect> rects = new ArrayList<Rect>();
		for (Entry<GarbageImageID, AtlasInfo> entry : atlas_images.entrySet()) {
			Rect rect = new Rect();
			rect.width = entry.getKey().width + 2 * border_size;
			rect.height = entry.getKey().height + 2 * border_size;
			rect.user_data = entry;
			rects.add(rect);
		}
		while (true) {
			ArrayList<Rect> placed = new ArrayList<Rect>();
			RectPacker packer = new RectPacker();
			boolean complete = packer.pack(rects, placed, image_size, image_size, 1, false);
			ByteBuffer atlas_buffer = BufferUtils.createByteBuffer(image_size * image_size * 4);

			int texture_unit = next_texture_id();
			glActiveTexture(GL_TEXTURE0 + texture_unit);
			IntBuffer texture_id = stack.mallocInt(1);
			glGenTextures(texture_id);
			glBindTexture(GL_TEXTURE_2D, texture_id.get(0));

			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);
			IntBuffer channels = stack.mallocInt(1);
			for (Rect rect : placed) {
				remove_rect_by_user_data(rects, rect.user_data);
				@SuppressWarnings("unchecked")
				Entry<GarbageImageID, AtlasInfo> entry = (Entry<GarbageImageID, AtlasInfo>) rect.user_data;
				ByteBuffer img_buffer = ResourceLoader.LoadTexture(entry.getKey().file_name, width, height, channels);

				ByteBuffer img_buffer_cropped = crop_image_buffer(img_buffer, entry.getKey());
				img_buffer.rewind();
				STBImage.stbi_image_free(img_buffer);

				entry.getValue().texture_unit = texture_unit;
				entry.getValue().texture_id = texture_id.get(0);

				Rect img_rect = new Rect();
				img_rect.width = rect.width - 2 * border_size;
				img_rect.height = rect.height - 2 * border_size;
				img_rect.x = rect.x + border_size;
				img_rect.y = rect.y + border_size;
				if (entry.getKey().width == img_rect.width) {
					assert(entry.getKey().height == img_rect.height);
					place_image_rgba(entry.getValue(), atlas_buffer, image_size, image_size, img_buffer_cropped, img_rect, false);
				} else if (entry.getKey().width == img_rect.height) {
					assert(entry.getKey().height == img_rect.width);
					place_image_rgba(entry.getValue(), atlas_buffer, image_size, image_size, img_buffer_cropped, img_rect, true);
				} else {
					assert(false);
				}
			}
			atlas_buffer.rewind();
			print_atlas(atlas_buffer, image_size, image_size, texture_unit);
			atlas_buffer.rewind();

			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image_size, image_size, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlas_buffer);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

			if (complete) {
				break;
			}
		}

		sort_garbage_handles(sorted_image_handles);

		stack.pop();
	}

	private ByteBuffer crop_image_buffer(ByteBuffer img_buffer, GarbageImageID img_info) {
		ByteBuffer buffer = BufferUtils.createByteBuffer(4 * img_info.width * img_info.height);
		for (int j = 0; j < img_info.height; ++j) {
			for (int i = 0; i < img_info.width; ++i) {
				img_buffer.position(4 * (img_info.x + i + img_info.full_width * (img_info.y + j)));
				/* 4 for RGBA */
				for (int k = 0; k < 4; ++k) {
					buffer.put(img_buffer.get());
				}
			}
		}
		buffer.rewind();
		return buffer;
	}

	private void remove_rect_by_user_data(ArrayList<Rect> rects, Object user_data) {
		for (int i = 0; i < rects.size(); ++i) {
			if (rects.get(i).user_data == user_data) {
				rects.remove(i);
				return;
			}
		}
	}

	public void print_atlas(ByteBuffer img, int width, int height, int texture_unit) {
		try {
			File file = new File("./atlas_image_" + texture_unit + ".ppm");
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
	private void place_image_rgba(AtlasInfo atlas_info,
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
			atlas_info.raw_uv_coordinates = new float[] {
				uv_u, uv_v + uv_height,
				uv_u + uv_width, uv_v + uv_height,
				uv_u + uv_width, uv_v,
				uv_u, uv_v + uv_height,
				uv_u + uv_width, uv_v,
			  uv_u, uv_v
			};
		} else {
			atlas_info.raw_uv_coordinates = new float[] {
					uv_u, uv_v,
					uv_u, uv_v + uv_height,
					uv_u + uv_width, uv_v + uv_height,
					uv_u, uv_v,
					uv_u + uv_width, uv_v + uv_height,
					uv_u + uv_width, uv_v
				};
		}
	}

	@Override
	public void unloadImage(Object raw_handle) {
		GarbageHandle handle = (GarbageHandle) raw_handle;
		AtlasInfo atlas_info = atlas_images.get(handle.image);
		--atlas_info.ref_count;
		if (atlas_info.ref_count == 0) {
			atlas_images.remove(handle.image);
			/* TODO: Check to delete texture_id if not in use */
		}
		sorted_image_handles.remove(handle);
	}

	@Override
	public void renderBatchStart() {
		unhandled_events_lock.lock();
		// TODO improve
    for (GarbageHandle handle : sorted_image_handles) {
    	handle.raw_triangle_data = new float[] {
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
		unhandled_events_lock.unlock();
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

		if (sorted_image_handles.size() > 0) {
			int texture_unit = atlas_images.get(sorted_image_handles.get(0).image).texture_unit;
			int i = 0;
			ArrayList<Float> uv_coords = new ArrayList<Float>();
			ArrayList<Float> triangle_coords = new ArrayList<Float>();
			while (true) {
				/* Only initialized to null to make the dumb Java compiler happy,
				 * it will always have a value before used */
				AtlasInfo atlas_info = null;
				if (i == sorted_image_handles.size()
						|| texture_unit != (atlas_info = atlas_images.get(sorted_image_handles.get(i).image)).texture_unit) {
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
		  		glUniform1i(texture_location, texture_unit);
		  		check_gl_errors();
		  		glDrawArrays(GL_TRIANGLES, 0, triangle_coords.size());
		  		check_gl_errors();

		  		uv_coords.clear();
		  		triangle_coords.clear();
		  		if (i == sorted_image_handles.size()) {
		  			break;
		  		} else {
		  			texture_unit = atlas_info.texture_unit;
		  		}
				} else {
					GarbageHandle handle = sorted_image_handles.get(i);
		    	assert(handle.raw_triangle_data.length == 3 * 6);
		    	assert(atlas_info.raw_uv_coordinates.length == 2 * 6);
					for (float f : handle.raw_triangle_data) {
						triangle_coords.add(f);
					}
					for (float f : atlas_info.raw_uv_coordinates) {
						uv_coords.add(f);
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
  		glUniform1i(texture_location, image.getValue().texture_unit);
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
		//System.out.printf("Render frame time %6.2f ms Next Delay %6.2f ms\n",
		//		frame_time / (1000f * 1000f),
		//		render_wait_time / (1000f * 1000f));

		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDeleteBuffers(vbo);
		glDeleteBuffers(vbo_uv);

		stack.pop();
	}

	private ArrayList<GarbageHandle> sort_garbage_handles(ArrayList<GarbageHandle> handles) {
		handles.sort(new Comparator<GarbageHandle>() {
			@Override
			public int compare(GarbageHandle a, GarbageHandle b) {
				/* TODO: Yikes make this faster (single lookup for each initially?) */
				AtlasInfo a_info = atlas_images.get(a.image);
				AtlasInfo b_info = atlas_images.get(b.image);
				if (a_info.texture_unit == b_info.texture_unit) {
					return 0;
				} else if (a_info.texture_unit > b_info.texture_unit) {
					return 1;
				} else {
					return -1;
				}
			}
		});
		return handles;
	}

	@Override
	public void batchImage(Object raw_handle, int layer, int x, int y) {
		GarbageHandle handle = (GarbageHandle) raw_handle;

		MemoryStack stack = MemoryStack.stackPush();

		IntBuffer raw_width = stack.mallocInt(1);
		IntBuffer raw_height = stack.mallocInt(1);
		glfwGetWindowSize(window, raw_width, raw_height);
		float screen_width = raw_width.get(0);
		float screen_height = raw_height.get(0);
		batchImageScreenScaled(handle, layer, x / screen_width, y / screen_height,
				handle.image.width / screen_width, handle.image.height / screen_height);

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
	public void batchImageScreenScaled(Object raw_handle, int layer, float x, float y, float width, float height) {
		GarbageHandle handle = (GarbageHandle) raw_handle;

		float fixed_x = 2 * x - 1;
		float fixed_y = 2 * y - 1;
		float fixed_width = 2 * width;
		float fixed_height = 2 * height;
		batchImageRaw(handle, layer, fixed_x, fixed_y, fixed_width, fixed_height);
	}
	
	private void batchImageRaw(GarbageHandle handle, int layer, float x, float y, float width, float height) {
		handle.raw_triangle_data = new float[] {
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
