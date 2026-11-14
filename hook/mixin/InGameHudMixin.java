/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_329
 *  net.minecraft.class_332
 *  net.minecraft.class_9779
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package hook.mixin;

import hook.event.events.Render2DEvent;
import hook.manager.EventManager;
import hook.utils.RenderUtils;
import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_329.class})
public class InGameHudMixin {
    @Inject(method={"render"}, at={@At(value="HEAD")})
    private void onRenderHud(class_332 ctx, class_9779 rtc, CallbackInfo ci) {
        RenderUtils.setContext(ctx);
        EventManager.b(new Render2DEvent(ctx, rtc.method_60637(true)));
    }
}

