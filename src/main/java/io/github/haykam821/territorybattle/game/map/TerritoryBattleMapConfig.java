package io.github.haykam821.territorybattle.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public class TerritoryBattleMapConfig {
	public static final Codec<TerritoryBattleMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(map -> map.x),
			Codec.INT.fieldOf("z").forGetter(map -> map.z),
			BlockState.CODEC.optionalFieldOf("floor", Blocks.WOOL.white().defaultBlockState()).forGetter(TerritoryBattleMapConfig::getFloor),
			BlockState.CODEC.optionalFieldOf("floor_outline", Blocks.SPRUCE_PLANKS.defaultBlockState()).forGetter(TerritoryBattleMapConfig::getFloorOutline),
			BlockState.CODEC.optionalFieldOf("wall", Blocks.COBBLESTONE_WALL.defaultBlockState()).forGetter(TerritoryBattleMapConfig::getWall),
			BlockState.CODEC.optionalFieldOf("wall_top", Blocks.SPRUCE_SLAB.defaultBlockState()).forGetter(TerritoryBattleMapConfig::getWallTop)
		).apply(instance, TerritoryBattleMapConfig::new);
	});

	public final int x;
	public final int z;

	private final BlockState floor;
	private final BlockState floorOutline;
	private final BlockState wall;
	private final BlockState wallTop;

	public TerritoryBattleMapConfig(int x, int z, BlockState floor, BlockState floorOutline, BlockState wall, BlockState wallTop) {
		this.x = x;
		this.z = z;

		this.floor = floor;
		this.floorOutline = floorOutline;
		this.wall = wall;
		this.wallTop = wallTop;
	}

	public BlockState getFloor() {
		return this.floor;
	}

	public BlockState getFloorOutline() {
		return this.floorOutline;
	}

	public BlockState getWall() {
		return this.wall;
	}

	public BlockState getWallTop() {
		return this.wallTop;
	}
}