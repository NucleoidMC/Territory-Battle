package io.github.haykam821.territorybattle.game;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.numbers.FixedFormat;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class PlayerTerritory implements Comparable<PlayerTerritory> {
	private final PlayerRef playerRef;
	private final BlockState territoryState;

	private Vec3 previousPos;
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

	public Vec3 getPreviousPos(ServerPlayer player) {
		if (this.previousPos == null) {
			this.updatePreviousPos(player);
		}

		return this.previousPos;
	}

	public void updatePreviousPos(ServerPlayer player) {
		this.previousPos = player.position();
	}

	public void incrementSize() {
		this.size += 1;
	}

	private ItemStack getTerritoryStack() {
		return new ItemStack(this.territoryState.getBlock());
	}

	public void giveTerritoryStack(ServerPlayer player) {
		player.getInventory().setItem(8, this.getTerritoryStack());

		// Update inventory
		player.containerMenu.broadcastChanges();
		player.inventoryMenu.slotsChanged(player.getInventory());
	}

	public Component getWinMessage(ServerLevel level) {
		Player winner = this.getPlayerRef().getEntity(level);
		if (winner == null) {
			return Component.literal("The winner is offline!").withStyle(ChatFormatting.GOLD);
		}

		return winner.getDisplayName().copy()
			.append(" has won the game with a territory of " + this.size + " blocks!")
			.withStyle(ChatFormatting.GOLD);
	}

	private String getSidebarEntryName(ServerLevel level) {
		Player player = this.getPlayerRef().getEntity(level);
		return player == null ? "<Unknown>" : player.getScoreboardName();
	}

	public Component getSidebarEntryText(ServerLevel level) {
		return Component.literal(this.getSidebarEntryName(level)).setStyle(TerritoryBattleSidebar.NAME_STYLE);
	}

	public NumberFormat getSidebarNumberFormat() {
		Component text = Component.literal(this.size + "").setStyle(TerritoryBattleSidebar.NUMBER_STYLE);
		return new FixedFormat(text);
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