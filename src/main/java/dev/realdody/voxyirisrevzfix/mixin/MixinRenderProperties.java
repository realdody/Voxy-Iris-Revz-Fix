package dev.realdody.voxyirisrevzfix.mixin;

import dev.realdody.voxyirisrevzfix.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Voxy decides whether it renders in reverse z or forward z in
 * {@code RenderProperties.useReverseZ()}, where it forces forward z whenever a shader pack is
 * enabled. That was correct for Iris before 1.11.4, which switched the world to forward z under
 * shaders, but since 1.11.4 the Iris depth transformer keeps the framebuffer in the native
 * reverse z layout and only translates what the pack sees. So when the depth transformer is
 * present, use the native state again.
 */
@Mixin(targets = "me.cortex.voxy.client.core.RenderProperties", remap = false)
public abstract class MixinRenderProperties {
	private static boolean logged;

	@Inject(method = "useReverseZ", at = @At("RETURN"), cancellable = true, remap = false)
	private static void voxyirisrevzfix$useNativeReverseZ(CallbackInfoReturnable<Boolean> cir) {
		if (IrisCompat.hasDepthTransformer()) {
			boolean reverseZ = IrisCompat.nativeReverseZ();
			cir.setReturnValue(reverseZ);
			if (!logged) {
				logged = true;
				org.slf4j.LoggerFactory.getLogger("voxy-iris-revz-fix")
					.info("Overriding voxy useReverseZ() -> {}", reverseZ);
			}
		}
	}
}
