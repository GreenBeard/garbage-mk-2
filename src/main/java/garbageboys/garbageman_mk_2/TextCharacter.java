package garbageboys.garbageman_mk_2;

public class TextCharacter {//provides a class for each separate character on fontImage
	int ascii_char;
	int x;
	int y;
	int width;
	int height;
	
	Object fontImage;
	
	public TextCharacter(int a, int x_pos, int y_pos, int w) {
		ascii_char = a;
		x = x_pos;
		y = y_pos;
		width = w;
		height = 85;
		fontImage = App.get_renderer().loadImage("/munroFont.png", x, y, width, height);
	}
}


