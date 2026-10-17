package dev.realdody.voxyirisrevzfix.mixin;

import dev.realdody.voxyirisrevzfix.IrisCompat;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The matrices Voxy exposes to shader packs (vxProj, vxViewProj and their derived inverse and
 * previous variants) are in Voxy's own (reverse z) space, but while the Iris depth transformer is
 * active the pack expects the undo-rev-z (forward z) form, exactly like gbufferProjection. Undo
 * the reverse z on the way out so pack code that decodes vxDepthTex with vxProjInv agrees with
 * the depth values it reads.
 */
@Mixin(targets = "me.cortex.voxy.client.iris.VoxyUniforms", remap = false)
public abstract class MixinVoxyUniforms {
	@Inject(method = "getProjection", at = @At("RETURN"), cancellable = true, remap = false)
	private static void voxyirisrevzfix$fixProjection(CallbackInfoReturnable<Matrix4f> cir) {
		cir.setReturnValue(IrisCompat.fixPackMatrix(cir.getReturnValue()));
	}

	@Inject(method = "getViewProjection", at = @At("RETURN"), cancellable = true, remap = false)
	private static void voxyirisrevzfix$fixViewProjection(CallbackInfoReturnable<Matrix4f> cir) {
		cir.setReturnValue(IrisCompat.fixPackMatrix(cir.getReturnValue()));
	}
}
