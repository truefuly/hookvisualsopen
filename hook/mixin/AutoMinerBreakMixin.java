package hook.mixin;

import hook.feature.AutoMinerModule;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops vanilla from cancelling Auto Miner's block breaking when the attack key isn't held. */
@Mixin(value = class_310.class, remap = false)
public abstract class AutoMinerBreakMixin {
    @Inject(method = "method_1590(Z)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void hook$autoMinerKeepsBreaking(boolean breaking, CallbackInfo ci) {
        if (!breaking && AutoMinerModule.ownsBreaking()) ci.cancel();
    }
}
