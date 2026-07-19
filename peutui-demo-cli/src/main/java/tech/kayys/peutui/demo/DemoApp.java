package tech.kayys.peutui.demo;

import tech.kayys.peutui.agent.AgentContext;
import tech.kayys.peutui.agent.AgentEvent;
import tech.kayys.peutui.agent.AgentMessage;
import tech.kayys.peutui.agent.AgentOrchestrationStrategy;
import tech.kayys.peutui.core.autocomplete.AutocompleteItem;
import tech.kayys.peutui.core.autocomplete.AutocompleteProvider;
import tech.kayys.peutui.core.autocomplete.AutocompleteSuggestions;
import tech.kayys.peutui.core.autocomplete.CompletionEdit;
import tech.kayys.peutui.core.autocomplete.CompositeAutocompleteEngine;
import tech.kayys.peutui.project.ProjectRegistry;
import tech.kayys.peutui.provider.ProviderRegistry;
import tech.kayys.peutui.provider.ProviderSelectionStrategy;
import tech.kayys.peutui.session.Session;
import tech.kayys.peutui.session.SessionManager;
import tech.kayys.peutui.terminal.TerminalDriver;
import tech.kayys.peutui.widgets.App;
import tech.kayys.peutui.widgets.ChatHistoryComponent;
import tech.kayys.peutui.widgets.SpinnerComponent;
import tech.kayys.peutui.widgets.StatusBarComponent;
import tech.kayys.peutui.widgets.TextInputComponent;
import tech.kayys.peutui.widgets.VerticalStackComponent;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runnable reference wiring: builds a chat REPL component tree
 * (history + spinner + status bar + text input) and drives it from a real
 * {@link TerminalDriver}, dispatching submitted messages through the
 * configured {@link AgentOrchestrationStrategy} and persisting the
 * conversation via the configured {@link SessionManager}.
 *
 * <p>
 * Improvements over the baseline demo:
 * <ul>
 * <li>A {@link SpinnerComponent} is embedded between the history and the
 * status bar; it animates while the agent is streaming and disappears
 * when the turn completes.</li>
 * <li>A welcome message is displayed on first run to orient the user.</li>
 * <li>The status bar shows the active provider id (from
 * {@link ProviderSelectionStrategy}) so the user knows which backend
 * is answering.</li>
 * <li>The {@link TextInputComponent} is wired to a slash-command
 * {@link CompositeAutocompleteEngine} that suggests {@code /help},
 * {@code /clear}, and {@code /reset} on Tab after a '/'.</li>
 * </ul>
 */
@QuarkusMain
public final class DemoApp implements QuarkusApplication {

