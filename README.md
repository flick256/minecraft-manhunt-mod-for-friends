# Manhunt mod for friends

A server-side Fabric mod that runs a **manhunt** (speedrun-style hide-and-seek) game for you and your friends.
One player is the **owner** who controls the game from a chest-style control panel. Runners try to beat the
Ender Dragon, hunters try to kill them first. The mod adds shared-life runner teams, a 3-team mode, head-start
freezes, math-quiz respawns, hunter tracking compasses, a victory ceremony and a single-player **Test Lab**.

---

## Requirements

| Component | Version |
|---|---|
| Minecraft | **26.2** |
| Fabric Loader | **0.19.5** or newer |
| Fabric API | **0.161.0+26.2** or newer |
| Java | **25** |

## Installing

1. Put `manhunt-<version>.jar` and Fabric API into the `mods` folder of the **server**.
2. Start the server. The config file `config/manhunt.json` is created the first time settings are saved (for example when the server stops).

**Clients do not need the mod.** Everything runs on the server, and the menus are ordinary chest GUIs.
Installing the mod on the clients is also fine.

Single-player works too: put the jar in the `mods` folder of your Minecraft instance. The integrated server
runs the game, and the first player to join it (you) is the owner.

---

## Who can run the game (ownership)

Only **owners** can open the control panel and change the game. A player counts as an owner if any of these is true:

* the **console** (always);
* the **host of an integrated (single-player / LAN) server**: the first player who joins it;
* the player is listed in `owners` in `config/manhunt.json` (by name or by UUID);
* the player has the Fabric permission node **`manhunt:admin`**. Permission mods (for example LuckPerms
  for Fabric) can grant it.

If `owners` is empty **and** no permission mod answers the check, the mod falls back to the vanilla
`OWNERS` permission level (operators at level 4). Command blocks are never owners.

On an `online-mode=false` server player names are not authenticated, so list UUIDs in `owners` instead of names.

Everybody else can only use the player commands: `join`, `leave`, `answer`, `status`.

### Playing with the Essential mod (essential.gg)

