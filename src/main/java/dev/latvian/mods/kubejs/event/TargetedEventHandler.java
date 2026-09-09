package dev.latvian.mods.kubejs.event;

import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.script.ScriptTypeHolder;
import dev.latvian.mods.kubejs.script.ScriptTypePredicate;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.HideFromJS;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TargetedEventHandler<E> extends EventHandler {
	protected @Nullable Map<Object, @Nullable EventHandlerContainer[]> extraEventContainers;

	TargetedEventHandler(EventGroup g, String n, ScriptTypePredicate st, EventTargetType<E> target, Supplier<Class<? extends KubeEvent>> e) {
		super(g, n, st, e);
		this.target = target;
		this.extraEventContainers = null;
	}

	@Override
	@HideFromJS
	public TargetedEventHandler<E> hasResult(TypeInfo result) {
		super.hasResult(result);
		return this;
	}

	@Override
	public TargetedEventHandler<E> hasResult() {
		super.hasResult();
		return this;
	}

	@Override
	@HideFromJS
	public TargetedEventHandler<E> exceptionHandler(EventExceptionHandler handler) {
		this.exceptionHandler = handler;
		return this;
	}

	@Override
	public boolean hasListeners() {
		return eventContainers != null || extraEventContainers != null;
	}

	public boolean hasListeners(@Nullable E extraId) {
		var extra = extraEventContainers;
		return eventContainers != null || extraId != null && extra != null && extra.containsKey(extraId);
	}

	/// @see TargetedEventHandler#post(ScriptTypeHolder, E, KubeEvent)
	public EventResult post(KubeEvent event, @Nullable E extraId) {
		if (scriptTypePredicate instanceof ScriptTypeHolder type) {
			return postInternal(type, extraId, event);
		} else {
			throw new IllegalStateException("You must specify which script type to post event to");
		}
	}

	/// @return EventResult that can contain an object. What previously returned true on [KubeEvent#cancel(Context)] now returns [EventResult#interruptFalse()]
	/// @see KubeEvent#cancel(Context)
	/// @see KubeEvent#success(Context)
	/// @see KubeEvent#exit(Context)
	/// @see KubeEvent#cancel(Context, Object)
	/// @see KubeEvent#success(Context, Object)
	/// @see KubeEvent#exit(Context, Object)
	public EventResult post(ScriptTypeHolder type, @Nullable E extraId, KubeEvent event) {
		return postInternal(type, extraId, event);
	}

	@Override
	@HideFromJS
	public void clear(ScriptType type) {
		super.clear(type);

		if (extraEventContainers != null) {
			var entries = extraEventContainers.entrySet().iterator();

			while (entries.hasNext()) {
				var entry = entries.next();
				entry.getValue()[type.ordinal()] = null;

				if (EventHandlerContainer.isEmpty(entry.getValue())) {
					entries.remove();
				}
			}

			if (extraEventContainers.isEmpty()) {
				extraEventContainers = null;
			}
		}
	}

	@Override
	protected @Nullable EventHandlerContainer[] createMap(@Nullable Object extraId) {
		if (extraId == null) {
			return super.createMap(null);
		}

		if (extraEventContainers == null) {
			extraEventContainers = target.identity ? new Reference2ObjectOpenHashMap<>() : new HashMap<>();
		}

		var map = extraEventContainers.get(extraId);

		//noinspection Java8MapApi
		if (map == null) {
			map = new EventHandlerContainer[ScriptType.VALUES.length];
			extraEventContainers.put(extraId, map);
		}

		return map;
	}

	@Override
	public void forEachListener(ScriptType type, Consumer<EventHandlerContainer> callback) {
		super.forEachListener(type, callback);

		if (extraEventContainers != null) {
			for (var entry : extraEventContainers.entrySet()) {
				var c = entry.getValue()[type.ordinal()];

				while (c != null) {
					callback.accept(c);
					c = c.child;
				}
			}
		}
	}

	public Set<E> findUniqueExtraIds(ScriptType type) {
		if (!hasListeners()) {
			return Set.of();
		}

		var set = new HashSet<E>();

		forEachListener(type, c -> {
			if (c.target != null) {
				set.add((E) c.target);
			}
		});

		return set;
	}
}
