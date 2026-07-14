package io.github.haykam821.territorybattle.game.phase;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import io.github.haykam821.territorybattle.enclosure.EnclosureResult;
import io.github.haykam821.territorybattle.enclosure.EnclosureTraversal;
import io.github.haykam821.territorybattle.game.PlayerTerritory;
import io.github.haykam821.territorybattle.game.TerritoryBattleConfig;
import io.github.haykam821.territorybattle.game.TerritoryBattleSidebar;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMap;
import io.github.haykam821.territorybattle.game.map.TerritoryBattleMapConfig;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.BossBarWidget;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class TerritoryBattleActivePhase {
	private static final Direction[] NEXT_TO_DIRECTIONS = new Direction[] {
		Direction.NORTH,
		Direction.EAST,
		Direction.SOUTH,
		Direction.WEST
	};

	private static final int INTERPOLATION_STEPS = 30;

	private final ServerLevel level;
	private final GameSpace gameSpace;
	private final TerritoryBattleMap map;
	private final TerritoryBattleConfig config;
	private HolderAttachment guideText;
	private List<PlayerTerritory> territories;
	private int ticksLeft;
	private BossBarWidget timerBar;
	private final TerritoryBattleSidebar sidebar;
	private int availableTerritory;
	private int guideTicksLeft = 0;
	private int ticksUntilClose = -1;

	public TerritoryBattleActivePhase(GameSpace gameSpace, ServerLevel level, TerritoryBattleMap map, TerritoryBattleConfig config, HolderAttachment guideText, List<PlayerTerritory> territories, GlobalWidgets widgets) {
		this.level = level;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.guideText = guideText;
		this.territories = territories;
		this.guideTicksLeft = this.config.getGuideTicks().sample(this.level.getRandom());
		this.ticksLeft = this.config.getTime();
		this.availableTerritory = this.config.getMapConfig().x * this.config.getMapConfig().z;

		Component timerTitle = Component.literal("Territory Battle");
		this.timerBar = widgets.addBossBar(timerTitle, BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
		this.sidebar = new TerritoryBattleSidebar(widgets, this, timerTitle);
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.PORTALS);
		activity.deny(GameRuleType.PVP);
		activity.deny(GameRuleType.THROW_ITEMS);
	}

	private static List<PlayerTerritory> getTerritories(Iterable<ServerPlayer> players, HolderSet<Block> platformBlocks) {
		List<PlayerTerritory> territories = Lists.newArrayList();

		if (platformBlocks.size() == 0) {
			throw new IllegalStateException("No player block available from " + platformBlocks);
		}

		int index = 0;
		for (Player player : players) {
			Block platformBlock = platformBlocks.get(index).value();

			territories.add(new PlayerTerritory(PlayerRef.of(player), platformBlock.defaultBlockState()));

			index += 1;
			if (index >= platformBlocks.size()) {
				index = 0;
			}
		}

		return territories;
	}

	public static void open(GameSpace gameSpace, ServerLevel level, TerritoryBattleMap map, TerritoryBattleConfig config, HolderAttachment guideText) {
		gameSpace.setActivity(activity -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);

			List<PlayerTerritory> territories = TerritoryBattleActivePhase.getTerritories(gameSpace.getPlayers().participants(), config.getPlayerBlocks());

			TerritoryBattleActivePhase phase = new TerritoryBattleActivePhase(gameSpace, level, map, config, guideText, territories, widgets);

			TerritoryBattleActivePhase.setRules(activity);

			// Listeners
			activity.listen(GameActivityEvents.ENABLE, phase::enable);
			activity.listen(GameActivityEvents.TICK, phase::tick);
			activity.listen(GamePlayerEvents.ACCEPT, phase::onAcceptPlayers);
			activity.listen(GamePlayerEvents.OFFER, JoinOffer::acceptSpectators);
			activity.listen(PlayerDeathEvent.EVENT, phase::onPlayerDeath);
		});
	}

	private double getDistance() {
		TerritoryBattleMapConfig mapConfig = this.config.getMapConfig();

		if (mapConfig.x < mapConfig.z) {
			return (this.config.getMapConfig().x - 3) / (double) 2;
		} else {
			return (this.config.getMapConfig().z - 3) / (double) 2;
		}
	}

	private void enable() {
		double distance = this.getDistance();
		for (int i = 0; i < this.territories.size(); i++) {
			PlayerTerritory territory = this.territories.get(i);
			ServerPlayer player = territory.getPlayerRef().getEntity(this.level);
			if (player != null) {
				player.getInventory().clearContent();
				territory.giveTerritoryStack(player);

				player.setGameMode(GameType.ADVENTURE);

				double theta = ((double) i / this.territories.size()) * 2 * Math.PI;
				this.spawn(player, theta, distance);

				this.level.setBlockAndUpdate(player.blockPosition().below(), territory.getTerritoryState());
				this.availableTerritory -= 1;
			}
		}

		for (ServerPlayer player : this.gameSpace.getPlayers().spectators()) {
			TerritoryBattleWaitingPhase.spawn(this.level, this.map, player);
			this.setSpectator(player);
		}

		this.sidebar.update();
	}

	private boolean isNextToState(BlockPos pos, BlockState state) {
		for (Direction direction : NEXT_TO_DIRECTIONS) {
			if (this.level.getBlockState(pos.relative(direction)) == state) {
				return true;
			}
		}
		return false;
	}

	private void tick() {
		this.guideTicksLeft -= 1;

		if (this.guideTicksLeft == 0) {
			this.guideText.destroy();
			this.guideText = null;
		}

		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		boolean territoryUpdated = false;
		for (PlayerTerritory territory : this.territories) {
			ServerPlayer player = territory.getPlayerRef().getEntity(this.level);

			if (player != null && this.tickTerritory(territory, player)) {
				territoryUpdated = true;
			}

			territory.updatePreviousPos(player);
		}

		if (territoryUpdated) {
			this.sidebar.update();
		}

		this.ticksLeft -= 1;
 		this.timerBar.setProgress(this.ticksLeft / (float) this.config.getTime());
		if (this.ticksLeft == 0 || this.availableTerritory <= 0) {
			this.gameSpace.getPlayers().sendMessage(this.getEndingMessage());
			this.gameSpace.getPlayers().playSound(SoundEvents.PLAYER_LEVELUP, SoundSource.UI, 1, 1);
			this.ticksUntilClose = this.config.getTicksUntilClose().sample(this.level.getRandom());
		}
	}

	private boolean tickTerritory(PlayerTerritory territory, ServerPlayer player) {
		boolean territoryUpdated = false;

		Vec3 start = new Vec3(player.xo, player.yo - Mth.EPSILON, player.zo);
		Vec3 end = territory.getPreviousPos(player).subtract(0, Mth.EPSILON, 0);

		if (!start.equals(end)) {
			double relativeX = end.x() - start.x();
			double relativeY = end.y() - start.y();
			double relativeZ = end.z() - start.z();

			BlockPos.MutableBlockPos steppingPos = new BlockPos.MutableBlockPos();

			for (int step = 1; step <= INTERPOLATION_STEPS; step += 1) {
				double progress = step / (double) INTERPOLATION_STEPS;

				steppingPos.setX((int) (start.x() + relativeX * progress));
				steppingPos.setY((int) (start.y() + relativeY * progress));
				steppingPos.setZ((int) (start.z() + relativeZ * progress));

				if (this.tickTerritoryAtPos(territory, player, steppingPos)) {
					territoryUpdated = true;
				}
			}
		}

		return territoryUpdated;
	}

	private boolean tickTerritoryAtPos(PlayerTerritory territory, ServerPlayer player, BlockPos steppingPos) {
		BlockState state = this.level.getBlockState(steppingPos);
		BlockState floorState = this.config.getMapConfig().getFloor();
		if (state != floorState) return false;

		BlockState territoryState = territory.getTerritoryState();
		if (!this.isNextToState(steppingPos, territoryState)) return false;

		this.placeTerritory(steppingPos, territory, 0.5f);

		if (this.config.shouldFloodFill()) {
			BlockPos.MutableBlockPos enclosedPos = new BlockPos.MutableBlockPos();

			for (Direction direction : NEXT_TO_DIRECTIONS) {
				BlockPos enclosablePos = steppingPos.relative(direction);
				EnclosureResult result = EnclosureTraversal.findEnclosure(this.level, enclosablePos, this.map.getTerritoryBounds(), territoryState, floorState);

				for (long pos : result) {
					enclosedPos.set(pos);
					this.placeTerritory(enclosedPos, territory, 0.05f);
				}
			}
		}

		return true;
	}

	private void placeTerritory(BlockPos pos, PlayerTerritory territory, float volume) {
		this.level.setBlockAndUpdate(pos, territory.getTerritoryState());
		this.level.playSound(null, pos, SoundEvents.SNOW_PLACE, SoundSource.BLOCKS, volume, 1);

		territory.incrementSize();
		this.availableTerritory -= 1;
	}

	private Component getEndingMessage() {
		if (this.territories.size() == 0) {
			return Component.literal("Nobody won the game!").withStyle(ChatFormatting.RED);
		}

		List<PlayerTerritory> sortedTerritories = this.territories.stream().sorted().collect(Collectors.toList());
		PlayerTerritory winnerTerritory = sortedTerritories.get(sortedTerritories.size() - 1);
		return winnerTerritory.getWinMessage(this.level);
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	private void setSpectator(ServerPlayer player) {
		player.setGameMode(GameType.SPECTATOR);
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.level, this.map.getWaitingSpawnPos()).thenRunForEach(player -> {
			this.setSpectator(player);
		});
	}

	private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
		// Respawn player
		TerritoryBattleWaitingPhase.spawn(this.level, this.map, player);
		return EventResult.ALLOW;
	}

	public void spawn(ServerPlayer player, double theta, double distance) {
		Vec3 center = map.getPlatform().center();

		double x = center.x() + Math.sin(theta) * distance;
		double z = center.z() - Math.cos(theta) * distance;

		player.teleportTo(this.level, Math.floor(x) + 0.5, 1, Math.floor(z) + 0.5, Set.of(), (float) Math.toDegrees(theta), 0, true);
	}

	public ServerLevel getLevel() {
		return this.level;
	}

	public List<PlayerTerritory> getTerritories() {
		return this.territories;
	}
}
