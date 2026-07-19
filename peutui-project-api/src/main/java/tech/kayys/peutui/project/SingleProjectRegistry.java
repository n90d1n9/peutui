package tech.kayys.peutui.project;

import java.util.List;
import java.util.Optional;

/**
 * {@link ProjectRegistry} strategy that pins the application to exactly one
 * fixed project for its whole lifetime.
 */
public final class SingleProjectRegistry implements ProjectRegistry {

    private final Project project;

    public SingleProjectRegistry(Project project) {
        this.project = project;
    }

    @Override
    public Project active() {
        return project;
    }

    @Override
    public List<Project> all() {
        return List.of(project);
    }

    @Override
    public Optional<Project> find(String projectId) {
        return project.id().equals(projectId) ? Optional.of(project) : Optional.empty();
    }

    @Override
    public void setActive(String projectId) {
        if (!project.id().equals(projectId)) {
            throw new UnsupportedOperationException(
                    "This application is configured for a single fixed project: " + project.id());
        }
    }
}
