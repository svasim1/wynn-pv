package dev.wynnpv.ui;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Whether Wynncraft's own resource pack is loaded, i.e. the player is on Wynncraft and accepted its
 * pack. Only then do items with Wynncraft's custom model data show Wynncraft's icons; the mod never
 * ships or copies those textures, it only asks the game to draw the items.
 */
final class WynncraftPack {
	private WynncraftPack() {}

	static boolean loaded() {
		Minecraft minecraft = Minecraft.getInstance();
		ServerData server = minecraft.getCurrentServer();
		if (server == null || !server.ip.toLowerCase(Locale.ROOT).contains("wynncraft")) {
			return false;
		}
		// Packs a server sends are selected with ids starting with "server".
		return minecraft.getResourcePackRepository().getSelectedIds().stream().anyMatch(id -> id.startsWith("server"));
	}
}
