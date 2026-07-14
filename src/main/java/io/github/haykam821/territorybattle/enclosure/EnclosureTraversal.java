package io.github.haykam821.territorybattle.enclosure;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.util.BlockTraversal;
import xyz.nucleoid.plasmid.api.util.BlockTraversal.Connectivity;
import xyz.nucleoid.plasmid.api.util.BlockTraversal.Result;

/**
 * Generic utilities for determining whether a floor region is enclosed by a certain block state.
 */
public final class EnclosureTraversal {
	private static final Connectivity CONNECTIVITY = Connectivity.four(Direction.Axis.Y);
	private static final BlockTraversal TRAVERSAL = BlockTraversal.create().connectivity(CONNECTIVITY);

	private EnclosureTraversal() {
		return;
	}

	public static EnclosureResult findEnclosure(ServerLevel world, BlockPos origin, BlockBounds bounds, BlockState outlineState, BlockState floorState) {
		EnclosureResult result = new EnclosureResult();

		TRAVERSAL.accept(origin, (pos, fromPos, depth) -> {
			if (result.isUnenclosed() || !bounds.contains(pos)) {
				return Result.TERMINATE;
			}

			BlockState state = world.getBlockState(pos);

			if (state == outlineState) {
				return Result.TERMINATE;
			} else if (state != floorState) {
				result.markUnenclosed();
				return Result.TERMINATE;
			}

			result.addPosition(pos);
			return Result.CONTINUE;
		});

		return result;
	}
}
