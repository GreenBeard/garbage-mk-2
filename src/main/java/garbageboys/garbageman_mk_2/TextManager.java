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
	public void openText(String text, 
							int max_height, 
							int width, 
							int x,
							int y);
	
	public void closeText(String text);
	

}
