package hook.mixin;

import hook.feature.AntiBreakModule;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Anti Break: refuses to start or continue breaking a block while the held tool is about to break. */
@Mixin(class_636.class)
public abstract class AntiBreakMixin {
    @Inject(method = "method_2910(Lnet/minecraft/class_2338;Lnet/minecraft/class_2350;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void hook$antiBreakStart(class_2338 pos, class_2350 side, CallbackInfoReturnable<Boolean> cir) {
        if (AntiBreakModule.blocksMining()) cir.setReturnValue(false);
    }

    @Inject(method = "method_2902(Lnet/minecraft/class_2338;Lnet/minecraft/class_2350;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private void hook$antiBreakContinue(class_2338 pos, class_2350 side, CallbackInfoReturnable<Boolean> cir) {
        if (AntiBreakModule.blocksMining()) {
            ((class_636)(Object)this).method_2925();
            cir.setReturnValue(false);
        }
    }
}
