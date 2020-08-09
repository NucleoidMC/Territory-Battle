package io.github.haykam821.territorybattle.game.map;

import net.gegy1000.plasmid.game.map.template.MapTemplate;
import net.gegy1000.plasmid.game.map.template.TemplateChunkGenerator;
import net.gegy1000.plasmid.util.BlockBounds;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class TerritoryBattleMap {
	private final MapTemplate template;
	private final BlockBounds platform;

	public TerritoryBattleMap(MapTemplate template, BlockBounds platform) {
		this.template = template;
		this.platform = platform;
	}

	public BlockBounds getPlatform() {
		return this.platform;
	}

	public ChunkGenerator createGenerator() {
		return new TemplateChunkGenerator(this.template, BlockPos.ORIGIN);
	}
}