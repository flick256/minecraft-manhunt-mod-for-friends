package io.github.flick256.manhunt.menu;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** ItemStack factories for menu icons. Every name and lore line is explicitly non-italic. */
final class Icons {

	private Icons() {
	}

	/** A non-italic, colored text component. */
	static Component text(String value, ChatFormatting color) {
		return Component.literal(value).setStyle(Style.EMPTY.withItalic(false).withColor(color));
	}

	/** Gray, non-italic lore lines. */
	static List<Component> lore(String... lines) {
		List<Component> out = new ArrayList<>(lines.length);
		for (String line : lines) {
			out.add(text(line, ChatFormatting.GRAY));
		}
		return out;
	}

	/** An item with a custom (non-italic) name and lore. Empty lore leaves the lore component unset. */
	static ItemStack make(Item item, String name, ChatFormatting color, List<Component> lore) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, text(name, color));
		if (!lore.isEmpty()) {
			// Verified: new ItemLore(List<Component>) used by fabric-item-api tests.
			stack.set(DataComponents.LORE, new ItemLore(lore));
		}
		return stack;
	}

	/** Same as {@link #make(Item, String, ChatFormatting, List)} with gray lore lines. */
	static ItemStack make(Item item, String name, ChatFormatting color, String... lines) {
		return make(item, name, color, lore(lines));
	}

	/** A switch icon: "Label: ON" (green, enchant glint) or "Label: OFF" (red). */
	static ItemStack toggle(Item item, String label, boolean on, String... lines) {
		ItemStack stack = make(item, label + ": " + (on ? "ON" : "OFF"),
				on ? ChatFormatting.GREEN : ChatFormatting.RED, lines);
		if (on) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		return stack;
	}

	/** Blank gray pane used to fill empty slots. */
	static ItemStack filler() {
		return make(Items.GRAY_STAINED_GLASS_PANE, " ", ChatFormatting.GRAY);
	}

	/** Player head with the name (plain head: the skin is not resolved). */
	static ItemStack head(String name, ChatFormatting color, List<Component> lore) {
		return make(Items.PLAYER_HEAD, name, color, lore);
	}

	/** Wool block for a chat color name ("red", "lime"...). Unknown names give white wool. */
	static Item woolFor(String chatColor) {
		String c = chatColor == null ? "" : chatColor.toLowerCase(Locale.ROOT);
		return switch (c) {
			case "red", "dark_red" -> Items.RED_WOOL;
			case "blue", "dark_blue" -> Items.BLUE_WOOL;
			case "green" -> Items.LIME_WOOL;
			case "dark_green" -> Items.GREEN_WOOL;
			case "aqua", "dark_aqua" -> Items.CYAN_WOOL;
			case "yellow" -> Items.YELLOW_WOOL;
			case "light_purple", "pink" -> Items.PINK_WOOL;
			case "dark_purple" -> Items.PURPLE_WOOL;
			case "gold" -> Items.ORANGE_WOOL;
			case "gray" -> Items.LIGHT_GRAY_WOOL;
			case "dark_gray" -> Items.GRAY_WOOL;
			case "black" -> Items.BLACK_WOOL;
			default -> Items.WHITE_WOOL;
		};
	}
}
