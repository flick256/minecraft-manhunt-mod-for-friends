# Manhunt mod – architecture & module contract

Fabric mod for **Minecraft 26.2** (Java 25, Mojang names, no remapping). Root package `io.github.flick256.manhunt`.
Server-side only logic: menus are vanilla chest GUIs (`ChestMenu`), so clients do not need the mod
(but it is also fine if everybody has it). Single-player works through the integrated server.

## Design decisions

| Topic | Decision |
|---|---|
| Owner | `Owner.isOwner`: console, the integrated-server host (first player to join an integrated server), names/UUIDs in `config/manhunt.json -> owners`, or Fabric permission `manhunt:admin` (what Essentials-style permission mods / LuckPerms grant). If `owners` is empty AND no permission provider answered, fall back to vanilla `PermissionLevel.OWNERS`. Players who are not owner only get `/manhunt join <team>`, `/manhunt leave`, `/manhunt answer <n>`, `/manhunt status`. |
| Shared pool | One "shared life" per runner team. Every member's `MAX_HEALTH` base attribute = `settings.poolHearts*2` (default 20 hearts = 40 hp, shown as 2 rows). Each tick we diff each member against the last value we wrote and merge (damage from different members in the same tick sums, heals take the max). |
| Shared hunger | Same diff-merge, except drain uses the max (not the sum) and is divided by `settings.hungerMultiplier` (default 2.0 => "double hunger bar"). |
| Shared inventory | Per team canonical list of `Inventory.getContainerSize()` slots + ender chest. Each tick, every member is diffed against the canonical list; the first member that changed a slot wins; the new canonical list is pushed to everyone. |
| Hunters | Frozen during the head-start (all at once, or staggered every N seconds), get a tracking compass, optional math-quiz to respawn. |
| Win | Runners kill the Ender Dragon => RUNNERS win. Any runner team dies once => in CLASSIC the HUNTERS win; in TEAMS mode that team is eliminated, last team standing wins, dragon kill => that team wins. Victory ceremony = titles, fanfare note melody, particles, lightning (hunters). |
| Test Lab | A menu page to test every feature alone: "virtual teammates" are simply direct operations on the team pool (`TeamSync.inject*`), plus buttons to freeze yourself, run the math quiz, play each ceremony, and a `timeScale` (fast timers). |

## Package layout / ownership

```
core/    (NO net.minecraft imports; pure Java, unit tested)            -> W1
  Role, GameKind, ReleaseMode, Phase, Side, Winner, Settings, TeamInfo, Roster,
  SlotSync, HealthPool, HungerPool, MathQuiz, MathQuestion, Schedule,
  ManhuntConfig, ConfigIO
util/    Msg, Owner, TextUtil                                           -> W3
cmd/     ManhuntCommands                                                -> W3
menu/    GuiMenu, Icons, MainMenu, SettingsMenu, TeamsMenu,
         PlayerPickerMenu, TestLabMenu, Menus (entry points)            -> W2
game/    TeamSync                                                       -> W4
game/    ManhuntGame, FreezeManager                                     -> W5
game/    RespawnQuiz, Ceremony, TestLab                                 -> W6
game/    CompassTracker; ManhuntMod (event wiring); README              -> W7
```

Only edit the files you own. If you need something from another module that the contract below does not list, do NOT invent it – use only what is listed (or ask the orchestrator in your final report).

## core (pure Java; Gson allowed: `com.google.gson`, ships with Minecraft)

