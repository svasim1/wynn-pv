package dev.wynnpv;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Shared constants. */
public final class WynnPv {
	public static final String MOD_ID = "wynnpv";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private WynnPv() {}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
