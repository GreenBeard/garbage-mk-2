package garbageboys.garbageman_mk_2;

import java.util.List;

public interface SoundManager {

	enum SoundTypes {
		Music,
		Effects
	};

	/**
	 * Loads an audio file.
	 * @param resource - e.g. "/assets/Sounds/Songs/Cheery.wav"
	 * 
	 * @return true on success
	 */
	public boolean loadSound(String resource, SoundTypes type);

	/**
	 * Call after loading a set of files to prepare them for rendering.
	 */
	public void refreshSounds();

	/**
	 * Unloads an audio file, and cleans up resources.
	 * @param resource - e.g. "/assets/Sounds/Songs/Cheery.wav"
	 * 
	 * @return true on success
	 */
	public boolean unloadSound(String resource);
	
	public boolean unloadSoundType(SoundTypes type);
	
	public boolean unloadAllSounds();

	public void playSound(String resource);
	
	public void stopSound(String resource);

	public void resetSounds(SoundTypes type);
	
	public void loopSound(String resource);

    public void unloopSound(String resource);
    
    public boolean checkSoundRunning(String resource);
    
    public List<String> getRunningResources();

}
