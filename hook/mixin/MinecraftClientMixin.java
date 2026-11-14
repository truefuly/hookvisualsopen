/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1041
 *  net.minecraft.class_310
 *  net.minecraft.class_437
 *  net.minecraft.class_638
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package hook.mixin;

import hook.Hook;
import hook.event.events.AttackEvent;
import hook.event.events.BlockBreakingEvent;
import hook.event.events.PostItemUseEvent;
import hook.event.events.PreItemUseEvent;
import hook.event.events.ResolutionChangedEvent;
import hook.event.events.SetScreenEvent;
import hook.event.events.TickEvent;
import hook.manager.EventManager;
import net.minecraft.class_1041;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_638;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={class_310.class})
public class MinecraftClientMixin {
    @Shadow
    @Nullable
    public class_638 field_1687;
    @Shadow
    @Final
    private class_1041 field_1704;
    @Shadow
    private int field_1752;

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void onTick(CallbackInfo ci) {
        hook.feature.SwordcoreModuleSupport.onClientTick((class_310)(Object)this);
        if (this.field_1687 != null) {
            EventManager.b(new TickEvent());
        }
    }

    @Inject(method={"onResolutionChanged"}, at={@At(value="HEAD")})
    private void onResolutionChanged(CallbackInfo ci) {
        EventManager.b(new ResolutionChangedEvent(this.field_1704));
    }

    @Inject(method={"doItemUse"}, at={@At(value="RETURN")}, cancellable=true)
    private void onItemUseReturn(CallbackInfo ci) {
        PostItemUseEvent event = new PostItemUseEvent(this.field_1752);
        EventManager.b(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
        this.field_1752 = event.cooldown;
    }

    @Inject(method={"doItemUse"}, at={@At(value="HEAD")}, cancellable=true)
    private void onItemUseHead(CallbackInfo ci) {
        PreItemUseEvent event = new PreItemUseEvent(this.field_1752);
        EventManager.b(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
        this.field_1752 = event.cooldown;
    }

    @Inject(method={"doAttack"}, at={@At(value="HEAD")}, cancellable=true)
    private void onAttack(CallbackInfoReturnable<Boolean> cir) {
        AttackEvent event = new AttackEvent();
        EventManager.b(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method={"handleBlockBreaking"}, at={@At(value="HEAD")}, cancellable=true)
    private void onBlockBreaking(boolean breaking, CallbackInfo ci) {
        BlockBreakingEvent event = new BlockBreakingEvent();
        EventManager.b(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"setScreen"}, at={@At(value="HEAD")}, cancellable=true)
    private void onSetScreen(class_437 screen, CallbackInfo ci) {
        if (screen != null && (screen.getClass().getName().equals("com.spotifyclient.gui.click.ClickGuiScreen")
                || screen.getClass().getName().equals("com.spotifyclient.gui.click.TutorialScreen")
                || screen.getClass().getName().equals("thetruetrident.swordcore.SwordcoreConfigScreen"))) {
            ci.cancel();
            return;
        }
        SetScreenEvent event = new SetScreenEvent(screen);
        EventManager.b(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method={"stop"}, at={@At(value="HEAD")})
    private void onClose(CallbackInfo callbackInfo) {
        Hook.INSTANCE.getSettingsManager().shutdown();
    }
}