```java
enum Role { NONE, HUNTER, RUNNER }
enum GameKind { CLASSIC, TEAMS }
enum ReleaseMode { ALL_AT_ONCE, STAGGERED }
enum Phase { IDLE, HEAD_START, RUNNING, CELEBRATION }
enum Side { RUNNERS, HUNTERS, TEAM }
record Winner(Side side, String teamId)        // teamId null unless side==TEAM (or RUNNERS in classic: the runner team id)

final class Settings {                          // public mutable fields, Gson friendly, defaults below
  int poolHearts = 20;            // whole hearts shared by a runner team, 1..200
  double hungerMultiplier = 2.0;  // 1.0..8.0
  int headStartSeconds = 60;      // 0..3600
  ReleaseMode releaseMode = ALL_AT_ONCE;
  int staggerSeconds = 60;        // 1..600, gap between hunters when STAGGERED
  boolean mathRespawn = false;
  int mathQuestions = 1;          // 1..10 correct answers needed
  int mathDifficulty = 1;         // 1..3
  boolean shareInventory = true, shareEnderChest = true, shareHunger = true;
  boolean teamSelfSelect = true;  // non-owners may /manhunt join <team> while IDLE
  boolean clearInventoryOnStart = true;
  boolean giveCompass = true;
  void clamp();                   // clamps every field into its range, null-enum -> default
  float poolHealth();             // poolHearts * 2f
}

final class TeamInfo { String id; String displayName; String color; /* chat color name e.g. "red" */ LinkedHashSet<UUID> members; }

final class Roster {                            // all UUID based; no Minecraft types
  static final String CLASSIC_TEAM = "runners";
  Roster();
  Map<UUID,String> names();  void rememberName(UUID,String); String nameOf(UUID);
  Role roleOf(UUID); String teamOf(UUID) /*null if none*/; boolean isHunter(UUID); boolean isRunner(UUID);
  Set<UUID> hunters(); /*unmodifiable*/ Collection<TeamInfo> teams(); /*unmodifiable, insertion order*/ TeamInfo team(String id) /*null*/;
  Set<UUID> allRunners();
  String createTeam(String id, String displayName, String color);   // null ok, else error text (dup, blank, >16 chars, bad chars [a-z0-9_-])
  String removeTeam(String id);                                      // members become NONE
  String assignHunter(UUID);                                         // removes from any team
  String assignRunner(UUID, String teamId);                          // removes from hunters/other team; error if team unknown
  void unassign(UUID);  void clearAll();  void ensureClassicTeam();
  /** TEAMS mode helper: shuffle players into `teamCount` (2..8) teams named from palette (Red, Blue, Green, Yellow, Aqua, Pink, Gold, White) as evenly as possible; replaces all teams. Returns null or error. */
  String autoBalance(List<UUID> players, int teamCount, Random rnd);
  List<String> describe();   // human readable lines for /manhunt status
}

final class SlotSync {          // generic canonical-merge for shared inventories
  static <T> List<T> merge(List<T> canonical, List<List<T>> memberViews, int startIndex, BiPredicate<T,T> same);
  // returns NEW list = canonical with, per slot, the first (rotating from startIndex) member whose slot !same(canonical slot) taking over. Never mutates inputs. All lists same size else IllegalArgumentException.
}

final class HealthPool {        // team shared hp
  HealthPool(float current, float max);
  float current(); float max(); void setMax(float max) /*clamps current*/; void set(float);
  /** merge observed member healths (what players have NOW) against the value we last wrote. damage sums, heal = max. returns new current (also stored). Dead (<=0) possible. */
  float merge(float[] memberHealth);
  boolean isDead();             // current <= 0.0001
  void damage(float amt, boolean lethal);  // lethal=false => never below 1.0
  void heal(float amt);
}

final class HungerPool {       // food + saturation with drain scaling
  HungerPool(int food, float sat);
  int food(); float saturation(); void set(int food, float sat);
  void merge(int[] memberFood, float[] memberSat, double drainMultiplier);   // see algorithm in class javadoc: exact doubles, drain = max member drop / multiplier, gain = max member gain; written food = ceil(exact-1e-9), saturation clamped 0..food
  void drain(int foodPoints);   // direct drain (test lab) – NOT scaled
}

record MathQuestion(String text, int answer)
final class MathQuiz { static MathQuestion next(int difficulty, Random rnd); static boolean check(MathQuestion q, String input) /*trim, parse int, tolerant of "=" prefix*/ }

final class Schedule { static int[] releaseSeconds(int hunterCount, int headStart, ReleaseMode mode, int stagger); }
   // ALL_AT_ONCE: every entry = headStart. STAGGERED: entry i = headStart + i*stagger. Seconds since game start.

final class ManhuntConfig { Settings settings = new Settings(); List<String> owners = new ArrayList<>(); GameKind kind = CLASSIC; }
final class ConfigIO { static ManhuntConfig load(Path file); static void save(Path file, ManhuntConfig cfg); }   // missing/corrupt file => defaults, clamp() applied, never throws
```

