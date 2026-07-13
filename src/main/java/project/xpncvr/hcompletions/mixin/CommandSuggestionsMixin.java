package project.xpncvr.hcompletions.mixin;

import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions.SuggestionsList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jspecify.annotations.Nullable;

@Mixin(net.minecraft.client.gui.components.CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
	private static final int HC_GAP = 4;
	private static final int HC_PAD = 4;
	private static final int HC_ROW_HEIGHT = 12;
	private static final int HC_CHEVRON = 6;

	@Shadow @Final private EditBox input;
	@Shadow @Final private Font font;
	@Shadow @Final private Screen screen;
	@Shadow @Final private boolean anchorToBottom;
	@Shadow @Final private int fillColor;
	@Shadow @Nullable private SuggestionsList suggestions;

	@Inject(method = "extractSuggestions", at = @At("HEAD"), cancellable = true)
	private void hc$horizontalRender(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final CallbackInfoReturnable<Boolean> cir) {
		if (this.suggestions != null) {
			this.hc$render(this.suggestions, graphics, mouseX, mouseY);
			cir.setReturnValue(true);
		}
	}

	@Redirect(
		method = "mouseClicked",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/components/CommandSuggestions$SuggestionsList;mouseClicked(II)Z"
		)
	)
	private boolean hc$horizontalClick(final SuggestionsList instance, final int x, final int y) {
		return this.hc$click(instance, x, y);
	}

	private int hc$rowTop() {
		return this.anchorToBottom ? this.screen.height - 27 : 72;
	}

	private int hc$startX() {
		return this.input.getScreenX(0);
	}

	private int hc$available() {
		return this.input.getInnerWidth() - 2 * HC_CHEVRON - HC_PAD;
	}

	private int hc$entryWidth(final String text) {
		return this.font.width(text) + HC_PAD;
	}

	private int[] hc$window(final List<Suggestion> list, final int current, final int available) {
		int n = list.size();
		int first = current;
		int last = current;
		int used = this.hc$entryWidth(list.get(current).getText());

		while (true) {
			boolean added = false;
			if (last < n - 1) {
				int w = HC_GAP + this.hc$entryWidth(list.get(last + 1).getText());
				if (used + w <= available) {
					used += w;
					last++;
					added = true;
				}
			}

			if (first > 0) {
				int w = HC_GAP + this.hc$entryWidth(list.get(first - 1).getText());
				if (used + w <= available) {
					used += w;
					first--;
					added = true;
				}
			}

			if (!added) {
				break;
			}
		}

		return new int[]{first, last};
	}

	private void hc$render(final SuggestionsList list, final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
		SuggestionsListAccessor access = (SuggestionsListAccessor)list;
		List<Suggestion> entries = access.hc$getSuggestionList();
		int n = entries.size();
		int rowTop = this.hc$rowTop();
		int startX = this.hc$startX();
		int[] window = this.hc$window(entries, access.hc$getCurrent(), this.hc$available());
		int first = window[0];
		int last = window[1];

		Vec2 previousMouse = access.hc$getLastMouse();
		boolean mouseMoved = previousMouse.x != mouseX || previousMouse.y != mouseY;
		if (mouseMoved) {
			access.hc$setLastMouse(new Vec2(mouseX, mouseY));
		}

		int x = startX;
		if (first > 0) {
			graphics.text(this.font, "<", x, rowTop + 2, -1);
			x += HC_CHEVRON;
		}

		for (int i = first; i <= last; i++) {
			Suggestion suggestion = entries.get(i);
			String text = suggestion.getText();
			int width = this.hc$entryWidth(text);
			graphics.fill(x, rowTop, x + width, rowTop + HC_ROW_HEIGHT, this.fillColor);

			boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= rowTop && mouseY < rowTop + HC_ROW_HEIGHT;
			if (hovered && mouseMoved) {
				list.select(i);
			}

			int color = i == access.hc$getCurrent() ? -256 : -5592406;
			graphics.text(this.font, text, x + 2, rowTop + 2, color);
			x += width + HC_GAP;
		}

		if (last < n - 1) {
			graphics.text(this.font, ">", x, rowTop + 2, -1);
			x += HC_CHEVRON;
		}

		int current = access.hc$getCurrent();
		if (current >= first && current <= last) {
			Suggestion selected = entries.get(current);
			if (mouseX >= startX && mouseX < x && mouseY >= rowTop && mouseY < rowTop + HC_ROW_HEIGHT) {
				Message tooltip = selected.getTooltip();
				if (tooltip != null) {
					graphics.setTooltipForNextFrame(this.font, ComponentUtils.fromMessage(tooltip), mouseX, mouseY);
				}
			}
		}

		if (mouseX >= startX && mouseX < x && mouseY >= rowTop && mouseY < rowTop + HC_ROW_HEIGHT) {
			graphics.requestCursor(CursorTypes.POINTING_HAND);
		}
	}

	private boolean hc$click(final SuggestionsList list, final int mouseX, final int mouseY) {
		SuggestionsListAccessor access = (SuggestionsListAccessor)list;
		int rowTop = this.hc$rowTop();
		if (mouseY < rowTop || mouseY >= rowTop + HC_ROW_HEIGHT) {
			return false;
		}

		List<Suggestion> entries = access.hc$getSuggestionList();
		int[] window = this.hc$window(entries, access.hc$getCurrent(), this.hc$available());
		int first = window[0];
		int last = window[1];

		int x = this.hc$startX();
		if (first > 0) {
			x += HC_CHEVRON;
		}

		for (int i = first; i <= last; i++) {
			int width = this.hc$entryWidth(entries.get(i).getText());
			if (mouseX >= x && mouseX < x + width) {
				list.select(i);
				list.useSuggestion();
				return true;
			}

			x += width + HC_GAP;
		}

		return mouseX >= this.hc$startX() && mouseX < x;
	}
}
