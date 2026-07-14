package io.github.haykam821.territorybattle.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapConfig;
import net.minecraft.SharedConstants;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.HolderSet;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public class TerritoryBattleConfig {
	public static final MapCodec<TerritoryBattleConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> {
		return instance.group(
			TerritoryBattleMapConfig.CODEC.fieldOf("map").forGetter(TerritoryBattleConfig::getMapConfig),
			WaitingLobbyConfig.CODEC.fieldOf("players").forGetter(TerritoryBattleConfig::getPlayerConfig),
			IntProviders.NON_NEGATIVE_CODEC.optionalFieldOf("guide_ticks", ConstantInt.of(SharedConstants.TICKS_PER_SECOND * 5)).forGetter(TerritoryBattleConfig::getGuideTicks),
			IntProviders.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", ConstantInt.of(SharedConstants.TICKS_PER_SECOND * 5)).forGetter(TerritoryBattleConfig::getTicksUntilClose),
			RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("player_blocks").forGetter(TerritoryBattleConfig::getPlayerBlocks),
			Codec.BOOL.optionalFieldOf("flood_fill", true).forGetter(TerritoryBattleConfig::shouldFloodFill),
			Codec.INT.optionalFieldOf("time", 20 * 90).forGetter(TerritoryBattleConfig::getTime)
		).apply(instance, TerritoryBattleConfig::new);
	});

	private final TerritoryBattleMapConfig mapConfig;
	private final WaitingLobbyConfig playerConfig;
	private final IntProvider guideTicks;
	private final IntProvider ticksUntilClose;
	private final HolderSet<Block> playerBlocks;
	private final boolean floodFill;
	private final int time;

	public TerritoryBattleConfig(TerritoryBattleMapConfig mapConfig, WaitingLobbyConfig playerConfig, IntProvider guideTicks, IntProvider ticksUntilClose, HolderSet<Block> playerBlocks, boolean floodFill, int time) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.guideTicks = guideTicks;
		this.ticksUntilClose = ticksUntilClose;
		this.playerBlocks = playerBlocks;
		this.floodFill = floodFill;
		this.time = time;
	}

	public TerritoryBattleMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public WaitingLobbyConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public IntProvider getGuideTicks() {
		return this.guideTicks;
	}

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
	}

	public HolderSet<Block> getPlayerBlocks() {
		return this.playerBlocks;
	}

	public boolean shouldFloodFill() {
		return this.floodFill;
	}

	public int getTime() {
		return this.time;
	}
}