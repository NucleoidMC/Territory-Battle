package io.github.haykam821.territorybattle.game.map;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public final class TerritoryBattleGuideText {
	private static final Component TITLE = Component.translatable("gameType.territorybattle.territory_battle").withStyle(ChatFormatting.BOLD);

	private static final Component TEXT = Component.empty()
			.append(TITLE)
			.append(CommonComponents.NEW_LINE)
			.append("Run over blocks to claim them as part of your territory.")
			.append(CommonComponents.NEW_LINE)
			.append("Once a block is claimed, it cannot be claimed by another player.")
			.append(CommonComponents.NEW_LINE)
			.append("Claim the most blocks before time runs out!")
			.withStyle(ChatFormatting.GOLD);

	private TerritoryBattleGuideText() {
		return;
	}

	public static ElementHolder createElementHolder() {
		TextDisplayElement element = new TextDisplayElement(TEXT);

		element.setBillboardMode(BillboardConstraints.CENTER);
		element.setLineWidth(350);
		element.setInvisible(true);

		ElementHolder holder = new ElementHolder();
		holder.addElement(element);

		return holder;
	}
}
