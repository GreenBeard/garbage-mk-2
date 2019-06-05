package garbageboys.garbageman_mk_2;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ResourceLoader {
	/**
	 * Returns a string or throws an error
	 * @param file_name - Technically a path relative to classes
	 * @return
	 */
	public static String LoadShader(String file_name) {
		try {
			URL url = ResourceLoader.class.getClass().getResource(file_name);
			if (url == null && file_name.substring(0, 1).equals("/")) {
				/* Fucky Java resources */
				url = ResourceLoader.class.getClassLoader().getResource(file_name.substring(1));
			}
			return new String(Files.readAllBytes(Paths.get(url.toURI())));
		} catch (IOException | URISyntaxException e) {
			if (file_name.length() > 0 && !file_name.substring(0, 1).equals("/")) {
				System.out.println("Your file_name should probably start with a \"/\": " + file_name);
			} else {
				System.out.println("Error loading file: " + file_name);
			}
			e.printStackTrace();
			return null;
		}
	}
}
