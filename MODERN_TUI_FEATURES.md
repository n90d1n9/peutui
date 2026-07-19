# Peutui Modern TUI Features

A comprehensive guide to the modern TUI features added to Peutui, an agnostic TUI framework specialized for AI agents.

**Related Documentation:**
- [AGENT.md](./AGENT.md) - Agent integration and orchestration guide
- [README.md](./README.md) - Framework overview and module map

## Table of Contents

1. [Core Components](#core-components)
2. [Streaming Support](#streaming-support)
3. [Command Palette](#command-palette)
4. [Advanced Layout](#advanced-layout)
5. [Extension Guide](#extension-guide)
6. [Integration Examples](#integration-examples)

---

## Core Components

### HeaderComponent
Modern header with tab navigation support.

**Features:**
- Title and subtitle display
- Tab navigation with keyboard shortcuts (arrows, Home/End, 1-9)
- Visual active/inactive states
- Callback handler for tab changes
- Responsive design with ellipsis truncation

**Usage:**
```java
HeaderComponent header = new HeaderComponent("My App", "AI Assistant");
header.addTab("Chat", () -> System.out.println("Switched to Chat"));
header.addTab("Settings", () -> System.out.println("Switched to Settings"));
header.selectTab(0);
```

### ChatBoxComponent
Enhanced scrollable chat with modern features.

**Features:**
- Smooth scrolling (Up/Down, Page Up/Down, Home/End)
- Timestamps for each message (HH:mm format)
- Role-based avatars (● user, ○ assistant, ⚙ system, ⚡ tool)
- Customizable per-role colors
- Message separators
- Visual scroll indicator (scrollbar)
- Auto-scroll to bottom option

**Usage:**
```java
ChatBoxComponent chat = new ChatBoxComponent();
chat.setAutoScroll(true);
chat.setShowTimestamps(true);
chat.append(new AgentMessage(MessageRole.USER, "Hello!"));
chat.append(new AgentMessage(MessageRole.ASSISTANT, "Hi there!"));
```

### ChatInputComponent
Advanced text input with history and shortcuts.

**Features:**
- Blinking cursor animation
- Character count with warning near limit
- Input history navigation (Up/Down arrows)
- Optional line numbers
- Advanced shortcuts (Ctrl+A/E/W/U/K, Page Up/Down)
- Configurable max lines/characters

**Usage:**
```java
ChatInputComponent input = new ChatInputComponent();
input.setMaxLines(5);
input.setMaxCharacters(1000);
input.setOnSubmit(text -> sendMessage(text));
```

### PanelComponent
Flexible container with customizable borders.

**Features:**
- 6 border styles: NONE, SINGLE, DOUBLE, ROUNDED, BOLD, ASCII
- Centered title on top border
- Configurable padding
- Child component wrapping
- Focus passthrough

**Usage:**
```java
PanelComponent panel = new PanelComponent("My Panel");
panel.setBorderStyle(PanelComponent.BorderStyle.ROUNDED);
panel.setPadding(1, 1, 1, 1);
panel.setContent(childComponent);
```

---

## Streaming Support

### Architecture
The streaming module provides abstractions for rendering incremental content from AI models.

**Key Classes:**
- `StreamingToken` - Represents a single token from a stream
- `StreamingRenderer<T>` - Abstract base for custom renderers
- `MarkdownStreamingRenderer` - Markdown parser with streaming support
- `MarkdownStreamComponent` - Ready-to-use component for markdown streams

### Usage Example

```java
// Create markdown stream component
MarkdownStreamComponent streamView = new MarkdownStreamComponent();
streamView.setTitle("AI Response");

// Feed tokens as they arrive
streamingResponse.subscribe(token -> {
    streamView.feed(token);
    if (token.isComplete()) {
        streamView.complete();
    }
});

// Reset for new response
streamView.reset();
```

### Extending the Renderer

Create custom renderers by extending `StreamingRenderer`:

```java
public class JsonStreamingRenderer extends StreamingRenderer<StreamingToken> {
    @Override
    public void onData(StreamingToken token) {
        buffer.append(token.content());
        // Custom parsing logic
        triggerRender();
    }
    
    @Override
    protected String formatContent() {
        // Return formatted JSON with syntax highlighting
        return JsonHighlighter.highlight(getContent());
    }
}
```

---

## Command Palette

VS Code-style command palette for quick access to actions.

### Components

**CommandItem**
```java
CommandItem item = new CommandItem(
    "agent.new-chat",      // ID
    "New Chat",            // Name
    "Start a new conversation",  // Description
    "Ctrl+N",              // Shortcut
    "Agent",               // Category
    true                   // Enabled
);
```

**CommandPaletteComponent**
```java
CommandPaletteComponent palette = new CommandPaletteComponent();
palette.register(new CommandItem("chat.clear", "Clear Chat", "Clear conversation history"));
palette.register(new CommandItem("settings.theme", "Theme", "Change theme"));

palette.setOnExecute(cmd -> {
    switch (cmd.id()) {
        case "chat.clear" -> clearChat();
        case "settings.theme" -> showThemeSelector();
    }
});

// Toggle visibility with Ctrl+P
palette.toggle();
```

### Features
- Fuzzy filtering by name, description, category, or shortcut
- Keyboard navigation (↑↓, Enter, Esc)
- Scrollable list with visual selection
- Customizable placeholder text
- Close callback support

---

## Advanced Layout

### Split Layout (Planned)
Dynamic split views with resizable panes for complex layouts.

```java
SplitLayout layout = new SplitLayout(Orientation.HORIZONTAL);
layout.addComponent(markdownView, 0.6);  // 60% width
layout.addComponent(chatBox, 0.4);       // 40% width
```

### Grid Layout (Planned)
Flexible grid-based layouts for dashboard-style interfaces.

```java
GridLayout grid = new GridLayout(2, 2);
grid.addComponent(header, 0, 0, 2, 1);  // Span 2 columns
grid.addComponent(sidebar, 0, 1, 1, 1);
grid.addComponent(main, 1, 1, 1, 1);
```

---

## Extension Guide

### Creating Custom Components

1. **Implement the Component interface:**
```java
public class MyCustomComponent implements Component {
    @Override
    public boolean isFocusable() { return true; }
    
    @Override
    public Size measure(BoxConstraints constraints) {
        return new Size(constraints.maxWidth(), constraints.maxHeight());
    }
    
    @Override
    public void render(RenderContext context) {
        // Render logic here
    }
    
    @Override
    public boolean handleInput(InputEvent event) {
        // Handle keyboard/mouse input
        return false;
    }
}
```

2. **Use AnsiCodes for styling:**
```java
String styled = AnsiCodes.BOLD + AnsiCodes.GREEN + "Success!" + AnsiCodes.RESET;
```

3. **Handle scrolling:**
```java
private int scrollOffset = 0;
private void scrollUp() { scrollOffset = Math.max(0, scrollOffset - 1); }
private void scrollDown() { scrollOffset++; }
```

### Theme Support

All components use `AnsiCodes` constants for easy theming:

```java
// Define custom theme colors
public class MyTheme {
    public static final String PRIMARY = AnsiCodes.BRIGHT_BLUE;
    public static final String SECONDARY = AnsiCodes.BRIGHT_MAGENTA;
    public static final String SUCCESS = AnsiCodes.BRIGHT_GREEN;
    public static final String WARNING = AnsiCodes.BRIGHT_YELLOW;
    public static final String ERROR = AnsiCodes.BRIGHT_RED;
}
```

### Best Practices

1. **Composability**: Build complex UIs by composing simple components
2. **State Management**: Keep component state minimal and immutable where possible
3. **Performance**: Use efficient rendering (only update changed areas)
4. **Accessibility**: Provide clear focus indicators and keyboard navigation
5. **Responsiveness**: Handle different terminal sizes gracefully

---

## Integration Examples

### Full Chat Application

```java
import tech.kayys.peutui.agent.api.Agent;
import tech.kayys.peutui.widgets.*;
import tech.kayys.peutui.core.app.App;
import tech.kayys.peutui.core.component.Component;

public class ChatApp extends App {
    private final Agent agent;
    private HeaderComponent header;
    private ChatBoxComponent chatBox;
    private ChatInputComponent input;
    private CommandPaletteComponent palette;
    
    public ChatApp(Agent agent) {
        this.agent = agent;
    }
    
    @Override
    protected Component buildUI() {
        // Header with tabs
        header = new HeaderComponent("AI Assistant", "Powered by Peutui");
        header.addTab("Chat", this::showChat);
        header.addTab("History", this::showHistory);
        
        // Chat box with auto-scroll
        chatBox = new ChatBoxComponent();
        chatBox.setAutoScroll(true);
        chatBox.setShowTimestamps(true);
        
        // Input with history
        input = new ChatInputComponent();
        input.setMaxLines(5);
        input.setOnSubmit(this::sendMessage);
        
        // Command palette (Ctrl+P)
        palette = new CommandPaletteComponent();
        registerCommands();
        
        // Main layout
        VerticalStackComponent mainLayout = new VerticalStackComponent();
        mainLayout.addComponent(header);
        mainLayout.addComponent(chatBox);
        mainLayout.addComponent(input);
        
        return mainLayout;
    }
    
    private void registerCommands() {
        palette.registerAll(List.of(
            new CommandItem("/new", "New Chat", "Start new conversation", "Ctrl+N"),
            new CommandItem("/clear", "Clear", "Clear chat history"),
            new CommandItem("/export", "Export", "Export conversation")
        ));
        palette.setOnExecute(cmd -> handleCommand(cmd.id()));
    }
    
    private void sendMessage(String text) {
        chatBox.append(new AgentMessage(MessageRole.USER, text));
        
        // Stream markdown response
        MarkdownStreamComponent streamView = new MarkdownStreamComponent();
        agent.send(text).subscribe(token -> {
            streamView.feed(token);
            if (token.isComplete()) {
                chatBox.append(new AgentMessage(MessageRole.ASSISTANT, streamView.getContent()));
            }
        });
    }
    
    private void handleCommand(String cmdId) {
        switch (cmdId) {
            case "/new" -> createNewChat();
            case "/clear" -> chatBox.clear();
            case "/export" -> exportConversation();
        }
    }
}
```

### Command-Driven Interface

```java
public class CommandApp extends App {
    private CommandPaletteComponent palette;
    
    @Override
    protected Component buildUI() {
        palette = new CommandPaletteComponent();
        registerCommands();
        
        // Bind Ctrl+P to toggle palette
        terminal.onKey(KeyEvent.ctrl('P'), () -> palette.toggle());
        
        return palette;
    }
    
    private void registerCommands() {
        palette.registerAll(List.of(
            new CommandItem("file.new", "New File", "Create new file", "Ctrl+N"),
            new CommandItem("file.open", "Open File", "Open existing file", "Ctrl+O"),
            new CommandItem("edit.undo", "Undo", "Undo last action", "Ctrl+Z"),
            new CommandItem("agent.chat", "New Chat", "Start chat with AI", "Ctrl+Shift+C")
        ));
        
        palette.setOnExecute(cmd -> executeCommand(cmd.id()));
    }
}
```

---

## Future Enhancements

- **Syntax Highlighting**: Integrate with code highlighters for better code block rendering
- **Mouse Support**: Click-to-scroll, click-to-select in command palette
- **Resizable Panes**: Drag-to-resize split layouts
- **Diff Viewer**: Side-by-side diff for code changes
- **Tree View**: File system browser component
- **Table Component**: Sortable, filterable data tables
- **Progress Indicators**: Animated progress bars and spinners
- **Notifications**: Toast-style notifications

---

## Contributing

Peutui is designed to be extensible. We welcome contributions for:
- New component types
- Theme improvements
- Performance optimizations
- Documentation enhancements

See the source code in `/workspace/peutui-widgets` for implementation examples.
