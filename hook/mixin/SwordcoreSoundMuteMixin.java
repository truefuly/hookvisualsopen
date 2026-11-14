package hook.mixin;

import net.minecraft.class_310;
import net.minecraft.class_3414;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import thetruetrident.swordcore.SwordcoreClient;

/**
 * Swordcore plays a bell/feedback sound whenever the target swings (also for other players' fights) and keeps its
 * own sound toggle forced on. Only the sounds are skipped here; every other Swordcore feature stays as it is.
 */
@Mixin(value = SwordcoreClient.class, remap = false)
public abstract class SwordcoreSoundMuteMixin {
    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hook$muteSound(class_310 client, class_3414 sound, float volume, float pitch, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "playMasterSound", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hook$muteMasterSound(class_310 client, class_3414 sound, float volume, float pitch, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "playKeybindSound", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hook$muteKeybindSound(class_310 client, CallbackInfo ci) {
        ci.cancel();
    }
}
