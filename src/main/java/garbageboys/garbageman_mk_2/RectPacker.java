package garbageboys.garbageman_mk_2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RectPacker {
	private enum OffDirection {
		NORTHWEST,
		NORTHEAST,
		SOUTHWEST,
		SOUTHEAST;
	}
	private enum Direction {
		NORTH,
		SOUTH,
		EAST,
		WEST;
		
		void increment(Rect rec) {
			switch (this) {
				case NORTH:
					++rec.y;
					break;
				case SOUTH:
					--rec.y;
					break;
				case EAST:
					++rec.x;
					break;
				case WEST:
					--rec.x;
					break;
			}
		}
		void decrement(Rect rec) {
			switch (this) {
				case NORTH:
					--rec.y;
					break;
				case SOUTH:
					++rec.y;
					break;
				case EAST:
					--rec.x;
					break;
				case WEST:
					++rec.x;
					break;
			}
		}
	}
	class Pair<T1, T2> {
		T1 first;
		T2 second;
		public Pair(T1 first, T2 second) {
			this.first = first;
			this.second = second;
		}
	}
	
	private void setup_poten_rect(Rect potential_rect, Direction dir, Rect old, int val) {
		switch (dir) {
			case NORTH:
			case SOUTH:
				potential_rect.width = old.width;
				potential_rect.x = old.x;
				break;
			case EAST:
			case WEST:
				potential_rect.height = old.height;
				potential_rect.y = old.y;
				break;
			default:
				throw new RuntimeException();
		}
		switch (dir) {
			case NORTH:
				potential_rect.y = old.y + old.height;
				potential_rect.height = val + 1 - potential_rect.y;
				break;
			case SOUTH:
				potential_rect.y = val;
				potential_rect.height = old.y - potential_rect.y;
				break;
			case EAST:
				potential_rect.x = old.x + old.width;
				potential_rect.width = val + 1 - potential_rect.x;
				break;
			case WEST:
				potential_rect.x = val;
				potential_rect.width = old.x - potential_rect.x;
				break;
			default:
				throw new RuntimeException();
		}
	}

	public boolean pack(final List<Rect> input, List<Rect> placed, int pack_width, int pack_height) {
		assert(input != null);
		assert(placed != null);
		assert(pack_width > 0 && pack_height > 0);
		
		List<Rect> input_sorted = new ArrayList<Rect>(input);

		/* Lay all rects so they are longer horizontally */
		/*for (Rect rect : input_sorted) {
			if (rect.width < rect.height) {
				int tmp = rect.height;
				rect.height = rect.width;
				rect.width = tmp;
			}
		}*/
		
		/* Place largest items towards index 0 */
		input_sorted.sort(new Comparator<Rect>() {
			@Override
			public int compare(Rect a, Rect b) {
				int a_value = a.width * a.height;
				int b_value = b.width * b.height;
				
				if (a_value == b_value) {
					return 0;
				} else if (a_value < b_value) {
					return 1;
				} else {
					return -1;
				}
			}
		});
		
		while (input_sorted.size() > 0) {
			assert(validate(placed, pack_width, pack_height));
			ArrayList<Pair<Rect, OffDirection>> empty_rects = new ArrayList<Pair<Rect, OffDirection>>();
			for (int i = 0; i < placed.size(); ++i) {
				Rect old = placed.get(i);
				for (Direction dir : Direction.values()) {
					add_empty_rects(dir, old, empty_rects, placed, pack_width, pack_height);
				}
			}
			boolean placed_rect = false;
			for (int i = 0; i < input_sorted.size() / 2; ++i) {
				Rect rect = input_sorted.get(i);
				Pair<Rect, OffDirection> chosen_pair = null;
				float percentage = 0.0f;
				for (Pair<Rect, OffDirection> empty_pair : empty_rects) {
					Rect empty_rect = empty_pair.first;
					if (empty_rect.width >= rect.width && empty_rect.height >= rect.height) {
						float tmp_percentage = rect.width * rect.height / (empty_rect.width * empty_rect.height);
						if (percentage < tmp_percentage) {
							percentage = tmp_percentage;
							chosen_pair = empty_pair;
						}
					}
				}
				if (chosen_pair != null && percentage > 0.90) {
					int x;
					int y;
					Rect empty_rect = chosen_pair.first;
					switch (chosen_pair.second) {
						case NORTHEAST:
						case NORTHWEST:
							y = empty_rect.y + empty_rect.height - rect.height;
							break;
						case SOUTHEAST:
						case SOUTHWEST:
							y = empty_rect.y;
							break;
						default:
							throw new RuntimeException();
					}
					switch (chosen_pair.second) {
						case NORTHEAST:
						case SOUTHEAST:
							x = empty_rect.x + empty_rect.width - rect.width;
							break;
						case NORTHWEST:
						case SOUTHWEST:
							x = empty_rect.x;
							break;
						default:
							throw new RuntimeException();
					}
					rect.x = x;
					rect.y = y;
					placed.add(rect);
					input_sorted.remove(rect);
					placed_rect = true;
					break;
				}
			}
			if (!placed_rect) {
				/* Place largest shape maximizing perimeter touching */
				Rect largest_rect = input_sorted.get(0);
				Rect largest_rect_flipped = new Rect();
				largest_rect_flipped.width = largest_rect.height;
				largest_rect_flipped.height = largest_rect.width;
				largest_rect_flipped.id = largest_rect.id;
				PerimInfo info = place_maximize_perim(largest_rect, placed, pack_width, pack_height);
				PerimInfo info_flipped = place_maximize_perim(largest_rect_flipped, placed, pack_width, pack_height);
				if (info.perimeter >= 0 || info_flipped.perimeter >= 0) {
					if (info.perimeter > info_flipped.perimeter) {
						largest_rect.x = info.x;
						largest_rect.y = info.y;
						placed.add(largest_rect);
					} else {
						largest_rect_flipped.x = info_flipped.x;
						largest_rect_flipped.y = info_flipped.y;
						placed.add(largest_rect_flipped);
					}
					input_sorted.remove(largest_rect);
					placed_rect = true;
				} else {
					print_placed(placed, pack_width, pack_height);
					return false;
				}
			}
		}
		return true;
	}

	private class PerimInfo {
		int perimeter;
		int x;
		int y;
		PerimInfo() {
			this.perimeter = -1;
			this.x = 0;
			this.y = 0;
		}
	}

	private PerimInfo place_maximize_perim(Rect largest_rect, List<Rect> placed, int pack_width, int pack_height) {
		PerimInfo info = new PerimInfo();
		for (int i = 0; i <= pack_width - largest_rect.width; ++i) {
			for (int j = 0; j <= pack_height - largest_rect.height; ++j) {
				largest_rect.x = i;
				largest_rect.y = j;
				if (find_intersects(placed, largest_rect).size() == 0) {
					int tmp_perim = find_perimeter_intersect(placed, largest_rect);
					if (info.perimeter < tmp_perim) {
						info.perimeter = tmp_perim;
						info.x = i;
						info.y = j;
					}
				}
			}
		}
		return info;
	}
	
	private int find_perimeter_intersect(List<Rect> placed, Rect rect) {
		int perim = 0;
		Rect test_rect = new Rect();
		test_rect.width = 1;
		test_rect.height = 1;
		{
			/* top */
			test_rect.y = rect.y + rect.height;
			for (int i = 0; i < rect.width; ++i) {
				test_rect.x = i + rect.x;
				Rect tmp_rect = find_point_intersect(placed, test_rect);
				if (tmp_rect != null) {
					++perim;
				}
			}
		}
		{
			/* bot */
			test_rect.y = rect.y - 1;
			for (int i = 0; i < rect.width; ++i) {
				test_rect.x = i + rect.x;
				Rect tmp_rect = find_point_intersect(placed, test_rect);
				if (tmp_rect != null) {
					++perim;
				}
			}
		}
		{
			/* left */
			test_rect.x = rect.x - 1;
			for (int i = 0; i < rect.height; ++i) {
				test_rect.y = i + rect.y;
				Rect tmp_rect = find_point_intersect(placed, test_rect);
				if (tmp_rect != null) {
					++perim;
				}
			}
		}
		{
			/* right */
			test_rect.x = rect.x + rect.height;
			for (int i = 0; i < rect.height; ++i) {
				test_rect.y = i + rect.y;
				Rect tmp_rect = find_point_intersect(placed, test_rect);
				if (tmp_rect != null) {
					++perim;
				}
			}
		}
		return perim;
	}

	private int before_first_empty(Direction dir, int x, int y, final List<Rect> placed) {
		Rect single_rect = new Rect();
		single_rect.width = 1;
		single_rect.height = 1;
		single_rect.x = x;
		single_rect.y = y;
		while (find_point_intersect(placed, single_rect) != null) {
			dir.increment(single_rect);
		}
		dir.decrement(single_rect);
		switch (dir) {
			case NORTH:
			case SOUTH:
				return single_rect.y;
			case EAST:
			case WEST:
				return single_rect.x;
			default:
				assert(false);
				return -1;
		}
	}
	
	private void add_empty_rects(final Direction dir, final Rect old,
			ArrayList<Pair<Rect, OffDirection>> empty_rects, final List<Rect> placed,
			int pack_width, int pack_height) {
		int val_a;
		int val_b;
		OffDirection off_dir;
		switch (dir) {
			case SOUTH:
				/* left */
				val_a = before_first_empty(dir, old.x - 1, old.y - 1, placed);
				/* right */
				val_b = before_first_empty(dir, old.x + old.width, old.y - 1, placed);
				if (val_a < val_b) {
					off_dir = OffDirection.NORTHWEST;
				} else {
					off_dir = OffDirection.NORTHEAST;
				}
				break;
			case NORTH:
				/* left */
				val_a = before_first_empty(dir, old.x - 1, old.y + old.height, placed);
				/* right */
				val_b = before_first_empty(dir, old.x + old.width, old.y + old.height, placed);
				if (val_a > val_b) {
					off_dir = OffDirection.SOUTHWEST;
				} else {
					off_dir = OffDirection.SOUTHEAST;
				}
				break;
			case WEST:
				/* bottom */
				val_a = before_first_empty(dir, old.x - 1, old.y - 1, placed);
				/* top */
				val_b = before_first_empty(dir, old.x - 1, old.y + old.height, placed);
				if (val_a > val_b) {
					off_dir = OffDirection.NORTHEAST;
				} else {
					off_dir = OffDirection.SOUTHEAST;
				}
				break;
			case EAST:
				/* bottom */
				val_a = before_first_empty(dir, old.x + old.width, old.y - 1, placed);
				/* top */
				val_b = before_first_empty(dir, old.x + old.width, old.y + old.height, placed);
				if (val_a < val_b) {
					off_dir = OffDirection.NORTHWEST;
				} else {
					off_dir = OffDirection.SOUTHWEST;
				}
				break;
			default:
				throw new RuntimeException();
		}

		int val;
		switch (dir) {
			case SOUTH:
			case WEST:
				val = val_a < val_b ? val_a : val_b;
				break;
			case NORTH:
			case EAST:
				val = val_a > val_b ? val_a : val_b;
				break;
			default:
				throw new RuntimeException();
		}

		Rect potential_rect = new Rect();
		setup_poten_rect(potential_rect, dir, old, val);
		List<Rect> poten_intersects = find_intersects(placed, potential_rect);
		if (poten_intersects.size() > 0) {
			switch (dir) {
				case NORTH:
					sort_by_bot(poten_intersects);
					break;
				case SOUTH:
					sort_by_top(poten_intersects);
					break;
				case EAST:
					sort_by_left(poten_intersects);
					break;
				case WEST:
					sort_by_right(poten_intersects);
					break;
				default:
					throw new RuntimeException();
			}
			int new_val;
			Rect tmp_rect;
			switch (dir) {
				case NORTH:
				case EAST:
					tmp_rect = poten_intersects.get(0);
					break;
				case SOUTH:
				case WEST:
					tmp_rect = poten_intersects.get(poten_intersects.size() - 1);
					break;
				default:
					throw new RuntimeException();
			}
			switch (dir) {
				case NORTH:
					new_val = tmp_rect.y - 1;
					break;
				case SOUTH:
					new_val = tmp_rect.y + tmp_rect.height;
					break;
				case EAST:
					new_val = tmp_rect.x - 1;
					break;
				case WEST:
					new_val = tmp_rect.x + tmp_rect.width;
					break;
				default:
					throw new RuntimeException();
			}
			setup_poten_rect(potential_rect, dir, old, new_val);
		}
		if (potential_rect.height > 0 && potential_rect.width > 0) {
			if (potential_rect.x < 0 || potential_rect.y < 0
					|| potential_rect.x + potential_rect.width > pack_width
					|| potential_rect.y + potential_rect.height > pack_height) {
				assert(false);
			}
			if (find_intersects(placed, potential_rect).size() != 0) {
				print_placed(placed, pack_width, pack_height);
				assert(false);
			}
			empty_rects.add(new Pair<Rect, OffDirection>(potential_rect, off_dir));
		}
	}
	
	public void print_placed(List<Rect> placed, int width, int height) {
		char default_char = '-';
		String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Rect test_rect = new Rect();
		test_rect.width = 1;
		test_rect.height = 1;
		for (int j = width - 1; j >= 0; --j) {
			for (int i = 0; i < height; ++i) {
				test_rect.x = i;
				test_rect.y = j;
				Rect rect = find_point_intersect(placed, test_rect);
				if (rect != null) {
					int index = placed.indexOf(rect) % chars.length();
					System.out.print(chars.charAt(index));
				} else {
					System.out.print(default_char);
				}
			}
			System.out.println();
		}
	}

	private Rect find_point_intersect(List<Rect> placed, Rect rect) {
		assert(rect.width == 1 && rect.height == 1);
		List<Rect> rects = find_intersects(placed, rect);
		if (rects.size() > 0) {
			return rects.get(0);
		} else {
			return null;
		}
	}

	private void sort_by_bot(List<Rect> intersects) {
		intersects.sort(new Comparator<Rect>() {
			@Override
			public int compare(Rect a, Rect b) {
				if (a.y == b.y) {
					return 0;
				} else if (a.y > b.y) {
					return 1;
				} else {
					return -1;
				}
			}
		});
	}

	private void sort_by_left(List<Rect> intersects) {
		intersects.sort(new Comparator<Rect>() {
			@Override
			public int compare(Rect a, Rect b) {
				if (a.x == b.x) {
					return 0;
				} else if (a.x > b.x) {
					return 1;
				} else {
					return -1;
				}
			}
		});
	}
	private void sort_by_right(List<Rect> intersects) {
		intersects.sort(new Comparator<Rect>() {
			@Override
			public int compare(Rect a, Rect b) {
				if (a.x + a.width == b.x + b.width) {
					return 0;
				} else if (a.x + a.width > b.x + b.width) {
					return 1;
				} else {
					return -1;
				}
			}
		});
	}
	private void sort_by_top(List<Rect> intersects) {
		intersects.sort(new Comparator<Rect>() {
			@Override
			public int compare(Rect a, Rect b) {
				if (a.y + a.height == b.y + b.height) {
					return 0;
				} else if (a.y + a.height > b.y + b.height) {
					return 1;
				} else {
					return -1;
				}
			}
		});
	}

	private List<Rect> find_intersects(final List<Rect> placed, Rect test_rect) {
		List<Rect> rects = new ArrayList<Rect>();
		if (test_rect.width == 0 || test_rect.height == 0) {
			return rects;
		}
		for (Rect rect : placed) {
	    if (rect.x < test_rect.x + test_rect.width
	    		&& test_rect.x < rect.x + rect.width
	    		&& rect.y < test_rect.y + test_rect.height
	    		&& test_rect.y < rect.y + rect.height) {
	    	rects.add(rect);
	    }
		}
		return rects;
	}

	public boolean validate(List<Rect> placed, int width, int height) {
		for (Rect rect : placed) {
			if (rect.x < 0 || rect.x + rect.width > width
					|| rect.y < 0 || rect.y + rect.height > height) {
				return false;
			}
			List<Rect> matches = find_intersects(placed, rect);
			if (matches.size() != 1 || matches.get(0) != rect) {
				return false;
			}
		}
		return true;
	}
}
