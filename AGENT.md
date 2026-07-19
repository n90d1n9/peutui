# Peutui Agent Integration Guide

A comprehensive guide for integrating Peutui with AI agents, building agentic TUI applications, and extending the framework for multi-agent orchestration.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Agent Architecture](#agent-architecture)
3. [Single Agent Setup](#single-agent-setup)
4. [Multi-Agent Orchestration](#multi-agent-orchestration)
5. [Model Providers](#model-providers)
6. [Streaming Integration](#streaming-integration)
7. [Session Management](#session-management)
8. [Tool Integration](#tool-integration)
9. [Advanced Patterns](#advanced-patterns)
10. [Best Practices](#best-practices)

---

## Quick Start

### Minimal Agent Application

```java
import tech.kayys.peutui.agent.api.Agent;
import tech.kayys.peutui.agent.api.AgentEvent;
import tech.kayys.peutui.widgets.ChatBoxComponent;
import tech.kayys.peutui.widgets.ChatInputComponent;
import tech.kayys.peutui.widgets.HeaderComponent;
import tech.kayys.peutui.core.app.App;
import tech.kayys.peutui.core.component.Component;
import tech.kayys.peutui.core.layout.VerticalStackComponent;

public class SimpleAgentApp extends App {
    
    private final Agent agent;
    private ChatBoxComponent chatBox;
    private ChatInputComponent input;
    
    public SimpleAgentApp(Agent agent) {
        this.agent = agent;
    }
    
    @Override
    protected Component buildUI() {
        // Header with tabs
        HeaderComponent header = new HeaderComponent("AI Assistant", "Peutui Agent");
        header.addTab("Chat", () -> switchToChat());
        header.addTab("Settings", () -> showSettings());
        
        // Chat components
        chatBox = new ChatBoxComponent();
        chatBox.setAutoScroll(true);
        chatBox.setShowTimestamps(true);
        
        input = new ChatInputComponent();
        input.setMaxLines(5);
        input.setOnSubmit(this::sendMessage);
        
        // Layout
        VerticalStackComponent layout = new VerticalStackComponent();
        layout.addComponent(header);
        layout.addComponent(chatBox);
        layout.addComponent(input);
        
        return layout;
    }
    
    private void sendMessage(String text) {
        // Add user message to chat
        chatBox.append(new AgentMessage(MessageRole.USER, text));
        
        // Stream response from agent
        agent.send(text).subscribe(token -> {
            if (token.isComplete()) {
                chatBox.append(new AgentMessage(MessageRole.ASSISTANT, token.content()));
            } else {
                // Handle streaming update if needed
            }
        });
    }
}
```

### Running with Quarkus

```properties
# application.properties
peutui.agent.mode=single
peutui.agent.default-agent-id=assistant
peutui.provider.selection=single
peutui.provider.default-provider-id=anthropic

# Provider credentials
anthropic.api.key=${ANTHROPIC_API_KEY}
```

```bash
mvn quarkus:dev
```

---

## Agent Architecture

### Core Concepts

**Agent**: The primary interface for AI interactions
```java
public sealed interface Agent {
    Flow.Publisher<AgentEvent> send(String message);
    String id();
    String name();
    String description();
}
```

**AgentEvent**: Streaming events from agent
```java
public sealed interface AgentEvent {
    // Token, Complete, Error, ToolCall, etc.
}
```

**ModelProvider**: Abstraction for LLM providers
```java
public interface ModelProvider {
    Flow.Publisher<StreamingToken> stream(ChatRequest request);
    String id();
    ProviderCapabilities capabilities();
}
```

### Strategy Pattern

Peutui uses strategy interfaces for all varying axes:

| Axis | Strategy Interface | Implementations |
|------|-------------------|-----------------|
| Agent Count | `AgentOrchestrationStrategy` | Single, Multi |
| Provider Selection | `ProviderSelectionStrategy` | Single, Round-Robin, Failover |
| Project Mode | `ProjectRegistry` | Single, Multi |
| Session Mode | `SessionManager` | Single, Multi |
| Storage Backend | `StorageBackend` | Local File, Local DB, Cloud |

---

## Single Agent Setup

### Creating a Custom Agent

```java
import tech.kayys.peutui.agent.api.Agent;
import tech.kayys.peutui.agent.api.AgentEvent;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.concurrent.Flow;

@ApplicationScoped
public class MyCustomAgent implements Agent {
    
    private final ModelProvider provider;
    
    @Inject
    public MyCustomAgent(ModelProvider provider) {
        this.provider = provider;
    }
    
    @Override
    public String id() {
        return "my-assistant";
    }
    
    @Override
    public String name() {
        return "My Assistant";
    }
    
    @Override
    public String description() {
        return "A helpful AI assistant specialized for code review";
    }
    
    @Override
    public Flow.Publisher<AgentEvent> send(String message) {
        ChatRequest request = ChatRequest.builder()
            .message(message)
            .temperature(0.7)
            .maxTokens(2048)
            .build();
            
        return provider.stream(request)
            .map(token -> AgentEvent.token(token.content()))
            .onErrorResume(error -> Flow.Publisher.of(AgentEvent.error(error)));
    }
}
```

### Configuration

```properties
# Single agent mode
peutui.agent.mode=single
peutui.agent.default-agent-id=my-assistant

# Provider selection
peutui.provider.selection=single
peutui.provider.default-provider-id=anthropic
```

---

## Multi-Agent Orchestration

### Capability-Based Routing

```java
import tech.kayys.peutui.agent.api.Agent;
import tech.kayys.peutui.agent.api.AgentOrchestrationStrategy;
import java.util.List;
import java.util.Map;

public class CapabilityBasedRouter implements AgentOrchestrationStrategy {
    
    private final Map<String, List<String>> agentCapabilities;
    private final Map<String, Agent> agents;
    
    public CapabilityBasedRouter(List<Agent> agents) {
        this.agents = agents.stream()
            .collect(Collectors.toMap(Agent::id, a -> a));
            
        this.agentCapabilities = Map.of(
            "coder", List.of("code-generation", "debugging", "refactoring"),
            "writer", List.of("content-writing", "editing", "summarization"),
            "analyst", List.of("data-analysis", "visualization", "reporting")
        );
    }
    
    @Override
    public Agent route(String intent) {
        // Analyze intent and route to appropriate agent
        if (intent.contains("code") || intent.contains("bug")) {
            return agents.get("coder");
        } else if (intent.contains("write") || intent.contains("edit")) {
            return agents.get("writer");
        }
        return agents.get("analyst");
    }
    
    @Override
    public List<Agent> getAllAgents() {
        return List.copyOf(agents.values());
    }
}
```

### Round-Robin Distribution

```java
public class RoundRobinRouter implements AgentOrchestrationStrategy {
    
    private final List<Agent> agents;
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public RoundRobinRouter(List<Agent> agents) {
        this.agents = agents;
    }
    
    @Override
    public Agent route(String intent) {
        int index = Math.abs(counter.getAndIncrement() % agents.size());
        return agents.get(index);
    }
    
    @Override
    public List<Agent> getAllAgents() {
        return agents;
    }
}
```

### Configuration

```properties
# Multi-agent mode
peutui.agent.mode=multi
peutui.agent.router=capability-based  # or round-robin

# Agent definitions (auto-discovered via CDI beans)
```

---

## Model Providers

### Registering a Provider

```java
import tech.kayys.peutui.provider.api.ModelProvider;
import tech.kayys.peutui.provider.api.StreamingToken;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AnthropicProvider implements ModelProvider {
    
    @ConfigProperty(name = "anthropic.api.key")
    String apiKey;
    
    @Override
    public String id() {
        return "anthropic";
    }
    
    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
            true,  // streaming support
            100000, // max context window
            Set.of("text", "code", "vision")
        );
    }
    
    @Override
    public Flow.Publisher<StreamingToken> stream(ChatRequest request) {
        // Implement streaming call to Anthropic API
        return Flux.create(sink -> {
            // HTTP streaming logic here
            client.streamCompletion(request, token -> {
                sink.next(new StreamingToken(token, false));
            }, () -> {
                sink.next(new StreamingToken("", true));
                sink.complete();
            });
        });
    }
}
```

### Provider Selection Strategies

**Single Provider**
```properties
peutui.provider.selection=single
peutui.provider.default-provider-id=anthropic
```

**Round-Robin**
```properties
peutui.provider.selection=round-robin
peutui.provider.providers=anthropic,openai,local
```

**Failover**
```properties
peutui.provider.selection=failover
peutui.provider.failover-order=anthropic,openai,local-gollek
```

---

## Streaming Integration

### Handling Streaming Tokens

```java
import tech.kayys.peutui.core.streaming.StreamingToken;
import tech.kayys.peutui.core.streaming.StreamingRenderer;
import tech.kayys.peutui.widgets.MarkdownStreamComponent;

public class StreamingChatHandler {
    
    private final MarkdownStreamComponent streamView;
    private final StringBuilder buffer = new StringBuilder();
    
    public StreamingChatHandler(MarkdownStreamComponent streamView) {
        this.streamView = streamView;
    }
    
    public void handleStream(Flow.Publisher<StreamingToken> publisher) {
        publisher.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
                buffer.setLength(0);
                streamView.reset();
            }
            
            @Override
            public void onNext(StreamingToken token) {
                buffer.append(token.content());
                streamView.feed(token);
                
                if (token.isComplete()) {
                    onComplete();
                }
            }
            
            @Override
            public void onError(Throwable throwable) {
                streamView.showError("Error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                streamView.complete();
            }
        });
    }
}
```

### Custom Streaming Renderer

```java
import tech.kayys.peutui.core.streaming.StreamingRenderer;
import tech.kayys.peutui.core.streaming.StreamingToken;

public class CodeStreamingRenderer extends StreamingRenderer<StreamingToken> {
    
    private final String language;
    private boolean inCodeBlock = false;
    
    public CodeStreamingRenderer(String language) {
        this.language = language;
    }
    
    @Override
    public void onData(StreamingToken token) {
        String content = token.content();
        
        // Detect code block start/end
        if (content.startsWith("```")) {
            inCodeBlock = !inCodeBlock;
        }
        
        buffer.append(content);
        
        // Apply syntax highlighting incrementally
        if (inCodeBlock) {
            renderWithHighlighting();
        } else {
            triggerRender();
        }
    }
    
    @Override
    protected String formatContent() {
        if (inCodeBlock) {
            return SyntaxHighlighter.highlight(buffer.toString(), language);
        }
        return buffer.toString();
    }
}
```

---

## Session Management

### Session Lifecycle

```java
import tech.kayys.peutui.session.api.Session;
import tech.kayys.peutui.session.api.SessionManager;
import tech.kayys.peutui.session.api.Message;

public class SessionHandler {
    
    private final SessionManager sessionManager;
    
    @Inject
    public SessionHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    
    public void createNewSession(String title) {
        Session session = sessionManager.createSession(title);
        sessionManager.activateSession(session.id());
    }
    
    public void addMessage(MessageRole role, String content) {
        Session current = sessionManager.getActiveSession();
        current.addMessage(new Message(role, content));
        sessionManager.saveSession(current);
    }
    
    public List<Message> getHistory() {
        return sessionManager.getActiveSession().getMessages();
    }
}
```

### Session Storage Modes

**In-Memory (Development)**
```properties
peutui.session.mode=single
peutui.storage.mode=memory
```

**Local File**
```properties
peutui.session.mode=multi
peutui.storage.mode=local-file
peutui.storage.local-file.root-path=.peutui/data
```

**Database**
```properties
peutui.storage.mode=local-database
peutui.storage.database.url=jdbc:sqlite:peutui.db
```

---

## Tool Integration

### Defining Tools

```java
import tech.kayys.peutui.agent.api.Tool;
import tech.kayys.peutui.agent.api.ToolResult;

public class FileSystemTool implements Tool {
    
    @Override
    public String name() {
        return "read_file";
    }
    
    @Override
    public String description() {
        return "Read contents of a file";
    }
    
    @Override
    public ToolSchema schema() {
        return ToolSchema.builder()
            .addParameter("path", "string", "File path to read", true)
            .build();
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String path = (String) arguments.get("path");
        try {
            String content = Files.readString(Path.of(path));
            return ToolResult.success(content);
        } catch (IOException e) {
            return ToolResult.error("Failed to read file: " + e.getMessage());
        }
    }
}
```

### Tool Call Visualization

```java
import tech.kayys.peutui.widgets.PanelComponent;
import tech.kayys.peutui.core.text.AnsiCodes;

public class ToolCallInspector {
    
    public Component renderToolCall(ToolCall call) {
        PanelComponent panel = new PanelComponent("🔧 " + call.toolName());
        panel.setBorderStyle(PanelComponent.BorderStyle.ROUNDED);
        
        StringBuilder content = new StringBuilder();
        content.append(AnsiCodes.BOLD).append("Arguments:").append(AnsiCodes.RESET).append("\n");
        
        call.arguments().forEach((key, value) -> {
            content.append("  ")
                   .append(AnsiCodes.CYAN).append(key).append(AnsiCodes.RESET)
                   .append(": ")
                   .append(value)
                   .append("\n");
        });
        
        if (call.result() != null) {
            content.append("\n")
                   .append(AnsiCodes.BOLD).append("Result:").append(AnsiCodes.RESET)
                   .append("\n")
                   .append(call.result().output());
        }
        
        // Content rendering logic here
        return panel;
    }
}
```

---

## Advanced Patterns

### Chain-of-Thought Visualization

```java
public class ThinkingStateComponent implements Component {
    
    private final List<String> thoughtSteps = new ArrayList<>();
    private boolean isThinking = false;
    private long startTime;
    
    @Override
    public void render(RenderContext context) {
        if (!isThinking) return;
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        context.write(AnsiCodes.YELLOW + "⚡ Thinking..." + AnsiCodes.RESET);
        context.write(" (" + elapsed + "ms)\n");
        
        // Render thought steps
        for (int i = 0; i < thoughtSteps.size(); i++) {
            context.write("  " + (i + 1) + ". " + thoughtSteps.get(i) + "\n");
        }
    }
    
    public void addThought(String thought) {
        thoughtSteps.add(thought);
        triggerRender();
    }
    
    public void startThinking() {
        isThinking = true;
        startTime = System.currentTimeMillis();
        thoughtSteps.clear();
    }
    
    public void stopThinking() {
        isThinking = false;
    }
}
```

### Context Window Visualizer

```java
public class ContextWindowIndicator implements Component {
    
    private int currentTokens = 0;
    private int maxTokens = 100000;
    
    @Override
    public void render(RenderContext context) {
        double usage = (double) currentTokens / maxTokens;
        String color = usage > 0.9 ? AnsiCodes.RED : 
                       usage > 0.7 ? AnsiCodes.YELLOW : AnsiCodes.GREEN;
        
        int barWidth = 20;
        int filled = (int) (usage * barWidth);
        
        context.write(color + "[");
        context.write("=".repeat(filled));
        context.write(" ".repeat(barWidth - filled));
        context.write("] " + currentTokens + "/" + maxTokens + AnsiCodes.RESET);
    }
    
    public void updateTokens(int count) {
        this.currentTokens = count;
        triggerRender();
    }
}
```

### Slash Command Handler

```java
import tech.kayys.peutui.widgets.CommandPaletteComponent;
import tech.kayys.peutui.widgets.CommandItem;

public class SlashCommandHandler {
    
    private final CommandPaletteComponent palette;
    
    public SlashCommandHandler() {
        this.palette = new CommandPaletteComponent();
        registerCommands();
    }
    
    private void registerCommands() {
        palette.registerAll(List.of(
            new CommandItem("/new", "New Chat", "Start new conversation", "Ctrl+N"),
            new CommandItem("/clear", "Clear", "Clear chat history"),
            new CommandItem("/model", "Change Model", "Select AI model"),
            new CommandItem("/temp", "Temperature", "Set temperature (0-1)"),
            new CommandItem("/tokens", "Token Limit", "Set max tokens"),
            new CommandItem("/export", "Export", "Export conversation"),
            new CommandItem("/help", "Help", "Show available commands")
        ));
        
        palette.setOnExecute(cmd -> handleCommand(cmd.id()));
    }
    
    private void handleCommand(String command) {
        switch (command) {
            case "/new" -> createNewChat();
            case "/clear" -> clearHistory();
            case "/model" -> showModelSelector();
            case "/temp" -> showTemperatureSlider();
            // ... handle other commands
        }
    }
}
```

---

## Best Practices

### 1. Separation of Concerns

```
✅ DO: Keep UI logic separate from agent logic
❌ DON'T: Mix rendering code with API calls

// Good
public class ChatViewModel {
    private final Agent agent;
    private final ChatBoxComponent view;
    
    public void sendMessage(String text) {
        view.appendUserMessage(text);
        agent.send(text).subscribe(view::appendAssistantMessage);
    }
}
```

### 2. Error Handling

```java
// Always handle streaming errors gracefully
agent.send(message).subscribe(
    token -> handleToken(token),
    error -> showErrorNotification(error),
    () -> onStreamComplete()
);
```

### 3. Resource Management

```java
// Close sessions properly
@Override
public void onStop() {
    sessionManager.saveAllSessions();
    terminal.restoreScreen();
}
```

### 4. Performance Optimization

```java
// Use diffed rendering
@Override
public void render(RenderContext context) {
    // Only render changed portions
    if (dirtyFlag) {
        renderContent(context);
        dirtyFlag = false;
    }
}
```

### 5. User Experience

- Provide visual feedback during streaming (spinners, progress indicators)
- Show typing indicators while waiting for response
- Implement keyboard shortcuts for common actions
- Support mouse scrolling where available
- Display clear error messages with recovery options

### 6. Testing

```java
@Test
public void testAgentStreaming() {
    Agent agent = new MockAgent();
    TestSubscriber<AgentEvent> subscriber = new TestSubscriber<>();
    
    agent.send("test").subscribe(subscriber);
    
    subscriber.assertValueCount(5);
    subscriber.assertComplete();
}
```

---

## Troubleshooting

### Common Issues

**Issue**: Streaming stops mid-response  
**Solution**: Check token buffer limits and increase if needed

**Issue**: UI freezes during API calls  
**Solution**: Ensure all API calls are async and non-blocking

**Issue**: Terminal artifacts after resize  
**Solution**: Subscribe to SIGWINCH and re-render on resize events

**Issue**: Colors not displaying correctly  
**Solution**: Verify terminal supports ANSI colors and check TERM variable

---

## Additional Resources

- [MODERN_TUI_FEATURES.md](./MODERN_TUI_FEATURES.md) - Complete TUI component guide
- [README.md](./README.md) - Framework overview and module map
- [peutui-widgets/src](./peutui-widgets/src) - Component implementations
- [peutui-agent-api/src](./peutui-agent-api/src) - Agent API source code

---

## Contributing

We welcome contributions for:
- New agent orchestration strategies
- Additional model provider implementations
- Enhanced tool integrations
- UI component improvements
- Documentation enhancements

See the main README for contribution guidelines.
