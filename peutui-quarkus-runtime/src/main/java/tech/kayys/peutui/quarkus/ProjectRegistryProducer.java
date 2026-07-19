package tech.kayys.peutui.quarkus;

import tech.kayys.peutui.project.MultiProjectRegistry;
import tech.kayys.peutui.project.Project;
import tech.kayys.peutui.project.ProjectRegistry;
import tech.kayys.peutui.project.SingleProjectRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.nio.file.Path;

/**
 * Produces a {@link ProjectRegistry} strategy (single-fixed vs
 * multi-switchable) driven by {@code peutui.project.mode}.
 */
@ApplicationScoped
public final class ProjectRegistryProducer {

    @Inject
    PeutuiConfig config;

    @Produces
    @ApplicationScoped
    public ProjectRegistry projectRegistry() {
        Path root = Path.of(config.project().rootPath()).toAbsolutePath().normalize();
        Project initial = Project.of(root.getFileName() != null ? root.getFileName().toString() : "default",
                root.toString(), root);
        return switch (config.project().mode()) {
            case SINGLE -> new SingleProjectRegistry(initial);
            case MULTI -> new MultiProjectRegistry().add(initial);
        };
    }
}