[Essential](https://essential.gg/) is a client-side mod that hosts your own world for friends over a
peer-to-peer connection, so the world runs on **the host's computer** (an integrated server, like "Open to LAN").
That fits this mod:

* The **host** needs Fabric Loader, Fabric API, **this mod** and Essential in their `mods` folder (Essential has
  a Fabric 26.2 build). The mod runs inside the host's world.
* The host is automatically the **owner** (the first player to join their own integrated server). Friends who join
  through Essential are not owners, so only the host gets the control panel and the commands. Friends can still use
  `/manhunt join <team>`, `/manhunt leave`, `/manhunt answer <n>` and `/manhunt status`.
* Friends do not need this mod, because all the logic and the menus run on the host. They only need whatever
  Essential needs to join. Installing it on their side as well does no harm.
* Cheats do not need to be on: the mod checks ownership itself.
* The Test Lab works in the same world, so you can try everything alone before inviting anyone.

### Dedicated servers

Plugins for Bukkit/Spigot/Paper (EssentialsX and similar) **cannot load Fabric mods**. On a real Fabric server you
can list yourself in `owners`, or use a permission mod that supports the Fabric permission API (for example
LuckPerms for Fabric) and grant the node `manhunt:admin`.

---

## Quick start: your first game

1. Join the server (or start a single-player world). You are the owner.
2. Type `/manhunt menu` to open the control panel.
3. **Settings**: check the hearts, hunger and head-start values (defaults: 20 hearts shared, double hunger, 60 s head start).
4. **Teams**: set the kind to *CLASSIC* (one runner team, the others are hunters) or *TEAMS* (2 to 8 teams).
5. **Player picker**: make your friends hunters or runners.
6. Press **Start**. Hunters are frozen during the head start, then released.
7. Hunters get a *Runner Tracker* compass. Follow it.

The game ends when the dragon dies, a runner team is wiped out, or you press **Stop**.

---

## The control panel (`/manhunt menu`)

The panel is a set of chest GUIs. Every click is re-checked for owner permission on the server.

* **Main**: the entry point. Shows the phase (idle, head start, running, celebration), the number of hunters and
  runners, and buttons for *Start*, *Stop*, and to open the other pages.
* **Settings**: changes the numbers and switches described in *Settings reference* below. Changes made during a
  game are applied live where possible (for example the shared hearts).
* **Teams**: create and remove teams, auto-balance online players into 2 to 8 teams (TEAMS mode), and list the
  teams and their members.
* **Player picker**: pick a player from the online list and make them a **hunter** (CLASSIC) or put them on a
  **runner team**. Use it to undo a choice too (*unassign*).
* **Test Lab**: solo testing buttons for every feature. See *Test Lab* below.

Everything in the panel can also be done with commands, see *Command reference*.

---

## Game kinds

### CLASSIC (one runner team vs. hunters)

* All runners share one team (`runners`). The shared life is one pool for the whole team.
* Hunters are everybody else who is online and assigned as a hunter.
* A game needs at least one online runner and one online hunter.
* **Runners win** when the Ender Dragon dies.
* **Hunters win** as soon as any runner dies (the shared pool is empty).

### TEAMS (3v3v3, any number of teams from 2 to 8)

* Players are split into named teams (Red, Blue, Green, Yellow, Aqua, Pink, Gold, White, or your own names).
* **Every participant is a runner** and also a hunter of the other teams: each player's compass points at the
  nearest player of **another** team.
* Each team has its own shared life pool. When a team's pool is empty, that team is **eliminated**.
  Its players become spectators.
* The **last team standing** wins. If a team kills the Ender Dragon, that team wins at once.
* A game needs at least two teams with at least one online member each.

---

## Shared health, hunger and inventory

Each runner team behaves like **one player with one life bar**.

* **Health.** Every member's max health is the team's pool: `poolHearts` hearts (default 20 hearts, which is
  40 hp, shown as two rows of hearts). If one member takes 4 hp of damage, the whole team loses 4 hp.
  Damage from several members in the same tick adds up. Healing from several members takes the highest value.
* **Hunger.** Food and saturation are shared the same way. Hunger drain is divided by `hungerMultiplier`
  (default **2.0**, so the shared food bar lasts twice as long). Only the biggest drain of the team counts, so
  team members do not multiply the hunger cost.
* **Inventory and ender chest.** One shared inventory per team. When a member changes a slot, the change is
  copied to everybody. If two members change the same slot in the same tick, the first one wins.
  Turn the sharing off with `shareInventory`, `shareEnderChest` and `shareHunger`.

### Why the hunger bar looks the same

Minecraft's food bar has 10 icons, so it cannot show more than full. Doubling the hunger is therefore done by
**halving the drain**, not by adding more icons.

---

## Head start and release

When you press **Start**, the hunters are **frozen** for the head start (`headStartSeconds`, default 60 s).
A frozen player cannot move, break blocks, attack or use items. Runners play from the start.

* **All at once** (default): all hunters are released together when the head start ends.
* **Staggered**: hunter number *n* is released at `headStart + n * staggerSeconds`. Use this for a slower
  hunt, so the hunters do not all arrive at the same time.

The Test Lab can show the release schedule for four fake hunters in chat (*preview stagger*).

---

## Hunter tracking compass

Hunters (CLASSIC) and every player (TEAMS) receive a **Runner Tracker** compass when the game starts
(turn it off with `giveCompass`).

* It points at the **nearest enemy in your dimension**. Enemies are the runners (CLASSIC) or players of other
  teams (TEAMS).
* If no enemy is in your dimension, it points at the **last place** an enemy was seen in your dimension.
  That is useful when the runner has gone to the Nether or the End.
* While you hold the compass, the action bar shows `Tracking <name> - 123 blocks`.
  `(last seen)` is added when the position is old.
* Only compasses named *Runner Tracker* are updated. Your own normal compasses are not touched.

---

## Math respawn

With `mathRespawn` on, a hunter who dies does not come back straight away. They become a spectator and get
math questions instead. They must answer `mathQuestions` questions correctly (1 to 10) before they respawn.
Difficulty is `mathDifficulty` (1 to 3).

* Answer in chat, or with `/manhunt answer <number>`.
* The question is repeated on the action bar every few seconds.

---

## Win conditions and victory ceremony

| Event | CLASSIC | TEAMS |
|---|---|---|
| Ender Dragon killed by runners | Runners win | That team wins |
| A runner team's shared life runs out | Hunters win | That team is eliminated (last team standing wins) |

The victory ceremony lasts about 15 seconds: titles, a fanfare, particles and sounds.
Runner wins show golden particles, a totem effect and the dragon roar. Hunter wins show lightning (visual only),
wither and ender sounds and red particles. When the ceremony ends, the game resets to idle.

---

## Command reference

Owner commands are under `/manhunt`. The player commands are listed below as well.

### Game control

| Command | What it does |
|---|---|
| `/manhunt menu` | Opens the control panel (Main page) |
| `/manhunt start` | Starts the game (head start first) |
| `/manhunt stop` | Aborts the game from any phase, restores everybody |
| `/manhunt status` | Shows phase, kind, teams, hunters, runners and settings |
| `/manhunt test` | Opens the Test Lab (solo testing, see below) |

### Roles and teams

| Command | What it does |
|---|---|
| `/manhunt kind <classic\|teams>` | Switches the game kind (idle only) |
| `/manhunt hunter <players>` | Makes the selected players hunters (CLASSIC) |
| `/manhunt runner <players> [team]` | Puts players on a runner team. The team is optional in CLASSIC (it is `runners`) and required in TEAMS |
| `/manhunt unassign <players>` | Removes players from any role |
| `/manhunt team create <id> ["display name"] [color]` | Creates a team (TEAMS mode, at most 8 teams). The id uses `a-z 0-9 _ -`, up to 16 characters. Put the display name in quotes if it has spaces |
| `/manhunt team remove <id>` | Removes a team (its members become unassigned) |
| `/manhunt team auto <count>` | TEAMS: shuffles all online players into `count` teams (2 to 8) |
| `/manhunt team list` | Lists the teams and their members |

### Settings

| Command | Range / default | What it does |
|---|---|---|
| `/manhunt set hearts <n>` | 1 to 200, default 20 | Shared hearts per runner team |
| `/manhunt set hunger <x>` | 1.0 to 8.0, default 2.0 | Hunger multiplier (drain is divided by this) |
| `/manhunt set headstart <seconds>` | 0 to 3600, default 60 | Head start before the hunters are released |
| `/manhunt set release all` / `set release staggered [seconds]` | default all | Release mode for the hunters (the optional seconds also set the stagger gap) |
| `/manhunt set stagger <seconds>` | 1 to 600, default 60 | Gap between hunters when staggered |
| `/manhunt set mathquiz off` | default off | Turns the hunters' math respawn off |
| `/manhunt set mathquiz on [questions 1-10] [difficulty 1-3]` | defaults 1 and 1 | Turns the math respawn on; questions = correct answers needed, difficulty = size of the numbers |

### Owners

| Command | What it does |
|---|---|
| `/manhunt owner add <name or uuid>` | Adds an owner to `owners` in the config. Any owner can add or remove other owners; you cannot remove yourself |
| `/manhunt owner remove <name or uuid>` | Removes an owner from `owners` |
| `/manhunt owner list` | Lists the owners from the config |

### Player commands (for everybody)

| Command | What it does |
|---|---|
| `/manhunt join <team>` | Joins a team while the game is idle (when self-select is on). In CLASSIC only the `runners` team exists; hunters cannot switch themselves |
| `/manhunt leave` | Leaves your team while the game is idle (not for hunters, and not when self-select is off) |
| `/manhunt answer <number>` | Answers the current math question (same as typing the number in chat) |
| `/manhunt status` | Shows the game status and your own role |

---

## Test Lab: trying every feature alone

You can test almost everything in single-player, without friends. Open the Test Lab with `/manhunt test`
(or *Test Lab* on the Main page).

0. **Classic mode only.** Switch the Main page to CLASSIC first; the solo test refuses to run in TEAMS mode.
1. **Start solo.** You become the only runner (your real inventory is kept). The buttons act as a *virtual
   teammate* on your team's shared pool, so the shared bars are real. The game starts immediately, with no hunters
   needed. The other buttons (freeze, quiz) need the solo test to be running; the ceremony previews work any time.
2. **Damage / heal.** Lowers or raises the shared health. Watch the hearts change. Damage from the Test Lab is
   never lethal (it stops at 1 hp).
3. **Hunger.** Drains the shared food bar, so you can see the hunger multiplier at work.
4. **Virtual gear.** Puts a diamond sword, a diamond pickaxe, armor, golden carrots and a shield into the shared inventory,
   so you can test the shared inventory.
5. **Preview freeze.** Freezes you for a few seconds with a countdown on the action bar. Try to move, break
   a block or attack something.
6. **Preview stagger.** Writes the release schedule for four fake hunters into chat, using your current
   head-start and release settings.
7. **Quiz now.** Starts a math respawn question for you. Answer in chat or with `/manhunt answer <n>`.
8. **Ceremony.** Plays the victory ceremony as a runner win, a hunter win or a team win.
9. **Fast timers.** Scales the game timers down to 10 percent of their normal length, for quick testing.
10. **End solo.** Stops the test game and restores everybody to normal.

**Not testable alone:** the hunter compass and hunter release need a second player (a friend or an alt account).
The compass needs an enemy to point at.

---

## Config file: `config/manhunt.json`

The file is created the first time settings are saved. Missing or broken files fall back to the defaults. Values outside
their range are clamped into it. Teams and roles are **not** saved: you set them up again after a restart.

```json
{
  "kind": "CLASSIC",
  "owners": ["YourMinecraftName"],
  "settings": {
    "poolHearts": 20,
    "hungerMultiplier": 2.0,
    "headStartSeconds": 60,
    "releaseMode": "ALL_AT_ONCE",
    "staggerSeconds": 60,
    "mathRespawn": false,
    "mathQuestions": 1,
    "mathDifficulty": 1,
    "shareInventory": true,
    "shareEnderChest": true,
    "shareHunger": true,
    "teamSelfSelect": true,
    "clearInventoryOnStart": true,
    "giveCompass": true
  }
}
```

### Settings reference

| Key | Range / values | Default | Meaning |
|---|---|---|---|
| `poolHearts` | 1 to 200 | 20 | Shared hearts per runner team |
| `hungerMultiplier` | 1.0 to 8.0 | 2.0 | Hunger drain is divided by this |
| `headStartSeconds` | 0 to 3600 | 60 | Head start before the hunters are released |
| `releaseMode` | `ALL_AT_ONCE`, `STAGGERED` | `ALL_AT_ONCE` | How hunters are released |
| `staggerSeconds` | 1 to 600 | 60 | Gap between hunters in `STAGGERED` mode |
| `mathRespawn` | true / false | false | Hunters must answer questions to respawn |
| `mathQuestions` | 1 to 10 | 1 | Correct answers needed |
| `mathDifficulty` | 1 to 3 | 1 | Math question difficulty |
| `shareInventory` | true / false | true | Share the inventory inside a team |
| `shareEnderChest` | true / false | true | Share the ender chest inside a team |
| `shareHunger` | true / false | true | Share food and saturation inside a team |
| `teamSelfSelect` | true / false | true | Non-owners may join a team with `/manhunt join` while idle |
| `clearInventoryOnStart` | true / false | true | Clear the team inventory when the game starts |
| `giveCompass` | true / false | true | Give hunters the Runner Tracker compass |

`kind` is `CLASSIC` or `TEAMS`. `owners` takes player names or UUIDs.

---

## Building from source

```
./gradlew build
```

The jar is written to `build/libs/` (`manhunt-<version>.jar`, plus a `-sources.jar`).
The GitHub Actions workflow (`.github/workflows/build.yml`) runs the same build on every push and pull
request. Download the jar from the run's **manhunt-mod** artifact.

The project uses Java 25 and Fabric Loom. The versions are in `gradle.properties`.

---

## Known limitations

* **Same slot, same tick:** if two team members change the same inventory slot in the same tick, the first
  change wins and the other one is lost.
* **Hunger bar:** the food bar cannot show more than 10 icons, so the double hunger is shown as slower drain,
  not as a longer bar.
* **Teams and roles are not saved** in the config. Only the settings, the owners and the game kind are saved.
* **Bukkit/Spigot/Paper servers are not supported.** Use a Fabric server (see *Who can run the game*).
* **Compass and hunter release** cannot be tested alone (see *Test Lab*).
* **One tick delay:** the shared pool is merged once per server tick, so a hit can show on the bar a tick later.
* **Server-side only:** the mod adds no client HUD elements. Everything is shown with vanilla chat, titles,
  the action bar and chest GUIs.
