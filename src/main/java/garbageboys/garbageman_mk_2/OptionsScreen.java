package garbageboys.garbageman_mk_2;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;

import garbageboys.garbageman_mk_2.Render2D.InteractEvent;

public class OptionsScreen implements Screen {

	private Render2D renderer;
	private App app;
	private SoundManager soundManager;
	
	List<Object> loadedItems;
	List<InteractEvent> events;
	
	Object background;
	Object volumeSliderBG;
	Object volumeSliderKnob;
	
	@Override
	public void init(Render2D renderer, App app, SoundManager soundManager, TextManager text) {
		this.renderer = renderer;
		this.app = app;
		this.soundManager = soundManager;
		loadedItems = new ArrayList<Object>();
		events = new ArrayList<InteractEvent>();
	}

	@Override
	public void loadAssets() {
		volumeSliderBG = renderer.loadImage("/assets/Sliders/DefaultSlider.png");
		volumeSliderKnob = renderer.loadImage("/assets/Sliders/DefaultKnob.png");
		loadedItems.add(volumeSliderBG);
		loadedItems.add(volumeSliderKnob);
	}

	@Override
	public void renderFrame(int frame) {
		
		renderer.fillEventList(events);
		for(InteractEvent e : events) {
			System.out.println(e.handle);
			if(e.handle != null) {
				if(e.handle.equals((volumeSliderBG))) {
					System.out.println("MATCH!!!!!!!!");
				}
			}
		}
		
		renderer.renderBatchStart();
		MemoryStack stack = MemoryStack.stackPush();
		renderer.batchImageScreenScaled(volumeSliderBG, 1, 0.40f, 0.508f, 0.23f, 0.15f);
		
		
		renderer.renderBatchEnd();
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
