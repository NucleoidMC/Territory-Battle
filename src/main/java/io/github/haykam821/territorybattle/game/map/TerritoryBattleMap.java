package io.github.haykam821.territorybattle.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

public class TerritoryBattleMap {
	private final MapTemplate template;

	private final BlockBounds platform;
	private final BlockBounds territoryBounds;

	private final Vec3 guideTextPos;
	private final Vec3 waitingSpawnPos;

	public TerritoryBattleMap(MapTemplate template, BlockBounds platform) {
		this.template = template;

		this.platform = platform;

		int territoryY = this.platform.min().getY();
		this.territoryBounds = BlockBounds.of(this.platform.min().getX() + 1, territoryY, this.platform.min().getZ() + 1, this.platform.max().getX() - 1, territoryY, this.platform.max().getZ() - 1);

		this.guideTextPos = this.createCenterPos(2.2, 0);
		this.waitingSpawnPos = this.createCenterPos(1, 4);
	}

	public BlockBounds getPlatform() {
		return this.platform;
	}

	public BlockBounds getTerritoryBounds() {
		return this.territoryBounds;
	}

	public Vec3 getGuideTextPos() {
		return this.guideTextPos;
	}

	public Vec3 getWaitingSpawnPos() {
		return this.waitingSpawnPos;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}

	private Vec3 createCenterPos(double offsetY, double offsetZ) {
		Vec3 center = this.getPlatform().centerBottom();

		double maxOffsetZ = this.platform.size().getZ() / 2 - 0.5;
		double clampedOffsetZ = Mth.clamp(offsetZ, -maxOffsetZ, maxOffsetZ);

		return new Vec3(center.x(), center.y() + offsetY, center.z() - clampedOffsetZ);
	}
}
