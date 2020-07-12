/**
 * 
 */
package garbageboys.garbageman_mk_2;

import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;

import java.nio.IntBuffer;
import java.util.HashMap;

import org.lwjgl.system.MemoryStack;

/**
 * @author Silas
 *
 */

public class TextLoader implements TextManager {
	HashMap<Integer, TextCharacter> char_list;
	TextCharacter temp_text;
	Render2D renderer;
	int i;
	
	public TextLoader()
	{
		int bot_y = 10;
		int mid_y = 114;
		int top_y = 230;
		
		int width_1 = 19;
		int width_5 = 28;
		int width_2 = 43;
		int width_3 = 53;
		int width_4 = 63;
		int width_6 = 91;
		char_list = new HashMap<Integer, TextCharacter>(91);
		
		//this part boutta get ugly: since there is no pattern to where each letter is, i need separate info for each character :'(
		//SUPPORTED CHARACTERS:
		//ABCDEFGHIJKLMNOPQRSTUVWXYZ 
		//abcdefghijklmnopqrstuvwxyz
		//0123456789 ()!@#$%&.,?;:

		//SPACE
		temp_text = new TextCharacter(32, 1200, bot_y-10, width_3);
		char_list.put(0, temp_text);
		
		//!
		temp_text = new TextCharacter(33, 586, bot_y, width_1);
		char_list.put(1, temp_text);
				
		//#
		temp_text = new TextCharacter(35, 705, bot_y+10, width_4);
		char_list.put(3, temp_text);
		
		//$
		temp_text = new TextCharacter(36, 775, bot_y-10, width_2);
		char_list.put(4, temp_text);
		
		//%
		temp_text = new TextCharacter(37, 824, bot_y, width_6);
		char_list.put(5, temp_text);
		
		//&
		temp_text = new TextCharacter(38, 917, bot_y, 75);
		char_list.put(6, temp_text);
		
		//'
		temp_text = new TextCharacter(40, 1259, bot_y, width_1);
		char_list.put(7, temp_text);
		
		//(
		temp_text = new TextCharacter(40, 553, bot_y, width_5);
		char_list.put(8, temp_text);
		
		//)
		temp_text = new TextCharacter(41, 1144, bot_y, width_5);
		char_list.put(9, temp_text);
		
		//,
		temp_text = new TextCharacter(44, 1027, bot_y-10, width_1);
		char_list.put(12, temp_text);
		
		//.
		temp_text = new TextCharacter(46, 1003, bot_y, width_1);
		char_list.put(14, temp_text);
		
		//:
		temp_text = new TextCharacter(58, 1098, bot_y, width_1);
		char_list.put(26, temp_text);
		
		//;
		temp_text = new TextCharacter(59, 1119, bot_y, width_1);
		char_list.put(27, temp_text);
		
		//?
		temp_text = new TextCharacter(63, 1048, bot_y, width_2);
		char_list.put(31, temp_text);
		
		//@
		temp_text = new TextCharacter(64, 610, bot_y, width_6);
		char_list.put(32, temp_text);
				
		//0
		temp_text = new TextCharacter(48, 0, bot_y, width_3);
		char_list.put(16, temp_text);
		
		//1
		temp_text = new TextCharacter(49, 58, bot_y, width_5);
		char_list.put(17, temp_text);
		
		//2
		temp_text = new TextCharacter(50, 92, bot_y, width_2);
		char_list.put(18, temp_text);
		
		//3
		temp_text = new TextCharacter(51, 141, bot_y, width_2);
		char_list.put(19, temp_text);
		
		//4
		temp_text = new TextCharacter(52, 188, bot_y, width_3);
		char_list.put(20, temp_text);
		
		//5
		temp_text = new TextCharacter(53, 248, bot_y, width_2);
		char_list.put(21, temp_text);
		
		//6
		temp_text = new TextCharacter(54, 294, bot_y, width_3);
		char_list.put(22, temp_text);
		
		//7
		temp_text = new TextCharacter(55, 351, bot_y, width_2);
		char_list.put(23, temp_text);
		
		//8
		temp_text = new TextCharacter(56, 399, bot_y, width_3);
		char_list.put(24, temp_text);
		
		//9
		temp_text = new TextCharacter(57, 462, bot_y, width_3);
		char_list.put(25, temp_text);
		
		//A
		temp_text = new TextCharacter(65, 0, mid_y, width_3);
		char_list.put(33, temp_text);
		
		//B
		temp_text = new TextCharacter(66, 58, mid_y, width_3);
		char_list.put(34, temp_text);
		
		//C
		temp_text = new TextCharacter(67, 118, mid_y, width_2);
		char_list.put(35, temp_text);
		
		//D
		temp_text = new TextCharacter(68, 164, mid_y, width_3);
		char_list.put(36, temp_text);
		
		//E
		temp_text = new TextCharacter(69, 223, mid_y, width_2);
		char_list.put(37, temp_text);
		
		//F
		temp_text = new TextCharacter(70, 271, mid_y, width_2);
		char_list.put(38, temp_text);
		
		//G
		temp_text = new TextCharacter(71, 318, mid_y, width_3);
		char_list.put(39, temp_text);
		
		//H
		temp_text = new TextCharacter(72, 377, mid_y, width_3);
		char_list.put(40, temp_text);
		
		//I
		temp_text = new TextCharacter(73, 436, mid_y, width_1);
		char_list.put(41, temp_text);
		
		//J
		temp_text = new TextCharacter(74, 460, mid_y, width_5);
		char_list.put(42, temp_text);
		
		//K
		temp_text = new TextCharacter(75, 495, mid_y, width_3);
		char_list.put(43, temp_text);
		
		//L
		temp_text = new TextCharacter(76, 554, mid_y, width_2);
		char_list.put(44, temp_text);
		
		//M
		temp_text = new TextCharacter(77, 602, mid_y, width_4);
		char_list.put(45, temp_text);
		
		//N
		temp_text = new TextCharacter(78, 672, mid_y, width_3);
		char_list.put(46, temp_text);
		
		//O
		temp_text = new TextCharacter(79, 730, mid_y, width_3);
		char_list.put(47, temp_text);
		
		//P
		temp_text = new TextCharacter(80, 790, mid_y, width_3);
		char_list.put(48, temp_text);
		
		//Q
		temp_text = new TextCharacter(81, 849, mid_y, width_3);
		char_list.put(49, temp_text);
		
		//R
		temp_text = new TextCharacter(82, 908, mid_y, width_3);
		char_list.put(50, temp_text);
		
		//S
		temp_text = new TextCharacter(83, 967, mid_y, width_2);
		char_list.put(51, temp_text);
		
		//T
		temp_text = new TextCharacter(84, 1014, mid_y, width_2);
		char_list.put(52, temp_text);
		
		//U
		temp_text = new TextCharacter(85, 1062, mid_y, width_3);
		char_list.put(53, temp_text);
		
		//V
		temp_text = new TextCharacter(86, 1121, mid_y, width_4);
		char_list.put(54, temp_text);
		
		//W
		temp_text = new TextCharacter(87, 1192, mid_y, width_4);
		char_list.put(55, temp_text);
		
		//X
		temp_text = new TextCharacter(88, 1263, mid_y, width_4);
		char_list.put(56, temp_text);
		
		//Y
		temp_text = new TextCharacter(89, 1333, mid_y, width_4);
		char_list.put(57, temp_text);
		
		//Z
		temp_text = new TextCharacter(90, 1404, mid_y, width_2);
		char_list.put(58, temp_text);
		
		//a
		temp_text = new TextCharacter(97, 0, top_y, width_3);
		char_list.put(65, temp_text);
		
		//b
		temp_text = new TextCharacter(98, 59, top_y, width_3);
		char_list.put(66, temp_text);
		
		//c
		temp_text = new TextCharacter(99, 117, top_y, width_2);
		char_list.put(67, temp_text);
		
		//d
		temp_text = new TextCharacter(100, 165, top_y, width_3);
		char_list.put(68, temp_text);
		
		//e
		temp_text = new TextCharacter(101, 223, top_y, width_3);
		char_list.put(69, temp_text);
		
		//f
		temp_text = new TextCharacter(102, 283, top_y, width_5+10);
		char_list.put(70, temp_text);
		
		//g
		temp_text = new TextCharacter(103, 318, top_y-25, width_3);
		char_list.put(71, temp_text);
		
		//h
		temp_text = new TextCharacter(104, 377, top_y, width_2);
		char_list.put(72, temp_text);
		
		//i
		temp_text = new TextCharacter(105, 437, top_y, width_1);
		char_list.put(73, temp_text);
		
		//j
		temp_text = new TextCharacter(106, 453, top_y, width_5);
		char_list.put(74, temp_text);
		
		//k
		temp_text = new TextCharacter(107, 484, top_y, width_3);
		char_list.put(75, temp_text);
		
		//l
		temp_text = new TextCharacter(108, 542, top_y, width_1);
		char_list.put(76, temp_text);
		
		//m
		temp_text = new TextCharacter(109, 566, top_y, width_6);
		char_list.put(77, temp_text);
		
		//n
		temp_text = new TextCharacter(110, 662, top_y, width_3);
		char_list.put(78, temp_text);
		
		//o
		temp_text = new TextCharacter(111, 719, top_y, width_4);
		char_list.put(79, temp_text);
		
		//p
		temp_text = new TextCharacter(112, 780, top_y-25, width_3);
		char_list.put(80, temp_text);
		
		//q
		temp_text = new TextCharacter(113, 838, top_y-25, width_4);
		char_list.put(81, temp_text);
		
		//r
		temp_text = new TextCharacter(114, 897, top_y, width_2);
		char_list.put(82, temp_text);
		
		//s
		temp_text = new TextCharacter(115, 944, top_y, width_2);
		char_list.put(83, temp_text);
		
		//t
		temp_text = new TextCharacter(116, 991, top_y, width_2);
		char_list.put(84, temp_text);
		
		//u
		temp_text = new TextCharacter(117, 1039, top_y, width_3);
		char_list.put(85, temp_text);
		
		//v
		temp_text = new TextCharacter(118, 1098, top_y, width_4);
		char_list.put(86, temp_text);
		
		//w
		temp_text = new TextCharacter(119, 1168, top_y, width_6);
		char_list.put(87, temp_text);
		
		//x
		temp_text = new TextCharacter(120, 1263, top_y, width_4);
		char_list.put(88, temp_text);
		
		//y
		temp_text = new TextCharacter(121, 1333, top_y-25, width_3);
		char_list.put(89, temp_text);
		
		//z
		temp_text = new TextCharacter(122, 1392, top_y, width_2);
		char_list.put(90, temp_text);
		//PHEW NOW I KNOW MY ABC'S
	}
	
	
	
	
	@Override
	public void openText(String text, float size, int x, int y, int max_height, int width) {
		
		int i; 
		int curr_width = 0;
		int curr_height = 0;
		renderer = App.get_renderer();
		
		MemoryStack stack = MemoryStack.stackPush();
		IntBuffer window_width = stack.mallocInt(1);
		IntBuffer window_height = stack.mallocInt(1);
		glfwGetWindowSize(renderer.getWindowID(), window_width, window_height);//gets window size
		
		for(i=0;i<text.length();i++)
		{
			renderer.batchImageScreenScaled(renderer.duplicateHandle(char_list.get(text.charAt(i) - 32).fontImage), 
											2, 
											(x+curr_width) /  (float) window_width.get(0), 
											(y-curr_height) / (float) window_height.get(0), 
											(char_list.get(text.charAt(i) - 32).width * size) / window_width.get(0), 
											(char_list.get(text.charAt(i) - 32).height * size) / window_height.get(0)
											);
			curr_width += char_list.get(text.charAt(i) - 32).width * size;
			if(curr_width >= width)
			{
				curr_width = 0;
				curr_height += char_list.get(text.charAt(i) - 32).height * size;
				if(curr_height >= max_height)
					return;
				
			}
		}
		stack.pop();
	}

	@Override
	public void closeText(String text) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void cleanupText()
	{
		int i;
		for(TextCharacter textCharacter : char_list.values())
		{
				renderer.unloadImage(textCharacter.fontImage);
		}
	}
}
