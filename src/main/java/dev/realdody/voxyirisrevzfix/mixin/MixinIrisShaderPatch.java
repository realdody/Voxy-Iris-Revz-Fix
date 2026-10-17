package dev.realdody.voxyirisrevzfix.mixin;

import dev.realdody.voxyirisrevzfix.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Voxy injects shader pack glsl (voxy_opaque.glsl / voxy_translucent.glsl) into its own shaders
 * after Iris has transformed the pack. Iris' depth transformer never sees that code, so it has to
 * be transformed here the same way: depth texture reads inverted, gl_FragCoord reads and
 * gl_FragDepth writes converted. Iris exposes TransformPatcher.patchDepth() for exactly this.
 */
@Mixin(targets = "me.cortex.voxy.client.iris.IrisShaderPatch", remap = false)
public abstract class MixinIrisShaderPatch {
	@Inject(method = "getPatchOpaqueSource", at = @At("RETURN"), cancellable = true, remap = false)
	private void voxyirisrevzfix$fixOpaquePatch(CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(IrisCompat.fixPatchSource(cir.getReturnValue()));
	}

	@Inject(method = "getPatchTranslucentSource", at = @At("RETURN"), cancellable = true, remap = false)
	private void voxyirisrevzfix$fixTranslucentPatch(CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(IrisCompat.fixPatchSource(cir.getReturnValue()));
	}
}
