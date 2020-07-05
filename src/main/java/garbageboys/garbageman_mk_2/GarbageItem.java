package garbageboys.garbageman_mk_2;

import java.awt.Point;
import java.util.List;

import garbageboys.garbageman_mk_2.GarbageLoader.GarbageTier;
import garbageboys.garbageman_mk_2.GarbageLoader.GarbageType;

public class GarbageItem {
	
	private List<Object> images;
	
	private String name;
	private Object image;
	GarbageType type;
	GarbageTier tier;
	private float nast;
	private Point loc;
	
	public GarbageItem(String name, List<Object> in_images, GarbageType type, GarbageTier tier, float nast) {
		this.name = name;
		this.images = in_images;
		this.nast = nast;
		this.type = type;
		this.tier = tier;
		this.loc = new Point(0, 0);
		
		if(this.nast <= GarbageLoader.NAST_ROT_CUTOFF) {
			this.image = in_images.get(0);
		}
		else if(this.nast <= GarbageLoader.NAST_STALE_CUTOFF) {
			this.image = in_images.get(1);
		}
		else {
			this.image = in_images.get(2);
		}
		
	}
	
	public void setLocation(int x, int y) {
		loc.setLocation(x, y);
	}
	
	public Point getLocation() {
		return loc;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setNast(float newNast) {
		this.nast = newNast;
		setImage();
	}
	
	public float getNast() {
		return this.nast;
	}
	
	private void setImage() {
		if(this.nast <= GarbageLoader.NAST_ROT_CUTOFF) {
			this.image = images.get(0);
		}
		else if(this.nast <= GarbageLoader.NAST_STALE_CUTOFF) {
			this.image = images.get(1);
		}
		else {
			this.image = images.get(2);
		}
	}
	
	public Object getImage() {
		return image;
	}
	
	public GarbageType getType() {
		return this.type;
	}
	
	public GarbageTier getTier() {
		return this.tier;
	}
	
}
