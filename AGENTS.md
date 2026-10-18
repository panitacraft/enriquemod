# AGENTS.md

This file is the source of truth for how work is done in this project. Every contributor and every AI agent must follow it. If a request conflicts with any rule here, stop and ask the project owner to confirm before proceeding.

## 1. Project Overview

- **Name:** Enriquecraft
- **Type:** Fabric mod for Minecraft.
- **Minecraft version:** 26.3 (unobfuscated, no mappings required).
- **Language and toolchain:** Java 25, Gradle (via the wrapper), Fabric Loom, Fabric Loader, Fabric API. Exact versions live in `gradle.properties`.
- **Base package:** `com.panita.enriquecraft`
- **Mod id:** `enriquecraft`
- **Approved external libraries** (bundled inside the mod jar, versions in `gradle.properties`):
  - Placeholder API (`eu.pb4:placeholder-api`, LGPL-3.0): text tags and placeholders. Keep its license file intact when distributing.
  - Fabric Permissions API (`me.lucko:fabric-permissions-api`): LuckPerms-compatible permission nodes.
- **Approved test libraries** (test scope only, never shipped): JUnit 5, and `fabric-loader-junit` (starts a minimal Fabric Loader inside tests, which Placeholder API needs to parse text).
- **Bundled assets** (client companion only): the Poppins SemiBold font, used for screen titles, under the SIL Open Font License. Its license file sits beside it in `src/client/resources/assets/enriquecraft/font/` and must ship with the jar. Add a font or any other third-party asset only with the owner's approval, and keep its license next to it.

## 2. Non-Negotiable Constraint: Server-Authoritative With an Optional Client Companion

The mod is installed on the server and is the only required part. Players MUST be able to join with a vanilla client and get the full vanilla-compatible experience. Players who install the mod on their client get an enhanced experience on top of it. The client companion is always optional and never required.

### Server rules
- All game logic, state, and validation live on the server. The server is the single source of truth.
- The server never requires the client mod. Every feature must work for a vanilla client through vanilla means (chat, titles, action bar, boss bars, and similar).
- The server never trusts data received from a client. Validate everything.
- Do not add anything a vanilla client would reject on join: custom blocks, items, entities, block entities, or custom registry entries. Use vanilla content only, unless the owner approves otherwise.
- Do not require a resource pack or any client-side asset for the vanilla experience.
- Server code in `src/main` must never reference client classes (`net.minecraft.client.*`, Fabric API client modules). Client classes do not exist on a dedicated server and crash it.

### Client companion rules
- Client-only code lives in `src/client` and only adds enhancements. It never changes server rules or gameplay.
- Every client-enhanced feature must have a defined vanilla fallback, implemented on the server, and both must be delivered together.
- The server sends custom payloads only to players whose client has announced support for that channel (`ServerPlayNetworking.canSend` or the configuration-phase equivalent). Never send custom payloads to a client that has not announced support.
- Payload types, codecs, and the protocol version are shared in `src/main` (`network` package). Client and server exchange the protocol version on join. On a mismatch, the server treats that player as vanilla.
- Client mixins are declared in the client mixin config and target client classes only.
- Whether a player's client offers an enhancement is answered by one service, `ClientCapabilities.supports(player, ClientFeature)`. Features call it and fall back to vanilla on false; they never inspect protocol versions or channels themselves. A new enhancement adds one `ClientFeature` value, and the client adds it to its announced set only once it implements it.
- The client sends `HelloC2S` on join, only after checking `ClientPlayNetworking.canSend`, because sending a payload the server did not register throws. What it announces is untrusted: the server accepts it only for the exact protocol version and only for features it knows.

### Environment
- `fabric.mod.json` uses `"environment": "*"` and declares the client entrypoint, since the capability announcement is the first client enhancement.
- Client-only text and key names live in `src/client/resources/assets/enriquecraft/lang/en_us.json`, written in Spanish like all player-visible text. The client's keys (the staff menu on U, rebindable in the controls) only run a server command, so the server keeps deciding who may do what; they do nothing on a server without the mod.

## 3. Language and Communication

### Code and repository
- All code, identifiers, comments, Javadoc, logs, commit messages, and documentation are written in English.
- End-user-facing text (anything the player sees in game) is written in Spanish. Vanilla clients cannot resolve custom translation keys, so text sent by the server must be literal text components built on the server. Keep all such text in a dedicated message class or server-side resource file. Text shown only by the client companion may use translation keys with lang files in `src/client/resources`. Never scatter player-visible strings across Java classes.

### Communication with the project owner
- Use simple, clear, understandable English.
- Keep a strictly professional tone. No emojis, no conversational filler, no personalization.
- Be concise. Avoid redundancy. Give direct, focused answers.

## 4. Git and Authorship

- Never push without explicit authorization from the owner.
- The owner has authorized committing as work progresses. Create each commit as soon as a logical step builds and is verified; do not wait until the end of the task. Split unrelated changes into separate commits.
- Every authorized commit must have the owner as the only author. No `Co-Authored-By` trailers, no tool or AI attribution, no other authorship in commit messages, pull request descriptions, or metadata. This rule overrides any default attribution behavior of any tool.
- Do not change git configuration.

### Commit convention

