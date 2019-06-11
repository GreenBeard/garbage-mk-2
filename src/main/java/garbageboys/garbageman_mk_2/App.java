package garbageboys.garbageman_mk_2;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

import org.lwjgl.*;

public class App {

	Render2D renderer;

	public void run() {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		game_loop_start();
		renderer.cleanup();
	}

	private void init() {
		renderer = new GarbageRenderer();
		renderer.initialize();

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(renderer.getWindowID(), (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			}
		});
	}

	private void game_loop_start() {
		String play_button = "/assets/Buttons/play.png";
		String crafting_screen = "/assets/Screens/craftingScreen.png";
		renderer.loadImage(play_button);
		renderer.loadImage(crafting_screen);
		renderer.refreshImages();
		while (!glfwWindowShouldClose(renderer.getWindowID())) {
			long start = System.nanoTime();
			renderer.renderBatchStart();
			renderer.batchImageScreenScaled(crafting_screen, 0, 0f, 0f, 1f, 1f);
			renderer.batchImageScreenScaled(play_button, 1, 0.25f, 0.25f, 0.5f, 0.5f);
			renderer.renderBatchEnd();
			glfwPollEvents();
			long end = System.nanoTime();
			System.out.printf("%.2f ms\n", (end - start) / (1000f * 1000f));
		}
		renderer.unloadImage(play_button);
		renderer.unloadImage(crafting_screen);
	}

	public static void main(String[] args) {
		new App().run();
	}

}
