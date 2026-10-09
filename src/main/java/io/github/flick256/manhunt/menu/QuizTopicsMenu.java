package io.github.flick256.manhunt.menu;

import io.github.flick256.manhunt.core.quiz.Topic;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Respawn quiz subjects: one icon per {@link Topic}. Click flips it on or off. The last subject that
 * is on cannot be switched off.
 */
final class QuizTopicsMenu extends GuiMenu {

	/** Icon per subject, in {@link Topic} order. All items are already used elsewhere in this package. */
	private static final Item[] ICONS = {
			Items.PAPER,             // MATH
			Items.ENCHANTED_BOOK,    // METHODS
			Items.GRASS_BLOCK,       // BIOLOGY
			Items.EXPERIENCE_BOTTLE, // CHEMISTRY
			Items.REDSTONE,          // PHYSICS
			Items.BOOK,              // HISTORY
			Items.BREAD              // FRENCH
	};

	QuizTopicsMenu(int containerId, Inventory inventory) {
		super(containerId, inventory);
		refresh();
	}

	@Override
	protected void build(ManhuntGame g) {
		show(4, Icons.make(Items.BOOK, "Quiz subjects", ChatFormatting.GOLD,
				"Hunters who die pick one of the ON subjects to answer.",
				"Needs Math respawn ON in Settings."));

		Topic[] topics = Topic.values();
		for (int i = 0; i < topics.length; i++) {
			Topic topic = topics[i];
			boolean on = g.settings().isTopicEnabled(topic);
			set(10 + i, Icons.toggle(ICONS[i], topic.display(), on,
					topic.blurb(),
					on ? "Click to switch off." : "Click to switch on."),
					(p, game, c) -> toggle(p, game, topic));
		}

		set(49, Icons.make(Items.ARROW, "Back", ChatFormatting.YELLOW, "Return to the settings panel."),
				p -> Menus.openSettings(p));
	}

	private void toggle(ServerPlayer p, ManhuntGame game, Topic topic) {
		boolean wanted = !game.settings().isTopicEnabled(topic);
		boolean now = game.settings().setTopic(topic, wanted);
		if (now != wanted) {
			Msg.send(p, Msg.bad("At least one subject must stay on."));
		}
		game.save();
		refresh();
	}
}
