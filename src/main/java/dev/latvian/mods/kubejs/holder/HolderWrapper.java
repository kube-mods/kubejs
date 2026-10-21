package dev.latvian.mods.kubejs.holder;

import dev.latvian.mods.kubejs.script.KubeJSContext;
import dev.latvian.mods.kubejs.util.Cast;
import dev.latvian.mods.kubejs.util.ID;
import dev.latvian.mods.kubejs.util.RegExpKJS;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.type.TypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.holdersets.OrHolderSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface HolderWrapper {
	TypeInfo HOLDER = TypeInfo.of(Holder.class);
	TypeInfo HOLDER_SET = TypeInfo.of(HolderSet.class);

	static Holder<?> wrap(KubeJSContext cx, @Nullable Object from, TypeInfo param) {
		if (from instanceof Holder<?> h) {
			return h;
		} else if (from == null) {
			throw Context.reportRuntimeError("Can't interpret 'null' as a Holder", cx);
		}

		var registry = cx.lookupRegistry(param, from);

		if (!ID.isKey(from)) {
			var h = registry.wrapAsHolder(Cast.to(from));

			if (h instanceof Holder.Direct) {
				var baseClass = cx.lookupRegistryType(param, from).baseClass();

				if (!baseClass.isInstance(from)) {
					throw Context.reportRuntimeError("Can't interpret '" + from + "' as Holder: can't cast object to '" + baseClass.getName() + "' of " + registry.key().identifier(), cx);
				}
			}

			return h;
		}

		var id = ID.mc(from);
		var holder = registry.get(id);
		return holder.isEmpty() ? DeferredHolder.create(registry.key(), id) : holder.get();
	}

	static Holder.Reference<?> wrapRef(KubeJSContext cx, Object from, TypeInfo param) {
		var h = wrap(cx, from, param);

		// Self or wrapped holder
		if (h.getDelegate() instanceof Holder.Reference<?> ref) {
			return ref;
		} else if (h instanceof Holder.Direct<?>) {
			// Create intrusive instead?
			throw Context.reportRuntimeError("Can't interpret '" + from + "' as a Reference Holder: cannot obtain its registry id", cx);
		}

		//noinspection DataFlowIssue
		return Holder.Reference.createStandAlone(Cast.to(cx.lookupRegistry(param, from)), h.key()); // Only null with direct holders
	}

	static HolderSet<?> wrapSet(KubeJSContext cx, Object from, TypeInfo param) {
		var registry = cx.lookupRegistry(param, from);

		var simpleHolders = wrapSimpleSet(registry, from);

		if (simpleHolders != null) {
			return simpleHolders;
		}

		if (from instanceof Iterable<?> itr) {
			var allDirects = Stream.<HolderSet<?>>builder();
			var complex = new ArrayList<HolderSet<?>>();

			for (var elem : itr) {
				var wrapped = wrapSet(cx, elem, param);

				if (wrapped instanceof HolderSet.Direct direct) {
					allDirects.accept(direct);
				} else {
					complex.add(wrapped);
				}
			}

			var compressedDirects = allDirects.build().flatMap(HolderSet::stream).distinct().toList();

			if (compressedDirects.isEmpty()) {
				return switch (complex.size()) {
					case 0 -> HolderSet.empty();
					case 1 -> complex.getFirst();
					default -> new OrHolderSet(complex);
				};
			} else {
				if (complex.isEmpty()) {
					return HolderSet.direct((List) compressedDirects);
				} else {
					complex.add(HolderSet.direct((List) compressedDirects));
					return new OrHolderSet(complex);
				}
			}
		} else {
			var holder = (Holder) cx.jsToJava(from, HOLDER.withParams(param));
			return HolderSet.direct(holder);
		}
	}

	@Nullable
	static <T> HolderSet<T> wrapSimpleSet(Registry<T> registry, Object from) {
		var regex = RegExpKJS.wrap(from);

		if (regex != null) {
			return new RegExHolderSet<>(registry.filterElements(t -> true), regex);
		}

		if (from instanceof CharSequence) {
			var s = from.toString();

			if (s.isEmpty()) {
				return HolderSet.empty();
			} else if (s.charAt(0) == '@') {
				return new NamespaceHolderSet<>(registry.filterElements(t -> true), s.substring(1));
			} else if (s.charAt(0) == '#') {
				var tagKey = TagKey.create(registry.key(), Identifier.parse(s.substring(1)));
				return registry.get(tagKey).get();
			}
		}

		return null;
	}

	@SuppressWarnings("all")
	private static <T> HolderSet<T> orEmpty(Optional<? extends HolderSet<T>> holder) {
		return ((Optional<HolderSet<T>>) holder).orElse(HolderSet.empty());
	}
}
