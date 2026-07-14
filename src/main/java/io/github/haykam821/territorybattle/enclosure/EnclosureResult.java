package io.github.haykam821.territorybattle.enclosure;

import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;

public final class EnclosureResult implements LongIterable {
	private final LongSet positions = new LongOpenHashSet();
	private boolean unenclosed = false;

	protected EnclosureResult() {
		return;
	}

	public void addPosition(BlockPos pos) {
		if (this.unenclosed) {
			throw new IllegalStateException("Cannot add positions to an unenclosed result");
		}

		this.positions.add(pos.asLong());
	}

	public boolean isUnenclosed() {
		return this.unenclosed;
	}

	public void markUnenclosed() {
		this.unenclosed = true;
		this.positions.clear();
	}

	@Override
	public LongIterator iterator() {
		return this.positions.iterator();
	}

	@Override
	public String toString() {
		return "EnclosureResult{unenclosed=" + this.unenclosed + ", positions=" + this.positions + "}";
	}
}
