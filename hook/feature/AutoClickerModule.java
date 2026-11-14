package hook.feature;

import hook.event.EventListener;
import hook.event.events.TickEvent;
import hook.mixin.MinecraftClientAccessor;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ModeSetting;
import hook.module.setting.NumberSetting;
import hook.module.setting.SettingVisibility;
import hook.utils.EncryptedString;
import net.minecraft.class_304;

/**
 * Automatically clicks for you (same options as Meteor's Auto Clicker). Instead of a tick delay you set how many
 * clicks per second or per minute it should do.
 */
public final class AutoClickerModule extends Module {
    public enum ClickMode { Disabled, Hold, Press }
    public enum Per { Second, Minute }

    private static final int MAX_CLICKS_PER_TICK = 5;

    private final BooleanSetting inScreens = new BooleanSetting(EncryptedString.of("In Screens"), true).setDescription(EncryptedString.of("Whether to keep clicking while a screen (chest, inventory, chat) is open"));
    private final ModeSetting<ClickMode> leftMode = new ModeSetting<ClickMode>(EncryptedString.of("Left Click Mode"), ClickMode.Press, ClickMode.class).setDescription(EncryptedString.of("Hold keeps left click held down, Press clicks at the set rate"));
    private final NumberSetting leftClicks = new NumberSetting(EncryptedString.of("Left Clicks"), 1.0, 1200.0, 10.0, 1.0).getValue(EncryptedString.of("How many left clicks per second or minute"));
    private final ModeSetting<Per> leftPer = new ModeSetting<Per>(EncryptedString.of("Left Clicks Per"), Per.Second, Per.class).setDescription(EncryptedString.of("Whether Left Clicks counts per second or per minute"));
    private final ModeSetting<ClickMode> rightMode = new ModeSetting<ClickMode>(EncryptedString.of("Right Click Mode"), ClickMode.Disabled, ClickMode.class).setDescription(EncryptedString.of("Hold keeps right click held down, Press clicks at the set rate"));
    private final NumberSetting rightClicks = new NumberSetting(EncryptedString.of("Right Clicks"), 1.0, 1200.0, 10.0, 1.0).getValue(EncryptedString.of("How many right clicks per second or minute"));
    private final ModeSetting<Per> rightPer = new ModeSetting<Per>(EncryptedString.of("Right Clicks Per"), Per.Second, Per.class).setDescription(EncryptedString.of("Whether Right Clicks counts per second or per minute"));

    private double leftDue;
    private double rightDue;
    private boolean holdingLeft;
    private boolean holdingRight;

    public AutoClickerModule() {
        super(EncryptedString.of("Auto Clicker"), EncryptedString.of("Clicks left and/or right for you at a set clicks per second or minute"), -1, Category.COMBAT);
        this.addSettings(this.inScreens, this.leftMode, this.leftClicks, this.leftPer, this.rightMode, this.rightClicks, this.rightPer);
        SettingVisibility.set(this.leftClicks, () -> this.leftMode.getValue() == ClickMode.Press);
        SettingVisibility.set(this.leftPer, () -> this.leftMode.getValue() == ClickMode.Press);
        SettingVisibility.set(this.rightClicks, () -> this.rightMode.getValue() == ClickMode.Press);
        SettingVisibility.set(this.rightPer, () -> this.rightMode.getValue() == ClickMode.Press);
    }

    @Override
    public void onEnable() {
        this.leftDue = 1.0; // first click right away
        this.rightDue = 1.0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.releaseHolds();
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (this.mc.field_1724 == null || this.mc.field_1690 == null) return;
        if (this.mc.field_1755 != null && !this.inScreens.getValue()) {
            this.releaseHolds();
            return;
        }
        class_304 attack = this.mc.field_1690.field_1886;
        class_304 use = this.mc.field_1690.field_1904;

        if (this.leftMode.getValue() == ClickMode.Hold) {
            attack.method_23481(true);
            this.holdingLeft = true;
        } else {
            if (this.holdingLeft) { attack.method_23481(false); this.holdingLeft = false; }
            if (this.leftMode.getValue() == ClickMode.Press) {
                this.leftDue += perTick(this.leftClicks, this.leftPer);
                for (int i = 0; this.leftDue >= 1.0 && i < MAX_CLICKS_PER_TICK; i++) {
                    this.leftDue -= 1.0;
                    ((MinecraftClientAccessor) this.mc).invokeDoAttack();
                }
                this.leftDue = Math.min(this.leftDue, 1.0);
            }
        }

        if (this.rightMode.getValue() == ClickMode.Hold) {
            use.method_23481(true);
            this.holdingRight = true;
        } else {
            if (this.holdingRight) { use.method_23481(false); this.holdingRight = false; }
            if (this.rightMode.getValue() == ClickMode.Press) {
                this.rightDue += perTick(this.rightClicks, this.rightPer);
                for (int i = 0; this.rightDue >= 1.0 && i < MAX_CLICKS_PER_TICK; i++) {
                    this.rightDue -= 1.0;
                    ((MinecraftClientAccessor) this.mc).invokeDoItemUse();
                }
                this.rightDue = Math.min(this.rightDue, 1.0);
            }
        }
    }

    private static double perTick(NumberSetting clicks, ModeSetting<Per> per) {
        double perSecond = per.getValue() == Per.Minute ? clicks.getValue() / 60.0 : clicks.getValue();
        return perSecond / 20.0;
    }

    private void releaseHolds() {
        if (this.mc.field_1690 == null) return;
        if (this.holdingLeft) this.mc.field_1690.field_1886.method_23481(false);
        if (this.holdingRight) this.mc.field_1690.field_1904.method_23481(false);
        this.holdingLeft = false;
        this.holdingRight = false;
    }
}
