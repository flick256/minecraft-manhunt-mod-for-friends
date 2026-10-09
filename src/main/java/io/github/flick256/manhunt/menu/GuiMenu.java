package io.github.flick256.manhunt.menu;

import io.github.flick256.manhunt.ManhuntMod;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.util.Msg;
import io.github.flick256.manhunt.util.Owner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * Base for the owner control panel. A 6-row chest GUI that is fully server-side: vanilla clients
 * render it with their normal chest screen, but every item movement is cancelled here.
 *
 * <p>Only plain left/right clicks (PICKUP) and shift-clicks (QUICK_MOVE) on the top 54 slots reach an
 * {@link Action}. Everything else (outside-window clicks, player inventory, drags, number keys,
 * middle click, drop key) is dropped silently. Subclasses implement {@link #build(ManhuntGame)} and
 * call {@link #set}, {@link #show} and {@link #fill}; {@link #refresh()} rebuilds the whole page.
 */
public abstract class GuiMenu extends AbstractContainerMenu {

	public static final int ROWS = 6;
	public static final int SIZE = ROWS * 9;

	/** Click details: right = secondary button, shift = shift-click (quick move). */
	public record Click(boolean right, boolean shift) {
	}

	/** A slot handler. Runs only for the owner, with the live game. */
	@FunctionalInterface
	public interface Action {
		void run(ServerPlayer player, ManhuntGame game, Click click);
	}

	/** Creates a menu for a freshly opened container id. */
	@FunctionalInterface
	public interface Factory {
		GuiMenu create(int containerId, Inventory inventory);
	}

	private final SimpleContainer top = new SimpleContainer(SIZE);
	private final Action[] actions = new Action[SIZE];

	protected GuiMenu(int containerId, Inventory inventory) {
		// VERIFY: AbstractContainerMenu(MenuType<?>, int) constructor and MenuType.GENERIC_9x6 field name
		// (neither appears in the reference tree; the test mod only subclasses DispenserMenu).
		super(MenuType.GENERIC_9x6, containerId);
		// Same slot layout as vanilla ChestMenu with 6 rows, so the vanilla client screen lines up.
		for (int row = 0; row < ROWS; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(top, row * 9 + col, 8 + col * 18, 18 + row * 18)); // VERIFY: Slot(Container,int,int,int)
			}
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(inventory, col, 8 + col * 18, 197));
		}
	}

	/**
	 * Opens a menu for the player. Callers have already checked owner and game; the factory runs
	 * once, for this open only.
	 */
	public static void open(ServerPlayer player, String title, Factory factory) {
		player.openMenu(new SimpleMenuProvider(
				(containerId, inventory, unused) -> factory.create(containerId, inventory),
				Component.literal(title)));
	}

	/** Renders the page for the current game state. Called by {@link #refresh()} only. */
	protected abstract void build(ManhuntGame game);

	/** Clears every slot and handler, then rebuilds the page from the current state. */
	protected final void refresh() {
		for (int i = 0; i < SIZE; i++) {
			top.setItem(i, ItemStack.EMPTY);
			actions[i] = null;
		}
		ManhuntGame game = ManhuntMod.game();
		if (game == null) {
			show(22, Icons.make(Items.BARRIER, "Manhunt is not ready", ChatFormatting.RED,
					"Try again once the server has started."));
			return;
		}
		build(game);
		fill(Icons.filler());
	}

	/** Shows an icon with a click handler. */
	protected final void set(int slot, ItemStack icon, Action action) {
		top.setItem(slot, icon);
		actions[slot] = action;
	}

	/** Shows an icon with a handler that only needs the player (back, close, sub-menus). */
	protected final void set(int slot, ItemStack icon, Consumer<ServerPlayer> onClick) {
		set(slot, icon, (player, game, click) -> onClick.accept(player));
	}

	/** Shows an icon that does nothing when clicked. */
	protected final void show(int slot, ItemStack icon) {
		top.setItem(slot, icon);
		actions[slot] = null;
	}

	/** Puts the filler into every top slot that is still empty. */
	protected final void fill(ItemStack filler) {
		for (int i = 0; i < SIZE; i++) {
			if (actions[i] == null && top.getItem(i).isEmpty()) {
				top.setItem(i, filler.copy());
			}
		}
	}

	@Override
	public void clicked(int slotId, int clickData, ContainerInput containerInput, Player player) {
		// Cancel everything: this menu never lets items move. Only our own top-slot icons react.
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return;
		}
		if (slotId < 0 || slotId >= SIZE) {
			return;
		}
		// VERIFY: ContainerInput constants PICKUP and QUICK_MOVE (ClickType renamed; names not in reference tree).
		if (containerInput != ContainerInput.PICKUP && containerInput != ContainerInput.QUICK_MOVE) {
			return;
		}
		Action action = actions[slotId];
		if (action == null) {
			return;
		}
		if (!Owner.isOwner(serverPlayer)) {
			Msg.send(serverPlayer, Msg.bad("Only the server owner can use this menu."));
			serverPlayer.closeContainer();
			return;
		}
		ManhuntGame game = ManhuntMod.game();
		if (game == null) {
			Msg.send(serverPlayer, Msg.bad("Manhunt is not ready."));
			serverPlayer.closeContainer();
			return;
		}
		// clickData 1 = right button for PICKUP (and right+shift for QUICK_MOVE).
		action.run(serverPlayer, game, new Click(clickData == 1, containerInput == ContainerInput.QUICK_MOVE));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		// VERIFY: abstract AbstractContainerMenu#quickMoveStack(Player, int) signature.
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		// VERIFY: abstract AbstractContainerMenu#stillValid(Player) signature.
		return true;
	}

	@Override
	public MenuType<?> getType() {
		return MenuType.GENERIC_9x6;
	}
}
