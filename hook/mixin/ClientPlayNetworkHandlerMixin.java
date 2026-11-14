/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1937
 *  net.minecraft.class_2604
 *  net.minecraft.class_2663
 *  net.minecraft.class_2666
 *  net.minecraft.class_2672
 *  net.minecraft.class_2676
 *  net.minecraft.class_310
 *  net.minecraft.class_634
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package hook.mixin;

import hook.event.events.ChunkDataEvent;
import hook.event.events.EntitySpawnEvent;
import hook.event.events.LightUpdateEvent;
import hook.event.events.TotemPopEvent;
import hook.manager.EventManager;
import net.minecraft.class_1937;
import net.minecraft.class_2604;
import net.minecraft.class_2663;
import net.minecraft.class_2672;
import net.minecraft.class_2676;
import net.minecraft.class_310;
import net.minecraft.class_634;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_634.class})
public abstract class ClientPlayNetworkHandlerMixin {
    @Inject(method={"method_11148"}, at={@At(value="TAIL")}, remap=false)
    private void onEntityStatus(class_2663 var1, CallbackInfo var2) {
        class_310 var3 = class_310.method_1551();
        if (var3.field_1687 != null && var1.method_11470() == 35 && var1.method_11469((class_1937)var3.field_1687) == var3.field_1724) {
            EventManager.b(new TotemPopEvent());
        }
    }

    @Inject(method={"method_11128"}, at={@At(value="TAIL")}, remap=false)
    private void onChunkData(class_2672 var1, CallbackInfo var2) {
        EventManager.b(new ChunkDataEvent(var1));
    }

    @Inject(method={"method_11143"}, at={@At(value="TAIL")}, remap=false)
    private void onLightUpdate(class_2676 var1, CallbackInfo var2) {
        EventManager.b(new LightUpdateEvent(var1));
    }

    @Inject(method={"method_11112"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void onEntitySpawn(class_2604 var1, CallbackInfo var2) {
        EntitySpawnEvent var3 = new EntitySpawnEvent(var1);
        EventManager.b(var3);
        if (var3.isCancelled()) {
            var2.cancel();
        }
    }
}

