package dev.wynnpv.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.wynnpv.WynnPv;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** The player's choices, saved in config/wynnpv/settings.json. */
public final class Settings {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve(WynnPv.MOD_ID).resolve("settings.json");
	private static Settings instance;

	public enum ThemeChoice { BOARD, TOME }

	public ThemeChoice theme = ThemeChoice.BOARD;

	public static Settings get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static Settings load() {
		try {
			if (Files.exists(FILE)) {
				Settings loaded = GSON.fromJson(Files.readString(FILE), Settings.class);
				if (loaded != null) {
					if (loaded.theme == null) {
						loaded.theme = ThemeChoice.BOARD;
					}
					return loaded;
				}
			}
		} catch (IOException | JsonParseException e) {
			WynnPv.LOGGER.warn("Could not read {}, using defaults", FILE, e);
		}
		return new Settings();
	}

	public void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, GSON.toJson(this));
		} catch (IOException e) {
			WynnPv.LOGGER.error("Could not save {}", FILE, e);
		}
	}
}
