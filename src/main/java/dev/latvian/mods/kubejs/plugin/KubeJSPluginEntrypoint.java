package dev.latvian.mods.kubejs.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface KubeJSPluginEntrypoint {
	/// Determines whether this plugin should be loaded on client-side only.
	boolean clientOnly() default false;

	/// Specifies a list of mod ids which should be present for this plugin to load.
	String[] requiredMods() default {};
}
