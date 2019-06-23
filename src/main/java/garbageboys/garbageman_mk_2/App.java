package garbageboys.garbageman_mk_2;

import static org.lwjgl.glfw.GLFW.*;

import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import org.lwjgl.*;
import org.lwjgl.system.MemoryStack;

public class App {

	Render2D renderer;

	public void run() {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		game_loop_start();
		renderer.cleanup();
	}

	private void init() {
		renderer = new RendererValidation(GarbageRenderer.class);
		renderer.initialize();

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(renderer.getWindowID(), (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			}
		});
	}

	private void game_loop_start() {
		Object play_button = renderer.loadImage("/assets/Buttons/play.png");
		Object crafting_screen = renderer.loadImage("/assets/Screens/craftingScreen.png");
		Object customer_a_0 = renderer.loadImage("/assets/Customers/Alan.png", 0, 0, 128, 128);
		Object customer_a_1 = renderer.loadImage("/assets/Customers/Alan.png", 128, 0, 128, 128);
		Object customer_a_2 = renderer.loadImage("/assets/Customers/Alan.png", 256, 0, 128, 128);
		Object customer_a_3 = renderer.loadImage("/assets/Customers/Alan.png", 384, 0, 128, 128);
		Object customer_b_0 = renderer.loadImage("/assets/Customers/Brett.png", 0, 0, 128, 128);
		Object customer_b_1 = renderer.loadImage("/assets/Customers/Brett.png", 128, 0, 128, 128);
		Object customer_b_2 = renderer.loadImage("/assets/Customers/Brett.png", 256, 0, 128, 128);
		Object customer_b_3 = renderer.loadImage("/assets/Customers/Brett.png", 384, 0, 128, 128);
		Object customer_c = renderer.loadImage("/assets/Customers/Dana.png");
		renderer.refreshImages();
		
		int frame = 0;
		while (!glfwWindowShouldClose(renderer.getWindowID())) {
			long start = System.nanoTime();
			MemoryStack stack = MemoryStack.stackPush();

			try {
				long sleep_time = renderer.getHintSleep() / 1000;
				Thread.sleep(sleep_time);
			} catch (InterruptedException e) { }

			long render_start = System.nanoTime();
			renderer.renderBatchStart();
			renderer.batchImageScreenScaled(crafting_screen, 0, 0f, 0f, 1f, 1f);
			renderer.batchImageScreenScaled(play_button, 1, 0.25f, 0.25f, 0.5f, 0.5f);

			int duration = 60 * 60;
			Object customer_a = null;
			switch ((frame / 30) % 4) {
				case 0:
					customer_a = customer_a_0;
					break;
				case 1:
					customer_a = customer_a_1;
					break;
				case 2:
					customer_a = customer_a_2;
					break;
				case 3:
					customer_a = customer_a_3;
					break;
			}
			float customer_a_x = (float) (frame % duration) / (duration - 1);
			Object customer_b = null;
			switch ((frame / 30) % 4) {
				case 0:
					customer_b = customer_b_0;
					break;
				case 1:
					customer_b = customer_b_1;
					break;
				case 2:
					customer_b = customer_b_2;
					break;
				case 3:
					customer_b = customer_b_3;
					break;
			}
			DoubleBuffer raw_mouse_x = stack.mallocDouble(1);
			DoubleBuffer raw_mouse_y = stack.mallocDouble(1);
			glfwGetCursorPos(renderer.getWindowID(), raw_mouse_x, raw_mouse_y);
			int mouse_x = (int) Math.floor(raw_mouse_x.get(0));
			int mouse_y = (int) Math.floor(raw_mouse_y.get(0));
			IntBuffer window_width = stack.mallocInt(1);
			IntBuffer window_height = stack.mallocInt(1);
			glfwGetWindowSize(renderer.getWindowID(), window_width, window_height);
			renderer.batchImageScreenScaled(customer_a, 2, customer_a_x, 0f, 0.125f, 0.25f);
			renderer.batchImageScaled(customer_b, 3, mouse_x - 64, window_height.get(0) - mouse_y - 64, 128, 128);
			renderer.batchImageScreenScaled(customer_c, 2, 0f, 0.5f, 0.5f, 0.25f);
			renderer.renderBatchEnd();
			long render_end = System.nanoTime();
			stack.pop();

			glfwPollEvents();

			long end = System.nanoTime();
			System.out.printf("Total Frame time %.2f ms Render Frame time %.2f ms\n",
					(end - start) / (1000f * 1000f),
					(render_end - render_start) / (1000f * 1000f));
			++frame;
		}

		renderer.unloadImage(play_button);
		renderer.unloadImage(crafting_screen);
		renderer.unloadImage(customer_a_0);
		renderer.unloadImage(customer_a_1);
		renderer.unloadImage(customer_a_2);
		renderer.unloadImage(customer_a_3);
		renderer.unloadImage(customer_b_0);
		renderer.unloadImage(customer_b_1);
		renderer.unloadImage(customer_b_2);
		renderer.unloadImage(customer_b_3);
		renderer.unloadImage(customer_c);
	}

	public static void main(String[] args) {
		new App().run();
	}

}
