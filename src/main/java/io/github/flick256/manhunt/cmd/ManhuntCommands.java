package io.github.flick256.manhunt.cmd;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import io.github.flick256.manhunt.ManhuntMod;
import io.github.flick256.manhunt.core.GameKind;
import io.github.flick256.manhunt.core.Phase;
import io.github.flick256.manhunt.core.ReleaseMode;
import io.github.flick256.manhunt.core.Role;
import io.github.flick256.manhunt.core.Roster;
import io.github.flick256.manhunt.core.Settings;
import io.github.flick256.manhunt.core.TeamInfo;
import io.github.flick256.manhunt.game.ManhuntGame;
import io.github.flick256.manhunt.menu.Menus;
import io.github.flick256.manhunt.util.Msg;
import io.github.flick256.manhunt.util.Owner;
import io.github.flick256.manhunt.util.TextUtil;

/** Brigadier tree for {@code /manhunt} and its alias {@code /mh}. */
public final class ManhuntCommands {
	private static final String NOT_READY = "Manhunt is not ready yet";

	private static final Predicate<CommandSourceStack> OWNER = src -> Owner.isOwner(src);

	/** Colors handed out automatically by "team create" (same palette the roster uses for auto teams). */
	private static final List<String> TEAM_PALETTE = List.of("red", "blue", "green", "yellow", "aqua", "pink", "gold", "white");

	private static final List<String> COLORS = List.of(
			"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray",
			"blue", "green", "aqua", "red", "light_purple", "pink", "yellow", "white");

	private static final SuggestionProvider<CommandSourceStack> TEAM_SUGGESTIONS =
			(ctx, builder) -> SharedSuggestionProvider.suggest(teamIds(), builder);

	private static final SuggestionProvider<CommandSourceStack> COLOR_SUGGESTIONS =
			(ctx, builder) -> SharedSuggestionProvider.suggest(COLORS, builder);

