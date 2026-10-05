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
	// muffinsko, a top player with a public profile: leaderboard places, a guild, former guilds and raids.
	private static final String PLAYER = "24369ee1-3d07-43cc-a660-a6c34218cbc9";
	// fusianasan_ hides their stats, characters and online status.
	private static final String RESTRICTED = "1c4246b0-2734-48d3-a9b9-7ca38e31e2a0";

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
			screenshotAll(context, profileScreen, (size[0] / size[2]) + "x" + (size[1] / size[2]), size);
		}
		hover(context, profileScreen);
		restricted(context);
		context.runOnClient(client -> client.setScreen(new TitleScreen()));
	}

	/** A player hiding their stats, characters and online status, at 427x240. */
	private static void restricted(ClientGameTestContext context) {
		ProfileScreen screen = context.computeOnClient(client -> {
			ProfileScreen hidden = new ProfileScreen(RESTRICTED);
			client.setScreen(hidden);
			return hidden;
		});
		context.waitFor(client -> screen.profile() != null, 20 * 30);
		context.getInput().setCursorPos(2, 2);
		context.waitTicks(3);
		context.takeScreenshot("restricted-1-characters");
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(2);
		context.takeScreenshot("restricted-2-deeds");
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(2);
		context.takeScreenshot("restricted-3-renown");
	}

	private static void screenshotAll(ClientGameTestContext context, ProfileScreen profileScreen, String name, int[] size) {
		context.getInput().resizeWindow(size[0], size[1]);
		context.runOnClient(client -> {
			client.options.guiScale().set(size[2]);
			client.resizeDisplay();
		});
		// Keep the mouse in a corner, off the panels.
		context.getInput().setCursorPos(2, 2);

		context.runOnClient(client -> client.setScreen(profileScreen));
		context.waitTicks(3);
		context.takeScreenshot(name + "-1-profile-characters");
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(2);
		context.takeScreenshot(name + "-2-profile-deeds");
		context.getInput().scroll(-100);
		context.waitTicks(2);
		context.takeScreenshot(name + "-2b-profile-deeds-end");
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(2);
		context.takeScreenshot(name + "-2c-profile-renown");
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		// The guild and former guilds are looked up the first time.
		context.waitTicks(20 * 3);
		context.takeScreenshot(name + "-2d-profile-guild");
		for (int i = 0; i < 3; i++) {
			context.getInput().pressKey(GLFW.GLFW_KEY_LEFT);
		}

		context.runOnClient(client -> {
			PlayerProfile profile = profileScreen.profile();
			PlayerProfile.Character active = profile.characters().stream()
				.filter(c -> c.uuid().equals(profile.activeCharacter())).findFirst().orElse(profile.characters().getFirst());
			client.setScreen(new CharacterScreen(profileScreen, profile, active));
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

	/** Hovering at 427x240: a character, then an ability. */
	private static void hover(ClientGameTestContext context, ProfileScreen profileScreen) {
		context.getInput().resizeWindow(854, 480);
		context.runOnClient(client -> {
			client.options.guiScale().set(2);
			client.resizeDisplay();
			client.setScreen(profileScreen);
		});
		context.getInput().setCursorPos(600, 200);
		context.waitTicks(3);
		context.takeScreenshot("hover-1-character");
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(3);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT);
		context.waitTicks(3);
		// The first ability: column 5, row 1.
		context.getInput().setCursorPos(605, 152);
		context.waitTicks(3);
		context.takeScreenshot("hover-2-ability");
	}
}
