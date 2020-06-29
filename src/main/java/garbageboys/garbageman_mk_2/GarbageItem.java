package garbageboys.garbageman_mk_2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GarbageItem {

	enum GarbageType {
		Junk, Meat, Veggie, Wrap, Filler, Sweetener, Sauce;
	};

	enum GarbageTier {
		Junk(0.5), Purchased(0), Common(0.3), Uncommon(0.12), Rare(0.0495), VeryRare(0.025), Legendary(0.005),
		QuestionMarkQuestionMarkQuestionMark(0.0005);

		private final double probability;

		private GarbageTier(final double probability) {
			this.probability = probability;
		}

		public double getProbability() {
			return probability;
		}
	};

	private static int number_of_items;
	
	private static ArrayList<String> names = new ArrayList<String>();
	private static ArrayList<String> files = new ArrayList<String>();
	private static ArrayList<List<Object>> images = new ArrayList<List<Object>>();
	private static ArrayList<GarbageType> types = new ArrayList<GarbageType>();
	private static ArrayList<GarbageTier> tiers = new ArrayList<GarbageTier>();
	
	private static String name;
	private static String file;
	private static List<Object> image;
	private static GarbageType type;
	private static GarbageTier tier;

	int identifier;

	public GarbageItem(String file) {
		int name_index = files.indexOf(file);
		if (name_index != -1) {
			identifier = name_index;
		} else {
			System.err.println("GarbageItemError: file not found - " + file);
		}
		load_from_identifier();
	}

	public GarbageItem() {
		identifier = generate_identifier();
		load_from_identifier();
	}
	
	public String getName() {
		return name;
	}
	
	public String getFile() {
		return file;
	}
	
	public List<Object> getImage() {
		return image;
	}
	
	public GarbageType getType() {
		return type;
	}
	
	public GarbageTier getTier() {
		return tier;
	}

	public static void init_garbage_items(Render2D renderer) {
		ArrayList<ArrayList<String>> ingredientData = ResourceLoader.getIngredientData();
		ArrayList<String> tableTitles = ingredientData.get(0);
		
		for (int i = 0; i < tableTitles.size(); i++) {
			String title = tableTitles.get(i);
			
			for (int j = 1; j < ingredientData.size(); j++) {
				
				ArrayList<String> row = ingredientData.get(j);
				String cell = row.get(i);
				
				switch (title.toLowerCase()) {
				case "file": {
					files.add(cell);
					List<Object> image = renderer.loadImageSeries("/assets/Garbage/" + cell, 32, 32, 3);
					images.add(image);
					break;
				}
				case "name": {
					names.add(cell);
					break;
				}
				case "type": {
					boolean found = false;
					for (GarbageType type : GarbageType.values()) {
						if (type.name().equalsIgnoreCase(cell)) {
							found = true;
							types.add(type);
							break;
						}
					}
					if (!found) {
						System.err.println("GarbageItemError: Type missing for " + String.join("\t", row));
					}
					break;
				}
				case "tier": {
					boolean found = false;
					for (GarbageTier tier : GarbageTier.values()) {
						if (tier.name().equalsIgnoreCase(cell)) {
							found = true;
							tiers.add(tier);
							break;
						}
					}
					if (!found) {
						System.err.println("GarbageItemError: Tier missing for " + String.join("\t", row));
					}
					break;
				}
				}
			}
		}
		number_of_items = files.size();
	}

	private int generate_identifier() {
		Random rand = new Random(); 
		double random = rand.nextDouble();
		double used_probability = 0;
		GarbageTier tier_choice = GarbageTier.values()[0];
		for (GarbageTier tier : GarbageTier.values()) {
			if (random < used_probability + tier.getProbability()) {
				tier_choice = tier;
				break;
			} else {
				used_probability += tier.getProbability();
			}
		}
		ArrayList<Integer> choices = new ArrayList<Integer>();
		for (int i = 0; i < number_of_items; i++) {
			if (tiers.get(i) == tier_choice) {
				choices.add(i);
			}
		}
		
		return choices.get(rand.nextInt(choices.size()));
	}
	
	private void load_from_identifier() {
		name = names.get(identifier);
		file = files.get(identifier);
		image = images.get(identifier);
		type = types.get(identifier);
		tier = tiers.get(identifier);
	}
}
