package dev.wynnpv;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.wynnpv.ui.ProfileScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jspecify.annotations.Nullable;

public class WynnPvClient implements ClientModInitializer {
	// The chat screen closes after a command runs, which would close a screen opened right away.
	private static @Nullable Screen pendingScreen;

	@Override
	public void onInitializeClient() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
			ClientCommandManager.literal("pv")
				.then(ClientCommandManager.argument("player", StringArgumentType.word())
					.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						context.getSource().getClient().getConnection() == null ? java.util.List.<String>of()
							: context.getSource().getClient().getConnection().getOnlinePlayers().stream()
								.map(PlayerInfo::getProfile).map(profile -> profile.name()).toList(),
						builder))
					.executes(context -> {
						pendingScreen = new ProfileScreen(StringArgumentType.getString(context, "player"));
						return 1;
					}))
				.executes(context -> {
					pendingScreen = new ProfileScreen(context.getSource().getClient().getUser().getName());
					return 1;
				})));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (pendingScreen != null) {
				client.setScreen(pendingScreen);
				pendingScreen = null;
			}
		});
	}
}
