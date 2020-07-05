package garbageboys.garbageman_mk_2;

public class TextCharacter {
	int ascii_char;
	int x;
	int y;
	int width;
	int height;
	
	Render2D renderer;
	Object fontImage;
	
	public TextCharacter(int a, int x_pos, int y_pos, int w) {
		ascii_char = a;
		x = x_pos;
		y = y_pos;
		width = w;
		height = 85;
		renderer = new RendererValidation(GarbageRenderer.class);
		fontImage = renderer.loadImage("munroFont.png", x, y, width, height);
	}
}