## util (W3)

```java
final class Msg {
  static Component prefix();                       // gold "[Manhunt] "
  static Component info(String); good(String); bad(String);   // prefixed Components (gray/green/red body)
  static void send(ServerPlayer, Component); static void broadcast(MinecraftServer, Component);
  static void title(ServerPlayer, Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks);
  static void actionbar(ServerPlayer, Component);
  static void sound(ServerPlayer, SoundEvent, float volume, float pitch);   // plays at the player, only to that player
  static Component colored(String text, ChatFormatting fmt);
  static ChatFormatting fmt(String colorName);     // "red"->RED ... default WHITE
}
final class Owner {
  static boolean isOwner(CommandSourceStack src);  static boolean isOwner(ServerPlayer p);   // rules in the table above
  static void noteJoin(ServerPlayer p);            // integrated-server host detection (first joiner)
  static void bind(ManhuntGame game);              // so Owner can read config owners list
}
final class TextUtil { static String ticksToClock(int ticks); static String secondsToClock(int s) /* "1:05" */; }
```

## game (MC layer)

```java
public final class ManhuntGame {                      // W5. ONE instance per server, created at SERVER_STARTED.
  public ManhuntGame(MinecraftServer server, ManhuntConfig config, Path configFile);
  public MinecraftServer server(); public ManhuntConfig config(); public Settings settings(); public Roster roster();
  public Phase phase(); public GameKind kind(); public String setKind(GameKind k);   // error text if phase != IDLE; switching to CLASSIC calls roster.ensureClassicTeam(), to TEAMS removes hunters
  public TeamSync sync(); public FreezeManager freeze(); public CompassTracker compass();
  public RespawnQuiz quiz(); public Ceremony ceremony(); public TestLab test();
  public void save();                                 // ConfigIO.save
  public double timeScale();                          // test().timeScale()
  // lifecycle
  public String start(ServerPlayer starter);          // null on success. Validates: IDLE; CLASSIC needs >=1 online runner and >=1 online hunter (unless test mode); TEAMS needs >=2 teams with >=1 online member each.
  public void stop(boolean announce);                 // abort from any phase: unfreeze, restore attributes/gamemodes, clear quiz/ceremony, phase=IDLE
  public void endGame(Winner w);                      // RUNNING/HEAD_START -> CELEBRATION, ceremony().play(...)
  public void finishCeremony();                       // called by Ceremony when done: resets everything, phase=IDLE
  public void tick();                                 // END_SERVER_TICK: phase machine, head-start countdown + per-hunter releases (Schedule), freeze/sync/compass/quiz/ceremony ticks
  public int secondsUntilRelease(UUID hunter);        // for scoreboard/actionbar
  // roster ops used by menus/commands (all return null on success or an error String)
  public String assignHunter(UUID id); public String assignRunner(UUID id, String teamId); public String unassign(UUID id);
  public String createTeam(String id, String displayName, String color); public String removeTeam(String id);
  public String autoAssign(int teamCount);           // TEAMS: all online players balanced into teamCount teams
  public String joinTeamSelf(ServerPlayer p, String teamId);  // non-owner self select (TEAMS mode / settings.teamSelfSelect, IDLE only)
  // queries
  public ServerPlayer player(UUID id);                // online or null
  public List<ServerPlayer> hunters();  public List<ServerPlayer> teamMembers(String teamId);  public List<ServerPlayer> allRunners();
  public boolean isParticipant(ServerPlayer p);       // has role != NONE and game is active
  public boolean isGameActive();                      // phase HEAD_START or RUNNING
  public String teamDisplay(String teamId);
  // event hooks (W7 wires the Fabric events to these)
  public void onPlayerJoin(ServerPlayer p);  public void onPlayerLeave(ServerPlayer p);
  public void onAfterDeath(LivingEntity e, DamageSource src);   // runner death => onRunnerTeamDown ; hunter death => quiz ; EnderDragon death => endGame(runners / team)
  public void onAfterRespawn(ServerPlayer oldP, ServerPlayer newP);  // hunter -> quiz().onHunterRespawned if enabled; eliminated -> spectator
  public boolean allowChat(PlayerChatMessage m, ServerPlayer sender);  // false = swallow (math answers)
  public void onRunnerTeamDown(String teamId);       // see rules in the table
}

public final class FreezeManager {                    // W5
  public FreezeManager();
  public void freeze(ServerPlayer p); public void unfreeze(ServerPlayer p); public boolean isFrozen(UUID id);
  public void tick(MinecraftServer server);           // snap back to frozen position (ServerGamePacketListenerImpl.teleport), zero velocity, slowness effect; frozen players are invulnerable (restore on unfreeze)
  public void clear(MinecraftServer server);          // unfreeze everybody
}

public final class TeamSync {                         // W4
  public TeamSync(ManhuntGame game);
  public void beginTeam(String teamId, List<ServerPlayer> members);  // set MAX_HEALTH attr = settings.poolHealth(), health=max, food 20/sat 5, ender+inventory canonical = first member (or cleared if settings.clearInventoryOnStart)
  public void addMember(String teamId, ServerPlayer p);   // late join / reconnect: copy canonical to p, set attrs
  public void removeMember(ServerPlayer p);                // leave (disconnect): do NOT reset team, just stop tracking p
  public void tick();                                      // run for all begun teams (called by ManhuntGame.tick while HEAD_START or RUNNING). Calls game.onRunnerTeamDown(teamId) exactly once per team when pool hits 0 or a member is dead/dying.
  public void applySettingsLive();                         // owner changed poolHearts / multiplier mid game
  public void endAll();                                    // restore MAX_HEALTH base to 20, clamp health, drop pools
  public boolean hasTeam(String teamId);
  public float poolHealth(String teamId); public float poolMax(String teamId);
  public int poolFood(String teamId);
  // TEST LAB hooks (act as a "virtual teammate"; players are updated on the next tick)
  public void injectDamage(String teamId, float hp);       // non lethal (never below 1)
  public void injectHeal(String teamId, float hp);
  public void injectHunger(String teamId, int foodPoints); // direct drain
  public void injectItem(String teamId, ItemStack stack);  // appears in shared inventory (first free slot)
  public String inventorySummary(String teamId);           // "3x diamond, 1x iron_pickaxe, ..." (max ~10 entries)
}

public final class RespawnQuiz {                      // W6
  public RespawnQuiz(ManhuntGame game);
  public void onHunterRespawned(ServerPlayer p);      // spectator + first question (needs settings.mathQuestions correct answers)
  public boolean handleAnswer(ServerPlayer p, String text);  // true if p is in quiz and text was consumed (right or wrong)
  public boolean isInQuiz(UUID id);
  public void start(ServerPlayer p);                  // used by test lab / onHunterRespawned
  public void tick();                                 // re-show question on actionbar every ~3s
  public void clear();                                // release everybody to survival
}

public final class Ceremony {                         // W6
  public Ceremony(ManhuntGame game);
  public void play(Winner w, List<ServerPlayer> winners, List<ServerPlayer> losers, String winnerLabel);  // ~15s of title, fanfare, particles; RUNNERS/TEAM: golden fireworks-style particles + totem + dragon roar; HUNTERS: lightning (visual only) + wither/ender sounds + red particles
  public boolean isPlaying();  public void tick();    // when finished calls game.finishCeremony()
  public void cancel();
  public int totalTicks();
}

public final class TestLab {                          // W6 (runtime only, not persisted)
  public TestLab(ManhuntGame game);
  public boolean isActive(); public double timeScale();           // 1.0 normally, 0.1 while fast timers
  public boolean fastTimers(); public void setFastTimers(boolean on);
  public String startSolo(ServerPlayer owner);        // classic game: owner = only runner on team "runners" with a virtual teammate, phase RUNNING immediately, no hunters required
  public void endSolo();                              // = game.stop(false) + clear flags
  public void virtualDamage(ServerPlayer owner, float hp); virtualHeal; virtualHunger(int); virtualGear(ServerPlayer owner) /* injects diamond sword,pickaxe,armor,16 golden carrots...*/;
  public void previewFreeze(ServerPlayer owner, int seconds);     // freeze owner for N seconds (timeScale ignored), actionbar countdown, then unfreeze
  public void previewStagger(ServerPlayer owner);     // chat: with current settings + 4 fake hunters, when each is released (Schedule.releaseSeconds)
  public void quizNow(ServerPlayer owner);            // RespawnQuiz.start(owner) even if setting off
  public void ceremony(ServerPlayer owner, Side side); // play ceremony for the owner (winners=[owner]) without ending anything
  public void tick();
}

public final class CompassTracker {                   // W7
  public CompassTracker(ManhuntGame game);
  public void tick();                                  // every 20 ticks
  public void giveTo(ServerPlayer hunterOrTeamPlayer); // adds one named "Runner Tracker" compass if missing
  public void clearLast();
}
```

