package garbageboys.garbageman_mk_2;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class ResourceLoader {
	
	static ArrayList<String> titles;
	static String titleTagsName = "/TitleTags.txt/";
	
	public static URL FindResourceURL(String file_name) {
		URL url = ResourceLoader.class.getClass().getResource(file_name);
		if (url == null && file_name.substring(0, 1).equals("/")) {
			/* Fucky Java resources */
			url = ResourceLoader.class.getClassLoader().getResource(file_name.substring(1));
			if (url == null) {
				if (file_name.length() > 0 && !file_name.substring(0, 1).equals("/")) {
					System.out.println("Your file_name should probably start with a \"/\": " + file_name);
				} else {
					System.out.println("Error loading file: " + file_name);
				}
			}
		}
		return url;
	}
	
	/**
	 * Returns a String or null on error
	 * @param file_name - Technically a path relative to classes
	 * @return
	 */
	public static String LoadShader(String file_name) {
		try {
			URL url = FindResourceURL(file_name);
			return new String(Files.readAllBytes(Paths.get(url.toURI())));
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Returns a ByteBuffer with the raw texture or null on error
	 * @param file_name - Technically a path relative to classes
	 * @param channels - The number of channels the file actually had
	 * @return
	 */
	public static ByteBuffer LoadTexture(String file_name, IntBuffer width, IntBuffer height, IntBuffer channels) {
		try {
			URL url = FindResourceURL(file_name);
			byte[] bytes = Files.readAllBytes(Paths.get(url.toURI()));
			
			ByteBuffer img_in_buffer = BufferUtils.createByteBuffer(bytes.length);
			img_in_buffer.put(bytes);
			img_in_buffer.rewind();

			ByteBuffer img_out_buffer = STBImage.stbi_load_from_memory(img_in_buffer, width, height, channels, 4);

			return img_out_buffer;
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String getTitleText() {
		
		if(titles == null) {
			titles = getPossibleTitles();
		}
		
		Random r = new Random();
		
		return titles.get(r.nextInt(titles.size()));
	}
	
	private static ArrayList<String> getPossibleTitles() {
		try {
			Scanner reader = new Scanner(Paths.get(FindResourceURL(titleTagsName).toURI()).toFile());
			ArrayList<String> t = new ArrayList<String>();
			while(reader.hasNextLine()) {
				t.add(reader.nextLine());
			}
			return t;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}
}
