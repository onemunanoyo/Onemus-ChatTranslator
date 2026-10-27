package onemunanoyo.translatormod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * このmodは純粋なクライアントonly mod。
 * neoforge.mods.toml で side = "CLIENT" を宣言しているため、
 * 専用サーバーには通常インストールされない。
 */
@Mod(TranslatorMod.MOD_ID)
public class TranslatorMod {
	public static final String MOD_ID = "translatormod";

	public TranslatorMod(IEventBus modEventBus, ModContainer modContainer) {
		TranslatorModClient.init(modEventBus);
	}
}
