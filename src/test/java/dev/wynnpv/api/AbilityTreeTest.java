package dev.wynnpv.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Real API responses saved on 2026-10-04: the archer tree layout, its pages 1-3 of descriptions,
 * and an archer's taken abilities (a flat list, unlike the paged shape in the docs).
 */
class AbilityTreeTest {
	private static String fixture(String name) throws IOException {
		try (InputStream in = AbilityTreeTest.class.getResourceAsStream("/" + name)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static AbilityTree archer() throws IOException {
		return AbilityTree.parse(fixture("ability-map-archer.json"), fixture("ability-tree-archer-pages-1-3.json"),
			fixture("abilities-character.json"));
	}

	private static AbilityTree.Node at(AbilityTree tree, int x, int y) {
		return tree.nodes().stream().filter(n -> n.cell().x() == x && n.cell().y() == y).findFirst().orElseThrow();
	}

	@Test
	void readsLayout() throws IOException {
		AbilityTree tree = archer();
		assertEquals(52, tree.rows());
		AbilityTree.Node arrowBomb = at(tree, 5, 1);
		assertTrue(arrowBomb.ability());
		assertEquals("arrowbomb", arrowBomb.id());
		assertEquals("nodeArcher", arrowBomb.style());
		assertEquals("Arrow Bomb", dev.wynnpv.ui.WynnText.plain(tree.abilities().get("arrowbomb").name()));
	}

	@Test
	void marksTakenAbilities() throws IOException {
		AbilityTree tree = archer();
		assertEquals(13, tree.takenAbilities());
		assertTrue(tree.isTaken(at(tree, 5, 1)));
		// Arrow Bomb Cost I sits next to Bow Proficiency but was not taken.
		assertFalse(tree.isTaken(at(tree, 7, 3)));
	}

	@Test
	void lightsOnlyTakenBranchesOfAConnector() throws IOException {
		AbilityTree tree = archer();
		// A junction by Windy Feet where all three branches were taken.
		AbilityTree.Node junction = at(tree, 2, 10);
		assertTrue(junction.connects(AbilityTree.Direction.RIGHT));
		assertTrue(tree.isTakenTowards(junction, AbilityTree.Direction.RIGHT));
		assertTrue(tree.isTakenTowards(junction, AbilityTree.Direction.DOWN));
		assertTrue(tree.isTakenTowards(junction, AbilityTree.Direction.LEFT));
		AbilityTree.Node untaken = at(tree, 6, 3);
		assertFalse(tree.isTakenTowards(untaken, AbilityTree.Direction.RIGHT));
	}
}
