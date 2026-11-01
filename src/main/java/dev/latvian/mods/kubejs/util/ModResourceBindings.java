package dev.latvian.mods.kubejs.util;

import dev.latvian.mods.kubejs.KubeJS;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.ScriptTypePredicate;
import net.neoforged.neoforgespi.locating.IModFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ModResourceBindings {

	private final Map<String, Collection<BindingProvider>> bindings = new HashMap<>();

	public void addBindings(BindingRegistry event) {
		for (var modBindings : bindings.entrySet()) {
			var modName = modBindings.getKey();
			var providers = modBindings.getValue();
			var addedBindings = new ArrayList<>();
			for (var provider : providers) {
				String name = provider.name();
				if (!provider.test(event.type())) {
					continue;
				}

				try {
					event.add(name, provider.generate());
					addedBindings.add(name);
				} catch (Exception e) {
					KubeJS.LOGGER.error("Error adding binding for script type {} from mod '{}': {}", event.type(), modName, name, e);
				}
			}
			KubeJS.LOGGER.info("Added bindings for script type {} from mod '{}': {}", event.type(), modName, addedBindings);
		}
	}

	public void readBindings(String modId, IModFile mod) throws IOException {
		var data = mod.getContents().readFile("kubejs.bindings.txt");
		if (data == null) {
			return;
		}
		@SuppressWarnings("NullableProblems") // there's no problem here idea you're smoking
		List<BindingProvider> providers = new String(data, StandardCharsets.UTF_8).lines()
			.map(s -> s.split("#", 2)[0].trim())
			.filter(line -> !line.isBlank())
			.map(line -> createProvider(modId, line))
			.filter(Objects::nonNull)
			.toList();
		bindings.put(modId, providers);
	}

	@Nullable
	private BindingProvider createProvider(String modId, String line) {
		// SERVER name class field/method/<init>?
		String[] split = line.split("\\s+");
		if (split.length < 3) {
			KubeJS.LOGGER.error("Invalid binding for '{}' in line: {}", modId, line);
			return null;
		}

		var scriptTypeFilter = typePredicateOf(split[0]);
		var name = split[1];
		var className = split[2];
		ClassBindingProvider classProvider = new ClassBindingProvider(name, scriptTypeFilter, className);
		if (split.length == 3) {
			return classProvider;
		}

		var methodFieldOrConstructor = split[3];
		if (methodFieldOrConstructor.equals("<init>")) {
			return new InstanceBindingProvider(classProvider);
		}

		return new InvokeBindingProvider(classProvider, methodFieldOrConstructor);
	}

	private ScriptTypePredicate typePredicateOf(String typeString) {
		var lower = typeString.toLowerCase(Locale.ROOT);
		return switch (lower) {
			case "*", "all" -> ScriptTypePredicate.ALL;
			case "common" -> ScriptTypePredicate.COMMON;
			case "startup_or_client" -> ScriptTypePredicate.STARTUP_OR_CLIENT;
			case "startup_or_server" -> ScriptTypePredicate.STARTUP_OR_SERVER;
			default -> {
				for (var type : ScriptType.VALUES) {
					if (type.name.equals(lower)) {
						yield type;
					}
				}
				throw new IllegalArgumentException("Unknown script type predicate: " + typeString);
			}
		};
	}

	sealed interface BindingProvider extends ScriptTypePredicate {
		String name();

		Object generate();
	}

	record ClassBindingProvider(String name, ScriptTypePredicate filter, String className) implements BindingProvider {
		@Override
		public Object generate() {
			try {
				return this.getClass().getClassLoader().loadClass(className);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public boolean test(ScriptType scriptType) {
			return filter.test(scriptType);
		}
	}

	record InstanceBindingProvider(ClassBindingProvider parent) implements BindingProvider {
		@Override
		public String name() {
			return parent().name();
		}

		@Override
		public Object generate() {
			Class<?> clazz = (Class<?>) parent().generate();
			try {
				Constructor<?> constructor = clazz.getConstructor();
				return constructor.newInstance();
			} catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
				throw new IllegalStateException("[Bindings] Failed to find default constructor in class '" + clazz.getName() + "'");
			}
		}

		@Override
		public boolean test(ScriptType scriptType) {
			return parent().test(scriptType);
		}
	}

	record InvokeBindingProvider(ClassBindingProvider parent, String methodOrField) implements BindingProvider {
		@Override
		public String name() {
			return parent().name();
		}

		@Override
		public Object generate() {
			Class<?> clazz = (Class<?>) parent().generate();

			Object f = byField(clazz);
			if (f != null) {
				return f;
			}

			Object m = byMethod(clazz);
			if (m != null) {
				return m;
			}

			throw new IllegalStateException("[Bindings] Failed to find static field or method '" + methodOrField + "' in class '" + clazz.getName() + "'");
		}

		@Override
		public boolean test(ScriptType scriptType) {
			return parent().test(scriptType);
		}

		@Nullable
		private Object byField(Class<?> clazz) {
			try {
				Field field = clazz.getField(methodOrField);
				if (Modifier.isStatic(field.getModifiers())) {
					return field.get(null);
				}
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("[Bindings] Failed to get static field '" + methodOrField + "' in class '" + clazz.getName() + "'", e);
			} catch (NoSuchFieldException ignored) {
			}
			return null;
		}

		@Nullable
		private Object byMethod(Class<?> clazz) {
			try {
				Method method = clazz.getMethod(methodOrField);
				if (Modifier.isStatic(method.getModifiers())) {
					return method.invoke(null);
				}
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new IllegalStateException("[Bindings] Failed to invoke static method '" + methodOrField + "' in class '" + clazz.getName() + "'", e);
			} catch (NoSuchMethodException ignored) {
			}
			return null;
		}
	}
}
