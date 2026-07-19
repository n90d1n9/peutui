package tech.kayys.peutui.demo;

import tech.kayys.peutui.provider.ModelProvider;
import tech.kayys.peutui.provider.ProviderEvent;
import tech.kayys.peutui.provider.ProviderRequest;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A canned, dependency-free {@link ModelProvider} for the demo: instead of
 * calling out to a real model, it streams back a deterministic response
 * word-by-word so the render loop can demonstrate incremental text
 * rendering without needing any API key configured.
 *
 * <p>
 * Responses are varied based on the conversation length and the content
 * of the user's message, so the demo feels more alive than a single
 * hard-coded echo. It also recognises the built-in slash commands and
 * returns structured help text for them.
 */
@ApplicationScoped
public final class MockModelProvider implements ModelProvider {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "peutui-demo-provider");
        t.setDaemon(true);
        return t;
    });

    /**
     * Per-provider turn counter — lets responses become progressively more
     * informative.
     */
    private final AtomicInteger turnCount = new AtomicInteger(0);

    @Override
    public String id() {
        return "mock";
    }

    @Override
    public String displayName() {
        return "Mock Streaming Provider";
    }

    @Override
    public Flow.Publisher<ProviderEvent> streamChat(ProviderRequest request) {
        SubmissionPublisher<ProviderEvent> publisher = new SubmissionPublisher<>();
        String lastUserMessage = request.messages().isEmpty()
                ? ""
                : request.messages().get(request.messages().size() - 1).content();
        int historySize = request.messages().size();
        int turn = turnCount.incrementAndGet();

        List<String> words = List.of(canned(lastUserMessage, historySize, turn).split(" "));
        int[] index = { 0 };
        java.util.concurrent.ScheduledFuture<?>[] futureHolder = new java.util.concurrent.ScheduledFuture<?>[1];
        futureHolder[0] = SCHEDULER.scheduleAtFixedRate(() -> {
            if (index[0] >= words.size()) {
                publisher.submit(new ProviderEvent.Done("end_turn", countTokens(words), 0));
                publisher.close();
                futureHolder[0].cancel(false);
                return;
            }
            String word = words.get(index[0]);
            publisher.submit(new ProviderEvent.TextDelta((index[0] == 0 ? "" : " ") + word));
            index[0]++;
        }, 0, 45, TimeUnit.MILLISECONDS);
        return publisher;
    }

    // -------------------------------------------------------------------------
    // Response selection
    // -------------------------------------------------------------------------

    private String canned(String userMessage, int historySize, int turn) {
        if (userMessage.isBlank()) {
            return "I'm ready — type a message and press Enter to chat.";
        }

        String lower = userMessage.strip().toLowerCase(Locale.ROOT);

        // Slash-command responses
        if (lower.startsWith("/help")) {
            return """
                    Here are the available slash commands:
                    /help — show this help message.
                    /clear — clear the chat history.
                    /reset — start a fresh session.
                    You can also type any free-form message to chat with me.""";
        }
        if (lower.startsWith("/clear")) {
            return "Chat history cleared. You can start a new conversation now.";
        }
        if (lower.startsWith("/reset")) {
            return "Session reset. Everything starts fresh from here.";
        }

        // Keyword-aware responses
        if (lower.contains("hello") || lower.contains("hi") || lower.contains("hey")) {
            return greetingResponse(historySize);
        }
        if (lower.contains("peutui")) {
            return peutuiResponse();
        }
        if (lower.contains("spinner") || lower.contains("animation") || lower.contains("loading")) {
            return "The spinner you see while I'm \"thinking\" is a SpinnerComponent from peutui-widgets. "
                    + "It renders Braille animation frames and is driven by a periodic tick during streaming. "
                    + "Call spinner.start(label) to show it and spinner.stop() when the turn completes.";
        }
        if (lower.contains("how are you") || lower.contains("how r u")) {
            return "Doing great, thanks for asking! I'm a deterministic mock — "
                    + "always streaming the same-shaped words at 45 ms/word. "
                    + "To connect a real model, swap this bean with an Anthropic or OpenAI provider.";
        }
        if (lower.contains("autocomplete") || lower.contains("slash") || lower.contains("command")) {
            return "Slash-command autocomplete is wired in! Try typing '/' and pressing Tab — "
                    + "you should see /help, /clear and /reset as suggestions. "
                    + "Add more SlashCommand beans in DemoApp to extend the list.";
        }
        if (lower.contains("session") || lower.contains("history")) {
            return sessionResponse(historySize);
        }
        if (lower.contains("provider") || lower.contains("model") || lower.contains("llm")) {
            return providerResponse();
        }

        // Progression-aware fallback
        return fallback(userMessage.strip(), historySize, turn);
    }

    private String greetingResponse(int historySize) {
        if (historySize <= 2) {
            return "Hello! Welcome to the Peutui demo. I'm a mock streaming provider that responds "
                    + "word-by-word to show off incremental rendering. "
                    + "Type /help to see what slash commands are available, "
                    + "or just chat normally to explore the UI.";
        }
        return "Hey again! We're on message " + historySize + " now. Things are going well.";
    }

    private String peutuiResponse() {
        return "Peutui is a framework-agnostic terminal UI toolkit for JVM agent front-ends. "
                + "It handles ANSI rendering, a diffed ScreenBuffer, component layout, "
                + "text input with word-navigation and autocomplete, scrollable history, "
                + "status bars, and spinners — all without opinions about which AI model "
                + "or agent framework you use. Swap any axis (agent, provider, session, "
                + "storage) by changing a single application.properties value.";
    }

    private String sessionResponse(int historySize) {
        return "The current session has " + historySize + " message"
                + (historySize == 1 ? "" : "s") + " in its history. "
                + "Sessions are persisted by the configured SessionStore — "
                + "in this demo that's a local-file store under .peutui/data. "
                + "Type /reset to start a new session, or configure "
                + "peutui.session.mode=multi to keep several open at once.";
    }

    private String providerResponse() {
        return "This demo runs the 'mock' ModelProvider — id='mock', displayName='Mock Streaming Provider'. "
                + "It streams a deterministic canned reply word-by-word with no network access needed. "
                + "To use a real model, register an @ApplicationScoped ModelProvider bean "
                + "(e.g. pointing at OpenAI or Anthropic) and set "
                + "peutui.provider.default-provider-id to its id() value.";
    }

    private String fallback(String userMessage, int historySize, int turn) {
        String intro = switch (turn % 4) {
            case 0 -> "Interesting question!";
            case 1 -> "Good point.";
            case 2 -> "Thanks for that.";
            default -> "Got it.";
        };
        return intro + " You said: \"" + userMessage + "\". "
                + "This is turn " + turn + " and the conversation has " + historySize + " message"
                + (historySize == 1 ? "" : "s") + " so far. "
                + "I'm a canned mock — type /help for available commands, "
                + "or ask me about peutui, providers, sessions, the spinner, or autocomplete.";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Rough approximation: one token ≈ one word. */
    private int countTokens(List<String> words) {
        return words.size();
    }
}
