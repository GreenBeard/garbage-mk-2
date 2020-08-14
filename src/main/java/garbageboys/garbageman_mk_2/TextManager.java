package garbageboys.garbageman_mk_2;

import java.util.ArrayList; 

public interface TextManager {

	/**
	 * Loads a string of text.
	 * @param text - e.g. "Print this text"
	 * @param max_height - cuts off text once height is reached
	 * @param width - goes to new line at width
	 * @param x,y - top left corner
	 * @return true on success
	 */
	
	public TextObject openText(String text, 
						float size,
						int x,
						int y,
						int max_height, 
						int width 
						);
	
	public void closeText(TextObject text_object);
	
	public void renderText(TextObject text_object);
	
	public void cleanupText();

}
