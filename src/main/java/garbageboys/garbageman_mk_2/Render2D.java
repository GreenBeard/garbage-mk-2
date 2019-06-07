package garbageboys.garbageman_mk_2;

public interface Render2D {

	/**
	 * Loads an image. This style of calls should ideally be batched in order
	 * to reduce unnecessary recalculations.
	 * @param resource - e.g. "/assets/Buttons/play.png"
	 * 
	 * @return true on success
	 */
	public boolean loadImage(String resource);
	
	/**
	 * Call after loading a set of files to prepare them for rendering.
	 */
	public void refreshImages();

	/**
	 * Unloads an image, and cleans up resources.
	 * @param resource - e.g. "/assets/Buttons/play.png"
	 * 
	 * @return true on success
	 */
	public boolean unloadImage(String resource);

	public void renderBatchStart();
	public void renderBatchEnd();

	/**
	 * Renders an image at (x, y) in its native size
	 * @param layer - Higher is closer to the camera
	 * @param x - lower left hand corner
	 * @param y
	 */
	public void batchImage(String resource, int layer, int x, int y);

	/**
	 * Renders an image at (x, y) scaled to a certain size
	 * @param layer - Higher is closer to the camera
	 * @param x - lower left hand corner
	 * @param y
	 * @param width - size in screen pixels
	 * @param height
	 */
	public void batchImageScaled(String resource, int layer, int x, int y, int width, int height);

	/**
	 * Renders an image at (x, y) scaled to a certain size
	 * @param layer - Higher is closer to the camera
	 * @param x - lower left hand corner (0, 0) to (1, 1)
	 * @param y
	 * @param width - size relative to screen width (0 to 1)
	 * @param height - size relative to screen height (0 to 1)
	 */
	public void batchImageScreenScaled(String resource, int layer, float x, float y, float width, float height);

}
