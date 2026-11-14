package hook.feature;

import hook.Hook;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.NumberSetting;
import hook.utils.EncryptedString;
import net.minecraft.class_1268;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2561;
import net.minecraft.class_9334;

/** Stops you from mining with a tool once its durability drops to the set percentage, so it never breaks. */
public final class AntiBreakModule extends Module {
    private static AntiBreakModule instance;

    private final NumberSetting minDurability = new NumberSetting(EncryptedString.of("Min Durability"), 1.0, 50.0, 5.0, 1.0).getValue(EncryptedString.of("Mining stops when the tool in your hand has this percent of durability or less left"));
    private final BooleanSetting warn = new BooleanSetting(EncryptedString.of("Warning"), true).setDescription(EncryptedString.of("Shows a message above the hotbar when mining is blocked"));

    private long lastWarn;

    public AntiBreakModule() {
        super(EncryptedString.of("Anti Break"), EncryptedString.of("Stops mining when your tool is about to break"), -1, Category.MISC);
        this.addSettings(this.minDurability, this.warn);
        instance = this;
    }

    /** Whether this stack is a mining tool at or below the durability limit (only while the module is enabled). */
    public static boolean protects(class_1799 stack) {
        AntiBreakModule m = instance;
        if (m == null || !m.isEnabled() || stack == null || stack.method_7960()) return false;
        if (!stack.method_7963()) return false;
        boolean tool = stack.method_57826(class_9334.field_50077) || stack.method_7909() == class_1802.field_8868;
        if (!tool) return false;
        int max = stack.method_7936();
        if (max <= 0) return false;
        double left = (max - stack.method_7919()) * 100.0 / max;
        return left <= m.minDurability.getValue();
    }

    /** Whether mining with the main hand is blocked right now; shows the warning when it is. */
    public static boolean blocksMining() {
        if (Hook.mc == null || Hook.mc.field_1724 == null) return false;
        class_1799 stack = Hook.mc.field_1724.method_5998(class_1268.field_5808);
        if (!protects(stack)) return false;
        AntiBreakModule m = instance;
        long now = System.currentTimeMillis();
        if (m.warn.getValue() && now - m.lastWarn > 1500L) {
            m.lastWarn = now;
            int max = stack.method_7936();
            int left = (int)Math.ceil((max - stack.method_7919()) * 100.0 / max);
            Hook.mc.field_1724.method_7353(class_2561.method_43470("§c[Anti Break] §fTool at " + left + "% - mining stopped"), true);
        }
        return true;
    }
}
