package io.github.haykam821.territorybattle.game.phase;

import java.util.Set;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleGuideText;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMap;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapBuilder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class TerritoryBattleWaitingPhase {
	private final GameSpace gameSpace;
	private final ServerLevel level;
	private final TerritoryBattleMap map;
	private final TerritoryBattleConfig config;

	private HolderAttachment guideText;

	public TerritoryBattleWaitingPhase(GameSpace gameSpace, ServerLevel level, TerritoryBattleMap map, TerritoryBattleConfig config) {
		this.gameSpace = gameSpace;
		this.level = level;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<TerritoryBattleConfig> context) {
		TerritoryBattleMapBuilder mapBuilder = new TerritoryBattleMapBuilder(context.config());

		TerritoryBattleMap map = mapBuilder.create();
		RuntimeLevelConfig levelConfig = new RuntimeLevelConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithLevel(levelConfig, (activity, level) -> {
			TerritoryBattleWaitingPhase phase = new TerritoryBattleWaitingPhase(activity.getGameSpace(), level, map, context.config());

			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());

			TerritoryBattleActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::onEnable);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
			activity.listen(GameActivityEvents.REQUEST_START, phase::requestStart);
		});
	}

	private void onEnable() {
		// Spawn guide text
		Vec3 guideTextPos = this.map.getGuideTextPos();

		if (guideTextPos != null) {
			ElementHolder holder = TerritoryBattleGuideText.createElementHolder();
			this.guideText = ChunkAttachment.of(holder, level, guideTextPos);
		}
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.level, this.map.getWaitingSpawnPos()).thenRunForEach(player -> {
			player.setGameMode(GameType.SPECTATOR);
		});
	}

	private GameResult requestStart() {
		TerritoryBattleActivePhase.open(this.gameSpace, this.level, this.map, this.config, this.guideText);
		return GameResult.ok();
	}

	private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		// Respawn player
		TerritoryBattleWaitingPhase.spawn(this.level, this.map, player);
		return EventResult.DENY;
	}

	public static void spawn(ServerLevel level, TerritoryBattleMap map, ServerPlayer player) {
		Vec3 spawnPos = map.getWaitingSpawnPos();
		player.teleportTo(level, spawnPos.x(), spawnPos.y(), spawnPos.z(), Set.of(), 0, 0, true);
	}
}
