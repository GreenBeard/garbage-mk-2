package garbageboys.garbageman_mk_2;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;

public class TitleScreen implements Screen {

	private Render2D renderer;
	private App app;
	private SoundManager soundManager;
	private TextManager text;
	
	int counter = 0;
	
	final String ICON0 = "/assets/Icons/Garbagecan0.png";
	final String ICON1 = "/assets/Icons/Garbagecan1.png";
	final String ICON2 = "/assets/Icons/Garbagecan2.png";
	final String ICON3 = "/assets/Icons/Garbagecan3.png";
	final String ICON4 = "/assets/Icons/Garbagecan4.png";
	final String TITLE_THEME = "/assets/Sounds/Songs/Themey.wav";
	
	Object play_button;
	List<Object> title_background_frames_1;
	List<Object> title_background_frames_2;
	boolean title_loop_complete = false;
	
	List<Object> loadedItems;
	
	@Override
	public void init(Render2D renderer, App app, SoundManager soundManager, TextManager text) {
		this.renderer = renderer;
		this.app = app;
		this.soundManager = soundManager;
		this.text = text;
		loadedItems = new ArrayList<Object>();
	}

	@Override
	public void loadAssets() {
		play_button = renderer.loadImage("/assets/Buttons/play.png");
		title_background_frames_1 = renderer.loadImageSeriesTopLeft("/assets/Screens/mainTitle.png", 384, 216, 23);
		title_background_frames_2 = renderer.loadImageSeriesTopLeft("/assets/Screens/mainTitle2.png", 384, 216, 10);
		loadedItems.add(play_button);
		loadedItems.addAll(title_background_frames_1);
		loadedItems.addAll(title_background_frames_2);
	}

	@Override
	public void renderFrame(int frame) {
		
		MemoryStack stack = MemoryStack.stackPush();

		renderer.renderBatchStart();
		
		//text.openText("GARBAGEMAN: One mans trash is anothers treasure", 1f, 0, 0, 85, 100);
		int title_frame;

		if(counter == 300) {
			renderer.setIcon(ICON1);
		}
		if(counter == 600) {
			renderer.setIcon(ICON2);
		}
		if(counter == 900) {
			renderer.setIcon(ICON3);
		}
		if(counter == 1200) {
			renderer.setIcon(ICON4);
		}
		if(counter == 1500) {
			renderer.setIcon(ICON0);
			counter = 0;
		}
		
		if (frame == title_background_frames_1.size() * 5) {
			title_loop_complete = true;
			//text.openText("PLAY", 1.25f, 700, 475, 100, 200);
			soundManager.loopSound(TITLE_THEME);
		}
		if (frame == title_background_frames_1.size() * 5 + 100) {
			
//			soundManager.fadeOutSong(TITLE_THEME, 3000, -.6f);
//			soundManager.fadeInSong(CHEERY, SoundTypes.Music, 3000, .6f, true);
		}
		List<Object> current_frames;
		if (title_loop_complete) {
			current_frames = title_background_frames_2;
			renderer.batchImageScreenScaled(play_button, 1, 0.40f, 0.508f, 0.23f, 0.15f);
		}
		else {
			current_frames = title_background_frames_1;
		}
		
		title_frame = (frame / 5) % current_frames.size();
		renderer.batchImageScreenScaled(
				current_frames.get(title_frame),
				0, 0.0f, 0.0f, 1.0f, 1.0f);
		//renderer.batchImageScaled(title_background_frames.get(title_frame), 0, 0, 0, 384 * 8, 216 * 8);
		renderer.renderBatchEnd();
		counter++;
		stack.pop();
		
	}

	@Override
	public void unloadAssets() {
		for(Object obj : loadedItems) {
			renderer.unloadImage(obj);
		}
	}

	@Override
	public void closeScreen() {
		unloadAssets();
	}
}
