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

### Environment
- `fabric.mod.json` uses `"environment": "*"` once the first client enhancement is added. Until then it stays `"server"`.

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
└─ core/                    the core module
   ├─ CoreModule.java       implements EnriquecraftModule; registers the core services
   ├─ framework/            infrastructure shared by all modules
   │  ├─ module/            EnriquecraftModule, ModuleManager
   │  ├─ command/           ModCommand, CommandSpec, CommandCatalog, CommandTreeBuilder, CommandRegistry, ...
   │  ├─ config/            ConfigManager, ConfigValue, ConfigSectionBuilder, ModConfig, ...
   │  ├─ listener/          ModListener
   │  ├─ inject/            ServiceRegistry (constructor injection)
   │  └─ scan/              ClassScanner
   ├─ config/               CoreConfig (the core module's config section)
   ├─ message/              Messenger, Message, Messages (all Spanish text), HelpView, channels
   ├─ service/              business logic (HelpService, ServerInfoService, ...)
   ├─ commands/             auto-discovered commands (see below)
   └─ listeners/            auto-discovered listeners
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
| `message` | Player-facing text (Spanish) and its delivery. Presentation only. |
| `network` | Shared protocol: payload types, codecs, protocol version, server-side capability detection. No client classes. |
| `mixin` | Mixin classes (server-safe only). Thin, delegating. |

The client companion lives in `src/client`, in the package `com.panita.enriquecraft.client`, with its own entrypoint (`EnriquecraftClient`), payload handlers, rendering, HUD, screens, and client mixins (`client.mixin`). It only adds enhancements.

Rules:
- The build uses split source sets. `src/main` holds server code and the shared protocol. `src/client` holds the client companion and client resources.
- `src/main` never depends on `src/client`. `src/client` may depend on `src/main`. The scanner never scans client packages.
- The entrypoint only registers modules. Commands and listeners are never registered by hand.
- Dependencies point inward: presentation and listeners depend on services, never the reverse.
- Do not create the client entrypoint or the client mixin config until the first client enhancement needs them.
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
- Declare each value once, with its default, a comment, and its rule: `bool`, `intRange(min, max)`, or `string` with a validator and a rule written as "must ...". Defaults of player-visible text come from `Messages`. Add a new value type to `ValueTypes` only when a setting needs it.
- Read values with `config.value.get()` at the moment they are needed. Never cache them, never use string-path getters, and never repeat a default at a call site.
- Behavior the framework guarantees: a missing key is added to the file with its comment; a value that is missing, has the wrong type, or breaks its rule uses the default and is reported; keys nobody declares are kept and reported; a file that cannot be parsed is never overwritten (startup uses defaults, a reload keeps the previous values). Values the administrator wrote are kept as written, even when invalid.
- `/enriquecraft reload` reloads the file. Text shown to players about config problems is Spanish and lists only paths; the detailed reasons are English and go to the console.

### Messaging
- All text shown to players goes through `Messenger`. Never call `sendSystemMessage`, title, or boss bar APIs directly.
- `Messenger` has two ways to send. A raw string (`send`, `prefixedSend`, `broadcast`, `prefixedBroadcast`, `sendActionBar`, `showTitle`, `showBossBar`, ...) is the quick way for plain text. A `Message` adds named arguments (`{name}`) and a level, and is used when a template has dynamic values. Every method that sends a prefixed message has a non-prefixed twin, and the `placeholder` variants resolve server placeholders (`%player:name%`) for a context player; the other variants do not resolve placeholders.
- Raw strings and templates accept text tags (`<bold>`) and legacy color codes (`&c`). Templates live in `Messages`, grouped by feature. Pass dynamic values with `Message.with(...)`, never by string concatenation: arguments are inserted as finished components, so their content is never parsed. Only administrator-written text may be sent as a raw string (as `/broadcast` does).
- Use `MessageLevel` (`info`, `success`, `warning`, `error`) for color and icon, and `prefixed()` when the mod prefix is wanted. The prefix comes from the config (`messages.prefix`).
- `/broadcast <prefixed|raw> <message>` sends the text as written, with or without the prefix. Commands never add their own labels or layouts to it.

## 7. Working Process

- Maintain context of the whole project at all times: current state, recent changes, and goals. Every new implementation must integrate with what already exists.
- Read the relevant existing code before changing it.
- Verify changes by building: `./gradlew build`, which also runs the tests (and `./gradlew compileJava compileClientJava` for a quick check). Report failures honestly.
- Logic that does not need the Minecraft runtime (for example the config framework and validators) must have unit tests in `src/test` using JUnit 5. A bug fix starts with a test that reproduces it when that is practical. Tests run from `build/test-run`.
- Do not include unrelated refactors in a feature change. Keep changes scoped to the request.
- If a request violates any rule in this file, ask the owner "are you sure?" and state which rule is affected before doing anything.
