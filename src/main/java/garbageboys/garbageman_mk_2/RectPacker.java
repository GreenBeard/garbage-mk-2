package garbageboys.garbageman_mk_2;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
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
	private class PerimTile {
		int x;
		int y;
		OffDirection dir;
	}

	private static final int normal_empty_tile = -1;
	private static final int perim_tile = -2;

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

	public boolean pack(final List<Rect> input, List<Rect> placed,
			int pack_width, int pack_height, int check_count, boolean enable_insanity) {
		assert(input != null);
		assert(placed != null);
		assert(pack_width > 0 && pack_height > 0);

		int[] filled_tiles = new int[pack_width * pack_height];
		/* clear ids */
		Arrays.fill(filled_tiles, normal_empty_tile);
		
		List<Rect> input_sorted = new ArrayList<Rect>(input);
		List<PerimTile> perim_tiles = new ArrayList<PerimTile>();

		/* Lay all rects so they are longer horizontally */
		for (Rect rect : input_sorted) {
			if (rect.width < rect.height) {
				int tmp = rect.height;
				rect.height = rect.width;
				rect.width = tmp;
			}
		}

		/* Place largest items towards index 0 */
		input_sorted.sort(new Comparator<Rect>() {
			@Override
			public int compare(Rect a, Rect b) {
				float a_value = a.width + a.height;
				float b_value = b.width + b.height;
				
				if (a_value == b_value) {
					return 0;
				} else if (a_value < b_value) {
					return 1;
				} else {
					return -1;
				}
			}
		});

		if (input.size() == 0) {
			return true;
		} else {
			Rect corner_rect = input_sorted.get(0);
			corner_rect.x = 0;
			corner_rect.y = 0;
			place_rect(placed, perim_tiles, corner_rect, filled_tiles, pack_width, pack_height);
			input_sorted.remove(0);
		}

		while (input_sorted.size() > 0) {
			boolean placed_rect = false;
			/* TODO check flipped ones */
			if (enable_insanity) {
				ArrayList<Pair<Rect, OffDirection>> empty_rects = new ArrayList<Pair<Rect, OffDirection>>();
				for (int i = 0; i < placed.size(); ++i) {
					Rect old = placed.get(i);
					for (Direction dir : Direction.values()) {
						add_empty_rects(dir, old, empty_rects, placed, filled_tiles, pack_width, pack_height);
					}
				}
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
					if (chosen_pair != null && percentage > 0.98) {
						place_rect_align(rect, chosen_pair.first, chosen_pair.second);
						place_rect(placed, perim_tiles, rect, filled_tiles, pack_width, pack_height);
						input_sorted.remove(rect);
						placed_rect = true;
						break;
					}
				}
			}
			if (!placed_rect) {
				/* Place largest shape maximizing perimeter touching */
				int top_count = check_count;
				int top_picks = input_sorted.size() >= top_count ? top_count : input_sorted.size();
				PerimInfo perim_info = new PerimInfo();
				perim_info.perimeter = -1;
				Rect place_rect = null;
				Rect real_rect = null;
				for (int i = 0; i < top_picks; ++i) {
					Rect rect = input_sorted.get(i);
					PerimInfo info = place_maximize_perim(rect, placed, perim_tiles, filled_tiles, pack_width, pack_height);
					if (info.perimeter != -1) {
						//info.perimeter += (float) input_sorted.size() / max_input_sorted_size * Math.sqrt(rect.width * rect.height);
						//info.perimeter += 100.0f / info.spots;
						//info.perimeter += 5 - i;
						if (info.perimeter > perim_info.perimeter) {
							place_rect = rect;
							real_rect = rect;
							perim_info = info;
						}
					}
					Rect rect_flipped = new Rect();
					rect_flipped.width = rect.height;
					rect_flipped.height = rect.width;
					rect_flipped.user_data = rect.user_data;
					PerimInfo flipped_info = place_maximize_perim(rect_flipped, placed, perim_tiles, filled_tiles, pack_width, pack_height);
					if (flipped_info.perimeter != -1) {
						//flipped_info.perimeter += (float) input_sorted.size() / max_input_sorted_size * Math.sqrt(rect_flipped.width * rect_flipped.height);
						//flipped_info.perimeter += 100.0f / flipped_info.spots;
						//flipped_info.perimeter += 5 - i;
						flipped_info.perimeter *= 0.95;
						if (flipped_info.perimeter > perim_info.perimeter) {
							place_rect = rect_flipped;
							real_rect = rect;
							perim_info = flipped_info;
						}
					}
				}
				if (perim_info.perimeter != -1) {
					place_rect.x = perim_info.x;
					place_rect.y = perim_info.y;
					place_rect(placed, perim_tiles, place_rect, filled_tiles, pack_width, pack_height);
					input_sorted.remove(real_rect);
					placed_rect = true;
				} else {
					//print_perim(filled_tiles, pack_width, pack_height, 0);
					//print_placed(placed, pack_width, pack_height, 0);
					return false;
				}
			}

			//print_perim(filled_tiles, pack_width, pack_width, placed.size());
			//print_placed(placed, pack_width, pack_width, placed.size());
			//System.out.printf("%3d/%3d complete", placed.size(), max_input_sorted_size);
			//System.out.printf(" (%.2f)\n", 100.0f * area_of(placed) / (area_of(input_sorted) + area_of(placed)));
		}
		return true;
	}

	@SuppressWarnings("unused")
	private int area_of(List<Rect> placed) {
		int area = 0;
		for (Rect r : placed) {
			area += r.width * r.height;
		}
		return area;
	}

	private class PerimInfo {
		float perimeter;
		int x;
		int y;
		PerimInfo() {
			this.perimeter = -1;
			this.x = 0;
			this.y = 0;
		}
	}

	private PerimInfo place_maximize_perim(Rect largest_rect, List<Rect> placed,
			List<PerimTile> perim_tiles,
			final int[] filled_tiles, int pack_width, int pack_height) {
		PerimInfo info = new PerimInfo();
		/*for (int i = 0; i <= pack_width - largest_rect.width; ++i) {
		for (int j = 0; j <= pack_height - largest_rect.height; ++j) {
			if (filled_tiles[i + j * pack_width] == perim_tile
					&& */
		for (PerimTile tile : perim_tiles) {
			int i = tile.x;
			int j = tile.y;
			Rect align_rect = new Rect();
			align_rect.width = 1;
			align_rect.height = 1;
			align_rect.x = i;
			align_rect.y = j;
			Rect out_rect = new Rect();
			out_rect.width = largest_rect.width;
			out_rect.height = largest_rect.height;
			place_rect_align(out_rect, align_rect, tile.dir);
			i = out_rect.x;
			j = out_rect.y;
			if (i >= 0 && j >= 0
					&& i + largest_rect.width <= pack_width
					&& j + largest_rect.height <= pack_height) {
				//if (!check_intersect(largest_rect, i, j, filled_tiles, pack_width, pack_height)) {
				if (!check_intersect_b(largest_rect, i, j, placed)) {
					int tmp_perim = find_perimeter_intersect(placed, largest_rect, i, j, filled_tiles, pack_width, pack_height);
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

	private int find_perimeter_intersect(List<Rect> placed, Rect rect,
			int x, int y,
			final int[] filled_tiles, int pack_width, int pack_height) {
		int perim = 0;
		int j_start = y;
		int j_end = y + rect.height;
		int i_start = x;
		int i_end = x + rect.width;
		assert(j_start >= 0 && j_end <= pack_height);
		assert(i_start >= 0 && i_end <= pack_width);
		if (i_start - 1 > 0) {
			for (int j = j_start; j < j_end; ++j) {
				int pos = i_start - 1 + pack_width * j;
				if (filled_tiles[pos] >= 0) {
					++perim;
				}
			}
		} else {
			perim += rect.width;
		}
		if (i_end < pack_width) {
			for (int j = j_start; j < j_end; ++j) {
				int pos = i_end + pack_width * j;
				if (filled_tiles[pos] >= 0) {
					++perim;
				}
			}
		} else {
			perim += rect.width;
		}
		if (j_start - 1 > 0) {
			for (int i = i_start; i < i_end; ++i) {
				int pos = i + pack_width * (j_start - 1);
				if (filled_tiles[pos] >= 0) {
					++perim;
				}
			}
		} else {
			perim += rect.height;
		}
		if (j_end < pack_height) {
			for (int i = i_start; i < i_end; ++i) {
				int pos = i + pack_width * j_end;
				if (filled_tiles[pos] >= 0) {
					++perim;
				}
			}
		} else {
			perim += rect.height;
		}
		return perim;
	}

	private void place_rect_align(Rect rect, Rect empty_rect, OffDirection dir) {
		switch (dir) {
			case NORTHEAST:
			case NORTHWEST:
				rect.y = empty_rect.y + empty_rect.height - rect.height;
				break;
			case SOUTHEAST:
			case SOUTHWEST:
				rect.y = empty_rect.y;
				break;
		}
		switch (dir) {
			case NORTHEAST:
			case SOUTHEAST:
				rect.x = empty_rect.x + empty_rect.width - rect.width;
				break;
			case NORTHWEST:
			case SOUTHWEST:
				rect.x = empty_rect.x;
				break;
		}
	}

	private void place_rect(List<Rect> placed, List<PerimTile> perim_tiles, Rect rect,
			int[] filled_tiles, int pack_width, int pack_height) {
		int j_start = rect.y;
		int j_end = rect.y + rect.height;
		int i_start = rect.x;
		int i_end = rect.x + rect.width;
		assert(j_start >= 0 && j_end <= pack_height);
		assert(i_start >= 0 && i_end <= pack_width);
		for (int j = j_start; j < j_end; ++j) {
			for (int i = i_start; i < i_end; ++i) {
				int pos = i + pack_width * j;
				if (filled_tiles[pos] == perim_tile) {
					for (int k = 0; k < perim_tiles.size(); ++k) {
						if (perim_tiles.get(k).x == i && perim_tiles.get(k).y == j) {
							perim_tiles.remove(k);
							break;
						}
					}
				}
				filled_tiles[pos] = placed.size();
			}
		}

		add_adjacent_perim(i_start, j_end - 1, OffDirection.NORTHWEST, perim_tiles, filled_tiles, pack_width, pack_height);
		add_adjacent_perim(i_start, j_start, OffDirection.SOUTHWEST, perim_tiles, filled_tiles, pack_width, pack_height);
		add_adjacent_perim(i_end - 1, j_end - 1, OffDirection.NORTHEAST, perim_tiles, filled_tiles, pack_width, pack_height);
		add_adjacent_perim(i_end - 1, j_start, OffDirection.SOUTHEAST, perim_tiles, filled_tiles, pack_width, pack_height);

		if (i_start - 1 >= 0) {
			for (int j = j_start; j < j_end; ++j) {
				int pos = i_start - 1 + pack_width * j;
				if (filled_tiles[pos] == normal_empty_tile) {
					filled_tiles[pos] = perim_tile;
					/*PerimTile tile = new PerimTile();
					tile.x = i_start - 1;
					tile.y = j;
					perim_tiles.add(tile);*/
				}
			}
		}
		if (i_end < pack_width) {
			for (int j = j_start; j < j_end; ++j) {
				int pos = i_end + pack_width * j;
				if (filled_tiles[pos] == normal_empty_tile) {
					filled_tiles[pos] = perim_tile;
					/*PerimTile tile = new PerimTile();
					tile.x = i_end;
					tile.y = j;
					perim_tiles.add(tile);*/
				}
			}
		}
		if (j_start - 1 >= 0) {
			for (int i = i_start; i < i_end; ++i) {
				int pos = i + pack_width * (j_start - 1);
				if (filled_tiles[pos] == normal_empty_tile) {
					filled_tiles[pos] = perim_tile;
					/*PerimTile tile = new PerimTile();
					tile.x = i;
					tile.y = j_start - 1;
					perim_tiles.add(tile);*/
				}
			}
		}
		if (j_end < pack_height) {
			for (int i = i_start; i < i_end; ++i) {
				int pos = i + pack_width * j_end;
				if (filled_tiles[pos] == normal_empty_tile) {
					filled_tiles[pos] = perim_tile;
					/*PerimTile tile = new PerimTile();
					tile.x = i;
					tile.y = j_end;
					perim_tiles.add(tile);*/
				}
			}
		}

		placed.add(rect);

		assert(validate_perim_tiles(filled_tiles, perim_tiles, pack_width, pack_height));
	}

	private OffDirection prefer_dir_calc(OffDirection inner_dir, Direction dir) {
		switch (dir) {
		case NORTH:
			switch (inner_dir) {
				case NORTHEAST:
				case SOUTHEAST:
					return OffDirection.NORTHEAST;
				case NORTHWEST:
				case SOUTHWEST:
					return OffDirection.NORTHWEST;
			}
		case SOUTH:
			switch (inner_dir) {
				case NORTHEAST:
				case SOUTHEAST:
					return OffDirection.SOUTHEAST;
				case NORTHWEST:
				case SOUTHWEST:
					return OffDirection.SOUTHWEST;
			}
		case EAST:
			switch (inner_dir) {
				case NORTHEAST:
				case NORTHWEST:
					return OffDirection.NORTHEAST;
				case SOUTHEAST:
				case SOUTHWEST:
					return OffDirection.SOUTHEAST;
			}
		case WEST:
			switch (inner_dir) {
				case NORTHEAST:
				case NORTHWEST:
					return OffDirection.NORTHWEST;
				case SOUTHEAST:
				case SOUTHWEST:
					return OffDirection.SOUTHWEST;
			}
		}
		return null;
	}

	private OffDirection perim_dir_calc(OffDirection inner_dir, Direction dir) {
		switch (dir) {
			case NORTH:
				return prefer_dir_calc(inner_dir, Direction.SOUTH);
			case SOUTH:
				return prefer_dir_calc(inner_dir, Direction.NORTH);
			case EAST:
				return prefer_dir_calc(inner_dir, Direction.WEST);
			case WEST:
				return prefer_dir_calc(inner_dir, Direction.EAST);
		}
		return null;
	}
	
	private void add_adjacent_perim(int x, int y,
			OffDirection inner_dir, List<PerimTile> perim_tiles,
			final int[] filled_tiles, int pack_width, int pack_height) {
		/* TODO fix truly horrifying code */
		Rect tmp_rect = new Rect();
		tmp_rect.x = x;
		tmp_rect.y = y;
		for (Direction dir : Direction.values()) {
			dir.increment(tmp_rect);
			PerimTile perim_tile = new PerimTile();
			perim_tile.x = tmp_rect.x;
			perim_tile.y = tmp_rect.y;
			perim_tile.dir = perim_dir_calc(inner_dir, dir);
			if (perim_tile.x >= 0 && perim_tile.x < pack_width
					&& perim_tile.y >= 0 && perim_tile.y < pack_height
					&& filled_tiles[perim_tile.x + pack_width * perim_tile.y] == normal_empty_tile) {
				perim_tiles.add(perim_tile);
			}
			dir.decrement(tmp_rect);
		}
	}

	private int before_first_empty(Direction dir, int x, int y, final List<Rect> placed,
			final int[] filled_tiles, int pack_width, int pack_height) {
		Rect single_rect = new Rect();
		single_rect.width = 1;
		single_rect.height = 1;
		single_rect.x = x;
		single_rect.y = y;
		while (find_point_intersect(placed, single_rect.x, single_rect.y, filled_tiles, pack_width, pack_height) != null) {
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
			final int[] filled_tiles, int pack_width, int pack_height) {
		int val_a;
		int val_b;
		OffDirection off_dir;
		switch (dir) {
			case SOUTH:
				/* left */
				val_a = before_first_empty(dir, old.x - 1, old.y - 1, placed, filled_tiles, pack_width, pack_height);
				/* right */
				val_b = before_first_empty(dir, old.x + old.width, old.y - 1, placed, filled_tiles, pack_width, pack_height);
				if (val_a < val_b) {
					off_dir = OffDirection.NORTHWEST;
				} else {
					off_dir = OffDirection.NORTHEAST;
				}
				break;
			case NORTH:
				/* left */
				val_a = before_first_empty(dir, old.x - 1, old.y + old.height, placed, filled_tiles, pack_width, pack_height);
				/* right */
				val_b = before_first_empty(dir, old.x + old.width, old.y + old.height, placed, filled_tiles, pack_width, pack_height);
				if (val_a > val_b) {
					off_dir = OffDirection.SOUTHWEST;
				} else {
					off_dir = OffDirection.SOUTHEAST;
				}
				break;
			case WEST:
				/* bottom */
				val_a = before_first_empty(dir, old.x - 1, old.y - 1, placed, filled_tiles, pack_width, pack_height);
				/* top */
				val_b = before_first_empty(dir, old.x - 1, old.y + old.height, placed, filled_tiles, pack_width, pack_height);
				if (val_a > val_b) {
					off_dir = OffDirection.NORTHEAST;
				} else {
					off_dir = OffDirection.SOUTHEAST;
				}
				break;
			case EAST:
				/* bottom */
				val_a = before_first_empty(dir, old.x + old.width, old.y - 1, placed, filled_tiles, pack_width, pack_height);
				/* top */
				val_b = before_first_empty(dir, old.x + old.width, old.y + old.height, placed, filled_tiles, pack_width, pack_height);
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
		List<Rect> poten_intersects = find_intersects(placed, potential_rect, filled_tiles, pack_width, pack_height);
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
			if (check_intersect(potential_rect, filled_tiles, pack_width, pack_height)) {
				print_placed(placed, pack_width, pack_height, 0);
				assert(false);
			}
			empty_rects.add(new Pair<Rect, OffDirection>(potential_rect, off_dir));
		}
	}

	public void print_perim(int[] tiles, int width, int height, int file_number) {
		try {
			File file = new File("./perim_image" + file_number + ".ppm");
			BufferedOutputStream stream = new BufferedOutputStream(new  FileOutputStream(file));
			stream.write(("P5 " + width + " " + height + " 1\n").getBytes(StandardCharsets.US_ASCII));
			for (int j = height - 1; j >= 0; --j) {
				for (int i = 0; i < width; ++i) {
					if (tiles[i + j * width] == perim_tile) {
						stream.write(0);
					} else {
						stream.write(1);
					}
				}
			}
			stream.flush();
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void print_placed(List<Rect> placed, int width, int height, int file_number) {
		try {
			File file = new File("./image" + file_number + ".ppm");
			BufferedOutputStream stream = new BufferedOutputStream(new  FileOutputStream(file));
			stream.write(("P5 " + width + " " + height + " 255\n").getBytes(StandardCharsets.US_ASCII));
			Rect test_rect = new Rect();
			test_rect.width = 1;
			test_rect.height = 1;
			for (int j = height - 1; j >= 0; --j) {
				for (int i = 0; i < width; ++i) {
					test_rect.x = i;
					test_rect.y = j;
					Rect rect = find_point_intersect_slow(placed, test_rect);
					if (rect != null) {
						int index = (8 * placed.indexOf(rect)) % 128;
						stream.write(index);
					} else {
						stream.write(255);
					}
				}
			}
			stream.flush();
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Rect find_point_intersect(List<Rect> placed, int x, int y,
			int[] filled_tiles, int pack_width, int pack_height) {
		if (x < 0 || y < 0
				|| x >= pack_width
				|| y >= pack_height) {
			return null;
		} else {
			int id = filled_tiles[x + pack_width * y];
			if (id >= 0) {
				return placed.get(id);
			} else {
				return null;
			}
		}
	}

	private boolean check_intersect(Rect test_rect,
			int[] filled_tiles, int pack_width, int pack_height) {
		return check_intersect(test_rect, test_rect.x, test_rect.y, filled_tiles, pack_width, pack_height);
	}

	int count = 0;
	private boolean check_intersect(Rect test_rect,
			int x, int y,
			int[] filled_tiles, int pack_width, int pack_height) {
		++count;
		int j_start = y;
		int j_end = y + test_rect.height;
		int i_start = x;
		int i_end = x + test_rect.width;
		assert(j_start >= 0 && j_end <= pack_height);
		assert(i_start >= 0 && i_end <= pack_width);
		/* Hint to JVM */
		assert(filled_tiles.length >= i_end - 1 + pack_width * (j_end - 1));
		for (int j = j_start; j < j_end; ++j) {
			for (int i = i_start; i < i_end; ++i) {
				int id = filled_tiles[i + pack_width * j];
				if (id >= 0) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean check_intersect_b(Rect test_rect,
			int x, int y,
			List<Rect> placed) {
  	for (Rect rect : placed) {
  		if (rect.x < x + test_rect.width
  				& x < rect.x + rect.width
  				& rect.y < y + test_rect.height
  				& y < rect.y + rect.height) {
  			return true;
  		}
  	}
		return false;
	}

	private List<Rect> find_intersects(List<Rect> placed, Rect test_rect,
			int[] filled_tiles, int pack_width, int pack_height) {
		HashSet<Integer> hash_rects = new HashSet<Integer>();
		int j_start = test_rect.y;
		int j_end = test_rect.y + test_rect.height;
		int i_start = test_rect.x;
		int i_end = test_rect.x + test_rect.width;
		assert(j_start >= 0 && j_end <= pack_height);
		assert(i_start >= 0 && i_end <= pack_width);
		for (int j = j_start; j < j_end; ++j) {
			for (int i = i_start; i < i_end; ++i) {
				int id = filled_tiles[i + pack_width * j];
				if (id >= 0) {
					hash_rects.add(Integer.valueOf(id));
				}
			}
		}
		List<Rect> rects = new ArrayList<Rect>();
		for (Integer i : hash_rects) {
			rects.add(placed.get(i.intValue()));
		}
		return rects;
	}

	private Rect find_point_intersect_slow(List<Rect> placed, Rect rect) {
		assert(rect.width == 1 && rect.height == 1);
		List<Rect> rects = find_intersects_slow(placed, rect);
		if (rects.size() > 0) {
			return rects.get(0);
		} else {
			return null;
		}
	}

  private List<Rect> find_intersects_slow(final List<Rect> placed, Rect test_rect) {
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

	public boolean validate(List<Rect> placed, int width, int height) {
		for (Rect rect : placed) {
			if (rect.x < 0 || rect.x + rect.width > width
					|| rect.y < 0 || rect.y + rect.height > height) {
				return false;
			}
			List<Rect> matches = find_intersects_slow(placed, rect);
			if (matches.size() != 1 || matches.get(0) != rect) {
				return false;
			}
		}
		return true;
	}

	private boolean validate_perim_tiles(int[] filled_tiles, List<PerimTile> perim_tiles,
			int pack_width, int pack_height) {
		for (PerimTile tile : perim_tiles) {
			if (filled_tiles[tile.x + pack_width * tile.y] != perim_tile) {
				return false;
			}
		}
		return true;
	}
}