    // -- Scheduled executor used to tick the spinner animation ----------------
    private static final ScheduledExecutorService SPINNER_TICKER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "peutui-demo-spinner");
        t.setDaemon(true);
        return t;
    });

    // -- CDI injected collaborators -------------------------------------------
    @Inject
    TerminalDriver terminalDriver;

    @Inject
    AgentOrchestrationStrategy orchestrationStrategy;

    @Inject
    SessionManager sessionManager;

    @Inject
    ProjectRegistry projectRegistry;

    @Inject
    ProviderRegistry providerRegistry;

    @Inject
    ProviderSelectionStrategy providerSelectionStrategy;

    public static void main(String[] args) {
        Quarkus.run(DemoApp.class, args);
    }

    @Override
    public int run(String... args) throws Exception {

        // -- Build the component tree -----------------------------------------
        ChatHistoryComponent history = new ChatHistoryComponent();
        history.setShowAgentTags(true);

        SpinnerComponent spinner = new SpinnerComponent();

        StatusBarComponent statusBar = new StatusBarComponent();

        TextInputComponent input = new TextInputComponent();
        input.setPlaceholder("Type a message and press Enter  ·  Tab for /commands  ·  Ctrl+C to quit");

        // Stack: flexible history | 1-row spinner (hidden when idle) | 1-row status |
        // 3-row input
        VerticalStackComponent root = new VerticalStackComponent()
                .addFlexible(history)
                .addFixed(spinner, 1)
                .addFixed(statusBar, 1)
                .addFixed(input, 3);

        App app = new App(terminalDriver, root);
        app.setFocused(root);
        root.setFocusTarget(input);

        // -- Populate status bar ----------------------------------------------
        String projectName = projectRegistry.active().name();
        String sessionId = sessionManager.current().id().substring(0, 8);
        String providerId = resolveProviderId();

        statusBar.setLeft("project", projectName);
        statusBar.setLeft("session", "session:" + sessionId);
        statusBar.setLeft("provider", "provider:" + providerId);
        statusBar.setRight("hint", "Ctrl+C to quit");

        // -- Attach slash-command autocomplete to the input -------------------
        input.setAutocomplete(buildSlashCommandAutocomplete());

        // -- Show welcome message (uses assistant role with agentId="system") --
        history.append(AgentMessage.assistant("system",
                "Welcome to Peutui demo! "
                        + "Provider: " + providerId + "  ·  Session: " + sessionId + "\n"
                        + "Type /help for available commands, or just start chatting."));

        // -- Spinner tick handle (started/stopped per turn) -------------------
        AtomicBoolean spinnerRunning = new AtomicBoolean(false);
        ScheduledFuture<?>[] tickHandle = { null };

        // -- Wire submit handler ---------------------------------------------
        input.setOnSubmit(text -> {
            if (text.isBlank()) {
                return;
            }

            AgentMessage userMessage = AgentMessage.user(text);
            history.append(userMessage);
            Session updated = sessionManager.persist(
                    sessionManager.current().withMessage(userMessage));
            app.requestRender();

            // Start spinner
            spinner.start("thinking…");
            spinnerRunning.set(true);
            tickHandle[0] = SPINNER_TICKER.scheduleAtFixedRate(() -> {
                spinner.tick();
                app.requestRender();
            }, 80, 80, TimeUnit.MILLISECONDS);
            statusBar.setRight("status", "streaming");
            app.requestRender();

            List<AgentMessage> conversationHistory = new ArrayList<>(updated.messages());
            AgentContext context = new AgentContext(
                    updated.id(), projectRegistry.active().id(), conversationHistory);

            StringBuilder streamed = new StringBuilder();
            history.append(AgentMessage.assistant("assistant", ""));

            orchestrationStrategy.dispatch(context).subscribe(new Flow.Subscriber<>() {
                private Flow.Subscription subscription;

                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(AgentEvent item) {
                    if (item instanceof AgentEvent.TextDelta delta) {
                        streamed.append(delta.text());
                        history.replaceLast(
                                AgentMessage.assistant(delta.agentId(), streamed.toString()));
                        app.requestRender();
                    } else if (item instanceof AgentEvent.TurnCompleted completed) {
                        stopSpinner(spinner, spinnerRunning, tickHandle, statusBar, app);
                        history.replaceLast(completed.finalMessage());
                        sessionManager.persist(
                                sessionManager.current().withMessage(completed.finalMessage()));
                        app.requestRender();
                    } else if (item instanceof AgentEvent.TurnFailed failed) {
                        stopSpinner(spinner, spinnerRunning, tickHandle, statusBar, app);
                        history.replaceLast(
                                AgentMessage.assistant("system", "Error: " + failed.reason()));
                        app.requestRender();
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    stopSpinner(spinner, spinnerRunning, tickHandle, statusBar, app);
                    history.replaceLast(
                            AgentMessage.assistant("system", "Error: " + throwable.getMessage()));
                    app.requestRender();
                }

                @Override
                public void onComplete() {
                    stopSpinner(spinner, spinnerRunning, tickHandle, statusBar, app);
                    app.requestRender();
                }
            });
        });

        app.start();
        app.awaitStop();
        return 0;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Stops the spinner animation and clears the "streaming" status hint. */
    private static void stopSpinner(
            SpinnerComponent spinner,
            AtomicBoolean running,
            ScheduledFuture<?>[] tickHandle,
            StatusBarComponent statusBar,
            App app) {
        if (running.compareAndSet(true, false)) {
            if (tickHandle[0] != null) {
                tickHandle[0].cancel(false);
                tickHandle[0] = null;
            }
            spinner.stop();
            statusBar.remove("status");
            app.requestRender();
        }
    }

    /**
     * Resolves the display id of the currently selected provider without
     * hard-coding anything: ask the selection strategy which provider it
     * would pick for an empty request, and return its id(). Falls back to
     * "mock" if resolution fails (e.g. empty registry in tests).
     */
    private String resolveProviderId() {
        try {
            var dummyRequest = new tech.kayys.peutui.provider.ProviderRequest("", List.of());
            return providerSelectionStrategy.select(providerRegistry, dummyRequest).id();
        } catch (Exception e) {
            return "mock";
        }
    }

    /**
     * Builds a {@link CompositeAutocompleteEngine} containing a single
     * slash-command provider for the three built-in demo commands.
     */
    private static CompositeAutocompleteEngine buildSlashCommandAutocomplete() {
        List<SlashEntry> commands = List.of(
                new SlashEntry("/help", "Show available slash commands"),
                new SlashEntry("/clear", "Clear the chat history"),
                new SlashEntry("/reset", "Start a fresh session"));

        AutocompleteProvider slashProvider = new AutocompleteProvider() {

            @Override
            public List<Character> triggerCharacters() {
                return List.of('/');
            }

            @Override
            public CompletableFuture<AutocompleteSuggestions> getSuggestions(
                    List<String> lines, int cursorLine, int cursorCol, boolean force) {
                String line = cursorLine < lines.size() ? lines.get(cursorLine) : "";
                String prefix = line.substring(0, cursorCol);

                // Only activate when the line starts with '/'
                if (!prefix.startsWith("/")) {
                    return CompletableFuture.completedFuture(new AutocompleteSuggestions(List.of(), ""));
                }

                List<AutocompleteItem> matches = commands.stream()
                        .filter(c -> c.name().startsWith(prefix))
                        .map(c -> new AutocompleteItem(c.name(), c.name(), c.description()))
                        .toList();

                return CompletableFuture.completedFuture(new AutocompleteSuggestions(matches, prefix));
            }

            @Override
            public CompletionEdit applyCompletion(
                    List<String> lines, int cursorLine, int cursorCol,
                    AutocompleteItem item, String prefix) {
                // Replace the slash prefix on the current line with the chosen command
                String line = cursorLine < lines.size() ? lines.get(cursorLine) : "";
                String before = line.substring(0, cursorCol - prefix.length());
                String after = line.substring(cursorCol);
                String newLine = before + item.value() + after;
                int newCol = before.length() + item.value().length();

                List<String> newLines = new ArrayList<>(lines);
                if (cursorLine < newLines.size()) {
                    newLines.set(cursorLine, newLine);
                } else {
                    newLines.add(newLine);
                }
                return new CompletionEdit(newLines, cursorLine, newCol);
            }
        };

        return new CompositeAutocompleteEngine().register(slashProvider);
    }

    /** Tiny value type for a slash command entry used only inside this class. */
    private record SlashEntry(String name, String description) {
    }
}
