package dev.realdody.voxyirisrevzfix;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Access to the Iris API this fix depends on, all resolved reflectively so that the mod simply
 * does nothing on Iris versions without the depth transformer instead of crashing.
 */
public final class IrisCompat {
	private static final Logger LOGGER = LoggerFactory.getLogger("voxy-iris-revz-fix");

	private static final Method PATCH_DEPTH;
	private static final Object SHADER_TYPE_FRAGMENT;
	private static final boolean HAS_DEPTH_TRANSFORMER;

	private static boolean voxyFixChecked;
	private static boolean voxyHasFix;
	private static boolean loggedMatrix;
	private static boolean loggedPatch;

	static {
		Method patchDepth = null;
		Object fragment = null;
		try {
			Class<?> patcher = Class.forName("net.irisshaders.iris.pipeline.transform.TransformPatcher");
			Class<?> shaderType = Class.forName("net.irisshaders.iris.gl.shader.ShaderType");
			for (Object constant : shaderType.getEnumConstants()) {
				if (((Enum<?>) constant).name().equals("FRAGMENT")) {
					fragment = constant;
				}
			}
			if (fragment != null) {
				patchDepth = patcher.getMethod("patchDepth", String.class, shaderType);
			}
		} catch (Throwable t) {
			// Old iris (or no iris installed): the depth transformer does not exist.
			LOGGER.info("Iris depth transformer not found, fix inactive", t);
		}
		PATCH_DEPTH = patchDepth;
		SHADER_TYPE_FRAGMENT = fragment;
		HAS_DEPTH_TRANSFORMER = patchDepth != null && fragment != null;
		LOGGER.info("Iris depth transformer available: {}", HAS_DEPTH_TRANSFORMER);
	}

	private IrisCompat() {
	}

	/**
	 * True when the running Iris has the depth transformer (Iris 1.11.4+), which is when Voxy's
	 * depth conventions have to be corrected.
	 */
	public static boolean hasDepthTransformer() {
		return HAS_DEPTH_TRANSFORMER;
	}

	/**
	 * Mirrors Voxy's own check: does the vanilla depth state use reverse z (greater-or-equal
	 * compare with a clear value of 0)?
	 */
	public static boolean nativeReverseZ() {
		return DepthStencilState.DEFAULT.depthTest().equals(CompareOp.GREATER_THAN_OR_EQUAL);
	}

	/**
	 * Detects whether the installed Voxy already carries its own reverse-z compatibility fix so
	 * this mod backs off instead of applying the correction twice.
	 */
	public static boolean voxyHasFix() {
		if (!voxyFixChecked) {
			voxyFixChecked = true;
			String fixedIn = fixedIn("me.cortex.voxy.client.iris.VoxyUniforms");
			if (fixedIn == null) {
				fixedIn = fixedIn("me.cortex.voxy.client.iris.IrisShaderPatch");
			}
			voxyHasFix = fixedIn != null;
			if (voxyHasFix) {
				LOGGER.info("Voxy already contains the reverse-z fix ({}), addon inactive", fixedIn);
			}
		}
		return voxyHasFix;
	}

	private static String fixedIn(String className) {
		try {
			Class<?> cls = Class.forName(className, false, IrisCompat.class.getClassLoader());
			for (Method method : cls.getDeclaredMethods()) {
				String name = method.getName();
				// Mixin generated handlers/accessors are injected into the target class. Our own
				// ones contain the mod id/package, which would otherwise match the "revz" marker
				// and make the addon think Voxy ships the fix.
				if (method.isSynthetic() || method.isBridge()
					|| name.startsWith("handler$") || name.startsWith("accessor$")
					|| name.contains("voxy-iris-revz-fix") || name.contains("voxyirisrevzfix")) {
					continue;
				}
				String lower = name.toLowerCase(Locale.ROOT);
				if (lower.contains("revz")
					|| lower.contains("patchdepth")
					|| lower.contains("applyirisdepthtransformer")
					|| lower.contains("stupidevilhack")) {
					return className + "#" + name;
				}
			}
		} catch (Throwable ignored) {
		}
		return null;
	}

	/**
	 * Converts one of Voxy's reverse-z matrices into the undo-rev-z (forward z) form that shader
	 * packs expect while the Iris depth transformer is active.
	 */
	public static Matrix4f fixPackMatrix(Matrix4f matrix) {
		if (matrix == null || !hasDepthTransformer() || !nativeReverseZ() || voxyHasFix()) {
			return matrix;
		}
		Matrix4f out = new Matrix4f(matrix);
		Vector4f row = out.getRow(2, new Vector4f());
		boolean zeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
		if (zeroToOne) {
			row = row.mul(-2.0f).add(out.getRow(3, new Vector4f()));
		} else {
			row.mul(-1.0f);
		}
		out.setRow(2, row);
		if (!loggedMatrix) {
			loggedMatrix = true;
			LOGGER.info("Applying reverse-z to shader pack matrices (zeroToOne={}): row2 {} -> {}",
				zeroToOne, matrix.getRow(2, new Vector4f()), row);
		}
		return out;
	}

	/**
	 * Runs one of Voxy's injected shader patches through Iris' own depth transformer. Voxy's
	 * shaders are compiled as #version 460 core, patchDepth needs a full shader, so a version
	 * directive is added for the transform and removed again afterwards.
	 */
	public static String fixPatchSource(String source) {
		if (source == null || !hasDepthTransformer() || voxyHasFix()) {
			return source;
		}
		try {
			String transformed = (String) PATCH_DEPTH.invoke(null, "#version 460 core\n" + source, SHADER_TYPE_FRAGMENT);
			if (!loggedPatch) {
				loggedPatch = true;
				LOGGER.info("Transformed voxy shader patch with iris depth transformer ({} -> {} chars)", source.length(), transformed.length());
			}
			return transformed.replaceFirst("\\A\\s*#version[^\\n]*\\n", "");
		} catch (Throwable t) {
			LOGGER.error("Failed to apply the iris depth transformer to a voxy shader patch, leaving it as-is", t);
			return source;
		}
	}
}
