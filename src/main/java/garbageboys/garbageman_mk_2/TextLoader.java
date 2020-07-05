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
	ArrayList<TextCharacter> char_list;
	TextCharacter temp_text;
	Render2D renderer;
	int i;
	
	public TextLoader()
	{
		int bot_y = 305;
		int mid_y = 201;
		int top_y = 85;
		int width_1 = 16;
		int width_5 = 25;
		int width_2 = 40;
		int width_3 = 50;
		int width_4 = 60;
		int width_6 = 88;
		char_list = new ArrayList<TextCharacter>(91);
		
		//this part boutta get ugly: since there is no pattern to where each letter is, i need separate info for each character :'(
		//SUPPORTED CHARACTERS:
		//ABCDEFGHIJKLMNOPQRSTUVWXYZ 
		//abcdefghijklmnopqrstuvwxyz
		//0123456789 ()!@#$%&.,?;:

		//SPACE
		temp_text = new TextCharacter(32, 1200, bot_y+10, width_3);
		char_list.add(0, temp_text);
		
		//!
		temp_text = new TextCharacter(33, 586, bot_y, width_1);
		char_list.add(1, temp_text);
		
		//#
		temp_text = new TextCharacter(35, 705, bot_y-10, width_4);
		char_list.add(3, temp_text);
		
		//$
		temp_text = new TextCharacter(36, 775, bot_y+10, width_2);
		char_list.add(4, temp_text);
		
		//%
		temp_text = new TextCharacter(37, 824, bot_y, width_6);
		char_list.add(5, temp_text);
		
		//&
		temp_text = new TextCharacter(38, 917, bot_y, 75);
		char_list.add(6, temp_text);
		
		//(
		temp_text = new TextCharacter(40, 553, bot_y, width_5);
		char_list.add(8, temp_text);
		
		//)
		temp_text = new TextCharacter(41, 1144, bot_y, width_5);
		char_list.add(9, temp_text);
		
		//,
		temp_text = new TextCharacter(44, 1027, bot_y+10, width_1);
		char_list.add(12, temp_text);
		
		//.
		temp_text = new TextCharacter(46, 1003, bot_y, width_1);
		char_list.add(14, temp_text);
		
		//:
		temp_text = new TextCharacter(58, 1098, bot_y, width_1);
		char_list.add(26, temp_text);
		
		//;
		temp_text = new TextCharacter(59, 1119, bot_y, width_1);
		char_list.add(27, temp_text);
		
		//?
		temp_text = new TextCharacter(63, 1048, bot_y, width_2);
		char_list.add(31, temp_text);
		
		//@
		temp_text = new TextCharacter(64, 610, bot_y, width_6);
		char_list.add(32, temp_text);
				
		//0
		temp_text = new TextCharacter(48, 0, bot_y, width_3);
		char_list.add(16, temp_text);
		
		//1
		temp_text = new TextCharacter(49, 58, bot_y, width_5);
		char_list.add(17, temp_text);
		
		//2
		temp_text = new TextCharacter(50, 92, bot_y, width_2);
		char_list.add(18, temp_text);
		
		//3
		temp_text = new TextCharacter(51, 141, bot_y, width_2);
		char_list.add(19, temp_text);
		
		//4
		temp_text = new TextCharacter(52, 188, bot_y, width_3);
		char_list.add(20, temp_text);
		
		//5
		temp_text = new TextCharacter(53, 248, bot_y, width_2);
		char_list.add(21, temp_text);
		
		//6
		temp_text = new TextCharacter(54, 294, bot_y, width_3);
		char_list.add(22, temp_text);
		
		//7
		temp_text = new TextCharacter(55, 351, bot_y, width_2);
		char_list.add(23, temp_text);
		
		//8
		temp_text = new TextCharacter(56, 399, bot_y, width_3);
		char_list.add(24, temp_text);
		
		//9
		temp_text = new TextCharacter(57, 351, bot_y, width_3);
		char_list.add(25, temp_text);
		
		//A
		temp_text = new TextCharacter(65, 0, mid_y, width_3);
		char_list.add(33, temp_text);
		
		//B
		temp_text = new TextCharacter(66, 58, mid_y, width_3);
		char_list.add(34, temp_text);
		
		//C
		temp_text = new TextCharacter(67, 118, mid_y, width_2);
		char_list.add(35, temp_text);
		
		//D
		temp_text = new TextCharacter(68, 0, mid_y, width_3);
		char_list.add(36, temp_text);
		
		//E
		temp_text = new TextCharacter(69, 223, mid_y, width_2);
		char_list.add(37, temp_text);
		
		//F
		temp_text = new TextCharacter(70, 271, mid_y, width_2);
		char_list.add(38, temp_text);
		
		//G
		temp_text = new TextCharacter(71, 318, mid_y, width_3);
		char_list.add(39, temp_text);
		
		//H
		temp_text = new TextCharacter(72, 377, mid_y, width_3);
		char_list.add(40, temp_text);
		
		//I
		temp_text = new TextCharacter(73, 436, mid_y, width_1);
		char_list.add(41, temp_text);
		
		//J
		temp_text = new TextCharacter(74, 460, mid_y, width_5);
		char_list.add(42, temp_text);
		
		//K
		temp_text = new TextCharacter(75, 495, mid_y, width_3);
		char_list.add(43, temp_text);
		
		//L
		temp_text = new TextCharacter(76, 554, mid_y, width_2);
		char_list.add(44, temp_text);
		
		//M
		temp_text = new TextCharacter(77, 602, mid_y, width_4);
		char_list.add(45, temp_text);
		
		//N
		temp_text = new TextCharacter(78, 672, mid_y, width_3);
		char_list.add(46, temp_text);
		
		//O
		temp_text = new TextCharacter(79, 730, mid_y, width_3);
		char_list.add(47, temp_text);
		
		//P
		temp_text = new TextCharacter(80, 790, mid_y, width_3);
		char_list.add(48, temp_text);
		
		//Q
		temp_text = new TextCharacter(81, 849, mid_y, width_3);
		char_list.add(49, temp_text);
		
		//R
		temp_text = new TextCharacter(82, 908, mid_y, width_3);
		char_list.add(50, temp_text);
		
		//S
		temp_text = new TextCharacter(83, 967, mid_y, width_2);
		char_list.add(51, temp_text);
		
		//T
		temp_text = new TextCharacter(84, 1014, mid_y, width_2);
		char_list.add(52, temp_text);
		
		//U
		temp_text = new TextCharacter(85, 1062, mid_y, width_3);
		char_list.add(53, temp_text);
		
		//V
		temp_text = new TextCharacter(86, 1121, mid_y, width_4);
		char_list.add(54, temp_text);
		
		//W
		temp_text = new TextCharacter(87, 1192, mid_y, width_4);
		char_list.add(55, temp_text);
		
		//X
		temp_text = new TextCharacter(88, 1263, mid_y, width_4);
		char_list.add(56, temp_text);
		
		//Y
		temp_text = new TextCharacter(89, 1333, mid_y, width_4);
		char_list.add(57, temp_text);
		
		//Z
		temp_text = new TextCharacter(90, 1404, mid_y, width_2);
		char_list.add(58, temp_text);
		
		//a
		temp_text = new TextCharacter(97, 0, top_y, width_3);
		char_list.add(65, temp_text);
		
		//b
		temp_text = new TextCharacter(98, 59, top_y, width_3);
		char_list.add(66, temp_text);
		
		//c
		temp_text = new TextCharacter(99, 117, top_y, width_2);
		char_list.add(67, temp_text);
		
		//d
		temp_text = new TextCharacter(100, 165, top_y, width_3);
		char_list.add(68, temp_text);
		
		//e
		temp_text = new TextCharacter(101, 223, top_y, width_3);
		char_list.add(69, temp_text);
		
		//f
		temp_text = new TextCharacter(102, 283, top_y, width_5+10);
		char_list.add(70, temp_text);
		
		//g
		temp_text = new TextCharacter(103, 318, top_y+25, width_3);
		char_list.add(71, temp_text);
		
		//h
		temp_text = new TextCharacter(104, 377, top_y, width_2);
		char_list.add(72, temp_text);
		
		//i
		temp_text = new TextCharacter(105, 437, top_y, width_1);
		char_list.add(73, temp_text);
		
		//j
		temp_text = new TextCharacter(106, 453, top_y, width_5);
		char_list.add(74, temp_text);
		
		//k
		temp_text = new TextCharacter(107, 484, top_y, width_3);
		char_list.add(75, temp_text);
		
		//l
		temp_text = new TextCharacter(108, 542, top_y, width_1);
		char_list.add(76, temp_text);
		
		//m
		temp_text = new TextCharacter(109, 566, top_y, width_6);
		char_list.add(77, temp_text);
		
		//n
		temp_text = new TextCharacter(110, 662, top_y, width_3);
		char_list.add(78, temp_text);
		
		//o
		temp_text = new TextCharacter(111, 719, top_y, width_3);
		char_list.add(79, temp_text);
		
		//p
		temp_text = new TextCharacter(112, 780, top_y+25, width_3);
		char_list.add(80, temp_text);
		
		//q
		temp_text = new TextCharacter(113, 838, top_y+25, width_4);
		char_list.add(81, temp_text);
		
		//r
		temp_text = new TextCharacter(114, 897, top_y, width_2);
		char_list.add(82, temp_text);
		
		//s
		temp_text = new TextCharacter(115, 944, top_y, width_2);
		char_list.add(83, temp_text);
		
		//t
		temp_text = new TextCharacter(116, 991, top_y, width_2);
		char_list.add(84, temp_text);
		
		//u
		temp_text = new TextCharacter(117, 1039, top_y, width_3);
		char_list.add(85, temp_text);
		
		//v
		temp_text = new TextCharacter(118, 1098, top_y, width_4);
		char_list.add(86, temp_text);
		
		//w
		temp_text = new TextCharacter(119, 1168, top_y, width_6);
		char_list.add(87, temp_text);
		
		//x
		temp_text = new TextCharacter(120, 1263, top_y, width_4);
		char_list.add(88, temp_text);
		
		//y
		temp_text = new TextCharacter(121, 1333, top_y+25, width_3);
		char_list.add(89, temp_text);
		
		//z
		temp_text = new TextCharacter(122, 1392, top_y, width_2);
		char_list.add(90, temp_text);
		//PHEW NOW I KNOW MY ABC'S
	}
	
	
	
	
	@Override
	public void openText(String text, float size, int x, int y, int max_height, int width) {
		// TODO Auto-generated method stub
		int i; 
		int curr_width = 0;
		int curr_height = 0;
		renderer = new RendererValidation(GarbageRenderer.class);
		renderer.renderBatchStart();
		//finds index of image
		for(i=0;i<text.length();i++)
		{
			curr_width += char_list.get(text.charAt(i)).width * size;
			if(curr_width >= width)
			{
				curr_width = 0;
				curr_height += char_list.get(text.charAt(i)).height * size;
				if(curr_height >= max_height)
					return;
				
			}
			renderer.batchImageScreenScaled(char_list.get(text.charAt(i)).fontImage, 
											2, 
											x+curr_width, 
											y+curr_height, 
											char_list.get(text.charAt(i)).width * size, 
											char_list.get(text.charAt(i)).height * size
											);
		}
		
	}

	@Override
	public void closeText(String text) {
		// TODO Auto-generated method stub
		renderer.renderBatchEnd();
	}
	
	@Override
	public void cleanupText()
	{
		int i;
		for(i=0;i<91;i++)
		{
			renderer.unloadImage(char_list.get(i).fontImage);
		}
	}
}
