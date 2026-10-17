package dev.realdody.voxyirisrevzfix;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Keeps the mixins from being applied at all when they cannot do anything useful (Iris older
 * than 1.11.4 has no depth transformer). This mirrors the version gating other Voxy fix addons
 * use so the mod degrades to a no-op instead of touching Voxy on unsupported setups.
 */
public class MixinGatingPlugin implements IMixinConfigPlugin {
	private static final boolean ACTIVE = detectActive();

	private static boolean detectActive() {
		Version irisVersion = FabricLoader.getInstance()
			.getModContainer("iris")
			.map(container -> container.getMetadata().getVersion())
			.orElse(null);
		if (irisVersion == null) {
			return false;
		}
		try {
			return irisVersion.compareTo(Version.parse("1.11.4")) >= 0;
		} catch (VersionParsingException e) {
			return false;
		}
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return ACTIVE;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
