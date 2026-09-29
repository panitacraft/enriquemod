# AGENTS.md

This file is the source of truth for how work is done in this project. Every contributor and every AI agent must follow it. If a request conflicts with any rule here, stop and ask the project owner to confirm before proceeding.

## 1. Project Overview

- **Name:** Enriquecraft
- **Type:** Fabric mod for Minecraft.
- **Minecraft version:** 26.3 (unobfuscated, no mappings required).
- **Language and toolchain:** Java 25, Gradle (via the wrapper), Fabric Loom, Fabric Loader, Fabric API. Exact versions live in `gradle.properties`.
- **Base package:** `com.panita.enriquecraft`
- **Mod id:** `enriquecraft`

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

- Never commit or push without explicit authorization from the owner.
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
  - a top-level package from the architecture table: `command`, `event`, `feature`, `service`, `config`, `message`, `network`, `util`, `mixin`, `client`
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
- `<description>`: imperative, present tense ("add", not "added" or "adds"), lowercase first letter, no trailing period.
- Breaking changes: put `!` right before the colon (`config, feat!: rename the settings file`) and explain the break in a `BREAKING CHANGE:` footer if the description alone is not clear.
- Body (optional): the motivation for the change, in imperative present tense.
- Footer (optional, mandatory for breaking changes): issue references (`Closes #123`) and/or a `BREAKING CHANGE:` explanation.

Examples:

- `command, feat: add command to toggle the spawn protection notice`
- `event, fix: prevent crash when the world is null on disconnect`
- `repo, build: bump Gradle wrapper to 9.7.1`
- `repo, chore: add gradle output folders to .gitignore`
- `config, feat!: rename the settings file`

  `BREAKING CHANGE: the old settings file is no longer read; users must recreate their settings.`

## 5. Engineering Principles

### Design
- Follow SOLID and make good use of OOP: single-responsibility classes, small interfaces, dependency inversion, composition over inheritance.
- Separate concerns. Never mix business logic with presentation (chat output, titles, action bar, boss bars). Message classes format and send text and delegate; logic lives in services or feature classes.
- Prefer Fabric API events over mixins. Use a mixin only when no event or API exists, and keep mixin bodies minimal by delegating to normal classes.

### Code quality
- Strict typing. Avoid raw types, unchecked casts, and unnecessary `Object` usage. Use `final`, records, enums, sealed types, and `Optional` where appropriate. Mark nullability clearly.
- No dead code: no unused classes, methods, imports, parameters, commented-out code, or empty placeholder files.
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

Follow the standard Fabric layout, with each concern in its own package. Commands, listeners, features, and configuration are never mixed in one class.

Target package structure under `com.panita.enriquecraft`:

| Package | Responsibility |
|---|---|
| (base package) | `Enriquecraft` entrypoint class. Only wires modules together; contains no feature logic. |
| `command` | Server command definitions. Parse input and delegate to services. |
| `event` | Event listeners. Subscribe to Fabric events and delegate to services. |
| `feature` | Feature modules. Each feature is self-contained and exposes a small interface. |
| `service` | Business logic shared across features. |
| `config` | Configuration model, loading, and saving. |
| `message` | Player-facing text (Spanish) and its delivery: chat, titles, action bar. Presentation only. |
| `network` | Shared protocol: payload types, codecs, protocol version, and server-side capability detection. No client classes. |
| `util` | Small stateless helpers. Add only when reuse is proven. |
| `mixin` | Mixin classes (server-safe only). Thin, delegating. |
| `client` | Client companion, in `src/client` only. Contains its own entrypoint (`EnriquecraftClient`), client payload handlers, rendering, HUD, screens, and client mixins (`client.mixin`). Enhancements only. |

Rules:
- The build uses split source sets. `src/main` holds server code and the shared `network` protocol. `src/client` holds the `client` package and client resources.
- `src/main` never depends on `src/client`. `src/client` may depend on `src/main`.
- The entrypoints register commands and listeners through dedicated registrar classes, not inline.
- Dependencies point inward: presentation and listeners depend on services, never the reverse.
- Do not create the client entrypoint or the client mixin config until the first client enhancement needs them.

## 7. Working Process

- Maintain context of the whole project at all times: current state, recent changes, and goals. Every new implementation must integrate with what already exists.
- Read the relevant existing code before changing it.
- Verify changes by building: `./gradlew build` (and `./gradlew compileJava compileClientJava` for a quick check). Report failures honestly.
- Do not include unrelated refactors in a feature change. Keep changes scoped to the request.
- If a request violates any rule in this file, ask the owner "are you sure?" and state which rule is affected before doing anything.
