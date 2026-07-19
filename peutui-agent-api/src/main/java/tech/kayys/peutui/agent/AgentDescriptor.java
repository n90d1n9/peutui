package tech.kayys.peutui.agent;

import java.util.List;

/**
 * Static metadata describing an {@link Agent}: identity and what it's good at.
 */
public record AgentDescriptor(String id, String displayName, String description, List<AgentCapability> capabilities) {
}
