package garbageboys.garbageman_mk_2;

import java.util.List;

public interface Render2D {

	public void initialize();
	public void cleanup();

	public long getWindowID();

	/**
	 * Returns hint to sleep to barely have frame finish rendering
	 * before display in microseconds
	 */
	public long getHintSleep();

	/**
	 * Loads an image. This style of calls should ideally be batched in order
	 * to reduce unnecessary recalculations.
	 * @param resource - e.g. "/assets/Buttons/play.png"
	 * 
	 * @return null on failure, otherwise handle
	 */
	public Object loadImage(String resource);

	public Object loadImage(String resource, int x, int y, int width, int height);

	/* Loads a set of frames from an image in book reading format (top left to bottom right) */
	public List<Object> loadImageSeries(String resource, int width, int height, int frame_count);

	/**
	 * Call after loading a set of files to prepare them for rendering.
	 */
	public void refreshImages();

	/**
	 * Unloads an image, and cleans up resources.
	 * @param resource - e.g. "/assets/Buttons/play.png"
	 */
	public void unloadImage(Object handle);

	public void renderBatchStart();
	public void renderBatchEnd();

	/**
	 * Renders an image at (x, y) in its native size
	 * @param layer - Higher is closer to the camera
	 * @param x - lower left hand corner
	 * @param y
	 */
	public void batchImage(Object image, int layer, int x, int y);

	/**
	 * Renders an image at (x, y) scaled to a certain size
	 * @param layer - Higher is closer to the camera
	 * @param x - lower left hand corner
	 * @param y
	 * @param width - size in screen pixels
	 * @param height
	 */
	public void batchImageScaled(Object image, int layer, int x, int y, int width, int height);

	/**
	 * Renders an image at (x, y) scaled to a certain size
	 * @param layer - Higher is closer to the camera
	 * @param x - lower left hand corner (0, 0) to (1, 1)
	 * @param y
	 * @param width - size relative to screen width (0 to 1)
	 * @param height - size relative to screen height (0 to 1)
	 */
	public void batchImageScreenScaled(Object image, int layer, float x, float y, float width, float height);

	public enum InteractEventType {
		LEFT_MOUSE_DOWN, LEFT_MOUSE_UP, RIGHT_MOUSE_DOWN, RIGHT_MOUSE_UP, SCROLL_UP, SCROLL_DOWN
	}

	public class InteractEvents {
		/* null if nothing matched! */
		public Object handle;
		public InteractEventType type;
		/* (0,0) is the bottom left */
		public int mouse_x;
		public int mouse_y;
	}

	/**
	 * Adds unhandled events to the provided list. (This events are then considered handled.)
	 * @param events - list to add events to
	 */
	public void fillEventList(List<InteractEvents> events);

}
