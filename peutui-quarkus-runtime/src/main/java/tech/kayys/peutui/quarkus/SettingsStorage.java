package tech.kayys.peutui.quarkus;

import jakarta.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CDI qualifier for the {@code StorageBackend} instance configured under
 * {@code peutui.settings-storage.*}, used by settings persistence.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE })
public @interface SettingsStorage {
}
