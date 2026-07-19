package tech.kayys.peutui.project;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ProjectRegistry} strategy supporting any number of registered,
 * switchable projects.
 */
public final class MultiProjectRegistry implements ProjectRegistry {

    private final Map<String, Project> projects = new LinkedHashMap<>();
    private String activeId;

    public MultiProjectRegistry add(Project project) {
        projects.put(project.id(), project);
        if (activeId == null) {
            activeId = project.id();
        }
        return this;
    }

    public void remove(String projectId) {
        projects.remove(projectId);
        if (projectId.equals(activeId)) {
            activeId = projects.isEmpty() ? null : projects.keySet().iterator().next();
        }
    }

    @Override
    public Project active() {
        if (activeId == null) {
            throw new IllegalStateException("No active project - register at least one project first");
        }
        return projects.get(activeId);
    }

    @Override
    public List<Project> all() {
        return List.copyOf(projects.values());
    }

    @Override
    public Optional<Project> find(String projectId) {
        return Optional.ofNullable(projects.get(projectId));
    }

    @Override
    public void setActive(String projectId) {
        if (!projects.containsKey(projectId)) {
            throw new IllegalArgumentException("Unknown project: " + projectId);
        }
        this.activeId = projectId;
    }
}
