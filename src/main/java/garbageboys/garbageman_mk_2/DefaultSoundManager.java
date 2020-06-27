package garbageboys.garbageman_mk_2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.HashMap;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;

public class DefaultSoundManager implements SoundManager {
	
	IntBuffer buffer = BufferUtils.createIntBuffer(1);
	
	HashMap<String, Clip> clips = new HashMap<String, Clip>();

	@Override
	public boolean loadSound(String resource) {
		
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
		
		clips.put(resource, clip);
		
		return true;
	}

	@Override
	public void refreshSounds() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean unloadSound(String resource) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void playSound(String resource, SoundTypes type) {
		Clip clip;
		
		clip = clips.get(resource);
		
		clip.start();
	}

	@Override
	public void resetSounds(SoundTypes type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void loopSound(String resource, SoundTypes type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unloopSound(String resource, SoundTypes type) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stopSound(String resource, SoundTypes type) {
		// TODO Auto-generated method stub
		
	}

}
