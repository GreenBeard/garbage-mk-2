package garbageboys.garbageman_mk_2;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.system.MemoryStack;

import junit.framework.TestCase;

public class PackTest extends TestCase {
	public PackTest(String testName) {
		super(testName);
	}

	public void testPack() {
		Random random = new Random(0);
		
		List<Rect> inputs = new ArrayList<Rect>();
		List<Rect> placed = new ArrayList<Rect>();
		
		int pack_width = 2048;
		int pack_height = 2048;
		int area = pack_width * pack_height;
		int used_area = 0;
		while (true) {
			Rect rect = new Rect();
			rect.width = random.nextInt(pack_width / 8);
			rect.height = random.nextInt(pack_width / 8);
			int new_area = rect.width * rect.height;
			if (used_area + new_area > 0.97 * area) {
				break;
			} else {
		  	used_area += new_area;
			}
			if (rect.width != 0 && rect.height != 0) {
		  	inputs.add(rect);
			}
		}
		System.out.println("Attempting to fill " + 100.0f * used_area / area + "% with " + inputs.size() + " rectangles");
		{
			long start_time = System.nanoTime();
			RectPacker packer = new RectPacker();
			boolean status = packer.pack(inputs, placed, pack_width, pack_height, 15, false);
			assertTrue(status);
			long end_time = System.nanoTime();
			System.out.printf("Took %.3f ms\n", (end_time - start_time) / 1000.0f / 1000.0f);
			status = packer.validate(placed, pack_width, pack_height);
			assertTrue(status);
			//packer.print_placed(placed, width, height, 0);
		}
	}
	
	public void testRealPack() {
	  RectPacker packer = new RectPacker();
	  
	  List<Rect> inputs = new ArrayList<Rect>();
	  List<Rect> placed = new ArrayList<Rect>();
	  
	  List<String> image_files = new ArrayList<String>();
	  load_image_files(image_files);
	  
	  int pack_width = 512;
	  int pack_height = 512;
	  int area = pack_width * pack_height;
	  int used_area = 0;
	  for (String file_name : image_files) {
	  	MemoryStack stack = MemoryStack.stackPush();
			IntBuffer width = stack.mallocInt(1);
			IntBuffer height = stack.mallocInt(1);
			IntBuffer channels = stack.mallocInt(1);
			ByteBuffer buffer = ResourceLoader.LoadTexture(file_name, width, height, channels);
			if (buffer != null) {
				Rect rect = new Rect();
				rect.id = 0;
				rect.width = width.get(0);
				rect.height = height.get(0);
		  	int new_area = rect.width * rect.height;
		  	if (used_area + new_area > 0.90 * area
		  			|| rect.width > pack_width / 2 || rect.height > pack_height / 2) {
		  		continue;
		  	} else {
		  		used_area += new_area;
		  	}
		  	if (rect.width != 0 && rect.height != 0) {
		  		inputs.add(rect);
		  	}
			}
			stack.pop();
	  }
	  System.out.println("Attempting to fill " + 100.0f * used_area / area + "%");
	  boolean status = packer.pack(inputs, placed, pack_width, pack_height, 15, false);
	  assertTrue(status);
	  status = packer.validate(placed, pack_width, pack_height);
	  assertTrue(status);
  }

	/*private void shrink_area(List<Rect> inputs, int width, int height) {
		int check_count = 1;
		while (true) {
			RectPacker packer = new RectPacker();
			List<Rect> tmp_placed = new ArrayList<Rect>();
			boolean status = packer.pack(inputs, tmp_placed, width, height, check_count, false);
			if (status) {
				System.out.printf("Success with height %d check size %d\n", height, check_count);
				packer.print_placed(tmp_placed, width, height, height);
				--height;
			} else if (check_count < 15) {
				check_count += 7;
			} else {
				break;
			}
		}
	}*/

	private void load_image_files(List<String> image_files) {
		try {
			load_image_files(image_files, "/assets");
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
	
	private void load_image_files(List<String> image_files, String dir_path) throws IOException {
		URL url = ResourceLoader.FindResourceURL(dir_path);
		File dir;
		try {
			dir = new File(url.toURI());
			for (File file : dir.listFiles()) {
				String path = dir_path + "/" + file.getName();
				if (file.isDirectory()) {
					load_image_files(image_files, path);
				} else {
					image_files.add(path);
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}

