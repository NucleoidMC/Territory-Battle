package io.github.haykam821.territorybattle;

import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.phase.TerritoryBattleWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Main implements ModInitializer {
	private static final String MOD_ID = "territorybattle";

	private static final Identifier PLAYER_BLOCKS_ID = Main.identifier("player_blocks");
	public static final TagKey<Block> PLAYER_BLOCKS = TagKey.of(RegistryKeys.BLOCK, PLAYER_BLOCKS_ID);

	private static final Identifier TERRITORY_BATTLE_ID = Main.identifier("territory_battle");
	public static final GameType<TerritoryBattleConfig> TERRITORY_BATTLE_TYPE = GameType.register(TERRITORY_BATTLE_ID, TerritoryBattleConfig.CODEC, TerritoryBattleWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}