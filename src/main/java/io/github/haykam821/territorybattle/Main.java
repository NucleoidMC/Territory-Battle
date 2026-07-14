package io.github.haykam821.territorybattle;

import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.phase.TerritoryBattleWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class Main implements ModInitializer {
	private static final String MOD_ID = "territorybattle";

	private static final Identifier PLAYER_BLOCKS_ID = Main.identifier("player_blocks");
	public static final TagKey<Block> PLAYER_BLOCKS = TagKey.create(Registries.BLOCK, PLAYER_BLOCKS_ID);

	private static final Identifier TERRITORY_BATTLE_ID = Main.identifier("territory_battle");
	public static final GameType<TerritoryBattleConfig> TERRITORY_BATTLE_TYPE = GameTypes.register(TERRITORY_BATTLE_ID, TerritoryBattleConfig.CODEC, TerritoryBattleWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}