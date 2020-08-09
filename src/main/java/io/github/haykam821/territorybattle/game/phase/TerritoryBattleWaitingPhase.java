package io.github.haykam821.territorybattle.game.phase;

import java.util.concurrent.CompletableFuture;

import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMap;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapBuilder;
import net.gegy1000.plasmid.game.GameWorld;
import net.gegy1000.plasmid.game.GameWorldState;
import net.gegy1000.plasmid.game.StartResult;
import net.gegy1000.plasmid.game.config.PlayerConfig;
import net.gegy1000.plasmid.game.event.OfferPlayerListener;
import net.gegy1000.plasmid.game.event.PlayerAddListener;
import net.gegy1000.plasmid.game.event.PlayerDeathListener;
import net.gegy1000.plasmid.game.event.RequestStartListener;
import net.gegy1000.plasmid.game.player.JoinResult;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class TerritoryBattleWaitingPhase {
	private final GameWorld gameWorld;
	private final TerritoryBattleMap map;
	private final TerritoryBattleConfig config;

	public TerritoryBattleWaitingPhase(GameWorld gameWorld, TerritoryBattleMap map, TerritoryBattleConfig config) {
		this.gameWorld = gameWorld;
		this.map = map;
		this.config = config;
	}

	public static CompletableFuture<Void> open(GameWorldState gameState, TerritoryBattleConfig config) {
		TerritoryBattleMapBuilder mapBuilder = new TerritoryBattleMapBuilder(config);

		return mapBuilder.create().thenAccept(map -> {
			GameWorld gameWorld = gameState.openWorld(map.createGenerator());
			TerritoryBattleWaitingPhase phase = new TerritoryBattleWaitingPhase(gameWorld, map, config);

			gameWorld.newGame(game -> {
				TerritoryBattleActivePhase.setRules(game);

				// Listeners
				game.on(PlayerAddListener.EVENT, phase::addPlayer);
				game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
				game.on(OfferPlayerListener.EVENT, phase::offerPlayer);
				game.on(RequestStartListener.EVENT, phase::requestStart);
			});
		});
	}

	private boolean isFull() {
		return this.gameWorld.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	private JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	private StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameWorld.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.notEnoughPlayers();
		}

		TerritoryBattleActivePhase.open(this.gameWorld, this.map, this.config);
		return StartResult.ok();
	}

	private void addPlayer(ServerPlayerEntity player) {
		TerritoryBattleWaitingPhase.spawn(this.gameWorld.getWorld(), this.map, player);
	}

	private boolean onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		TerritoryBattleWaitingPhase.spawn(this.gameWorld.getWorld(), this.map, player);
		return true;
	}

	public static void spawn(ServerWorld world, TerritoryBattleMap map, ServerPlayerEntity player) {
		Vec3d center = map.getPlatform().getCenter();
		player.teleport(world, center.getX(), center.getY() + 0.5, center.getZ(), 0, 0);
	}
}