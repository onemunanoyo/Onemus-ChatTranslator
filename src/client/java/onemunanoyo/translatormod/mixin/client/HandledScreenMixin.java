package onemunanoyo.translatormod.mixin.client;

import onemunanoyo.translatormod.TranslationHandler;
import onemunanoyo.translatormod.config.TranslatorConfig;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import onemunanoyo.translatormod.util.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {

	@Inject(
			method = "getTooltipFromContainerItem",
			at = @At("RETURN"),
			cancellable = true
	)
	private void translateTooltip(ItemStack stack, CallbackInfoReturnable<List<Component>> cir) {
		TranslatorConfig config = TranslatorConfig.getInstance();
		if (!config.enabled || !config.translateTooltips) return;
		if (config.resolveTargetLang() == null) return;

		List<Component> original = cir.getReturnValue();
		if (original == null || original.isEmpty()) return;

		List<Component> result = new ArrayList<>(original);
		for (int i = 0; i < result.size(); i++) {
			String text = result.get(i).getString();
			if (text.isBlank()) continue;
			String cached = TranslationHandler.getCachedTranslation(text);
			if (cached != null && !cached.equals(text)) {
				result.set(i, I18n.translate("translatormod.chat.tooltip_translated", text, cached));
			}
		}
		cir.setReturnValue(result);
	}
}