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
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.lwjgl.BufferUtils;

public class DefaultSoundManager implements SoundManager {

	private HashMap<SoundTypes, Float> volumes = new HashMap<SoundTypes, Float>();
	private HashMap<String, TypedClip> clips = new HashMap<String, TypedClip>();
	private float masterVol;
	
	public DefaultSoundManager() {
		volumes.put(SoundTypes.Effects, new Float(0f));
		volumes.put(SoundTypes.Music, new Float(0f));
		masterVol = 0f;
	}
	
	/**
	 * Loads a sound given a resource name and a sound type. Don't include a full address, just do something like "/assets/Sounds/Songs/Chilly.wav/"
	 * For that example, the type would be SoundManager.Music
	 */
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
		TypedClip tc = new TypedClip(clip, type);
		clips.put(resource, tc);
		setVolume(volumes.get(type),tc,type);
		
		return true;
	}

	/**
	 * Currently has no use. Might one day. Feel free to call it after you load a sound.
	 */
	@Override
	public void refreshSounds() {

	}

	/**
	 * Plays a loaded sound. Throws a runtime exception if it isn't loaded/doesn't exist.
	 */
	@Override
	public void playSound(String resource) {
		Clip clip = clips.get(resource).clip;
		clip.start();
	}

	/**
	 * Stops any sounds of the given type. E.g. resetSounds(SoundManager.Music) will stop the background music.
	 */
	@Override
	public void resetSounds(SoundTypes type) {
		for(TypedClip c : clips.values()) {
			if(c.type.equals(type)) {
				c.clip.stop();
			}
		}
	}

	/**
	 * Plays a sound on infinite loop.
	 */
	@Override
	public void loopSound(String resource) {
		Clip clip = clips.get(resource).clip;
		clip.loop(Clip.LOOP_CONTINUOUSLY);
	}

	/**
	 * Stops a sound on loop gracefully. That is to say, the sound/song or whatever will finish but not play again.
	 */
	@Override
	public void unloopSound(String resource) {
		Clip c = clips.get(resource).clip;
		c.loop(0);
	}

	/**
	 * Abruptly stops a sound.
	 */
	@Override
	public void stopSound(String resource) {
		Clip c = clips.get(resource).clip;
		c.stop();
	}

	/**
	 * Unloads a loaded sound. Probably useful for doing something like unloading the intro roll.
	 */
	@Override
	public boolean unloadSound(String resource) {
		TypedClip clip = clips.get(resource);
		clip.clip.stop();
		clip.clip.close();
		clips.remove(resource);
		return true;
	}

	/**
	 * Unloads all sounds of a certain type. Very niche, may never be used.
	 */
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

	/**
	 * Unloads all sounds. Probably useful if we have an option to turn off all sounds, and could be called on exit.
	 */
	@Override
	public boolean unloadAllSounds() {
		for(TypedClip c : clips.values()) {
			c.clip.stop();
			c.clip.close();
		}
		
		clips.clear();
		return false;
	}

	/**
	 * An internal class that acts as a wrapper for a sound Clip object. Includes the sound's type, and not much else at the moment.
	 * Might contain other info such as song length/size in the future.
	 * @author Pangur
	 *
	 */
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

	@Override
	public boolean setTypeVolume(float volume, SoundTypes type) {
		volumes.replace(type, volume);
		for(TypedClip c : clips.values()) {
			if(c.type.equals(type)) {
				setVolume(volume, c, type);
			}
		}
		return true;
	}
	
	private void setVolume(float volume, TypedClip clip, SoundTypes type) {
		FloatControl volControl = (FloatControl) clip.clip.getControl(FloatControl.Type.MASTER_GAIN);
		volControl.setValue(masterVol + volumes.get(type));
	}

	@Override
	public void setMasterVolume(float volume) {
		masterVol = volume;
		for(SoundTypes type : SoundTypes.values()) {
			setTypeVolume(volumes.get(type), type);
		}
	}
	
}
