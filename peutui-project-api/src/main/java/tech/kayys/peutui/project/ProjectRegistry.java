package tech.kayys.peutui.project;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface over how many projects/workspaces the host application
 * manages at once. {@link SingleProjectRegistry} pins one fixed project;
 * {@link MultiProjectRegistry} supports registering, switching between, and
 * removing many. Session and settings layers depend only on this interface
 * and the currently {@link #active()} project.
 */
public interface ProjectRegistry {

    Project active();

    List<Project> all();

    Optional<Project> find(String projectId);

    /**
     * Switches the active project. Implementations that don't support multiple
     * projects should reject any id other than the current one.
     */
    void setActive(String projectId);

    default boolean isMultiProject() {
        return all().size() > 1;
    }
}
