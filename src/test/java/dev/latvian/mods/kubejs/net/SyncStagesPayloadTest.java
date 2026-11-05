package dev.latvian.mods.kubejs.net;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SyncStagesPayloadTest {
	@Test
	void queuedPayloadRetainsStagesFromConstruction() {
		var stages = new LinkedHashSet<>(List.of("first", "second"));
		var payload = new SyncStagesPayload(stages);

		stages.remove("first");
		stages.add("later");

		assertEquals(List.of("first", "second"), roundTrip(payload));
	}

	@Test
	void queuedPayloadDoesNotIterateLiveSetDuringEncoding() {
		var stages = new ChangingStages();
		stages.addAll(List.of("first", "second"));
		var payload = new SyncStagesPayload(stages);
		// Model a game-thread mutation after the network encoder starts iteration.
		// This makes the original live-set packet fail deterministically, without sleeps.
		stages.changeDuringIteration = true;

		assertEquals(List.of("first", "second"), roundTrip(payload));
		assertFalse(stages.changed, "Encoding must not read the live stage set");
	}

	@Test
	void emptySnapshotStaysEmptyAfterStageIsAdded() {
		var stages = new ArrayList<String>();
		var payload = new SyncStagesPayload(stages);
		stages.add("later");

		assertEquals(List.of(), roundTrip(payload));
	}

	@Test
	void preservesOrderDuplicatesAndUnicodeOnWire() {
		assertEquals(List.of("second", "first", "first", "progress:\u661f"),
			roundTrip(new SyncStagesPayload(List.of("second", "first", "first", "progress:\u661f"))));
	}

	private static List<String> roundTrip(SyncStagesPayload payload) {
		var buffer = Unpooled.buffer();
		try {
			SyncStagesPayload.STREAM_CODEC.encode(buffer, payload);
			var decoded = SyncStagesPayload.STREAM_CODEC.decode(buffer);
			assertEquals(0, buffer.readableBytes());
			return new ArrayList<>(decoded.stages());
		} finally {
			buffer.release();
		}
	}

	private static class ChangingStages extends LinkedHashSet<String> {
		boolean changeDuringIteration;
		boolean changed;

		@Override
		public Iterator<String> iterator() {
			var delegate = super.iterator();
			return new Iterator<>() {
				@Override
				public boolean hasNext() {
					return delegate.hasNext();
				}

				@Override
				public String next() {
					var stage = delegate.next();
					if (changeDuringIteration && !changed) {
						changed = true;
						add("later");
					}
					return stage;
				}
			};
		}
	}
}