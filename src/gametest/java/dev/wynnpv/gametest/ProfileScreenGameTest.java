package dev.wynnpv.gametest;

import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.ui.CharacterScreen;
import dev.wynnpv.ui.ProfileScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;

/**
 * Opens a real profile from the live Wynncraft API and saves screenshots of every screen. Cursor
 * positions are window pixels; the default test window is 854x480 at GUI scale 2.
 */
public class ProfileScreenGameTest implements FabricClientGameTest {
	// Salted, Wynncraft's owner, has a public profile; their level 120 archer has abilities taken.
	private static final String PLAYER = "1ed075fc-5aa9-42e0-a29f-640326c1d80c";
	private static final String ARCHER = "92ddbed6-cbab-49ac-bdb0-5385f4ceab91";

	@Override
	public void runTest(ClientGameTestContext context) {
		ProfileScreen profileScreen = context.computeOnClient(client -> {
			ProfileScreen screen = new ProfileScreen(PLAYER);
			client.setScreen(screen);
			return screen;
		});
		context.waitFor(client -> profileScreen.profile() != null, 20 * 30);
		context.waitTicks(2);
		context.takeScreenshot("1-profile");

		context.runOnClient(client -> {
			PlayerProfile profile = profileScreen.profile();
			PlayerProfile.Character archer = profile.characters().stream()
				.filter(c -> c.uuid().equals(ARCHER)).findFirst().orElseThrow();
			client.setScreen(new CharacterScreen(profileScreen, profile, archer));
		});
		context.waitTicks(2);
		context.takeScreenshot("2-character-stats");

		clickTab(context, 1);
		context.takeScreenshot("3-character-dungeons-raids");
		clickTab(context, 2);
		context.takeScreenshot("4-character-quests");
		clickTab(context, 3);
		context.waitTicks(20 * 5); // three API requests
		context.takeScreenshot("5-ability-tree");

		// Hover Arrow Bomb, the first ability (column 5, row 1).
		context.getInput().setCursorPos(426, 154);
		context.waitTicks(2);
		context.takeScreenshot("6-ability-tooltip");

		context.getInput().setCursorPos(10, 470);
		context.getInput().scroll(-6);
		context.waitTicks(2);
		context.takeScreenshot("7-ability-tree-scrolled");

		context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
		context.waitForScreen(ProfileScreen.class);
		context.runOnClient(client -> client.setScreen(new TitleScreen()));
	}

	/** Tabs are 100 wide from x 13 at y 28-48 (GUI pixels) on the 427 wide test screen. */
	private static void clickTab(ClientGameTestContext context, int index) {
		context.getInput().setCursorPos((13 + 100 * index + 49) * 2, 38 * 2);
		context.getInput().pressMouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		context.waitTicks(2);
	}
}
