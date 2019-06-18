package garbageboys.garbageman_mk_2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class PackTest extends TestCase {
    public PackTest(String testName) {
        super(testName);
    }

    public void testPack() {
        Random random = new Random(0);
        RectPacker packer = new RectPacker();
        
        List<Rect> inputs = new ArrayList<Rect>();
        List<Rect> placed = new ArrayList<Rect>();
        
        int width = 512;
        int height = 512;
        int area = width * height;
        int used_area = 0;
        while (true) {
        	Rect rect = new Rect();
        	rect.width = random.nextInt(width / 8);
        	rect.height = random.nextInt(width / 8);
        	int new_area = rect.width * rect.height;
        	if (used_area + new_area > area - 25) {
        		break;
        	} else {
          	used_area += new_area; 
        	}
        	if (rect.width != 0 && rect.height != 0) {
          	inputs.add(rect);
        	}
        }
        System.out.println("Attempting to fill " + 100.0f * used_area / area + "%");
        boolean status = packer.pack(inputs, placed, width, height);
        assertTrue(status);
        status = packer.validate(placed, width, height);
        assertTrue(status);
        packer.print_placed(placed, width, height);
    }
}

