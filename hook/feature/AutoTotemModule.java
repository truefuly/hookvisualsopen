/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1657
 *  net.minecraft.class_1713
 *  net.minecraft.class_1792
 *  net.minecraft.class_1802
 */
package hook.feature;

import hook.Hook;
import hook.event.EventListener;
import hook.event.events.TickEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.NumberSetting;
import hook.utils.EncryptedString;
import net.minecraft.class_1657;
import net.minecraft.class_1713;
import net.minecraft.class_1792;
import net.minecraft.class_1802;

public final class AutoTotemModule
extends Module {
    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 0.0, 5.0, 1.0, 1.0);
    private int delayCounter;

    public AutoTotemModule() {
        super(EncryptedString.of("Auto Totem"), EncryptedString.of("Keeps a totem of undying in your off hand"), -1, Category.COMBAT);
        this.addSettings(this.delay);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (this.mc.field_1724 == null) {
            return;
        }
        // with a chest/container open the slot ids belong to that container - wait until it is closed
        if (this.mc.field_1724.field_7512 != this.mc.field_1724.field_7498) {
            return;
        }
        if (this.mc.field_1724.method_31548().method_5438(40).method_7909() == class_1802.field_8288) {
            this.delayCounter = this.delay.getIntValue();
        } else if (this.delayCounter > 0) {
            --this.delayCounter;
        } else {
            int slot = this.findItemSlot(class_1802.field_8288);
            if (slot != -1) {
                this.mc.field_1761.method_2906(this.mc.field_1724.field_7512.field_7763, AutoTotemModule.convertSlotIndex(slot), 40, class_1713.field_7791, (class_1657)this.mc.field_1724);
                this.delayCounter = this.delay.getIntValue();
            }
        }
    }

    public int findItemSlot(class_1792 item) {
        if (this.mc.field_1724 == null) {
            return -1;
        }
        for (int slot = 0; slot < 36; ++slot) {
            if (!this.mc.field_1724.method_31548().method_5438(slot).method_31574(item)) continue;
            return slot;
        }
        return -1;
    }

    private static int convertSlotIndex(int slot) {
        return slot < 9 ? slot + 36 : slot;
    }
}

