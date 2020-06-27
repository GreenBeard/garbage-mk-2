package garbageboys.garbageman_mk_2;

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
	public boolean loadSound(String resource);

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

	public void playSound(String resource, SoundTypes type);
	
	public void stopSound(String resource, SoundTypes type);

	public void resetSounds(SoundTypes type);
	
	public void loopSound(String resource, SoundTypes type);

    public void unloopSound(String resource, SoundTypes type);

}
