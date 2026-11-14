package hook.mixin;

import hook.feature.ForceRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium only searches for chunks to draw within render distance around the camera. In Freecam the camera is far
 * from the player, but the server sent chunks around the player, so the far side of the loaded area stayed empty
 * until you flew there. Force Render widens the search so every loaded chunk is reachable from the camera.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager", remap = false)
public abstract class ForceRenderSodiumMixin {
    @Inject(method = "getSearchDistance", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void hook$forceRenderSearchDistance(CallbackInfoReturnable<Float> cir) {
        float wanted = ForceRenderModule.searchDistance(cir.getReturnValueF());
        if (wanted > cir.getReturnValueF()) cir.setReturnValue(wanted);
    }
}
