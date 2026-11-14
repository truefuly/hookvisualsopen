/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1792
 */
package hook.module.setting;

import hook.module.setting.Setting;
import net.minecraft.class_1792;

public class ItemSetting
extends Setting {
    private final class_1792 defaultValue;
    private class_1792 value;
    private java.util.function.Predicate<class_1792> filter;

    public ItemSetting(CharSequence name, class_1792 value) {
        super(name);
        this.value = value;
        this.defaultValue = value;
    }

    /** Limits the picker to block items (shown with their 3D block icon). */
    public ItemSetting onlyBlocks() {
        this.filter = item -> item instanceof net.minecraft.class_1747;
        return this;
    }

    /** Whether the picker may offer this item. */
    public boolean accepts(Object item) {
        return this.filter == null || item instanceof class_1792 i && this.filter.test(i);
    }

    public boolean blocksOnly() {
        return this.filter != null;
    }

    public class_1792 getItem() {
        return this.value;
    }

    public void setItem(class_1792 a) {
        this.value = a;
    }

    public class_1792 getDefaultValue() {
        return this.defaultValue;
    }

    public void resetValue() {
        this.value = this.defaultValue;
    }
}

