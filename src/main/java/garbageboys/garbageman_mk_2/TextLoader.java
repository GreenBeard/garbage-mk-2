/**
 * 
 */
package garbageboys.garbageman_mk_2;

import java.util.ArrayList;

/**
 * @author Silas
 *
 */

public class TextLoader implements TextManager {
	ArrayList<Object> charlist = new ArrayList<Object>(75);
	
	charlist.add(10);
	
	@Override
	public void openText(String text, int max_height, int width, int x, int y) {
		// TODO Auto-generated method stub
		int i;
		char temp;//temporarily stores current char
		String resource = "munroFont.png";
		Render2D renderer;
		renderer = new RendererValidation(GarbageRenderer.class);
		
		
		for(i=0;i<text.length();i++)
		{
			Object fontImage = renderer.loadImage(resource);
			renderer.batchImageScreenScaled(fontImage, 2, x, y, int width, int height);
		}
		
	}

	@Override
	public void closeText(String text) {
		// TODO Auto-generated method stub
	}

}
