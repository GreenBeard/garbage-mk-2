package garbageboys.garbageman_mk_2;

import java.io.IOException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;

public class DefaultSoundManager implements SoundManager {
	
	IntBuffer buffer = BufferUtils.createIntBuffer(1);
	
	HashMap<String, TypedClip> clips = new HashMap<String, TypedClip>();

	@Override
	public boolean loadSound(String resource, SoundTypes type) {
		
		URL url = ResourceLoader.FindResourceURL(resource);
		AudioInputStream stream = null;
		Clip clip = null;
		
		try {
			stream = AudioSystem.getAudioInputStream(url);
			clip = AudioSystem.getClip();
			clip.open(stream);
			
		} catch (UnsupportedAudioFileException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (LineUnavailableException e) {
			e.printStackTrace();
			return false;
		}
		
		clips.put(resource, new TypedClip(clip, type));
		
		return true;
	}

	/**
	 * Currently has no use. Might one day.
	 */
	@Override
	public void refreshSounds() {

	}

	@Override
	public void playSound(String resource) {
		Clip clip = clips.get(resource).clip;
		clip.start();
	}

	@Override
	public void resetSounds(SoundTypes type) {
		for(TypedClip c : clips.values()) {
			if(c.type.equals(type)) {
				c.clip.stop();
			}
		}
	}

	@Override
	public void loopSound(String resource) {
		Clip clip = clips.get(resource).clip;
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}

	@Override
	public void unloopSound(String resource) {
		Clip c = clips.get(resource).clip;
		c.loop(0);
	}

	@Override
	public void stopSound(String resource) {
		Clip c = clips.get(resource).clip;
		c.stop();
	}

	@Override
	public boolean unloadSound(String resource) {
		TypedClip clip = clips.get(resource);
		clip.clip.stop();
		clip.clip.close();
		clips.remove(resource);
		return true;
	}


	@Override
	public boolean unloadSoundType(SoundTypes type) {
		for(String s : clips.keySet()) {
			TypedClip c = clips.get(s);
			if(c.type.equals(type)) {
				c.clip.stop();
				c.clip.close();
				clips.remove(s);
			}
		}
		return true;
	}


	@Override
	public boolean unloadAllSounds() {
		for(TypedClip c : clips.values()) {
			c.clip.stop();
			c.clip.close();
		}
		
		clips.clear();
		return false;
	}

	class TypedClip {
		
		public SoundTypes type;
		public Clip clip;
		
		public TypedClip(Clip clip, SoundTypes type) {
			this.clip = clip;
			this.type = type;
		}
		
		public Clip getClip() {
			return this.clip;
		}
		
		public SoundTypes getType() {
			return this.type;
		}
	}

	@Override
	public boolean checkSoundRunning(String resource) {
		Clip c = clips.get(resource).clip;
		return c.isRunning();
	}

	@Override
	public List<String> getRunningResources() {
		List<String> list = new ArrayList<String>();
		for(String s : clips.keySet()) {
			TypedClip c = clips.get(s);
			if(c.clip.isRunning()) {
				list.add(s);
			}
		}
		return list;
	}
	
}
