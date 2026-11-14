package hook.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import hook.feature.NoFogModule;
import net.minecraft.class_758;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * No Fog "Dark Sky & Void": vanilla fades the fog/sky colour to black near and below the world bottom. The only
 * clamp in the fog colour method computes that darkness, so returning 0 there keeps the normal colour everywhere.
 */
@Mixin(class_758.class)
public abstract class NoFogVoidMixin {
    @WrapOperation(method = "method_62185(Lnet/minecraft/class_4184;FLnet/minecraft/class_638;IF)Lorg/joml/Vector4f;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_3532;method_15363(FFF)F", remap = false), remap = false)
    private float hook$noVoidDarkness(float value, float min, float max, Operation<Float> original) {
        if (NoFogModule.removesDarkSky()) return 0.0f;
        return original.call(value, min, max);
    }
}
