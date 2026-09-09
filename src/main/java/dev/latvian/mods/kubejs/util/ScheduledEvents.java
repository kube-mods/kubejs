package dev.latvian.mods.kubejs.util;

import dev.latvian.mods.kubejs.script.ConsoleJS;
import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.RhinoException;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.type.TypeInfo;

import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ScheduledEvents {
	public static class ScheduledEvent {
		private static final Predicate<ScheduledEvent> TICK = ScheduledEvent::tick;

		public ScheduledEvents scheduledEvents;
		public int id;
		public boolean ofTicks;
		public boolean repeating;
		public long timer;
		public long endTime;
		public transient Callback callback;

		public ScheduledEvent reschedule() {
			this.endTime = (ofTicks ? scheduledEvents.currentTick : System.currentTimeMillis()) + timer;
			return this;
		}

		public ScheduledEvent reschedule(long timer) {
			this.timer = timer;
			return reschedule();
		}

		public void clear() {
			this.callback = null;
		}

		private boolean tick() {
			if (callback == null) {
				return true;
			} else if ((ofTicks ? scheduledEvents.currentTick : scheduledEvents.currentMillis) >= endTime) {
				try {
					callback.onCallback(this);
				} catch (RhinoException ex) {
					ConsoleJS.SERVER.error("Error occurred while handling scheduled event callback: " + ex.getMessage());
				} catch (Throwable ex) {
					ex.printStackTrace();
				}

				if (repeating) {
					reschedule();
					return false;
				} else {
					return true;
				}
			}

			return false;
		}
	}

	@FunctionalInterface
	public interface Callback {
		TypeInfo TYPE_INFO = TypeInfo.of(Callback.class);

		void onCallback(ScheduledEvent event);
	}

	public static class TimeoutJSFunction extends BaseFunction {
		private static final TypeInfo TEMPORAL_AMOUNT_TYPE_INFO = TypeInfo.of(TemporalAmount.class);

		public final ScheduledEvents scheduledEvents;
		public final boolean clear;
		public final boolean interval;

		public TimeoutJSFunction(ScheduledEvents scheduledEvents, boolean clear, boolean interval) {
			this.scheduledEvents = scheduledEvents;
			this.clear = clear;
			this.interval = interval;
		}

		@Override
		public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
			if (clear) {
				scheduledEvents.clear(ScriptRuntime.toInt32(cx, args[0]));
				return Undefined.INSTANCE;
			} else {
				var timer = (TemporalAmount) cx.jsToJava(args[1], TEMPORAL_AMOUNT_TYPE_INFO);
				var callback = (Callback) cx.createInterfaceAdapter(Callback.TYPE_INFO, (ScriptableObject) args[0]);
				return scheduledEvents.schedule(timer, interval, callback).id;
			}
		}
	}

	public final Supplier<ScheduledEvent> factory;
	public final LinkedList<ScheduledEvent> events;
	public final LinkedList<ScheduledEvent> futureEvents;
	public final AtomicInteger nextId;
	public long currentMillis;
	public long currentTick;
	private boolean ticksReset;

	public ScheduledEvents(Supplier<ScheduledEvent> factory) {
		this.factory = factory;
		this.events = new LinkedList<>();
		this.futureEvents = new LinkedList<>();
		this.nextId = new AtomicInteger(0);
		this.currentMillis = System.currentTimeMillis();
		this.currentTick = 0L;
		this.ticksReset = true;
	}

	public void clear() {
		events.clear();
		futureEvents.clear();
		ticksReset = true;
	}

	public ScheduledEvent schedule(TemporalAmount timer, boolean repeating, ScheduledEvents.Callback callback) {
		if (timer instanceof TickDuration(long ticks)) {
			return schedule(ticks, true, repeating, callback);
		} else if (timer instanceof Duration duration) {
			return schedule(duration.toMillis(), false, repeating, callback);
		} else {
			throw new IllegalArgumentException("Unsupported TemporalAmount: " + timer);
		}
	}

	public ScheduledEvent schedule(long timer, boolean ofTicks, boolean repeating, Callback callback) {
		var e = new ScheduledEvent();
		e.scheduledEvents = this;
		e.id = nextId.incrementAndGet();
		e.ofTicks = ofTicks;
		e.repeating = repeating;
		e.timer = timer;
		e.callback = callback;

		if (!ofTicks || !ticksReset) {
			e.reschedule();
		}

		futureEvents.add(e);
		return e;
	}

	public void tickAll(long nowTicks) {
		currentMillis = System.currentTimeMillis();
		currentTick = nowTicks;

		if (!futureEvents.isEmpty()) {
			if (ticksReset) {
				for (var e : futureEvents) {
					if (e.ofTicks) {
						e.reschedule();
					}
				}
			}

			events.addAll(futureEvents);
			futureEvents.clear();
		}

		ticksReset = false;

		if (!events.isEmpty()) {
			events.removeIf(ScheduledEvent.TICK);
		}
	}

	public void clear(int id) {
		for (var event : events) {
			if (event.id == id) {
				event.callback = null;
				break;
			}
		}

		for (var event : futureEvents) {
			if (event.id == id) {
				event.callback = null;
				return;
			}
		}
	}
}
