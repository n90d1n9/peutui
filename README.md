# Peutui

A framework-agnostic terminal UI (TUI) toolkit for building interactive AI
agent front-ends on the JVM, wired together with modern Quarkus (Java 21,
CDI/Arc, `@ConfigMapping`).

Peutui itself never talks to a specific model, a specific agent framework,
or a specific database. Every axis that tends to vary between projects is
expressed as a **strategy interface** with a couple of ready-made
implementations; a host application picks concrete implementations either
by writing plain CDI beans (`Agent`, `ModelProvider`) or by setting one
`application.properties` value per axis.

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>tech.kayys.peutui</groupId>
    <artifactId>peutui-widgets</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. Create Your First Component

```java
import tech.kayys.peutui.widgets.HeaderComponent;
import tech.kayys.peutui.core.app.App;
import tech.kayys.peutui.core.component.Component;

public class MyApp extends App {
    @Override
    protected Component buildUI() {
        HeaderComponent header = new HeaderComponent("My App", "Welcome");
        header.addTab("Home", this::showHome);
        header.addTab("About", this::showAbout);
        return header;
    }
}
```

### 3. Run the Application

```bash
java -jar your-app.jar
```

Press `Ctrl+C` to exit.

## Architecture Overview

Peutui follows a layered architecture:

```
┌─────────────────────────────────────┐
│         Your Application            │
├─────────────────────────────────────┤
│         peutui-widgets              │  ← UI Components (Header, ChatBox, etc.)
├─────────────────────────────────────┤
│         peutui-core                 │  ← Core Engine (Rendering, Events, Layout)
├─────────────────────────────────────┤
│      peutui-terminal                │  ← Terminal Driver (JLine backend)
└─────────────────────────────────────┘
```

**Key Design Principles:**

1. **Framework Agnostic**: Works with any Java framework or standalone
2. **Strategy Pattern**: All varying axes use interchangeable strategies
3. **Zero Opinions**: Core doesn't know about agents, sessions, or providers
4. **CDI Optional**: Quarkus integration is a convenience layer, not required

## Module Map

| Module | What it owns |
|---|---|
| `peutui-core` | ANSI-aware text measurement/slicing, grapheme-cluster width, word navigation, a diffed `ScreenBuffer`, the `Component` render-tree model, input events, and a pluggable autocomplete engine. Zero framework dependencies. |
| `peutui-terminal` | `TerminalDriver` strategy + a JLine-backed implementation: raw mode, alternate screen, bracketed paste, resize (`SIGWINCH`), and byte-level input decoding into `peutui-core` events. |
| `peutui-agent-api` | `Agent`, `AgentEvent` streaming model, and `AgentOrchestrationStrategy` — **single-agent** vs **multi-agent** (with pluggable `AgentRouter`s: round-robin, capability-based). |
| `peutui-provider-api` | `ModelProvider` (Anthropic / OpenAI-compatible / local Gollek / anything), `ProviderRegistry`, and `ProviderSelectionStrategy` — **single**, **round-robin**, or **failover** across registered providers. |
| `peutui-project-api` | `ProjectRegistry` — **single fixed project** vs **multi-project switcher**. |
| `peutui-session-api` | `Session` + `SessionStore` (in-memory, local-file JSON, or any `StorageBackend`) + `SessionManager` — **single active session** vs **multi concurrently-open sessions**. |
| `peutui-settings-api` | Layered settings (`GLOBAL < USER < PROJECT < SESSION` precedence) via `SettingsStore` + `LayeredSettingsResolver`. |
| `peutui-storage-api` | Generic `StorageBackend` strategy: **local file**, **local database** (any JDBC `DataSource` — SQLite/H2/Postgres/...), or **cloud** (generic REST/S3-style). Sessions and settings both layer on top of whichever mode is configured. |
| `peutui-widgets` | Ready-made components on top of `peutui-core`: `TextInputComponent` (cursor, word-nav, autocomplete popup), `ChatHistoryComponent` (word-wrapped, scrollable), `StatusBarComponent`, `SpinnerComponent`, `VerticalStackComponent` (simple layout), and the `App` render loop tying a `TerminalDriver` to a component tree with diffed frame output. |
| `peutui-quarkus-runtime` | `PeutuiConfig` (`@ConfigMapping(prefix = "peutui")`) plus CDI producers that translate config into the concrete strategy for every axis above. |
| `peutui-demo-cli` | A runnable chat REPL wiring a mock multi-word-streaming `ModelProvider` and an `Agent` through the full stack, to see it all move. |

## Configuring strategies

Every strategy choice lives under one `application.properties` prefix:

```properties
# One fixed project vs a switchable multi-project registry
peutui.project.mode=single|multi

# One active session vs several concurrently open
peutui.session.mode=single|multi

# Where sessions/settings actually live
peutui.storage.mode=local-file|local-database|cloud
peutui.storage.local-file.root-path=.peutui/data
peutui.settings-storage.mode=local-file|local-database|cloud

# One default agent vs capability-routed multi-agent
peutui.agent.mode=single|multi
peutui.agent.default-agent-id=assistant

# How to pick among registered ModelProvider beans
peutui.provider.selection=single|round-robin|failover
peutui.provider.default-provider-id=mock
peutui.provider.failover-order=anthropic,local-gollek
```

Registering a new agent or provider is just writing a plain CDI bean:

```java
@ApplicationScoped
public class MyAgent implements Agent { ... }

@ApplicationScoped
public class AnthropicProvider implements ModelProvider { ... }
```

`AgentOrchestrationProducer` / `ProviderRegistryProducer` collect every such
bean automatically — no manual registry wiring required.

## Running the demo

```bash
cd peutui-demo-cli
mvn quarkus:dev
```

(or `mvn package` then `java -jar target/quarkus-app/quarkus-run.jar`)

It opens a full-screen chat REPL against a canned, word-by-word streaming
mock provider — press `Ctrl+C` to quit. Swap `peutui.provider.*` /
`peutui.agent.*` in `application.properties` (see the commented alternatives
in that file) to try multi-agent / round-robin / failover without touching
any code.

## Design notes

- **Strategy over inheritance.** Every varying axis (agent count, project
  count, session count, provider count, storage location) is one interface
  with a small number of interchangeable implementations, never a base
  class hierarchy to subclass.
- **`peutui-core` has no opinions.** It doesn't know what an agent, a
  session, or a provider is — it only knows how to measure/slice ANSI text,
  diff a screen buffer, and route input events to a focused `Component`.
  Everything else composes on top.
- **CDI is optional.** `peutui-quarkus-runtime` is a convenience wiring
  layer; every API module works standalone (`new SingleAgentOrchestrationStrategy(...)`,
  `new FileSessionStore(path)`, etc.) for non-Quarkus hosts.
- Built for Java 21 (records, sealed interfaces, pattern-matching `switch`)
  and targets `quarkus-bom` 3.15.1.

## Documentation

- **[AGENT.md](./AGENT.md)** - Complete guide for AI agent integration, multi-agent orchestration, and streaming
- **[MODERN_TUI_FEATURES.md](./MODERN_TUI_FEATURES.md)** - Modern TUI components (Header, ChatBox, Command Palette, Streaming)
- **[CHANGELOG.md](./CHANGELOG.md)** - Version history and changes

## Not included

This repository was generated without network/Maven access in the build
sandbox, so it has not been compiled with `mvn` here — every file was
hand-reviewed for package/import consistency and brace/paren balance, but
run a `mvn -q -DskipTests package` locally before relying on it in
production, and expect the odd small fix on first compile of a project this
size.
