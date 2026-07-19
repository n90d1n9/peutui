package tech.kayys.peutui.quarkus;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

import java.util.List;
import java.util.Optional;

/**
 * Single configuration root (prefix {@code peutui}) that drives every
 * strategy choice this library offers. Example {@code application.properties}:
 *
 * <pre>{@code
 * peutui.project.mode=single
 * peutui.session.mode=multi
 * peutui.storage.mode=local-file
 * peutui.storage.local-file.root-path=.peutui/data
 * peutui.agent.mode=multi
 * peutui.agent.default-agent-id=planner
 * peutui.provider.selection=failover
 * peutui.provider.failover-order=anthropic,local-gollek
 * }</pre>
 */
@ConfigMapping(prefix = "peutui")
public interface PeutuiConfig {

    ProjectConfig project();

    SessionConfig session();

    StorageConfig storage();

    StorageConfig settingsStorage();

    AgentConfig agent();

    ProviderConfig provider();

    interface ProjectConfig {
        @WithDefault("single")
        Mode mode();

        @WithDefault(".")
        String rootPath();

        enum Mode {
            SINGLE, MULTI
        }
    }

    interface SessionConfig {
        @WithDefault("single")
        Mode mode();

        enum Mode {
            SINGLE, MULTI
        }
    }

    interface StorageConfig {
        @WithDefault("local-file")
        Mode mode();

        LocalFileConfig localFile();

        DatabaseConfig database();

        CloudConfig cloud();

        enum Mode {
            @WithName("local-file")
            LOCAL_FILE,
            @WithName("local-database")
            LOCAL_DATABASE,
            @WithName("cloud")
            CLOUD
        }

        interface LocalFileConfig {
            @WithDefault(".peutui/data")
            String rootPath();
        }

        interface DatabaseConfig {
            @WithDefault("peutui")
            String label();
        }

        interface CloudConfig {
            Optional<String> baseUrl();

            Optional<String> authorizationHeader();
        }
    }

    interface AgentConfig {
        @WithDefault("single")
        Mode mode();

        Optional<String> defaultAgentId();

        enum Mode {
            SINGLE, MULTI
        }
    }

    interface ProviderConfig {
        @WithDefault("single")
        Selection selection();

        Optional<String> defaultProviderId();

        Optional<List<String>> failoverOrder();

        enum Selection {
            @WithName("single")
            SINGLE,
            @WithName("round-robin")
            ROUND_ROBIN,
            @WithName("failover")
            FAILOVER
        }
    }
}
