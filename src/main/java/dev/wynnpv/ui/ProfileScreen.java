package dev.wynnpv.ui;

import dev.wynnpv.api.PlayerProfile;
import dev.wynnpv.api.WynncraftApi;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.CompletionException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/** A player's profile: overview on the left, their characters on the right. */
public final class ProfileScreen extends Screen {
	private static final int TITLE = 0xFFFFFFFF;
	private static final int HEADER = 0xFFFFD866;
	private static final int LABEL = 0xFFA0A8B0;
	private static final int VALUE = 0xFFFFFFFF;
	private static final int ERROR = 0xFFFF6B6B;
	private static final int HOVER = 0x30FFFFFF;
	private static final int ROW_HEIGHT = 24;
	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.ROOT);

	private final String query;
	private @Nullable PlayerProfile profile;
	private @Nullable String error;
	private double scroll;
	private int listLeft;
	private int listTop;
	private int listWidth;

	public ProfileScreen(String player) {
		super(Component.translatable("wynnpv.profile"));
		this.query = player;
		WynncraftApi.player(player).whenComplete((result, failure) -> minecraft().execute(() -> {
			if (failure != null) {
				Throwable cause = failure;
				while (cause instanceof CompletionException && cause.getCause() != null) {
					cause = cause.getCause();
				}
				error = cause instanceof WynncraftApi.LookupException ? cause.getMessage() : "Could not load " + player + ".";
			} else {
				profile = result;
			}
		}));
	}

	private static net.minecraft.client.Minecraft minecraft() {
		return net.minecraft.client.Minecraft.getInstance();
	}

	/** The loaded profile, or null while loading or after an error. */
	public @Nullable PlayerProfile profile() {
		return profile;
	}

	@Override
	protected void init() {
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
			.bounds(width / 2 - 75, height - 28, 150, 20)
			.build());
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		if (error != null) {
			graphics.drawCenteredString(font, error, width / 2, height / 2 - 4, ERROR);
			return;
		}
		if (profile == null) {
			graphics.drawCenteredString(font, Component.translatable("wynnpv.loading", query), width / 2, height / 2 - 4, LABEL);
			return;
		}
		graphics.drawCenteredString(font, title(profile), width / 2, 12, TITLE);
		int columnWidth = Math.min(220, (width - 30) / 2);
		int left = width / 2 - columnWidth - 5;
		int right = width / 2 + 5;
		renderOverview(graphics, profile, left, 34);
		renderCharacters(graphics, profile, right, 34, columnWidth, mouseX, mouseY);
	}

	private static String title(PlayerProfile profile) {
		// Staff ranks win over paid ranks, like the in-game chat tag.
		String rank = profile.rank() != null && !profile.rank().equals("Player") ? "[" + profile.rank() + "] "
			: profile.supportRank() != null ? "[" + profile.supportRank().toUpperCase(Locale.ROOT) + "] " : "";
		return rank + profile.username();
	}

	private void renderOverview(GuiGraphics graphics, PlayerProfile profile, int x, int y) {
		graphics.drawString(font, "Overview", x, y, HEADER);
		y += 14;
		if (profile.restricted("onlineStatus")) {
			y = row(graphics, x, y, "Status", hidden());
		} else {
			y = row(graphics, x, y, "Status", profile.online() ? "Online on " + profile.server() : "Offline");
			y = row(graphics, x, y, "Last seen", date(profile.lastJoin()));
		}
		y = row(graphics, x, y, "First joined", date(profile.firstJoin()));
		y = row(graphics, x, y, "Playtime", profile.playtimeHours() == null ? hidden() : NUMBERS.format(Math.round(profile.playtimeHours())) + " hours");
		PlayerProfile.Guild guild = profile.guild();
		y = row(graphics, x, y, "Guild", guild == null ? "None" : guild.name() + " [" + guild.prefix() + "]"
			+ (guild.rank() == null ? "" : ", " + capitalize(guild.rank())));
		y += 8;
		graphics.drawString(font, "Totals", x, y, HEADER);
		y += 14;
		PlayerProfile.Global global = profile.global();
		if (global == null) {
			row(graphics, x, y, "Stats", hidden());
			return;
		}
		y = row(graphics, x, y, "Total level", NUMBERS.format(global.totalLevel()));
		y = row(graphics, x, y, "Quests", NUMBERS.format(global.completedQuests()));
		y = row(graphics, x, y, "Dungeons", NUMBERS.format(global.dungeons()));
		y = row(graphics, x, y, "Raids", NUMBERS.format(global.raids()));
		y = row(graphics, x, y, "Wars", NUMBERS.format(global.wars()));
		y = row(graphics, x, y, "World events", NUMBERS.format(global.worldEvents()));
		y = row(graphics, x, y, "Lootruns", NUMBERS.format(global.lootruns()));
		y = row(graphics, x, y, "Caves", NUMBERS.format(global.caves()));
		y = row(graphics, x, y, "Mobs killed", NUMBERS.format(global.mobsKilled()));
		row(graphics, x, y, "Chests found", NUMBERS.format(global.chestsFound()));
	}

	private void renderCharacters(GuiGraphics graphics, PlayerProfile profile, int x, int y, int width, int mouseX, int mouseY) {
		graphics.drawString(font, "Characters (" + profile.characters().size() + ")", x, y, HEADER);
		y += 14;
		if (profile.restricted("characterListAccess") || profile.restricted("characterDataAccess")) {
			graphics.drawString(font, hidden(), x, y, LABEL);
			return;
		}
		int bottom = height - 36;
		listLeft = x;
		listTop = y;
		listWidth = width;
		scroll = Mth.clamp(scroll, 0, Math.max(0, profile.characters().size() * ROW_HEIGHT - (bottom - y)));
		graphics.enableScissor(x - 2, y, x + width + 2, bottom);
		int rowY = y - (int) scroll;
		for (PlayerProfile.Character character : profile.characters()) {
			if (rowY + ROW_HEIGHT > y && rowY < bottom) {
				if (character == characterAt(mouseX, mouseY)) {
					graphics.fill(x - 2, rowY - 2, x + width + 2, rowY + ROW_HEIGHT - 2, HOVER);
				}
				String name = character.className() + (character.nickname() != null ? " \"" + character.nickname() + "\"" : "");
				graphics.drawString(font, name, x, rowY, VALUE);
				String level = "Lv. " + character.level();
				graphics.drawString(font, level, x + width - font.width(level), rowY, VALUE);
				StringBuilder details = new StringBuilder("Total level " + character.totalLevel());
				if (character.quests() != null) {
					details.append(", ").append(character.quests().size()).append(" quests");
				}
				character.gamemodes().forEach(mode -> details.append(", ").append(capitalize(mode)));
				graphics.drawString(font, font.plainSubstrByWidth(details.toString(), width), x, rowY + 10, LABEL);
			}
			rowY += ROW_HEIGHT;
		}
		graphics.disableScissor();
	}

	private PlayerProfile.@Nullable Character characterAt(double mouseX, double mouseY) {
		if (profile == null || mouseX < listLeft - 2 || mouseX > listLeft + listWidth + 2 || mouseY < listTop || mouseY >= height - 36) {
			return null;
		}
		int index = (int) Math.floor((mouseY - listTop + scroll + 2) / ROW_HEIGHT);
		return index >= 0 && index < profile.characters().size() ? profile.characters().get(index) : null;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
		PlayerProfile.Character character = characterAt(event.x(), event.y());
		if (character != null && profile != null) {
			minecraft.setScreen(new CharacterScreen(this, profile, character));
			return true;
		}
		return super.mouseClicked(event, isDoubleClick);
	}

	private int row(GuiGraphics graphics, int x, int y, String label, String value) {
		graphics.drawString(font, label, x, y, LABEL);
		graphics.drawString(font, value, x + 80, y, VALUE);
		return y + 11;
	}

	private static String hidden() {
		return Component.translatable("wynnpv.hidden").getString();
	}

	/** "2013-03-27T13:10:34Z" becomes "2013-03-27". */
	private static String date(@Nullable String timestamp) {
		return timestamp == null ? hidden() : timestamp.substring(0, Math.min(10, timestamp.length()));
	}

	private static String capitalize(String text) {
		String spaced = text.replace('_', ' ');
		return spaced.isEmpty() ? spaced : java.lang.Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1).toLowerCase(Locale.ROOT);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		scroll -= scrollY * ROW_HEIGHT;
		return true;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
