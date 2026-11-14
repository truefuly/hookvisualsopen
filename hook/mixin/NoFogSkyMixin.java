package hook.mixin;

import hook.feature.NoFogModule;
import net.minecraft.class_638;
import net.minecraft.class_9975;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * No Fog "Dark Sky & Void": vanilla draws a black disc below the horizon whenever the player (not the camera) is
 * below sea level, which in Freecam blacks out everything around the camera.
 */
@Mixin(class_9975.class)
public abstract class NoFogSkyMixin {
    @Inject(method = "method_74925(FLnet/minecraft/class_638;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void hook$noDarkDisc(float tickDelta, class_638 world, CallbackInfoReturnable<Boolean> cir) {
        if (NoFogModule.removesDarkSky()) cir.setReturnValue(false);
    }
}
