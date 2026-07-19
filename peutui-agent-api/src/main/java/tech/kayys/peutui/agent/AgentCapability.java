package tech.kayys.peutui.agent;

/**
 * A declared capability tag an {@link Agent} advertises, used for routing in
 * multi-agent setups.
 */
public record AgentCapability(String name) {
    public static final AgentCapability CODE = new AgentCapability("code");
    public static final AgentCapability RESEARCH = new AgentCapability("research");
    public static final AgentCapability PLANNING = new AgentCapability("planning");
    public static final AgentCapability GENERAL = new AgentCapability("general");
}
