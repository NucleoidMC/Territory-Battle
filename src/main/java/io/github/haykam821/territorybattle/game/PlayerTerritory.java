package io.github.haykam821.territorybattle.game;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.number.FixedNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class PlayerTerritory implements Comparable<PlayerTerritory> {
	private final PlayerRef playerRef;
	private final BlockState territoryState;

	private Vec3d previousPos;
	private int size = 0;

	public PlayerTerritory(PlayerRef playerRef, BlockState territoryState) {
		this.playerRef = playerRef;
		this.territoryState = territoryState;
	}

	public PlayerRef getPlayerRef() {
		return this.playerRef;
	}

	public BlockState getTerritoryState() {
		return this.territoryState;
	}

	public Vec3d getPreviousPos(ServerPlayerEntity player) {
		if (this.previousPos == null) {
			this.updatePreviousPos(player);
		}

		return this.previousPos;
	}

	public void updatePreviousPos(ServerPlayerEntity player) {
		this.previousPos = player.getPos();
	}

	public void incrementSize() {
		this.size += 1;
	}

	private ItemStack getTerritoryStack() {
		return new ItemStack(this.territoryState.getBlock());
	}

	public void giveTerritoryStack(ServerPlayerEntity player) {
		player.getInventory().setStack(8, this.getTerritoryStack());

		// Update inventory
		player.currentScreenHandler.sendContentUpdates();
		player.playerScreenHandler.onContentChanged(player.getInventory());
	}

	public Text getWinMessage(ServerWorld world) {
		PlayerEntity winner = this.getPlayerRef().getEntity(world);
		if (winner == null) {
			return Text.literal("The winner is offline!").formatted(Formatting.GOLD);
		}

		return winner.getDisplayName().copy()
			.append(" has won the game with a territory of " + this.size + " blocks!")
			.formatted(Formatting.GOLD);
	}

	private String getSidebarEntryName(ServerWorld world) {
		PlayerEntity player = this.getPlayerRef().getEntity(world);
		return player == null ? "<Unknown>" : player.getNameForScoreboard();
	}

	public Text getSidebarEntryText(ServerWorld world) {
		return Text.literal(this.getSidebarEntryName(world)).setStyle(TerritoryBattleSidebar.NAME_STYLE);
	}

	public NumberFormat getSidebarNumberFormat() {
		Text text = Text.literal(this.size + "").setStyle(TerritoryBattleSidebar.NUMBER_STYLE);
		return new FixedNumberFormat(text);
	}

	@Override
	public int compareTo(PlayerTerritory other) {
		return this.size - other.size;
	}

	@Override
	public String toString() {
		return "PlayerTerritory{playerRef=" + this.getPlayerRef() + ", territoryState=" + this.getTerritoryState() + ", size=" + this.size + "}";
	}
}