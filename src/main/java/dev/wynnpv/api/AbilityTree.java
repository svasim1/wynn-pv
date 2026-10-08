package dev.wynnpv.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * A class's ability tree with one character's choices marked. The layout comes from
 * {@code /ability/map/{class}}, names and descriptions from {@code /ability/tree/{class}} and the
 * choices from the character's {@code /abilities}, which lists every taken node and connector.
 *
 * <p>The tree is a grid 9 cells wide; {@code y} counts rows over all pages, 6 rows per page.
 */
public record AbilityTree(List<Node> nodes, Map<String, Ability> abilities, Set<Cell> taken, int rows) {
	public static final int COLUMNS = 9;
	public static final int ROWS_PER_PAGE = 6;

	public record Cell(int x, int y) {}

	/**
	 * A grid cell holding an ability or a connector. For abilities {@code style} is the icon name
	 * (e.g. "nodeYellow", "ultimateTrapper"), for connectors the directions it joins (e.g.
	 * "connector_right_down_left").
	 */
	/**
	 * {@code item} and {@code model} say how Wynncraft's resource pack draws an ability's icon: that
	 * item with that custom model data (e.g. "minecraft:potion" and 73). Null for connectors.
	 */
	public record Node(Cell cell, int page, boolean ability, @Nullable String id, String style, @Nullable String item,
		@Nullable Integer model) {
		public boolean connects(Direction direction) {
			return !ability && style.contains(direction.name().toLowerCase(java.util.Locale.ROOT));
		}
	}

	public enum Direction {
		UP(0, -1), RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0);

		public final int dx;
		public final int dy;

		Direction(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}
	}

	/** Name and description lines with Wynncraft's website markup (see {@code WynnText}). */
	public record Ability(String name, List<String> description, @Nullable List<String> locks) {}

	public boolean isTaken(Node node) {
		return taken.contains(node.cell());
	}

	/** Whether the line from a taken connector towards {@code direction} is part of the taken path. */
	public boolean isTakenTowards(Node node, Direction direction) {
		return isTaken(node) && taken.contains(new Cell(node.cell().x() + direction.dx, node.cell().y() + direction.dy));
	}

	public int takenAbilities() {
		return (int) nodes.stream().filter(node -> node.ability() && isTaken(node)).count();
	}

	public static AbilityTree parse(String mapJson, String treeJson, String characterJson) {
		List<Node> nodes = new ArrayList<>();
		int rows = 0;
		for (Map.Entry<String, JsonElement> page : JsonParser.parseString(mapJson).getAsJsonObject().entrySet()) {
			for (JsonElement element : page.getValue().getAsJsonArray()) {
				Node node = parseNode(element.getAsJsonObject());
				nodes.add(node);
				rows = Math.max(rows, node.cell().y());
			}
		}

		Map<String, Ability> abilities = new HashMap<>();
		JsonObject pages = JsonParser.parseString(treeJson).getAsJsonObject().getAsJsonObject("pages");
		for (Map.Entry<String, JsonElement> page : pages.entrySet()) {
			for (Map.Entry<String, JsonElement> entry : page.getValue().getAsJsonObject().entrySet()) {
				JsonObject ability = entry.getValue().getAsJsonObject();
				List<String> description = new ArrayList<>();
				if (ability.get("description") instanceof JsonArray lines) {
					lines.forEach(line -> description.add(line.getAsString()));
				}
				List<String> locks = null;
				if (ability.get("locks") instanceof JsonArray lockArray) {
					locks = new ArrayList<>();
					for (JsonElement lock : lockArray) {
						locks.add(lock.getAsString());
					}
				}
				abilities.put(entry.getKey(), new Ability(ability.get("name").getAsString(), List.copyOf(description), locks));
			}
		}

		Set<Cell> taken = new HashSet<>();
		for (JsonElement element : characterNodes(JsonParser.parseString(characterJson))) {
			taken.add(parseNode(element.getAsJsonObject()).cell());
		}
		return new AbilityTree(List.copyOf(nodes), Map.copyOf(abilities), Set.copyOf(taken), rows);
	}

	/** The docs describe nodes grouped by page; the live API (v3.7.2) sends one flat list. Accept both. */
	private static List<JsonElement> characterNodes(JsonElement root) {
		List<JsonElement> result = new ArrayList<>();
		if (root instanceof JsonArray array) {
			array.forEach(result::add);
		} else if (root instanceof JsonObject pages) {
			pages.entrySet().forEach(page -> page.getValue().getAsJsonArray().forEach(result::add));
		}
		return result;
	}

	private static Node parseNode(JsonObject node) {
		JsonObject coordinates = node.getAsJsonObject("coordinates");
		JsonObject meta = node.getAsJsonObject("meta");
		Cell cell = new Cell(coordinates.get("x").getAsInt(), coordinates.get("y").getAsInt());
		int page = meta.get("page").getAsInt();
		boolean ability = "ability".equals(node.get("type").getAsString());
		if (!ability) {
			return new Node(cell, page, false, null, meta.get("icon").getAsString(), null, null);
		}
		JsonObject value = meta.getAsJsonObject("icon").getAsJsonObject("value");
		String icon = value.get("name").getAsString();
		Integer model = null;
		if (value.get("customModelData") instanceof JsonObject data && data.get("rangeDispatch") instanceof JsonArray range
			&& !range.isEmpty()) {
			model = range.get(0).getAsInt();
		}
		return new Node(cell, page, true, meta.get("id").getAsString(), icon.substring(icon.indexOf('.') + 1),
			value.get("id").getAsString(), model);
	}
}