## menu (W2) – all menus are `GuiMenu` (ChestMenu subclass, server only). Entry points in `Menus`:

```java
final class Menus { static void openMain(ServerPlayer p); static void openSettings(ServerPlayer p); static void openTeams(ServerPlayer p);
                    static void openPicker(ServerPlayer p, PickerMode mode, String teamId); static void openTestLab(ServerPlayer p); }
enum PickerMode { HUNTER, RUNNER_TO_TEAM }
```
Every click handler MUST re-check `Owner.isOwner(player)` (menus can be opened by command only for owners, but never trust it).
Fetch the game via `ManhuntMod.game()` (static accessor, null before server started => send error, close).

## Entry point (W7): `ManhuntMod implements ModInitializer`
`public static ManhuntGame game()`; registers: `CommandRegistrationCallback` -> `ManhuntCommands.register(dispatcher)`;
`ServerLifecycleEvents.SERVER_STARTED` -> create game (`ConfigIO.load(FabricLoader.getInstance().getConfigDir().resolve("manhunt.json"))`), `Owner.bind`;
`SERVER_STOPPING` -> `game.stop(false)`, save, null; `ServerTickEvents.END_SERVER_TICK` -> `game.tick()`;
`ServerPlayerEvents.JOIN/LEAVE`, `AFTER_RESPAWN`; `ServerLivingEntityEvents.AFTER_DEATH`;
`ServerMessageEvents.ALLOW_CHAT_MESSAGE`; interaction callbacks (`AttackBlockCallback`, `AttackEntityCallback`, `UseBlockCallback`, `UseItemCallback`, `PlayerBlockBreakEvents.BEFORE`) cancel for frozen players (`game.freeze().isFrozen(uuid)`).