	private ManhuntCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(root("manhunt"));
		dispatcher.register(root("mh"));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
		return literal(name)
				.executes(ManhuntCommands::bare)
				// ---- owner: game flow
				.then(literal("menu").requires(OWNER).executes(ManhuntCommands::openMenu))
				.then(literal("start").requires(OWNER).executes(ManhuntCommands::start))
				.then(literal("stop").requires(OWNER).executes(ManhuntCommands::stop))
				// ---- owner: roster
				.then(literal("hunter").requires(OWNER)
						.executes(usageOf("/manhunt hunter <players>"))
						.then(argument("players", EntityArgument.players()) // VERIFY: EntityArgument.players() (plural; singular player() is verified)
								.executes(ManhuntCommands::hunter)))
				.then(literal("runner").requires(OWNER)
						.executes(usageOf("/manhunt runner <players> [team]"))
						.then(argument("players", EntityArgument.players())
								.executes(c -> runner(c, null))
								.then(argument("team", StringArgumentType.word())
										.suggests(TEAM_SUGGESTIONS)
										.executes(c -> runner(c, StringArgumentType.getString(c, "team"))))))
				.then(literal("unassign").requires(OWNER)
						.executes(usageOf("/manhunt unassign <players>"))
						.then(argument("players", EntityArgument.players())
								.executes(ManhuntCommands::unassign)))
				// ---- owner: mode
				.then(literal("kind").requires(OWNER)
						.executes(usageOf("/manhunt kind classic|teams"))
						.then(literal("classic").executes(c -> kind(c, GameKind.CLASSIC)))
						.then(literal("teams").executes(c -> kind(c, GameKind.TEAMS))))
				// ---- owner: teams
				.then(literal("team").requires(OWNER)
						.executes(usageOf("/manhunt team create|remove|auto|list"))
						.then(literal("create")
								.executes(usageOf("/manhunt team create <id> [name] [color]"))
								.then(argument("id", StringArgumentType.word())
										.executes(c -> teamCreate(c, null, null))
										.then(argument("name", StringArgumentType.string())
												.executes(c -> teamCreate(c, StringArgumentType.getString(c, "name"), null))
												.then(argument("color", StringArgumentType.word())
														.suggests(COLOR_SUGGESTIONS)
														.executes(c -> teamCreate(c, StringArgumentType.getString(c, "name"),
																StringArgumentType.getString(c, "color")))))))
						.then(literal("remove")
								.executes(usageOf("/manhunt team remove <id>"))
								.then(argument("id", StringArgumentType.word())
										.suggests(TEAM_SUGGESTIONS)
										.executes(ManhuntCommands::teamRemove)))
						.then(literal("auto")
								.executes(usageOf("/manhunt team auto <count 2-8>"))
								.then(argument("count", IntegerArgumentType.integer(2, 8))
										.executes(ManhuntCommands::teamAuto)))
						.then(literal("list").executes(ManhuntCommands::teamList)))
				// ---- owner: settings
				.then(literal("set").requires(OWNER)
						.executes(usageOf("/manhunt set hearts|hunger|headstart|release|stagger|mathquiz ..."))
						.then(literal("hearts")
								.executes(usageOf("/manhunt set hearts <1-200>"))
								.then(argument("value", IntegerArgumentType.integer(1, 200))
										.executes(ManhuntCommands::setHearts)))
						.then(literal("hunger")
								.executes(usageOf("/manhunt set hunger <1.0-8.0>"))
								.then(argument("value", DoubleArgumentType.doubleArg(1.0, 8.0))
										.executes(ManhuntCommands::setHunger)))
						.then(literal("headstart")
								.executes(usageOf("/manhunt set headstart <seconds>"))
								.then(argument("seconds", IntegerArgumentType.integer(0, 3600))
										.executes(ManhuntCommands::setHeadStart)))
						.then(literal("release")
								.executes(usageOf("/manhunt set release all|staggered [seconds]"))
								.then(literal("all").executes(c -> setRelease(c, ReleaseMode.ALL_AT_ONCE, false)))
								.then(literal("staggered")
										.executes(c -> setRelease(c, ReleaseMode.STAGGERED, false))
										.then(argument("seconds", IntegerArgumentType.integer(1, 600))
												.executes(c -> setRelease(c, ReleaseMode.STAGGERED, true)))))
						.then(literal("stagger")
								.executes(usageOf("/manhunt set stagger <seconds>"))
								.then(argument("seconds", IntegerArgumentType.integer(1, 600))
										.executes(ManhuntCommands::setStagger)))
						.then(literal("mathquiz")
								.executes(usageOf("/manhunt set mathquiz on|off [questions 1-10] [difficulty 1-3]"))
								.then(literal("off").executes(c -> setMathQuiz(c, false, -1, -1)))
								.then(literal("on")
										.executes(c -> setMathQuiz(c, true, -1, -1))
										.then(argument("questions", IntegerArgumentType.integer(1, 10))
												.executes(c -> setMathQuiz(c, true,
														IntegerArgumentType.getInteger(c, "questions"), -1))
												.then(argument("difficulty", IntegerArgumentType.integer(1, 3))
														.executes(c -> setMathQuiz(c, true,
																IntegerArgumentType.getInteger(c, "questions"),
																IntegerArgumentType.getInteger(c, "difficulty"))))))))
				// ---- owner: owner list
				.then(literal("owner").requires(OWNER)
						.executes(usageOf("/manhunt owner add|remove <name> | owner list"))
						.then(literal("add")
								.executes(usageOf("/manhunt owner add <name or uuid>"))
								.then(argument("name", StringArgumentType.word())
										.executes(ManhuntCommands::ownerAdd)))
						.then(literal("remove")
								.executes(usageOf("/manhunt owner remove <name or uuid>"))
								.then(argument("name", StringArgumentType.word())
										.executes(ManhuntCommands::ownerRemove)))
						.then(literal("list").executes(ManhuntCommands::ownerList)))
				// ---- owner: test lab
				.then(literal("test").requires(OWNER).executes(ManhuntCommands::test))
				// ---- everybody
				.then(literal("status").executes(ManhuntCommands::status))
				.then(literal("join")
						.executes(usageOf("/manhunt join <team>"))
						.then(argument("team", StringArgumentType.word())
								.suggests(TEAM_SUGGESTIONS)
								.executes(ManhuntCommands::join)))
				.then(literal("leave").executes(ManhuntCommands::leave))
				.then(literal("answer")
						.executes(usageOf("/manhunt answer <number>"))
						.then(argument("answer", StringArgumentType.greedyString())
								.executes(ManhuntCommands::answer)));
	}

	// ------------------------------------------------------------------ root / help

	private static int bare(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ServerPlayer p = src.getPlayer();
		if (p != null && Owner.isOwner(p)) {
			ManhuntGame g = ManhuntMod.game();
			if (g == null) {
				return fail(src, NOT_READY);
			}
			Menus.openMain(p);
			return 1;
		}
		return help(src);
	}

	private static int help(CommandSourceStack src) {
		infoMsg(src, "Commands:");
		infoMsg(src, "  /manhunt status - phase, teams and your role");
		infoMsg(src, "  /manhunt join <team> - join a team while idle (teams mode)");
		infoMsg(src, "  /manhunt leave - leave your team while idle");
		infoMsg(src, "  /manhunt answer <number> - answer your respawn math question");
		if (Owner.isOwner(src)) {
			infoMsg(src, "Owner: /manhunt menu (GUI), start, stop, hunter, runner, unassign, kind, team, set, owner, test");
		}
		return 1;
	}

	// ------------------------------------------------------------------ game flow

	private static int openMenu(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		if (ManhuntMod.game() == null) {
			return fail(src, NOT_READY);
		}
		ServerPlayer p = src.getPlayer();
		if (p == null) {
			return fail(src, "Menus can only be opened by a player");
		}
		Menus.openMain(p);
		return 1;
	}

	private static int start(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		String err = g.start(src.getPlayer()); // NOTE: null when started from console (see report)
		if (err != null) {
			return fail(src, err);
		}
		okMsg(src, "Manhunt started.");
		return 1;
	}

	private static int stop(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		g.stop(true);
		okMsg(src, "Manhunt stopped.");
		return 1;
	}

	// ------------------------------------------------------------------ roster

	private static int hunter(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "players"); // VERIFY: EntityArgument.getPlayers(ctx, name) (singular getPlayer is verified in reference)
		return forPlayers(src, targets, "Hunters", p -> g.assignHunter(p.getUUID()));
	}

	private static int runner(CommandContext<CommandSourceStack> c, String teamArg) throws CommandSyntaxException {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		String teamId;
		if (teamArg != null) {
			teamId = teamArg.toLowerCase(Locale.ROOT);
		} else if (g.kind() == GameKind.CLASSIC) {
			teamId = Roster.CLASSIC_TEAM;
			g.roster().ensureClassicTeam();
		} else {
			return fail(src, "Name the team: /manhunt runner <players> <team>");
		}
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "players"); // VERIFY: see hunter()
		return forPlayers(src, targets, "Runners in " + teamId, p -> g.assignRunner(p.getUUID(), teamId));
	}

	private static int unassign(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Collection<ServerPlayer> targets = EntityArgument.getPlayers(c, "players"); // VERIFY: see hunter()
		return forPlayers(src, targets, "Unassigned", p -> g.unassign(p.getUUID()));
	}

	/** Applies op to every target, reports each failure, and reports the successful names together. */
	private static int forPlayers(CommandSourceStack src, Collection<ServerPlayer> targets, String verb, Function<ServerPlayer, String> op) {
		if (targets.isEmpty()) {
			return fail(src, "No matching players are online");
		}
		List<String> done = new ArrayList<>();
		for (ServerPlayer p : targets) {
			String name = p.getGameProfile().name();
			String err = op.apply(p);
			if (err == null) {
				done.add(name);
			} else {
				failMsg(src, name + ": " + err);
			}
		}
		if (!done.isEmpty()) {
			okMsg(src, verb + ": " + String.join(", ", done));
		}
		return done.size();
	}

	private static int kind(CommandContext<CommandSourceStack> c, GameKind kind) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		String err = g.setKind(kind);
		if (err != null) {
			return fail(src, err);
		}
		g.save();
		okMsg(src, "Mode set to " + kind.name().toLowerCase(Locale.ROOT) + ".");
		return 1;
	}

	// ------------------------------------------------------------------ teams

	private static int teamCreate(CommandContext<CommandSourceStack> c, String name, String color) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		String id = StringArgumentType.getString(c, "id").toLowerCase(Locale.ROOT);
		String displayName = name == null ? id : name;
		String chosenColor = color == null ? nextFreeColor(g) : color.toLowerCase(Locale.ROOT);
		if (!COLORS.contains(chosenColor)) {
			return fail(src, "Unknown color '" + chosenColor + "'. Use one of: " + String.join(", ", COLORS));
		}
		String err = g.createTeam(id, displayName, chosenColor);
		if (err != null) {
			return fail(src, err);
		}
		okMsg(src, "Created team " + displayName + " (" + id + ", " + chosenColor + ").");
		return 1;
	}

	private static int teamRemove(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		String id = StringArgumentType.getString(c, "id").toLowerCase(Locale.ROOT);
		String err = g.removeTeam(id);
		if (err != null) {
			return fail(src, err);
		}
		okMsg(src, "Removed team " + id + ". Its members are now unassigned.");
		return 1;
	}

	private static int teamAuto(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		int count = IntegerArgumentType.getInteger(c, "count");
		String err = g.autoAssign(count);
		if (err != null) {
			return fail(src, err);
		}
		okMsg(src, "Balanced online players into " + count + " teams.");
		return 1;
	}

	private static int teamList(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Collection<TeamInfo> teams = g.roster().teams();
		if (teams.isEmpty()) {
			infoMsg(src, "No teams yet. Create one with /manhunt team create <id> [name] [color].");
			return 1;
		}
		for (TeamInfo t : teams) {
			List<String> names = new ArrayList<>();
			for (UUID id : t.members) {
				names.add(nameOf(g, id));
			}
			String members = names.isEmpty() ? "no members" : String.join(", ", names);
			infoMsg(src, t.id + " (" + t.displayName + ", " + t.color + "): " + members);
		}
		return teams.size();
	}

	// ------------------------------------------------------------------ settings

	private static int setHearts(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Settings s = g.settings();
		s.poolHearts = IntegerArgumentType.getInteger(c, "value");
		s.clamp();
		g.save();
		g.sync().applySettingsLive();
		okMsg(src, "Shared pool per runner team: " + s.poolHearts + " hearts.");
		return 1;
	}

	private static int setHunger(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Settings s = g.settings();
		s.hungerMultiplier = DoubleArgumentType.getDouble(c, "value");
		s.clamp();
		g.save();
		g.sync().applySettingsLive();
		okMsg(src, "Hunger drain multiplier: " + String.format(Locale.ROOT, "%.1f", s.hungerMultiplier) + "x.");
		return 1;
	}

	private static int setHeadStart(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Settings s = g.settings();
		s.headStartSeconds = IntegerArgumentType.getInteger(c, "seconds");
		s.clamp();
		g.save();
		okMsg(src, "Head start: " + TextUtil.secondsToClock(s.headStartSeconds) + " (" + s.headStartSeconds + "s).");
		return 1;
	}

	/** "set release all" or "set release staggered [seconds]"; the optional seconds is the gap between hunters. */
	private static int setRelease(CommandContext<CommandSourceStack> c, ReleaseMode mode, boolean withSeconds) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Settings s = g.settings();
		s.releaseMode = mode;
		if (withSeconds) {
			s.staggerSeconds = IntegerArgumentType.getInteger(c, "seconds");
		}
		s.clamp();
		g.save();
		if (s.releaseMode == ReleaseMode.ALL_AT_ONCE) {
			okMsg(src, "Hunters are released all at once after the head start.");
		} else {
			okMsg(src, "Hunters are released every " + s.staggerSeconds + "s after the head start.");
		}
		return 1;
	}

	private static int setStagger(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Settings s = g.settings();
		s.staggerSeconds = IntegerArgumentType.getInteger(c, "seconds");
		s.clamp();
		g.save();
		okMsg(src, "Stagger gap between hunters: " + s.staggerSeconds + "s (used when release is staggered).");
		return 1;
	}

	private static int setMathQuiz(CommandContext<CommandSourceStack> c, boolean on, int questions, int difficulty) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Settings s = g.settings();
		s.mathRespawn = on;
		if (questions >= 0) {
			s.mathQuestions = questions;
		}
		if (difficulty >= 0) {
			s.mathDifficulty = difficulty;
		}
		s.clamp();
		g.save();
		if (on) {
			okMsg(src, "Math respawn quiz on: " + s.mathQuestions + " correct answer(s), difficulty " + s.mathDifficulty + ".");
		} else {
			okMsg(src, "Math respawn quiz off.");
		}
		return 1;
	}

	// ------------------------------------------------------------------ owner list

	private static int ownerAdd(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		String name = StringArgumentType.getString(c, "name").trim();
		if (g.config().owners == null) {
			g.config().owners = new ArrayList<>();
		}
		List<String> owners = g.config().owners;
		for (String entry : owners) {
			if (sameOwner(entry, name)) {
				return fail(src, name + " is already an owner.");
			}
		}
		owners.add(name);
		g.save();
		okMsg(src, "Added owner " + name + ".");
		return 1;
	}

	private static int ownerRemove(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		String name = StringArgumentType.getString(c, "name").trim();
		List<String> owners = g.config().owners;
		if (owners == null || !owners.removeIf(entry -> sameOwner(entry, name))) {
			return fail(src, name + " is not in the owner list.");
		}
		g.save();
		okMsg(src, "Removed owner " + name + ".");
		if (owners.isEmpty()) {
			infoMsg(src, "The owner list is now empty: vanilla OWNERS permission (or manhunt:admin) decides who is owner.");
		}
		return 1;
	}

	private static int ownerList(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		if (ManhuntMod.game() == null) {
			return fail(src, NOT_READY);
		}
		UUID host = Owner.hostId();
		if (host != null) {
			infoMsg(src, "Integrated-server host (always owner): " + host);
		}
		List<String> owners = Owner.owners();
		if (owners.isEmpty()) {
			infoMsg(src, "Owner list is empty: vanilla OWNERS permission (or manhunt:admin) decides who is owner.");
		} else {
			infoMsg(src, "Owners: " + String.join(", ", owners));
		}
		return 1;
	}

	// ------------------------------------------------------------------ test lab

	private static int test(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		if (ManhuntMod.game() == null) {
			return fail(src, NOT_READY);
		}
		ServerPlayer p = src.getPlayer();
		if (p == null) {
			return fail(src, "The test lab can only be opened by a player");
		}
		Menus.openTestLab(p);
		return 1;
	}

	// ------------------------------------------------------------------ everybody

	private static int status(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		Phase phase = g.phase();
		infoMsg(src, "Phase: " + phase.name().toLowerCase(Locale.ROOT) + " | Mode: " + g.kind().name().toLowerCase(Locale.ROOT));
		for (String line : g.roster().describe()) {
			infoMsg(src, line);
		}
		ServerPlayer p = src.getPlayer();
		if (p != null) {
			UUID id = p.getUUID();
			Roster roster = g.roster();
			Role role = roster.roleOf(id);
			if (role == Role.NONE) {
				infoMsg(src, "You are not in the game.");
			} else {
				String team = roster.teamOf(id);
				infoMsg(src, "You: " + role.name().toLowerCase(Locale.ROOT) + (team == null ? "" : " in team " + team));
				if (phase == Phase.HEAD_START && role == Role.HUNTER) {
					infoMsg(src, "Your release in " + TextUtil.secondsToClock(g.secondsUntilRelease(id)) + ".");
				}
			}
		}
		if (Owner.isOwner(src)) {
			infoMsg(src, settingsSummary(g.settings()));
		}
		return 1;
	}

	private static int join(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		ServerPlayer p = src.getPlayer();
		if (p == null) {
			return fail(src, "Only a player can join a team");
		}
		String teamId = StringArgumentType.getString(c, "team").toLowerCase(Locale.ROOT);
		String err = g.joinTeamSelf(p, teamId);
		if (err != null) {
			return fail(src, err);
		}
		String shown = g.teamDisplay(teamId);
		okMsg(src, "You joined " + (shown == null ? teamId : shown) + ".");
		return 1;
	}

	private static int leave(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		ServerPlayer p = src.getPlayer();
		if (p == null) {
			return fail(src, "Only a player can leave a team");
		}
		if (g.phase() != Phase.IDLE) {
			return fail(src, "You can only leave a team while the game is idle");
		}
		String err = g.unassign(p.getUUID());
		if (err != null) {
			return fail(src, err);
		}
		okMsg(src, "You left your team.");
		return 1;
	}

	private static int answer(CommandContext<CommandSourceStack> c) {
		CommandSourceStack src = c.getSource();
		ManhuntGame g = ManhuntMod.game();
		if (g == null) {
			return fail(src, NOT_READY);
		}
		ServerPlayer p = src.getPlayer();
		if (p == null) {
			return fail(src, "Only a player can answer a math question");
		}
		String text = StringArgumentType.getString(c, "answer");
		if (!g.quiz().handleAnswer(p, text)) {
			return fail(src, "You have no math question to answer right now.");
		}
		return 1;
	}

	// ------------------------------------------------------------------ helpers

	/** Usage hint for a node that was given without its arguments. */
	private static Command<CommandSourceStack> usageOf(String usage) {
		return c -> fail(c.getSource(), "Usage: " + usage);
	}

	private static List<String> teamIds() {
		List<String> ids = new ArrayList<>();
		ManhuntGame g = ManhuntMod.game();
		if (g != null) {
			for (TeamInfo t : g.roster().teams()) {
				ids.add(t.id);
			}
		}
		return ids;
	}

	private static String nextFreeColor(ManhuntGame g) {
		Set<String> used = new HashSet<>();
		for (TeamInfo t : g.roster().teams()) {
			if (t.color != null) {
				used.add(t.color.toLowerCase(Locale.ROOT));
			}
		}
		for (String color : TEAM_PALETTE) {
			if (!used.contains(color)) {
				return color;
			}
		}
		return "white";
	}

	private static String nameOf(ManhuntGame g, UUID id) {
		String name = g.roster().nameOf(id);
		return name != null ? name : id.toString();
	}

	/** True if an owner-list entry names the same player as {@code name} (name or UUID, case-insensitive). */
	private static boolean sameOwner(String entry, String name) {
		if (entry == null) {
			return false;
		}
		String e = entry.trim();
		if (e.equalsIgnoreCase(name)) {
			return true;
		}
		UUID a = Owner.parseUuid(e);
		UUID b = Owner.parseUuid(name);
		return a != null && a.equals(b);
	}

	private static String settingsSummary(Settings s) {
		return "Settings: hearts " + s.poolHearts
				+ " | hunger x" + String.format(Locale.ROOT, "%.1f", s.hungerMultiplier)
				+ " | head start " + s.headStartSeconds + "s"
				+ " | release " + (s.releaseMode == ReleaseMode.ALL_AT_ONCE ? "all" : "staggered " + s.staggerSeconds + "s")
				+ " | math quiz " + (s.mathRespawn ? s.mathQuestions + " question(s), difficulty " + s.mathDifficulty : "off");
	}

	private static void okMsg(CommandSourceStack src, String text) {
		src.sendSuccess(() -> Msg.good(text), false);
	}

	private static void infoMsg(CommandSourceStack src, String text) {
		src.sendSuccess(() -> Msg.info(text), false);
	}

	private static void failMsg(CommandSourceStack src, String text) {
		src.sendFailure(Msg.bad(text));
	}

	private static int fail(CommandSourceStack src, String text) {
		failMsg(src, text);
		return 0;
	}
}
