package dev.wynnpv.gametest;

import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.ui.CharacterScreen;
import dev.wynnpv.ui.ProfileScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Opens a real profile from the live Wynncraft API and screenshots every page of the book at
 * several window sizes and GUI scales, to catch layout that breaks when the screen is small or big.
 */
public class ProfileScreenGameTest implements FabricClientGameTest {
	// Salted, Wynncraft's owner, has a public profile; their level 120 archer has abilities taken.
	private static final String PLAYER = "1ed075fc-5aa9-42e0-a29f-640326c1d80c";
	private static final String ARCHER = "92ddbed6-cbab-49ac-bdb0-5385f4ceab91";

	/** Window width, height and GUI scale; the comment is the resulting GUI size. */
	private static final int[][] SIZES = {
		{854, 480, 2},   // 427x240, the default window
		{1280, 960, 4},  // 320x240, Minecraft's smallest GUI
		{1920, 1080, 4}, // 480x270
		{1920, 1080, 3}, // 640x360
		{1920, 1080, 2}, // 960x540
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		ProfileScreen profileScreen = context.computeOnClient(client -> {
			ProfileScreen screen = new ProfileScreen(PLAYER);
			client.setScreen(screen);
			return screen;
		});
		context.waitFor(client -> profileScreen.profile() != null, 20 * 30);

		for (int[] size : SIZES) {
			String name = (size[0] / size[2]) + "x" + (size[1] / size[2]);
			context.getInput().resizeWindow(size[0], size[1]);
			context.runOnClient(client -> {
				client.options.guiScale().set(size[2]);
				client.resizeDisplay();
			});
			// Keep the mouse in a corner, off the pages.
			context.getInput().setCursorPos(2, 2);

			context.runOnClient(client -> client.setScreen(profileScreen));
			context.waitTicks(3);
			context.takeScreenshot(name + "-1-profile-characters");
			context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
			context.waitTicks(2);
			context.takeScreenshot(name + "-2-profile-deeds");
			context.getInput().pressKey(GLFW.GLFW_KEY_LEFT);

			context.runOnClient(client -> {
				PlayerProfile profile = profileScreen.profile();
				PlayerProfile.Character archer = profile.characters().stream()
					.filter(c -> c.uuid().equals(ARCHER)).findFirst().orElseThrow();
				client.setScreen(new CharacterScreen(profileScreen, profile, archer));
			});
			context.waitTicks(3);
			context.takeScreenshot(name + "-3-character-deeds");
			String[] tabs = {"4-character-crafts", "5-character-quests", "6-character-abilities"};
			for (String tab : tabs) {
				context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
				// The ability tree takes three API requests the first time.
				context.waitTicks(tab.endsWith("abilities") ? 20 * 4 : 2);
				context.takeScreenshot(name + "-" + tab);
			}
		}

		// Hovering, at 427x240: a character row, then an ability.
		context.getInput().resizeWindow(854, 480);
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeDisplay();
			client.setScreen(profileScreen);
		});
		context.getInput().setCursorPos(600, 196);
		context.waitTicks(3);
		context.takeScreenshot("hover-1-character");
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(3);
		context.getInput().setCursorPos(628, 146);
		context.waitTicks(3);
		context.takeScreenshot("hover-2-ability");
		context.getInput().setCursorPos(600, 300);
		context.getInput().scroll(-4);
		context.waitTicks(3);
		context.takeScreenshot("hover-3-ability-tree-scrolled");

		context.runOnClient(client -> client.setScreen(new TitleScreen()));
	}
}
