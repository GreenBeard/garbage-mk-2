package garbageboys.garbageman_mk_2;

import java.util.HashMap;

public class TextObject {
	String text; 
	float size;
	int x;
	int y;
	int max_height; 
	int width;
	
	public TextObject(String init_text, float init_size, int init_x, int init_y, int init_max_height, int init_width) 
	{
		text = init_text;
		size = init_size;
		x = init_x;
		y = init_y;
		max_height = init_max_height;
		width = init_width;
		
	}	
}
