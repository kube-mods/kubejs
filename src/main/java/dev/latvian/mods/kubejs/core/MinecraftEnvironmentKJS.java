package dev.latvian.mods.kubejs.core;

import dev.latvian.mods.kubejs.util.ScheduledEvents;
import dev.latvian.mods.kubejs.util.TickDuration;
import dev.latvian.mods.rhino.util.RemapPrefixForJS;

import java.time.temporal.TemporalAmount;

@RemapPrefixForJS("kjs$")
public interface MinecraftEnvironmentKJS extends MessageSenderKJS {
	ScheduledEvents kjs$getScheduledEvents();

	default ScheduledEvents.ScheduledEvent kjs$schedule(TemporalAmount timer, ScheduledEvents.Callback callback) {
		return kjs$getScheduledEvents().schedule(timer, false, callback);
	}

	default ScheduledEvents.ScheduledEvent kjs$scheduleInTicks(TickDuration ticks, ScheduledEvents.Callback callback) {
		return kjs$getScheduledEvents().schedule(ticks, false, callback);
	}

	default ScheduledEvents.ScheduledEvent kjs$scheduleRepeating(TemporalAmount timer, ScheduledEvents.Callback callback) {
		return kjs$getScheduledEvents().schedule(timer, true, callback);
	}

	default ScheduledEvents.ScheduledEvent kjs$scheduleRepeatingInTicks(TickDuration ticks, ScheduledEvents.Callback callback) {
		return kjs$getScheduledEvents().schedule(ticks, true, callback);
	}
}
