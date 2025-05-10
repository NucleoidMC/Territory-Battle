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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
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

	private final ServerWorld world;
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

	public TerritoryBattleActivePhase(GameSpace gameSpace, ServerWorld world, TerritoryBattleMap map, TerritoryBattleConfig config, HolderAttachment guideText, List<PlayerTerritory> territories, GlobalWidgets widgets) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.guideText = guideText;
		this.territories = territories;
		this.guideTicksLeft = this.config.getGuideTicks().get(this.world.getRandom());
		this.ticksLeft = this.config.getTime();
		this.availableTerritory = this.config.getMapConfig().x * this.config.getMapConfig().z;

		Text timerTitle = Text.literal("Territory Battle");
		this.timerBar = widgets.addBossBar(timerTitle, BossBar.Color.BLUE, BossBar.Style.PROGRESS);
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

	private static List<PlayerTerritory> getTerritories(Iterable<ServerPlayerEntity> players, RegistryEntryList<Block> platformBlocks) {
		List<PlayerTerritory> territories = Lists.newArrayList();

		if (platformBlocks.size() == 0) {
			throw new IllegalStateException("No player block available from " + platformBlocks);
		}

		int index = 0;
		for (PlayerEntity player : players) {
			Block platformBlock = platformBlocks.get(index).value();

			territories.add(new PlayerTerritory(PlayerRef.of(player), platformBlock.getDefaultState()));

			index += 1;
			if (index >= platformBlocks.size()) {
				index = 0;
			}
		}

		return territories;
	}

	public static void open(GameSpace gameSpace, ServerWorld world, TerritoryBattleMap map, TerritoryBattleConfig config, HolderAttachment guideText) {
		gameSpace.setActivity(activity -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);

			List<PlayerTerritory> territories = TerritoryBattleActivePhase.getTerritories(gameSpace.getPlayers().participants(), config.getPlayerBlocks());

			TerritoryBattleActivePhase phase = new TerritoryBattleActivePhase(gameSpace, world, map, config, guideText, territories, widgets);

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
			ServerPlayerEntity player = territory.getPlayerRef().getEntity(this.world);
			if (player != null) {
				player.getInventory().clear();
				territory.giveTerritoryStack(player);

				player.changeGameMode(GameMode.ADVENTURE);

				double theta = ((double) i / this.territories.size()) * 2 * Math.PI;
				this.spawn(player, theta, distance);

				this.world.setBlockState(player.getBlockPos().down(), territory.getTerritoryState());
				this.availableTerritory -= 1;
			}
		}

		for (ServerPlayerEntity player : this.gameSpace.getPlayers().spectators()) {
			TerritoryBattleWaitingPhase.spawn(this.world, this.map, player);
			this.setSpectator(player);
		}

		this.sidebar.update();
	}

	private boolean isNextToState(BlockPos pos, BlockState state) {
		for (Direction direction : NEXT_TO_DIRECTIONS) {
			if (this.world.getBlockState(pos.offset(direction)) == state) {
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
			ServerPlayerEntity player = territory.getPlayerRef().getEntity(this.world);

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

			this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
		}
	}

	private boolean tickTerritory(PlayerTerritory territory, ServerPlayerEntity player) {
		boolean territoryUpdated = false;

		Vec3d start = new Vec3d(player.prevX, player.prevY - MathHelper.EPSILON, player.prevZ);
		Vec3d end = territory.getPreviousPos(player).subtract(0, MathHelper.EPSILON, 0);

		if (!start.equals(end)) {
			double relativeX = end.getX() - start.getX();
			double relativeY = end.getY() - start.getY();
			double relativeZ = end.getZ() - start.getZ();

			BlockPos.Mutable steppingPos = new BlockPos.Mutable();

			for (int step = 1; step <= INTERPOLATION_STEPS; step += 1) {
				double progress = step / (double) INTERPOLATION_STEPS;

				steppingPos.setX((int) (start.getX() + relativeX * progress));
				steppingPos.setY((int) (start.getY() + relativeY * progress));
				steppingPos.setZ((int) (start.getZ() + relativeZ * progress));

				if (this.tickTerritoryAtPos(territory, player, steppingPos)) {
					territoryUpdated = true;
				}
			}
		}

		return territoryUpdated;
	}

	private boolean tickTerritoryAtPos(PlayerTerritory territory, ServerPlayerEntity player, BlockPos steppingPos) {
		BlockState state = this.world.getBlockState(steppingPos);
		BlockState floorState = this.config.getMapConfig().getFloor();
		if (state != floorState) return false;

		BlockState territoryState = territory.getTerritoryState();
		if (!this.isNextToState(steppingPos, territoryState)) return false;

		this.placeTerritory(steppingPos, territory, 0.5f);

		if (this.config.shouldFloodFill()) {
			BlockPos.Mutable enclosedPos = new BlockPos.Mutable();

			for (Direction direction : NEXT_TO_DIRECTIONS) {
				BlockPos enclosablePos = steppingPos.offset(direction);
				EnclosureResult result = EnclosureTraversal.findEnclosure(this.world, enclosablePos, this.map.getTerritoryBounds(), territoryState, floorState);

				for (long pos : result) {
					enclosedPos.set(pos);
					this.placeTerritory(enclosedPos, territory, 0.05f);
				}
			}
		}

		return true;
	}

	private void placeTerritory(BlockPos pos, PlayerTerritory territory, float volume) {
		this.world.setBlockState(pos, territory.getTerritoryState());
		this.world.playSound(null, pos, SoundEvents.BLOCK_SNOW_PLACE, SoundCategory.BLOCKS, volume, 1);

		territory.incrementSize();
		this.availableTerritory -= 1;
	}

	private Text getEndingMessage() {
		if (this.territories.size() == 0) {
			return Text.literal("Nobody won the game!").formatted(Formatting.RED);
		}

		List<PlayerTerritory> sortedTerritories = this.territories.stream().sorted().collect(Collectors.toList());
		PlayerTerritory winnerTerritory = sortedTerritories.get(sortedTerritories.size() - 1);
		return winnerTerritory.getWinMessage(this.world);
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	private JoinAcceptorResult onAcceptPlayers(JoinAcceptor acceptor) {
		return acceptor.teleport(this.world, this.map.getWaitingSpawnPos()).thenRunForEach(player -> {
			this.setSpectator(player);
		});
	}

	private EventResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		// Respawn player
		TerritoryBattleWaitingPhase.spawn(this.world, this.map, player);
		return EventResult.ALLOW;
	}

	public void spawn(ServerPlayerEntity player, double theta, double distance) {
		Vec3d center = map.getPlatform().center();

		double x = center.getX() + Math.sin(theta) * distance;
		double z = center.getZ() - Math.cos(theta) * distance;

		player.teleport(this.world, Math.floor(x) + 0.5, 1, Math.floor(z) + 0.5, Set.of(), (float) Math.toDegrees(theta), 0, true);
	}

	public ServerWorld getWorld() {
		return this.world;
	}

	public List<PlayerTerritory> getTerritories() {
		return this.territories;
	}
}
