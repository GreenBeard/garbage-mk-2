package garbageboys.garbageman_mk_2;

import java.util.ArrayList;
import java.util.List;

public class RendererValidation implements Render2D {

	private class ImageInfo {
		String name;
		boolean loaded;
	}
	
	Render2D actual_renderer;
	List<ImageInfo> images;
	boolean batch_mode = false;
	
	private void validate_batch_resource(String resource) {
		if (batch_mode) {
			ImageInfo info = find_info(resource);
			if (info != null) {
				/* success */
			} else {
				throw new RuntimeException();
			}
		} else {
			throw new RuntimeException();
		}
	}
	
	private ImageInfo find_info(String resource) {
		for (ImageInfo info : images) {
			if (info.name.equals(resource)) {
				return info;
			}
		}
		return null;
	}

	@Override
	public void initialize() {
		actual_renderer = new GarbageRenderer();
		actual_renderer.initialize();
		
		images = new ArrayList<ImageInfo>();
	}

	@Override
	public void cleanup() {
		actual_renderer.cleanup();
		if (images.size() != 0) {
			throw new RuntimeException();
		}
	}

	@Override
	public long getWindowID() {
		return actual_renderer.getWindowID();
	}

	@Override
	public boolean loadImage(String resource) {
		if (find_info(resource) != null) {
			throw new RuntimeException();
		}
		ImageInfo info = new ImageInfo();
		info.name = resource;
		info.loaded = false;
		images.add(info);
		return actual_renderer.loadImage(resource);
	}

	@Override
	public void refreshImages() {
		for (ImageInfo info : images) {
			info.loaded = true;
		}
		actual_renderer.refreshImages();
	}

	@Override
	public boolean unloadImage(String resource) {
		if (find_info(resource) == null) {
			throw new RuntimeException();
		}
		for (int i = 0; i < images.size(); ++i) {
			if (images.get(i).name == resource) {
				images.remove(i);
				break;
			}
		}
		return false;
	}

	@Override
	public void renderBatchStart() {
		batch_mode = true;
		for (ImageInfo info : images) {
			assert(info.loaded);
		}
		actual_renderer.renderBatchStart();
	}

	@Override
	public void renderBatchEnd() {
		actual_renderer.renderBatchEnd();
		batch_mode = false;
	}

	@Override
	public void batchImage(String resource, int layer, int x, int y) {
		validate_batch_resource(resource);
		actual_renderer.batchImage(resource, layer, x, y);
	}

	@Override
	public void batchImageScaled(String resource, int layer, int x, int y, int width, int height) {
		validate_batch_resource(resource);
		actual_renderer.batchImageScaled(resource, layer, x, y, width, height);
	}

	@Override
	public void batchImageScreenScaled(String resource, int layer, float x, float y, float width, float height) {
		validate_batch_resource(resource);
		actual_renderer.batchImageScreenScaled(resource, layer, x, y, width, height);
	}

}
