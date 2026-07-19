# Modern TUI Features Added to Peutui

This document summarizes the new modern TUI (Terminal User Interface) features added to the Peutui framework.

## New Components

### 1. HeaderComponent (`HeaderComponent.java`)
A modern header component with:
- **Title and subtitle** display with styled formatting
- **Tab navigation** with visual active/inactive states
- **Keyboard navigation**: Left/Right arrows, Home/End, number keys (1-9) for direct tab selection
- **Configurable tabs**: Add/remove tabs dynamically with labels and IDs
- **Callback support**: `onTabSelected` handler for tab change events
- **Responsive design**: Tabs truncate with ellipsis when space is limited

**Usage Example:**
```java
HeaderComponent header = new HeaderComponent()
    .setTitle("My Application")
    .setSubtitle("Modern TUI Demo")
    .addTab("Chat", "chat")
    .addTab("Settings", "settings")
    .addTab("Help", "help")
    .setOnTabSelected(index -> System.out.println("Tab changed: " + index));
```

### 2. ChatBoxComponent (`ChatBoxComponent.java`)
An enhanced scrollable chat component with modern features:
- **Smooth scrolling**: Up/Down arrows, Page Up/Down, Home/End keys
- **Timestamps**: Configurable timestamp display for each message (HH:mm format)
- **Role-based avatars**: Visual indicators per message role (● for user, ○ for assistant, etc.)
- **Customizable styling**: Per-role color configuration (user, assistant, system, tool)
- **Message separators**: Optional separator lines between messages
- **Scroll indicator**: Visual scrollbar on the right showing position in conversation
- **Auto-scroll**: Configurable auto-scroll to bottom on new messages
- **Empty state**: Custom message when chat is empty
- **Border styling**: Left border with margin for modern look

**Features:**
- `setShowTimestamps(boolean)` - Toggle timestamp display
- `setShowSeparators(boolean)` - Toggle message separators  
- `setShowScrollIndicator(boolean)` - Toggle scroll position indicator
- `setAutoScroll(boolean)` - Enable/disable auto-scroll
- `setRoleStyle(MessageRole, RoleStyle)` - Customize role appearance
- `setEmptyMessage(String)` - Custom empty state message

### 3. ChatInputComponent (`ChatInputComponent.java`)
An enhanced chat input component with:
- **Blinking cursor**: Configurable cursor visibility and blink animation
- **Character count**: Display current/max characters with warning when near limit
- **Input history**: Navigate previous inputs with Up/Down arrows
- **Line numbers**: Optional line number display
- **Enhanced autocomplete**: Improved suggestion popup with descriptions
- **Advanced keyboard shortcuts**:
  - Ctrl+A/E: Jump to start/end of line
  - Ctrl+W: Delete word backward
  - Ctrl+U: Delete line before cursor
  - Ctrl+K: Delete line after cursor
  - Page Up/Down: Jump to first/last line
- **Configurable limits**: Max lines and max characters
- **Custom prompt prefix**: Changeable from default "╰─❯ "

**Features:**
- `setBlinkCursor(boolean)` - Enable/disable cursor blinking
- `setShowCharCount(boolean)` - Toggle character counter
- `setShowLineNumbers(boolean)` - Toggle line numbers
- `setMaxLines(int)` / `setMaxChars(int)` - Set input limits
- `addToHistory(String)` - Manually add to input history
- `setCursorVisible(boolean)` - Control cursor visibility

### 4. PanelComponent (`PanelComponent.java`)
A flexible panel container with:
- **Multiple border styles**: NONE, SINGLE, DOUBLE, ROUNDED, BOLD, ASCII
- **Centered title**: Optional title text centered on top border
- **Configurable padding**: Independent left/right/top/bottom padding
- **Child component wrapping**: Contains any other component
- **Focus passthrough**: Forwards input to child component

**Border Styles:**
```java
BorderStyle.NONE      // No border
BorderStyle.SINGLE    // ─ │ ┌ ┐ └ ┘
BorderStyle.DOUBLE    // ═ ║ ╔ ╗ ╚ ╝
BorderStyle.ROUNDED   // ─ │ ╭ ╮ ╰ ╯
BorderStyle.BOLD      // ━ ┃ ┏ ┓ ┗ ┛
BorderStyle.ASCII     // - | + + + +
```

**Usage Example:**
```java
PanelComponent panel = new PanelComponent()
    .setChild(chatBox)
    .setTitle("Conversation")
    .setBorderStyle(PanelComponent.BorderStyle.ROUNDED)
    .setPadding(1, 1, 0, 0);
```

## Enhanced Core Classes

### AnsiCodes.java
Added standard ANSI color constants:
- **Standard colors**: BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE
- **Bright colors**: BRIGHT_BLACK through BRIGHT_WHITE
- All colors use standard foreground codes (30-37, 90-97)

### BoxConstraints.java
Added convenience factory method:
- `BoxConstraints.tight(int width, int height)` - Create tight constraints without Size object

## Integration Example

Here's how to use these components together in a modern TUI application:

```java
// Create header with tabs
HeaderComponent header = new HeaderComponent()
    .setTitle("Peutui Chat")
    .setSubtitle("AI Assistant")
    .addTab("Chat", "chat")
    .addTab("History", "history");

// Create enhanced chat box
ChatBoxComponent chatBox = new ChatBoxComponent()
    .setShowTimestamps(true)
    .setShowSeparators(true)
    .setShowScrollIndicator(true)
    .setAutoScroll(true);

// Create enhanced input
ChatInputComponent input = new ChatInputComponent()
    .setPlaceholder("Type your message...")
    .setShowCharCount(true)
    .setBlinkCursor(true)
    .setMaxChars(2000);

// Wrap in panels
PanelComponent chatPanel = new PanelComponent()
    .setChild(chatBox)
    .setTitle("Messages")
    .setBorderStyle(PanelComponent.BorderStyle.ROUNDED);

PanelComponent inputPanel = new PanelComponent()
    .setChild(input)
    .setBorderStyle(PanelComponent.BorderStyle.SINGLE);

// Stack vertically
VerticalStackComponent root = new VerticalStackComponent()
    .addFixed(header, 3)
    .addFlexible(chatPanel)
    .addFixed(inputPanel, 4);

// Wire up app
App app = new App(terminalDriver, root);
app.setFocused(root);
root.setFocusTarget(input);
```

## Key Benefits

1. **Modern Look & Feel**: Rounded borders, avatars, timestamps, and scroll indicators
2. **Better UX**: Cursor blinking, character counts, input history
3. **Accessibility**: Clear visual feedback and keyboard navigation
4. **Flexibility**: Extensive configuration options for customization
5. **Performance**: Efficient rendering with the existing diff-based screen buffer
6. **Consistency**: Follows the existing Peutui component model

## Compatibility

All new components:
- Implement the standard `Component` interface
- Work with existing layout systems (VerticalStackComponent, etc.)
- Support the existing input event system
- Are compatible with Java 17+
- Use only existing Peutui core dependencies
