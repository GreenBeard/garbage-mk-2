package garbageboys.garbageman_mk_2;

/**
 * An interface for a 'Screen' object, like the title screen or inventory screen. Handles rendering on frames, and assets.
 * @author Pangur
 *
 */
public interface Screen {

	/**
	 * Call after object creation. Initializes the object.
	 * @param renderer whatever renderer you are using
	 * @param app The app you are calling this from
	 * @param soundManager The soundmanager object that this screen will interact with
	 */
	public void init(Render2D renderer, App app, SoundManager soundManager, TextManager text);
	
	/**
	 * Loads all of the assets in.
	 */
	public void loadAssets();
	
	/**
	 * Renders a frame. Needs the frame number for looping animations.
	 * @param frame
	 */
	public void renderFrame(int frame);
	
	/**
	 * Unloads all assets. Call this if you're putting the screen in the background and won't use it again soon, but want to keep the reference to it around.
	 */
	public void unloadAssets();
	
	/**
	 * Closes the screen, unloads all assets.
	 */
	public void closeScreen();

}
