package garbageboys.garbageman_mk_2;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class DefaultSoundManager implements SoundManager {

	private HashMap<SoundTypes, Float> volumes = new HashMap<SoundTypes, Float>();
	private HashMap<String, TypedClip> clips = new HashMap<String, TypedClip>();
	private HashMap<String, TypedClip> runningClips = new HashMap<String, TypedClip>();
	private float masterVol = 0f;
	
	public DefaultSoundManager() {
		volumes.put(SoundTypes.Effects, new Float(0f));
		volumes.put(SoundTypes.Music, new Float(0f));
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
			clip = AudioSystem.getClip(null);
			clip.open(stream);
		} catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
			e.printStackTrace();
			return false;
		}
		
		TypedClip tc = new TypedClip(clip, type, resource);
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
		TypedClip clip = clips.get(resource);
		Thread thread = new Thread(clip);
		thread.start();
		runningClips.put(resource,clip);
	}

	/**
	 * Stops any sounds of the given type. E.g. resetSounds(SoundManager.Music) will stop the background music.
	 */
	@Override
	public void resetSounds(SoundTypes type) {
		for(TypedClip c : clips.values()) { 
			if(c.type.equals(type)) { 
				c.stopClip();
			}
		}
	}

	/**
	 * Plays a sound on infinite loop.
	 */
	@Override
	public void loopSound(String resource) {
		TypedClip clip = clips.get(resource);
		Thread thread = new Thread(clip);
		clip.setLoop(Clip.LOOP_CONTINUOUSLY);
		thread.start();
		runningClips.put(resource,clip);
	}

	/**
	 * Stops a sound on loop gracefully. That is to say, the sound/song or whatever will finish but not play again.
	 */
	@Override
	public void unloopSound(String resource) {
		TypedClip c = clips.get(resource);
		c.unloop();
	}

	/**
	 * Abruptly stops a sound.
	 */
	@Override
	public void stopSound(String resource) {
		TypedClip c = clips.get(resource);
		c.stopClip();
	}

	/**
	 * Unloads a loaded sound. Probably useful for doing something like unloading the intro roll.
	 */
	@Override
	public boolean unloadSound(String resource) {
		TypedClip clip = clips.get(resource);
		clip.stopClip();
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
				c.stopClip();
				c.unload();
				clips.remove(s);
				runningClips.remove(s);
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
			c.stopClip();
			c.unload();
		}
		clips.clear();
		runningClips.clear();
		return true;
	}

	/**
	 * An internal class that acts as a wrapper for a sound Clip object. Includes the sound's type, and not much else at the moment.
	 * Might contain other info such as song length/size in the future.
	 * @author Pangur
	 *
	 */
	class TypedClip implements Runnable {
		
		public SoundTypes type;
		private Clip clip;
		public String resource;
		
		private boolean running = false;
		private boolean fading = false;
		private boolean fadein = false;
		
		private final int THREAD_SLEEP_TIME = 100;
		
		private float millisLeft;
		private float fadeIntensity;
		private float intensity;
		
		private int loopTimes = 0;
		
		public TypedClip(Clip clip, SoundTypes type, String resource) {
			this.clip = clip;
			this.type = type;
			this.resource = resource;
		}
		
		public void setLoop(int b) {
			this.loopTimes = b;
		}

		public void unloop() {
			this.clip.loop(0);
		}
		
		public void unload() {
			clip.close();
		}

		public Clip getClip() {
			return this.clip;
		}
		
		public SoundTypes getType() {
			return this.type;
		}

		@Override
		public void run() {
			clip.loop(loopTimes);
			//System.out.println(clip.isRunning());
			while(!clip.isRunning()) {
				try {
					Thread.sleep(1);					//This INFINITELY stupid sleep is because the thread associated with Java's clips doesn't initialize 
				} catch (InterruptedException e1) {		//immediately or something. If this is taken out, if(!clip.isRunning()) will fail, and things get screwed up.
					e1.printStackTrace();				//Very stupid fix, would be inefficient if this was called more than once per song.
				}
			}
			this.running = true;
			while(running) {
				try {
					if(fading || fadein) {
						this.millisLeft -= THREAD_SLEEP_TIME;
						modulateIntensity();
						if(millisLeft <= 0 && fading) {
							System.out.println("Faded out "+type+": "+resource);
							stopClip();
							fading = false;
						}
						if(millisLeft <= 0 && fadein) {
							System.out.println("Faded in "+type+": "+resource);
							FloatControl volControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
							volControl.setValue(masterVol + volumes.get(type));
							fadein = false;
						}
					}
					if(!clip.isRunning()) {
						System.out.println("Stopped running: "+resource);
						this.running = false;
					}
					Thread.sleep(THREAD_SLEEP_TIME);
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void stopClip(){
			clip.stop();
			this.running = false;
		}
		
		public void doFadeOut(int millis, float fadeIntensity) {
			System.out.println("Fading out "+type+": "+resource);
			this.millisLeft = (float) millis;
			this.fadeIntensity = fadeIntensity;
			fading = true;
			this.intensity = volumes.get(type) + masterVol;
		}
		
		public void doFadeIn(int millis, float fadeIntensity) {
			System.out.println("Fading in "+type+": "+resource);
			this.millisLeft = (float) millis;
			this.fadeIntensity = fadeIntensity;
			fadein = true;
			this.intensity = masterVol + volumes.get(type) - (fadeIntensity * (millis/100));
		}
		
		private void modulateIntensity() {
			FloatControl volControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			this.intensity += fadeIntensity;
			volControl.setValue(this.intensity);
		}
	}

	
	@Override
	public boolean isSoundRunning(String resource) {
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
	public boolean setTypeVolume(float volume, SoundTypes type, boolean overrideRunningClips) {
		volumes.replace(type, volume);
		for(TypedClip c : clips.values()) {
			if(c.type.equals(type) && (overrideRunningClips || !c.clip.isRunning())) {
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
			setTypeVolume(volumes.get(type), type, true);
		}
	}
	
	public float getMasterVolume() {
		return masterVol;
	}

	@Override
	public boolean fadeOutSong(String resource, int millis, float intensity) {
		TypedClip c = runningClips.get(resource);
		if(c == null) return false;
		c.doFadeOut(millis, intensity);
		return true;
	}

	@Override
	public boolean fadeInSong(String resource, SoundTypes type, int millis, float intensity, boolean loop) {
		TypedClip c = clips.get(resource);
		if(c == null) return false;
		if(loop) {
			loopSound(resource);
		}
		else{
			playSound(resource);
		}
		FloatControl volControl = (FloatControl) c.clip.getControl(FloatControl.Type.MASTER_GAIN);
		volControl.setValue(masterVol + volumes.get(type) - (intensity * (millis/100)));
		c.doFadeIn(millis, intensity);
		return true;
	}
	
}
