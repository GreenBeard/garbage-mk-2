package garbageboys.garbageman_mk_2;

import java.util.List;

public interface SoundManager {

	enum SoundTypes {
		Music,
		Effects
	};

	/**
	 * Loads an audio file. E.x. - loadSound("/assets/Sounds/Songs/Cheery.wav", SoundManager.SoundTypes.Music)
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
	
	/**
	 * Unloads a specific type of sound. E.g. unloadSoundType(SoundTypes.Music) would unload all loaded music.
	 * @param type the type of sound to be stopped - e.g. SoundTypes.Effects
	 * @return true on success
	 */
	public boolean unloadSoundType(SoundTypes type);
	
	/**
	 * Unloads all audio files currently loaded.
	 * @return true on success
	 */
	public boolean unloadAllSounds();

	/**
	 * Plays an audio file. The audio file will play until completion, or until stopped.
	 * @param resource - e.g. "/assets/Sounds/Songs/Cheery.wav"
	 */
	public void playSound(String resource);
	
	/**
	 * Stops an audio file if it is currently playing.
	 * @param resource - e.g. "/assets/Sounds/Songs/Cheery.wav"
	 */
	public void stopSound(String resource);

	/**
	 * Stops all sounds of a specific type.
	 * @param type - e.g. SoundTypes.Effects
	 */
	public void resetSounds(SoundTypes type);
	
	/**
	 * Plays an audio file on loop, until it is stopped, unloaded or unlooped.
	 * @param resource - e.g. "/assets/Sounds/Songs/Cheery.wav"
	 */
	public void loopSound(String resource);

	/**
	 * Stops a currently looping sound, so that the sound plays to completion and does not repeat.
	 * @param resource - e.g. "/assets/Sounds/Songs/Cheery.wav"
	 */
    public void unloopSound(String resource);
    
    /**
     * Checks if an audio file is currently being played.
     * @param resource - e.g. "/assets/Sounds/Songs/Cheery.wav"
     * @return true if audio file is playing
     */
    public boolean isSoundRunning(String resource);
    
    public List<String> getRunningResources();
    
    public boolean setTypeVolume(float volume, SoundTypes type, boolean overrideRunningClips);
    
    public void setMasterVolume(float volume);
    
    public float getMasterVolume();
    
    public boolean fadeInSong(String resource, SoundTypes type, int millis, float intensity, boolean loop);

	public boolean fadeOutSong(String resource, int millis, float intensity);

}
