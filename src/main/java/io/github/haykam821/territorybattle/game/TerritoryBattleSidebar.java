package io.github.haykam821.territorybattle.game;

import java.util.Comparator;

import io.github.haykam821.territorybattle.game.phase.TerritoryBattleActivePhase;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

public class TerritoryBattleSidebar {
	protected static final Style NAME_STYLE = Style.EMPTY
		.withColor(ChatFormatting.DARK_GRAY)
		.withBold(true);

	protected static final Style NUMBER_STYLE = Style.EMPTY
		.withColor(ChatFormatting.GOLD)
		.withBold(true);

	private final SidebarWidget widget;
	private final TerritoryBattleActivePhase phase;

	public TerritoryBattleSidebar(GlobalWidgets widgets, TerritoryBattleActivePhase phase, Component title) {
		Component name = title.copy().withStyle(style -> {
			return style.withBold(true);
		});
		this.widget = widgets.addSidebar(name);

		this.phase = phase;
	}

	public void update() {
		this.widget.set(content -> {
			this.phase.getTerritories().stream()
				.sorted(Comparator.reverseOrder())
				.forEach(territory -> {
					content.add(territory.getSidebarEntryText(this.phase.getLevel()), territory.getSidebarNumberFormat());
				});
		});
	}
}
