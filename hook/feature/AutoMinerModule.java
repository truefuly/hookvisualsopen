package hook.feature;

import hook.event.EventListener;
import hook.event.events.TickEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ItemSetting;
import hook.module.setting.NumberSetting;
import hook.utils.EncryptedString;
import net.minecraft.class_1268;
import net.minecraft.class_1657;
import net.minecraft.class_1713;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2338;
import net.minecraft.class_239;
import net.minecraft.class_3965;

/**
 * Places the chosen block from your offhand where you look and mines it again with the tool in your main hand,
 * over and over. Keeps the offhand stocked from your inventory and pauses while Anti Break protects your tool.
 */
public final class AutoMinerModule extends Module {
    private final ItemSetting block = new ItemSetting(EncryptedString.of("Block"), class_1802.field_20412).onlyBlocks();
    private final NumberSetting placeDelay = new NumberSetting(EncryptedString.of("Place Delay"), 0.0, 20.0, 1.0, 1.0).getValue(EncryptedString.of("Ticks to wait after a block broke before placing the next one"));
    private final BooleanSetting restock = new BooleanSetting(EncryptedString.of("Auto Restock"), true).setDescription(EncryptedString.of("Moves the block from your inventory into the offhand when it runs out"));
    private final BooleanSetting place = new BooleanSetting(EncryptedString.of("Auto Place"), true).setDescription(EncryptedString.of("Places the block from your offhand where you look"));
    private final BooleanSetting mine = new BooleanSetting(EncryptedString.of("Auto Mine"), true).setDescription(EncryptedString.of("Mines the block with the tool in your main hand"));

    private int waitTicks;
    private boolean warnedEmpty;

    public AutoMinerModule() {
        super(EncryptedString.of("Auto Miner"), EncryptedString.of("Places your chosen block from the offhand and mines it again and again"), -1, Category.MISC);
        this.addSettings(this.block, this.placeDelay, this.restock, this.place, this.mine);
    }

    @Override
    public void onEnable() {
        this.waitTicks = 0;
        this.warnedEmpty = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (this.mc.field_1761 != null) this.mc.field_1761.method_2925();
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1761 == null || this.mc.field_1755 != null) return;
        class_1792 wanted = this.block.getItem();
        if (!(wanted instanceof class_1747 blockItem)) return;

        if (this.restock.getValue()) this.restockOffhand(wanted);
        // Anti Break compatibility: never place or mine while the tool is protected
        if (AntiBreakModule.blocksMining()) {
            this.mc.field_1761.method_2925();
            return;
        }
        if (!(this.mc.field_1765 instanceof class_3965 hit) || hit.method_17783() != class_239.class_240.field_1332) return;
        class_2338 pos = hit.method_17777();
        boolean chosen = this.mc.field_1687.method_8320(pos).method_26204() == blockItem.method_7711();

        if (chosen) {
            if (!this.mine.getValue()) return;
            this.mc.field_1761.method_2902(pos, hit.method_17780());
            this.mc.field_1724.method_6104(class_1268.field_5808);
            this.waitTicks = this.placeDelay.getIntValue();
            return;
        }
        if (!this.place.getValue()) return;
        if (this.waitTicks > 0) {
            this.waitTicks--;
            return;
        }
        class_1799 off = this.mc.field_1724.method_6079();
        if (off.method_7909() != wanted) return;
        if (this.mc.field_1761.method_2896(this.mc.field_1724, class_1268.field_5810, hit).method_23665()) {
            this.mc.field_1724.method_6104(class_1268.field_5810);
        }
    }

    private void restockOffhand(class_1792 wanted) {
        class_1799 off = this.mc.field_1724.method_6079();
        if (off.method_7909() == wanted) {
            this.warnedEmpty = false;
            return;
        }
        // only swap when the offhand is free or holds something else and no container is open
        if (this.mc.field_1724.field_7512 != this.mc.field_1724.field_7498) return;
        for (int slot = 0; slot < 36; slot++) {
            if (this.mc.field_1724.method_31548().method_5438(slot).method_7909() != wanted) continue;
            int screenSlot = slot < 9 ? slot + 36 : slot;
            this.mc.field_1761.method_2906(this.mc.field_1724.field_7498.field_7763, screenSlot, 40, class_1713.field_7791, (class_1657)this.mc.field_1724);
            this.warnedEmpty = false;
            return;
        }
        if (!this.warnedEmpty) {
            this.warnedEmpty = true;
            this.mc.field_1724.method_7353(net.minecraft.class_2561.method_43470("§e[Auto Miner] §fNo more " + wanted.method_63680().getString() + " in your inventory"), true);
        }
    }
}