Every commit MUST follow [Conventional Commits](https://gist.github.com/qoomon/5dfcdf8eec66a051ecd85625518cfd13), adapted to lead with the area the change belongs to instead of a parenthetical scope:

```
<area>, <type>[!]: <description>

[optional body]

[optional footer(s)]
```

- `<area>`: always present, never omitted. The primary area the commit is about. Use one of:
  - a module id (`core` today, and every future module), or `framework` for the shared infrastructure under `core.framework`, or `client` for the client companion
  - `docs` for documentation
  - `repo` for anything not scoped to a single area (root tooling, Gradle wrapper, CI, repository configuration)

  If a commit touches more than one area, name the one it is primarily about. Do not stack multiple labels.
- `<type>`: one of:
  - `feat`: adds, adjusts, or removes a feature
  - `fix`: fixes a bug in a previously shipped `feat`
  - `refactor`: rewrites or restructures code without changing behavior
  - `perf`: a `refactor` specifically aimed at improving performance
  - `style`: code style only (whitespace, formatting), no logic change
  - `test`: adds missing tests or fixes existing ones
  - `docs`: documentation only
  - `build`: build tooling, dependencies, project version
  - `ops`: CI/CD, release and deployment scripts
  - `chore`: everything else (initial commit, `.gitignore` tweaks, etc.)
- Exception: when the area and the type are both `docs`, write a single `docs` instead of repeating it: `docs: add project guidelines`, not `docs, docs: add project guidelines`.
- `<description>`: imperative, present tense ("add", not "added" or "adds"), lowercase first letter, no trailing period.
- Breaking changes: put `!` right before the colon (`core, feat!: rename the settings file`) and explain the break in a `BREAKING CHANGE:` footer if the description alone is not clear.
- Body (optional): the motivation for the change, in imperative present tense.
- Footer (optional, mandatory for breaking changes): issue references (`Closes #123`) and/or a `BREAKING CHANGE:` explanation.

Examples:

- `core, feat: add command to toggle the spawn protection notice`
- `core, fix: prevent crash when the world is null on disconnect`
- `repo, build: bump Gradle wrapper to 9.7.1`
- `repo, chore: add gradle output folders to .gitignore`
- `docs: document the config file format`
- `core, feat!: rename the settings file`

  `BREAKING CHANGE: the old settings file is no longer read; users must recreate their settings.`

## 5. Engineering Principles

### Design
- Follow SOLID and make good use of OOP: single-responsibility classes, small interfaces, dependency inversion, composition over inheritance.
- Separate concerns. Never mix business logic with presentation (chat output, titles, action bar, boss bars). Message classes format and send text and delegate; logic lives in services or feature classes.
- Prefer Fabric API events over mixins. Use a mixin only when no event or API exists, and keep mixin bodies minimal by delegating to normal classes.

### Code quality
- Strict typing. Avoid raw types, unchecked casts, and unnecessary `Object` usage. Use `final`, records, enums, sealed types, and `Optional` where appropriate. Mark nullability clearly.
- No dead code: no unused classes, methods, imports, parameters, commented-out code, or empty placeholder files. The only exception is the public method set of `Messenger`, which mirrors the owner's Tezzlar III Messenger; methods there that nothing calls yet are intentional.
- DRY. Extract shared behavior instead of duplicating it. Do not over-abstract for a single use.
- Comments explain why, not what. Keep them short and in English.

### Reuse before building
- Always analyze the existing code, dependencies, and utilities before writing anything new. Use the current stack (Minecraft, Fabric Loader, Fabric API, existing project helpers) as the primary toolkit.
- Do not add external dependencies unless strictly necessary and explicitly justified to and approved by the owner.
- Do not reimplement what Minecraft or Fabric API already provides.

### File prudence and modularity
- Before creating a file, check whether the logic belongs in an existing module. Create files only when necessary.
- Keep files focused and readable. Split a file when it takes on more than one responsibility.

## 6. Architecture

The mod is split into modules. A module is a self-contained feature set in its own package under `com.panita.enriquecraft`. `core` is the first module and also hosts the framework that every module uses.

```
com.panita.enriquecraft
├─ Enriquecraft.java        entrypoint: registers modules, one line each
├─ core/                    the core module
│  ├─ CoreModule.java       implements EnriquecraftModule; registers the core services
│  ├─ framework/            infrastructure shared by all modules
│  │  ├─ module/            EnriquecraftModule, ModuleManager
│  │  ├─ command/           ModCommand, CommandSpec, CommandCatalog, CommandTreeBuilder, CommandRegistry, ...
│  │  ├─ config/            ConfigManager, ConfigValue, ConfigSectionBuilder, ModConfig, ...
│  │  ├─ data/              WorldData, SnbtStore, TimeCodecs (data files in the world folder)
│  │  ├─ io/                AtomicFiles
│  │  ├─ listener/          ModListener
│  │  ├─ inject/            ServiceRegistry (constructor injection)
│  │  └─ scan/              ClassScanner
│  ├─ gui/                  Menu, MenuFactory, ItemBuilder, ... (the chest toolkit behind UiChestMenu)
│  ├─ ui/                   UiMenu, UiPagedMenu, UiBuilder, UiService, UiSessions, ChatPrompts, ChestLayout (screens shown as the client screen or a chest)
│  ├─ item/                 CustomItemTag, ItemGiving
│  ├─ config/               CoreConfig (the core module's config section)
│  ├─ message/              Messenger, Message, Messages (core's Spanish text), HelpView, PlayerOnly, channels
│  ├─ network/              shared protocol: NetworkProtocol, HelloC2S, ClientFeature, ClientCapabilities, UiElement, the UI payloads
│  ├─ service/              business logic (HelpService, ServerInfoService, ...)
│  ├─ commands/             auto-discovered commands (see below)
│  └─ listeners/            auto-discovered listeners
├─ staff/                   the staff module: the /staff menu, /staff coords, /staff item, /staff invrestore
│  ├─ StaffModule.java
│  ├─ config/ commands/ listeners/ service/
│  ├─ data/                 records with their codecs (SavedCoordinate, SavedItem, DeathRecord)
│  ├─ gui/                  the module's menus
│  └─ message/              StaffMessages (the module's Spanish text) and the views that report outcomes
└─ auth/                    the auth module: who may play and how they prove it (see Authentication)
   ├─ AuthModule.java, AuthHooks.java (the one static door the mixins use)
   ├─ config/ commands/ listeners/ service/ mixin/
   ├─ data/                 AuthAccount, PinCredential, AuthSession, HeldState, AuthMode
   ├─ gui/                  PinMenu (the keypad screen) and PinKeypadMenu (its crafting-table form)
   ├─ event/                AuthEvents (PLAYER_READY: a player is let in)
   └─ message/              AuthMessages (Spanish text) and the views that report outcomes
└─ chat/                    the chat module: everything the server says in chat (see Chat)
   ├─ ChatModule.java, ChatHooks.java (the one static door the mixins use), ChatPermissions.java
   ├─ config/ commands/ listeners/ service/ mixin/
   ├─ channel/              ChannelDefinition, ChannelRegistry, ChannelSelection
   ├─ text/                 ChatTextParser, ChatCapability, EmojiCatalog, LinkDetector, TextRewriter
   ├─ data/                 ChatProfile
   ├─ event/                ChatEvents and SystemEvent: what other modules follow
   └─ message/              ChatMessages (Spanish text) and the views that report outcomes
└─ link/                    the link module: the whitelist follows the Panita accounts (see Link)
   ├─ LinkModule.java
   ├─ config/ commands/ listeners/ service/
   ├─ client/               PanitaApi, HttpPanitaApi, ApiKey, LinkedPlayer (the Panita API, read only)
   ├─ data/                 LinkedSnapshot (the last list applied)
   └─ message/              LinkMessages (Spanish text) and LinkView
└─ discord/                 the server's side of the Discord integration: one connection to the Panita Bot (see Discord)
   ├─ DiscordModule.java
   ├─ config/ commands/ listeners/ service/
   ├─ protocol/             the frames of the bridge protocol and their codec
   ├─ transport/            BridgeTransport and its WebSocket implementation
   └─ message/              DiscordMessages (Spanish text) and DiscordView
```

Every module follows the same layout:

| Package inside a module | Responsibility |
|---|---|
| `<Name>Module` | Implements `EnriquecraftModule`. Creates and registers the module's services. |
| `commands` | Commands, discovered automatically. |
| `listeners` | Event listeners, discovered automatically. |
| `service` | Business logic shared across the module's features. |
| `feature` | Self-contained features that expose a small interface. |
| `config` | The module's config class, discovered automatically (see Configuration). |
| `data` | Records stored in the world folder, each with its `Codec`. |
| `gui` | The module's menus. |
| `message` | The module's player-facing text (`<Name>Messages`, Spanish) and the views that report outcomes. Presentation only. |
| `network` | Shared protocol: payload types, codecs, protocol version, server-side capability detection. No client classes. |
| `mixin` | Mixin classes (server-safe only). Thin, delegating. |
| `event` | Fabric events (`EventFactory`) and their payload records, for the modules that follow this one (`AuthEvents`, `ChatEvents`). |

The client companion lives in `src/client`, in the package `com.panita.enriquecraft.client`, with its own entrypoint (`EnriquecraftClient`), payload handlers, rendering, HUD, screens (`client.ui` shows the screens the server describes), and client mixins (`client.mixin`). It only adds enhancements.

Rules:
- The build uses split source sets. `src/main` holds server code and the shared protocol. `src/client` holds the client companion and client resources.
- `src/main` never depends on `src/client`. `src/client` may depend on `src/main`. The scanner never scans client packages.
- The entrypoint only registers modules. Commands and listeners are never registered by hand.
- Dependencies point inward: presentation and listeners depend on services, never the reverse.
- The client mixin config is `enriquecraft.client.mixins.json` (in `src/client/resources`, declared in `fabric.mod.json` for the client environment only); a client enhancement that needs a mixin adds it there, in `client.mixin`, kept thin and delegating to `client.chat` and the like.
- A module that needs mixins has its own config, `enriquecraft.<module>.mixins.json`, listed in `fabric.mod.json`, whose package is `<module>.mixin`. Mixin forbids loading any other class from that package, so it holds only mixins. A mixin is not built by the service registry, so it reaches services through one static class in the module root (`AuthHooks`), installed when the module loads; without it every mixin leaves vanilla behavior untouched. This hooks class is the only static access to services.
- Fabric events cannot be unregistered, so modules cannot be enabled or disabled while the server runs.

### Modules
- To add a module, create its package and a `<Name>Module` class implementing `EnriquecraftModule`, then add one line in `Enriquecraft.onInitialize`. Modules load in registration order, and later modules can use the services of earlier ones.

### Commands
- A command is a class in `<module>.commands` that implements `ModCommand` and is annotated with `@CommandSpec(name, description, parent, aliases, access)`. It adds arguments and executors in `configure`. Nothing else is needed: no list, no registration call.
- `description` is Spanish and must reference a constant in `Messages`. `access` is the vanilla `PermissionLevel` used when no permission manager decides. Permission nodes are derived from the command path (`enriquecraft.command.<path>`); never declare them by hand.
- Naming and layout:
  - A standalone command is named `<Name>Command` and sits directly in `<module>.commands`.
  - A command with subcommands gets its own folder, `<module>.commands.<name>/`, holding the parent `<Name>Command` and its children `<Name>Subcommand`.
  - A subcommand points to its parent with `parent = <Name>Command.class`. A subcommand that has children of its own gets a nested folder inside its parent's folder.
- Do not write Brigadier permission, alias, or nesting code by hand; `CommandTreeBuilder` does it.
- A command that text in the chat runs when clicked (such as `/imagen`) is declared `clickable = true`. The game asks the player to confirm a click on any command its tree marks as needing permissions, and the permission manager marks every command that way, even one open to everyone; a clickable command is always in the tree, and its executor checks `CommandPermissions.canUse` itself. It takes only a token the server issued and looks nothing else up.
- Executors read input and delegate. Logic that is more than a getter belongs in a service.

### Listeners
- A listener is a class in `<module>.listeners` that implements `ModListener` and subscribes to events in `register()`. It is discovered and built automatically.

### Dependency injection
- Commands and listeners declare exactly one public constructor. Each parameter is resolved by its exact type from the `ServiceRegistry`.
- A module registers its services in `registerServices`. Services are created explicitly there, in one place. There is no static access to services.
- A missing service, a missing `@CommandSpec`, an unregistered parent, a parent cycle, or a duplicate literal stops startup with a message that names the class.

### Configuration
- There is one config file, `config/enriquecraft.json5`, with one top-level section per module. The section name is the last segment of the module's package name (`core`). Comments (`//`, `/* */`) are accepted when reading; trailing commas in objects are not.
- A module declares its settings in one class in `<module>.config` that implements `ModConfig`, has one public constructor taking a `ConfigSectionBuilder`, and exposes its values as public final `ConfigValue` fields. A module has at most one config class; it is discovered, bound, and registered as a service automatically, so other classes receive it by constructor injection.
- Declare each value once, with its default, a comment, and its rule: `bool`, `intRange(min, max)`, `string` with a validator and a rule written as "must ...", or `stringMap` for a table of texts (keys keep the order of the file). Defaults of player-visible text come from `Messages`. Add a new value type to `ValueTypes` only when a setting needs it. A setting with a shape of its own, such as the set of chat channels, implements the public `ValueType` in its module and is declared with `custom`; its `parse` throws an `InvalidValueException` that names the part that is wrong, since the whole value then falls back to its default. A default too long for one comment line is left out of the comment, since the file already shows it as the value.
- Read values with `config.value.get()` at the moment they are needed. Never cache them, never use string-path getters, and never repeat a default at a call site.
- Behavior the framework guarantees: a missing key is added to the file with its comment; a value that is missing, has the wrong type, or breaks its rule uses the default and is reported; keys nobody declares are kept and reported; a file that cannot be parsed is never overwritten (startup uses defaults, a reload keeps the previous values). Values the administrator wrote are kept as written, even when invalid.
- `/enriquecraft reload` reloads this file and the messages file. Text shown to players about config problems is Spanish and lists only paths; the detailed reasons are English and go to the console.

### Data files
- Game data (records, items, inventories) is stored as SNBT, never JSON, because JSON cannot tell an int from a byte or a long and would change the custom data of every item. The config file stays JSON5 since people edit it by hand.
- Register a store with `WorldData.register(file, codec, empty)` when it lives as long as the mod, or open one with `WorldData.open(...)` for files not known up front (one per player). Files live in `<world>/enriquecraft/`, are written on every change, atomically, and are named `.snbt`.
- Describe each record with a `Codec` in a `data` package. Write only what is present (for example the occupied slots), and use `TimeCodecs.INSTANT` for moments. A file that cannot be read is moved aside as `.broken` and never overwritten.
- Items are stored with `ItemStack.CODEC`, which keeps every component exactly. Never store an item as text of your own making.
- Never modify a stored stack: give out copies (`ItemGiving.give` copies, drops what does not fit, and never loses anything).
- Custom items carry `custom_item = enriquecraft:<name>` in their custom data. Test for one with `CustomItemTag.is(stack, name)`; do not read the data by hand.

### Menus
- A screen is a `UiMenu` (or a `UiPagedMenu<T>` for lists) in `core.ui`. It describes the screen once, in `describe(UiBuilder)`, as a tree of `UiElement`s: column, row, grid, label, button, text input, dropdown, scroll, divider, detail and spacer. `UiService` shows it in the form the player's client allows: the client companion's own screen for players who announced `ClientFeature.CUSTOM_UI`, and a vanilla chest laid out by `ChestLayout` for everyone else. Never branch on the client yourself and never write a screen twice.
- Create a new menu object for every player and every opening; it holds the state of that viewing, such as the page or a search. `describe` is called again on `refresh()`, so read data there, not in the constructor. The shape of the tree (how many chest rows it needs) must not change while the menu is open.
- Buttons and fields get their ids from `UiBuilder`; the client only reports an id, never a command. Actions receive a `UiClick` or a `UiSubmit`. Check `click.isLeft()` in a button action, and send anything destructive, such as deleting a record, through a `ConfirmMenu` so every confirmation looks and behaves the same. A text field's action must not assume how the value arrived (players without the companion type it in chat) and must treat an empty value as "cleared".
- Standard navigation and purpose use roles, not layout: give a button its `ButtonRole` (`CLOSE`, `BACK`, `PREVIOUS`, `NEXT`) and a list its `UiElement.Page`. The client companion places them itself (back and close in the header, search field and page buttons in the footer) and a chest ignores the roles, so never position them for the client. A screen whose last band is a row of controls gets that layout; keep that row last. Roles also decide how an action looks, so no action resembles an item cell: plain actions are tinted blue, `SUCCESS` (giving back, completing) is green, `DANGER` (deleting) is red, and `CONFIRM` and `CANCEL` are green and red labeled pills for questions (Escape presses `CANCEL`). Give every delete `DANGER` and every restore `SUCCESS`. A kind of action that should be recognized at a glance gets a color of its own with the `tint` overload of `UiBuilder.button`, using the constants in `Tints` (purple for teleporting, brown for storing in chests); use the same tint for the same kind of action in every menu.
- A `Detail` can carry an editable title (`Detail.withEditableTitle`) that the client companion edits in place with a check to save; a chest ignores it, so such a menu overrides `describeChest` and offers a `TextInput` there. Mark text that copies a value when clicked, such as an id, with `CopyText.of`, and mark only the value, never its label; the client underlines and copies just that part and honors no other click event. A picture of an item in front of a line, to tell lines apart, is `ItemIcons.of`.
- The client companion's screens never pop. A screen that replaces another grows from the other's panel shape while its content fades in from the panel color, and a refresh of the same screen eases the panel to its new size. They also fit the window they are in: when the panel is taller than the window the scroll areas shrink, then large icons and the title get smaller, and as a last resort the whole body scrolls. Do not animate or size anything on the server side, and never assume how tall the client's window is.
- A form that asks for one value is a label with the `TextInput` right below it in the body, and `ButtonRole.CANCEL` plus `UiBuilder.submitButton` in the controls row: the client companion focuses the field, and the confirm button sends the field's value exactly as Enter does, so the field's own action does the work. A chest, whose fields apply a value as soon as it is typed, gets its own arrangement from `describeChest`.
- Related data is grouped and laid out in columns with `Column(children, Align.START)` inside a `Row`; the client companion starts the columns at the top, and stacks the columns of a row one under the other when together they are wider than the window. Data that can be long gets an arrow button that shows or hides it, kept in the menu's own state. Group lines with blank space between groups, never run them together.
- A list whose entries can be arranged by hand overrides `UiPagedMenu.reorderable()` and `move(dragged, onto)`: the client companion drags one entry onto another (the dragged one takes the other's place), and a chest gets a switch that turns clicks into lifting an entry and placing it (`arranging()`, `arrange(entry)`). The order is stored and shared by all staff. Dragging goes through `UiBuilder.draggable` and `onDrop`, checked by `UiSessions` like any other action. The client companion makes it smooth by itself: the dragged item trails the mouse, and when the server answers with the new order every button glides from its old place to its new one.
- A coordinate's icon is an item, or the head of a player picked by name: the server resolves the name to the player's id and stores both, because the skin is found by id. A kind of icon that needs more than one click is a door entry in `CoordinateIconMenu` that opens its own menu (the head asks for a name, the structure maps list one map per structure).
- A pressable `Detail` placed in a `Row` is drawn by the client companion as a card: a fixed-size tile with the item above and a short title below in the game's font, cut when too long, so a row of cards stays uniform. Keep card titles short; use a `Detail` in a column for the full view of one thing.
- The staff menu (`/staff`, key U on the client) is `StaffMenus.open`. Each tool it offers is a `StaffSection` tied to the command that grants access, and is shown only to players who may run that command. A new staff tool adds one section, one entry in `StaffMenus.menuOf`, and a menu that accepts the menu to go back to.
- Show one thing in full (an item and everything about it) with a `UiElement.Detail`: the client companion draws the item large with the text under it, and a chest shows the item with the text as its tooltip. A menu names an item for the client's title with `icon()`; a chest has no place for it. A `Detail` can make its item pressable (`UiBuilder.detail`).
- Long content goes in a `UiElement.Scroll` (a limit in GUI pixels; the client companion also shrinks it to keep the panel inside the window) with a `UiElement.Divider` above it; a chest has no scrolling, so give it a different `describeChest` (the same facts as tooltips or rows) rather than paging the client's view. A choice among a few options is a `UiElement.Dropdown`; a list offers its filters through `UiPagedMenu.filters()`.
- Menus of one kind look and read alike, so share instead of repeating: click hint lines come from `ClickHints` (left click pastel green, right click pastel blue, always after a blank line), dates from `Timestamps`, player heads from `PlayerHeads`, confirmation screens from `ConfirmMenu`, and lines about a record from a view class shared by its list and its detail (`DeathInfo`, `ItemMetadata`). A new menu reuses these before adding anything.
- Positions shown to staff are block coordinates (`CoordinateView.block`); the exact values stay in the data.
- `UiSessions` checks every action a client reports before anything runs: the session, the element and its kind, the mouse button, the value's length and characters, and the pace. Never trust or re-implement that in a menu, and never add a way around it.
- Chest decoration is `chestStyle()`: `PLAIN`, `FRAMED` (lists, with the last band on the bottom edge) or `FILLED` (every slot without an element is filler) or `SLOTS` (like `FILLED`, but the empty cells of a grid stay empty, because each stands for a slot of something that held nothing, as in an inventory). Choose the style that keeps the chest looking as it should.
- Close a menu with `ui().close(player)`, never `player.closeContainer()`: a custom screen has no container behind it.
- Show a stored item with a button whose icon is `stack.copy()` and whose label is empty: it keeps its own name and lore, and extra tooltip lines are added after them. Never put the stored stack itself in a screen.
- `Menu` in `core.gui` is the chest toolkit behind `UiChestMenu`; do not subclass it for new screens. It is display only: `MenuScreen` never lets a click move, drag, swap, drop or clone an item, so never work around it and never put a real inventory behind a menu.
- Commands that need a player use `PlayerOnly.executes(...)`.
- Each module keeps its Spanish texts in its own `<Name>Messages` class; text shared by every module lives in `core.message.Messages`.
- A screen that a chest cannot be, such as the PIN keypad, overrides `UiMenu.openFallback(player)` to open a form of its own for players without the companion; it then looks after its own closing and redrawing (`PinMenu` does). A chest is still the default.
- `UiElement.PinPad` is the keypad element: the server keeps the digits and sends only how many are typed, the attempts left, a count of wrong PINs (the client shakes the dots when it grows) and whether it succeeded; the client reports keys by id (`UiBuilder.pinPad`, `PinKeys`). The client companion takes the keyboard as presses of the same keys and lets neither Escape nor a close button dismiss it. It uses a compact size when the window is small.

### Authentication
- The server runs with `online-mode=false`. A mixin on the login (`ServerLoginPacketListenerImplMixin`) answers the game's "does this server authenticate players" question for each connection alone, so Microsoft accounts are verified by Mojang's session servers and everyone else joins offline. No Mojang call is made for a listed name: the whitelist (and the op list) decide the mode, and an entry carries it in its UUID (the real one of an account is premium, the offline one of the name is a PIN account; `AuthMode.of`). When the whitelist is off, an unlisted name is looked up (`PremiumLookup`, cached, limited to what Mojang allows) and refused when Mojang cannot answer; never guess that a name has no account.
- `/whitelist add <name> [cracked|premium]` and `remove` are rewired by `WhitelistCommandMixin`, keeping the game's arguments and permissions and changing only what they run. Names are used exactly as typed: the game lowercases names it does not know, and an offline UUID depends on capitalization. A PIN player is let in under the capitalization of their whitelist entry. Changing a player's mode changes their UUID, so `PlayerDataMigration` copies their player data, statistics and advancements; it only runs while they are offline and never overwrites.
- A player who must enter a PIN is a held player (`HeldPlayers`): moved to the spawn (their real position and game mode are kept on disk until they leave, so a crash cannot strand them), put in spectator mode when `pin.spectatorWhileWaiting` is on, announced like any join so staff can report them, invulnerable, and everything their client sends except what the PIN screen needs is dropped in `PacketUtilsMixin` before it is handled. They leave this state by a correct PIN, a kick (too many wrong PINs, the timeout, staff) or disconnecting. The game ignores container clicks of spectators, so `ServerGamePacketListenerImplMixin` makes an exception for held players, or the keypad could not be pressed in spectator mode. Never add a way for a held player's packet to reach a handler.
- PINs are stored only as a salted PBKDF2 hash mixed with a pepper kept in `config/enriquecraft-auth.key`, outside the world data; the file is never regenerated silently, since that would invalidate every PIN. Remembered logins keep a keyed hash of the address, never the address, and count from the moment the PIN was typed. PINs use the digits 1 to 9 (no zero). Wrong PINs kick after `pin.maxAttempts`; `lockout.kicksBeforeBlock` kicks within `lockout.windowSeconds` block the address for `lockout.blockSeconds` (`AttemptLimiter`, in memory). A name that logs in with a PIN cannot be taken over while it is online, since the game would otherwise disconnect its owner.
- Login messages come from `LoginView`: a welcome title and a chat line about the session (premium players: "automatic login"; PIN players: how long the session lasts, via `Durations`). A client without the companion is first shown a title that explains the PIN screen and gets the chest after `pin.introSeconds`; a client that announced the companion gets its screen at once.
- The game turns down a message or a command with a signable argument from a player without a profile key when `enforce-secure-profile` and `online-mode` are on. A PIN player has no Microsoft account behind the name and can never have a key, so `ServerGamePacketListenerImplMixin` gives their connection a decoder that accepts unsigned messages (`AuthHooks.hasNoProfileKey`).
- The game builds the player from the profile it authenticated and only tells the client the profile the login finishes with. SkinsRestorer puts the skin a player chose on the second one, so it showed while connecting and was gone once the player was in. `ServerLoginPacketListenerImplMixin` makes the game keep the profile the login finishes with (`LoginProfiles.carry`), but only when it is the same player under the same name, so a mod can change how a player looks and never who joins.
- Connections of PIN players are not encrypted by the game, so a PIN travels in clear text. This is a known limit of offline-mode login that the owner has not yet confirmed as acceptable.

### Chat
- The chat module takes over chat in a way every vanilla client shows: messages are system messages built on the server. `ChatMessageListener` cancels the game's chat broadcast in `ALLOW_CHAT_MESSAGE` and sends the module's own line (core registers first, and Fabric stops at the first handler that returns false, so a chat prompt still gets its answer). It never decides what players may do in the world.
- What a player types is read only as far as their permissions allow (`ChatTextParser`, `ChatCapability`, one node each: `enriquecraft.chat.color`, `.format`, `.gradient`, `.hover`, `.click`, `.links`, `.emoji`). The administrator's parser never sees player text. A click tag can open a link, suggest a command or copy text; it can never run a command, at any permission. The result is inserted into the line as a finished component (`Message.with`), never parsed again. Control characters and the section sign are removed.
- Channels are the `chat.channels` setting (`ChannelsType`, `ChannelDefinition`): an id, aliases, a format with `{sender}`, `{message}` and `{channel}` and server placeholders, a permission node (empty for everyone) with a fallback level, and one default channel. `global` and `staff` must exist, since `/ac` and `/sc` send to them. A player writes in one channel (`ChannelSelection`, in memory, back to the default on join) and reads every channel they may use. The default format uses `%luckperms:prefix%` and `%luckperms:suffix%`, which LuckPerms for Fabric registers in Placeholder API; the module has no code for LuckPerms. The default formats are texts of `ChatMessages.Format`, so the messages file changes them and a channel's own entry overrides them.
- Emojis are the `chat.emojis` table: a trigger and what it turns into (tags allowed). They replace text after the player's text is parsed (`TextRewriter` over the literal text of the component), longest trigger first, never inside a part that can be clicked. Use only symbols the game's font draws; colored emoji would need a resource pack.
- Nicknames are display only (`NicknameService`, stored in `chat_profiles.snbt`). `PlayerDisplayNameMixin` makes the game's own `getDisplayName` show the nickname, so every game message that names a player does, and `ServerPlayerTabNameMixin` does the same for the tab list. Real names stay everywhere else: auth, whitelist, logs, hover text. A nickname is letters, digits and `_`, with no spaces, so it can be a command argument and a name in the client; it is never the real name of a player who may play here (online players and the whitelist, `KnownPlayers`) or the nickname of one, and nobody is let off that. It waits `nickname.cooldownSeconds` between changes (`enriquecraft.chat.nick.bypass` skips only the wait) and colors need `enriquecraft.chat.nick.color`. Placeholder API placeholders: `%enriquecraft:nickname%` and `%enriquecraft:channel%`.
- A client knows another player only by their entry in its player list, and writes that name over their head, suggests it when a command asks for a player and lists it in the tab list. `PlayerInfoEntryMixin` gives the entry a profile with the nickname as the name (same id and skin; the server's own profile is never changed), and `ClientNames.refresh` replaces the entry and shows the entity again to those who see it, so a change shows at once. `EntitySelectorNicknameMixin` lets a nickname stand for the player in every command argument that asks for a player by name; the real name keeps working but is not suggested.
- The game's messages about a player (joined, left, died, advancement) all end in `PlayerList.broadcastSystemMessage`; `PlayerListSystemMessageMixin` hands them to `SystemMessages`, which recognizes them by translation key (`SystemMessageClassifier`), announces them as a `SystemEvent` through `ChatEvents.SYSTEM` whether or not the administrator shows them, and shows the template of `ChatMessages.Events` unless the `events.*` setting is off. The game announces a join before it adds the player to its list, so the mixin remembers the player being placed. A first join is a player who has never left the server (the leave-game statistic is zero). Messages about team-only deaths are left as the game sends them. Every other message passes untouched.
- `/msg`, `/tell` and `/w` (and `/me`) keep the game's arguments and permissions; `MsgCommandMixin` and `EmoteCommandsMixin` swap what they run, through the same `CommandNodeAccessor` as the whitelist. The sender sees the name they used for the target: the real name when they wrote it, the nickname otherwise. `/r` and `/reply` answer the last player exchanged a private message with (`ReplyTracker`, in memory). `/say` and `/teammsg` stay as the game has them.
- `/sudo <player> <text>` (administrators, level 3) makes a player run a command (text starting with `/`) or say a message in the channel they write in. The command runs with the permissions of the player, and only when they have no higher command level than who asks (`SudoService`), so it is never a way to gain permissions. Held players are refused.
- The server list text is `ChatMessages.Motd.TEXT` (`motd.enabled`). The game rebuilds its status every few seconds and `MinecraftServerMotdMixin` puts a real component in it, so placeholders such as `%server:online%` stay current.
- Players who have not entered their PIN yet (`HeldPlayers`) neither write nor read chat, private messages and the messages about players; their join is still announced at connect, as auth does. The welcome message (`WelcomeListener`) waits for `AuthEvents.PLAYER_READY`, not for the connection.
- Other modules follow chat through `ChatEvents` (`MESSAGE`, with the channel, the sender, the plain text and whether it is a `/me`; `SYSTEM`) and never through the game's own events or the mixins.

### Link
- The link module keeps the whitelist in step with the Panita accounts. It only reads: `GET /v1/mod/players` of `panita-api` with the `mod` service key, through the JDK `HttpClient` (no dependency added). It never sends anything to the API and never contacts Discord; the Discord bridge is a later module.
- The service key is a secret and never goes in `enriquecraft.json5`. `ApiKey` reads the environment variable `ENRIQUECRAFT_LINK_API_KEY`, then the one-line file `config/enriquecraft-link.key`, at each sync, so a key put in place later is found by the next reload. The file is never created, logged or shown; `/link status` only says whether a key was found. Without a key a sync does nothing and the console says so once.
- There is no timer. A sync (`LinkSync`, never two at once) runs when the server has started, on `/enriquecraft reload` (through `ReloadHooks` in the framework), on `/whitelist reload` (`AuthEvents.WHITELIST_RELOADED`, fired by `WhitelistCommandMixin`), on `/link sync`, and when a player leaves while another player's change waits for them. Threads: the answer arrives on a network thread, the whitelist is only read and changed on the server thread, and the questions to Mojang that premium names need run on the module's worker (`LinkApplier.resolve`), so the server never waits on the network.
- `LinkPlan` compares the list with the whitelist itself, so a change that did not happen, or that staff undid by hand, is found again. Entries staff added by hand are never touched: only a player of the previous list (`linked_players.snbt`, the snapshot) who left the list is removed. Four actions: add, replace (the other mode, or another capitalization of an offline name, since that changes the UUID), rename (found by the stable Panita `user_id`) and remove. A switch or rename that changes the UUID waits while the player is online and is applied when they leave, with their data copied by `PlayerDataMigration`; a rename that waits keeps its old name in the snapshot and drops the ETag. A player removed while online is kicked with `LinkMessages.Kick.DEACTIVATED`.
- A list that is not exactly the documented shape fails as a whole, so half an answer never removes anyone. A list that would remove more than `link.maxRemovalPercent` (default 50) of the previous players is refused and reported. A failed sync changes nothing.
- Premium and cracked come from `minecraft_status`: cracked is the offline UUID of the exact name; premium asks Mojang through `WhitelistAdmin.resolve`, and is skipped when the whitelist already has a premium entry for the name. A name that Mojang does not know, or cannot answer for, is reported, never guessed.
- `/link sync` (administrators) and `/link status` (game masters). Reports to staff are Spanish; the reasons of failures are English and go to the console.

### Discord
- **The mod never talks to Discord.** The Panita Bot is the only Discord client (its commands, `/help`, deploy and rate limits stay in one place); the mod keeps one authenticated WebSocket to it and exchanges small JSON frames. No Discord library and no Discord token are in the mod, and no other dependency was added: the connection is the JDK's `java.net.http.WebSocket` and the JSON is Gson. The protocol (version 1) is defined by the bot, in `panita-bot/docs/minecraft-bridge-plan.md`, and mirrored by `protocol/`.
- The address is `discord.url` (`wss://`, or `ws://` for localhost only; empty keeps the module off). The bridge key is a secret: `config/enriquecraft-discord.key` or the environment variable `ENRIQUECRAFT_BRIDGE_KEY`, read with `SecretFile` at each attempt, never in the config file, never logged or shown; `/discord status` only says whether it was found.
- `BridgeConnection` opens the line after the server has started, says hello, waits for the welcome (which lists the chat channels the bot bridges) and comes back by itself when the line drops, waiting one second and doubling up to a minute with some randomness. It holds at most 100 chat lines and events for at most 60 seconds while the line is down, drops a line that is silent for 90 seconds (the bot pings every 30) and never blocks or throws at the server: the game does not depend on the bot. A bot with another protocol version leaves the connection `INCOMPATIBLE` until `/discord reconnect` or a reload. The transport is behind `BridgeTransport`, so an API relay could replace the WebSocket without anything else changing.
- Everything from the bot is untrusted: `FrameCodec` checks the shape and size of each frame, the channel must be one the server has, and a Discord member's text is shown as plain text (no formats, colors, hover or click), with control characters removed. A message from Discord enters the chat module through `ChatService.deliverExternal` and **does not fire `ChatEvents.MESSAGE`**, so it can never be sent back out. Nothing the bot sends can run a command or change any rule of the game.
- What the mod reports comes from the modules' own events, never the game's: `ChatEvents.MESSAGE` (lines and `/me`), `ChatEvents.SYSTEM` (deaths, advancements, who arrived for the first time), `AuthEvents.PLAYER_READY` (a join is reported when the player is ready, a leave only for players who were), `CoreEvents.BROADCAST` and the server lifecycle. Death lines go in English with their translation key; an advancement goes as its title alone (no player, no brackets) with its key, which says whether it is a task, a goal or a challenge.
- A player in a frame carries the hash of the skin the server shows them with (`skin`, left out when they have none), so the head the bot draws is the one players see in game. SkinsRestorer is not a dependency: it puts the skin it chose in the `textures` property of the player's game profile at login and at each `/skin` change, and `SkinTextures` reads it from there at each use. Only the hash of a `textures.minecraft.net` texture is sent, never an address, so the bot decides which service draws the head; without a skin the bot falls back to the head of the account, as before.
- Images from Discord (the `media` feature, protocol 1 with optional fields; design in `docs/discord-media-plan.md`, contract in the bot's `docs/minecraft-bridge-plan.md` section 4.4). The mod announces `media` in its `hello` (`discord.mediaEnabled`), and only then does the bot send a chat frame's images as `media` entries; to a mod that did not, the bot keeps writing `[adjunto]`. `FrameCodec` checks each entry's shape on its own (an invalid one, or one past four, is dropped and added to the connection's dropped count), and `DiscordDelivery` drops any entry whose host is not exactly one of `discord.mediaHosts` (an entry written `*.klipy.com` allows its subdomains, never the bare domain). The bot also turns links pasted in a message into entries: a direct image link, or a Klipy, Tenor or Giphy page through its `og:image` (https, or plain http only for localhost, for development). `MediaView` shows each as `[nombre]` (`[spoiler]` when marked) followed by `(Link)`, which opens the original after the game's own confirmation. The name is never read as formatting. The section sign, control characters and invisible formatting characters are removed from it. The line may be only images. Addresses expire after about a day.
- Clicking `[nombre]` runs `/imagen <token>` (open to everyone; the game sends a click command without a confirmation screen only when it is valid for a player with no permissions and has no signable argument). `MediaTokens` maps the short token to an image the server put in the chat (10 minutes, bounded, in memory), so a click can never ask for anything else. `ImageMaps` then downloads the image on a worker (`ImageFetcher`: allowed host, no redirects, `image/*` only, 8 MiB, 8 s), checks its dimensions before decoding (`ImageDecoder`, at most 40 megapixels; WebP is not supported), fits it into the colors of a vanilla map (`MapPixels`; an animated GIF is read frame by frame with its disposal rules by `GifDecoder`, at most 4 megapixels, 200 frames and 120 megapixels of drawing, and each frame is fitted without dithering) and `ShownMaps` shows it, repeating an animation by sending only the rectangle that changed in each frame (`discord.mediaAnimate` turns it off): the client is told, by packets only, that the slot the player holds has a filled map with a map id real maps never use. Nothing is added to the real inventory or saved in the world, and the map cannot be taken. Any packet outside the allow-list of movement, chat and keep-alives (`ShownMaps.isResting`, seen through `discord.mixin.PacketUtilsMixin` and `DiscordHooks`) puts the slot back as it is, and so do `discord.mediaMapSeconds`, a changed held slot, an open container and logging out. A player has one request at a time and waits `discord.mediaCooldownSeconds` between two.
- Images in the chat of the client companion (`ClientFeature.MEDIA_PREVIEW`, the enhancement of the label and the map). After a Discord line is delivered, `MediaPreviews` sends `MediaPreviewS2C(url, width, height)` for each image that is not a spoiler, whose host is one of the fixed hosts of `MediaLinks.isTrusted` (https only, never a local host and never widened by a setting, since the player's address goes to the host), to the readers who announced the feature; players without it get nothing extra and keep the label, the map and the link. The client (`client.chat`) fetches an announced address itself (`ImageFetcher`; a GIF is played with `GifDecoder`, at most 60 frames, each copied into the texture only when due and drawn; anything else is read by `ImageDecoder`), scales it once to the size `ChatImageLayout` gives (at most 200 x 108 GUI pixels, twice that in texture pixels), keeps at most 24 textures and forgets them on disconnect. `ChatComponentMixin` adds lines that hold slices of the image under a chat line that links to a ready image (matched by the address of its `(Link)`), and the mixins on the chat's two drawing classes draw them through `ChatImageRenderer`, so the image scrolls and fades with the chat. The player can turn it off with `chatImages=false` in `config/enriquecraft-client.properties`.
- Screenshot sharing (`ClientFeature.SCREENSHOT_SHARE`, the bot's `media_upload`; design in `docs/discord-media-plan.md`, contract in the bot's `docs/minecraft-bridge-plan.md` section 4.4). The client companion adds `[Copiar]` and `[Compartir]` buttons to the game's line about a saved screenshot (`ScreenshotMixin`; `[Compartir]` only on a server that takes uploads), which run the client commands `/enriquecraft_copy <file>` (the picture goes to the system clipboard through the JDK's, off the game thread) and `/enriquecraft_share <file>`; both only open a plain `.png` name inside the screenshots folder. Another mod may replace that line (Essential shows its own toast), so a rebindable key (G by default, `ShareLastKey`) opens the share screen for the newest screenshot without depending on it. The share screen has a copy button too. The server says nothing when a share succeeds: the image appears in the chat. The screen shows the picture and a caption and says once that a screenshot may show coordinates, chat and other players; nothing is sent before the share button. `ScreenshotSender` downscales and encodes it with the shared `ScreenshotEncoder` and sends `ShareBeginC2S`, `ShareChunkC2S` (30 KiB, a few per tick) and `ShareEndC2S`. The server never trusts any of it: `ScreenshotShares` needs the announced feature and a player who is not held, `ShareTransfers` checks size, order, one transfer per player and a 30 second limit, the share must be for a channel the bot bridges that the player may use and the bot must have offered `media_upload`, `discord.sharePerMinute` limits each player, and `ScreenshotEncoder` decodes and writes a new JPEG (no metadata, at most 1920 pixels, under 2 MiB) on a worker. `HttpMediaUploader` posts it to `<discord.url as https>/media` with the bridge key (read at each upload, never logged; the JDK `HttpClient`) and reads the answer strictly. The bot posts through the channel's webhook under the player's name, so every share is in Discord and in the game chat (there is no share to the chat alone, which would need a private channel just to host the image). The chat line is built from the address the bot answered, never from the client, and must pass the `discord.mediaHosts` check like any image; it goes through `ChatService.deliverExternal`, so it does not fire `ChatEvents.MESSAGE` and is not posted twice. Players without the companion cannot share (they can still paste a link).
- The bot may ask `playerlist`, `tps` and `status`; `QueryAnswers` answers on the server thread, without counting players still on the PIN screen. Frames from the network hop to the server thread in `FrameRouter`.
- `/discord status` (game masters) and `/discord reconnect` (administrators).

### Messaging
- All text shown to players goes through `Messenger`. Never call `sendSystemMessage`, title, or boss bar APIs directly. Text built from parts, such as a chat line that carries what a player typed, is formatted with `Messenger.format(Message, player)` (named arguments, placeholders resolved for that player) and sent with `Messenger.deliver` to the players who should read it, and to the console when the other overload is used.
- `Messenger` has two ways to send. A raw string (`send`, `prefixedSend`, `broadcast`, `prefixedBroadcast`, `sendActionBar`, `showTitle`, `showBossBar`, ...) is the quick way for plain text. A `Message` adds named arguments (`{name}`) and a level, and is used when a template has dynamic values. Every method that sends a prefixed message has a non-prefixed twin, and the `placeholder` variants resolve server placeholders (`%player:name%`) for a context player; the other variants do not resolve placeholders.
- Raw strings and templates accept text tags (`<bold>`) and legacy color codes (`&c`). Templates live in `Messages`, grouped by feature. Pass dynamic values with `Message.with(...)`, never by string concatenation: arguments are inserted as finished components, so their content is never parsed. Only administrator-written text may be sent as a raw string (as `/broadcast` does).
- Use `MessageLevel` (`info`, `success`, `warning`, `error`) for color and icon, and `prefixed()` when the mod prefix is wanted. The prefix is `{{PREFIX}} {{SEPARATOR}} ` (by default a gradient "Enriquecraft" and a dark gray »); both come from the `Variables` group of `Messages`, so the administrator changes them in the messages file (`core.variables.prefix`, `core.variables.separator`). Any text may use `{{PREFIX}}` and `{{SEPARATOR}}`, such as a title that is only the prefix; never write the prefix out by hand.
- Every player-visible text is configurable. Texts are the `public static final String` constants of nested classes in a module's `<Name>Messages` class, declared to the framework by `EnriquecraftModule.messages()`. `MessageCatalog` lists them in `config/enriquecraft-messages.json5`, one section per module and one key per constant (`Pin.ENTER_TITLE` is `pin.enterTitle`), and `Messenger`/`MessageFormatter` swap a constant for what the file says, so code keeps passing the constant. Constants with exactly the same wording are one setting. A text must be a whole constant: never build one by concatenating constants, because only whole constants are found again. `/enriquecraft reload` reloads this file too.
- `/broadcast <prefixed|raw> <message>` sends the text as written, with or without the prefix. Commands never add their own labels or layouts to it.

## 7. Working Process

- Maintain context of the whole project at all times: current state, recent changes, and goals. Every new implementation must integrate with what already exists.
- Read the relevant existing code before changing it.
- Verify changes by building: `./gradlew build`, which also runs the tests (and `./gradlew compileJava compileClientJava` for a quick check). Report failures honestly.
- Logic that does not need the Minecraft runtime (for example the config framework and validators) must have unit tests in `src/test` using JUnit 5. A bug fix starts with a test that reproduces it when that is practical. Tests run from `build/test-run`.
- Tests that need Minecraft items or components call `MinecraftTestSupport.bootstrap()`; menus are checked without a player through `MenuTesting`. Behavior that needs a real player (teleporting, giving items, death capture) is checked on a dev server with Fabric's `FakePlayer`; force-load the chunk first, because no entity exists in an inactive chunk.
- Do not include unrelated refactors in a feature change. Keep changes scoped to the request.
- If a request violates any rule in this file, ask the owner "are you sure?" and state which rule is affected before doing anything.
