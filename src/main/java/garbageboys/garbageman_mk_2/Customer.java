package garbageboys.garbageman_mk_2;

import java.awt.Point;

public interface Customer {
	
	public int getPickiness();
	
	public void setWalkToPoint(int x);
	
	public int getWalkToPoint();
	
	public void incrementCustomerFrame();
	
	public Object getCustomerImage();
	
	public boolean stepTowardsPoint();
}
