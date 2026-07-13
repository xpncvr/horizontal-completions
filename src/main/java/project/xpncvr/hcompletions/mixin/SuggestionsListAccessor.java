package project.xpncvr.hcompletions.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import java.util.List;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.gui.components.CommandSuggestions$SuggestionsList")
public interface SuggestionsListAccessor {
	@Accessor("suggestionList")
	List<Suggestion> hc$getSuggestionList();

	@Accessor("current")
	int hc$getCurrent();

	@Accessor("lastMouse")
	Vec2 hc$getLastMouse();

	@Accessor("lastMouse")
	void hc$setLastMouse(Vec2 lastMouse);
}