## Verified API facts for 26.2 (read from the Fabric API 26.2 sources)

Local reference checkout of Fabric API 26.2 (use `grep -r` on it to verify any Minecraft/Fabric symbol you are unsure of):
`/tmp/claude-0/-home-user-minecraft-manhunt-mod-for-friends/74f00f67-341d-5e54-882d-a109aa52503b/scratchpad/ref/fabric-api`
Minecraft itself is NOT available locally; Mojang names, unobfuscated. Real compile happens in GitHub Actions, so never guess: if you can't find evidence of a vanilla signature in the reference, use the most conservative long-stable API and mark the line with `// VERIFY:`.

* `ResourceLocation` is now `net.minecraft.resources.Identifier` (`Identifier.fromNamespaceAndPath(ns, path)`, `Identifier.parse`).
* `ClickType` is now `net.minecraft.world.inventory.ContainerInput`; `AbstractContainerMenu#clicked(int slotId, int clickData, ContainerInput containerInput, Player player)`. Menu opening: `player.openMenu(new SimpleMenuProvider((containerId, inventory, p) -> menu, title))` (`net.minecraft.world.SimpleMenuProvider`). `net.minecraft.world.SimpleContainer`, `net.minecraft.world.Container#getContainerSize()`, `net.minecraft.world.inventory.MenuType`.
* Permissions: `net.minecraft.server.permissions.PermissionLevel` (ALL, MODERATORS, GAMEMASTERS, ADMINS, OWNERS), `net.minecraft.server.permissions.PermissionSet`. Fabric: `net.fabricmc.fabric.api.permission.v1.{PermissionNode,PermissionPredicates,PermissionContextOwner}`. `CommandSourceStack` (and Entities) implement `PermissionContextOwner`: `src.checkPermission(Identifier id, boolean default)`, `src.checkPermission(Identifier id, PermissionLevel fallbackLevel)`, `src.checkPermission(Identifier)` -> `TriState`; `PermissionNode.of("manhunt","admin")` -> `PermissionNode<Boolean>`; `PermissionPredicates.require(Identifier, PermissionLevel)` is a `Predicate<T extends PermissionContextOwner>` usable in `.requires(...)`.
* Commands: `import static net.minecraft.commands.Commands.literal/argument`; `CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> ...)` (`net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback`); `ctx.getSource().sendSuccess(() -> Component.literal("..."), false)`; `sendFailure(Component)`; `sendSystemMessage(Component)`; `getSource().getPlayer()` / `getPlayerOrException()`; `getSource().getServer()`.
* Events (all `X.EVENT.register(...)` unless noted): `ServerTickEvents.END_SERVER_TICK` (`onEndTick(MinecraftServer)`); `ServerLifecycleEvents.SERVER_STARTED/SERVER_STOPPING` (`(MinecraftServer)`); `ServerPlayerEvents.JOIN/LEAVE` (`(ServerPlayer)`), `ServerPlayerEvents.AFTER_RESPAWN` (`(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive)`), `ServerPlayerEvents.COPY_FROM`; `ServerLivingEntityEvents.AFTER_DEATH` (`(LivingEntity, DamageSource)`), `ALLOW_DEATH` (`(LivingEntity, DamageSource, float) -> boolean`), `ALLOW_DAMAGE` (`(LivingEntity, DamageSource, float) -> boolean`), `AFTER_DAMAGE`; all in `net.fabricmc.fabric.api.entity.event.v1`. `ServerMessageEvents.ALLOW_CHAT_MESSAGE` (`(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound) -> boolean`, `net.fabricmc.fabric.api.message.v1`). Interaction callbacks live in `net.fabricmc.fabric.api.event.player` (`AttackBlockCallback`, `AttackEntityCallback`, `UseBlockCallback`, `UseItemCallback`, `PlayerBlockBreakEvents.BEFORE`) – read their sources in `fabric-events-interaction-v0` for exact lambdas; they return `net.minecraft.world.InteractionResult` (`FAIL`, `PASS`, `SUCCESS`).
* Data components used by Fabric tests: `DataComponents.CUSTOM_NAME`, `ITEM_NAME`, `LORE`, `CUSTOM_DATA`, `ENCHANTMENT_GLINT_OVERRIDE`, `TOOLTIP_DISPLAY`, `FIREWORKS`. `Inventory#getContainerSize()`, `player.getFoodData().getFoodLevel()`.
* Effects are `Holder`s: `MobEffects.BLINDNESS`, `.POISON`, `.SATURATION`, `.REGENERATION`, `.ABSORPTION`; `entity.hasEffect(holder)`, `entity.addEffect(new MobEffectInstance(holder, duration, amplifier, ambient, visible, showIcon))`. Game modes: `net.minecraft.world.level.GameType`.
* Java 25 language level is fine (records, switch patterns, `var`, text blocks, unnamed `_`).
* Do NOT use mixins and do NOT add new Gradle dependencies.
