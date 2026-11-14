package hook.feature;

import hook.event.EventListener;
import hook.event.events.PostItemUseEvent;
import hook.event.events.PreItemUseEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.NumberSetting;
import hook.utils.EncryptedString;
import hook.utils.InventoryUtil;
import net.minecraft.class_1268;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;

/**
 * Boosts a thrown ender pearl with a wind charge.
 *
 * A wind charge can't hit a pearl; it only bursts when it hits a block, and the burst pushes every entity within
 * 2.4 blocks away from its centre, harder the closer it is. So right after the pearl leaves your hand (same tick,
 * before it flies out of reach) this searches the directions around you for the block hit whose burst pushes the
 * pearl hardest along its flight path, throws the wind charge there with a silent rotation and switches back.
 */
public final class PearlCatchModule extends Module {
    /** Player-thrown pearls and wind charges both start at 1.5 blocks per tick. */
    private static final double THROW_SPEED = 1.5;
    /** Wind burst radius 1.2; entities up to twice that are pushed with strength (1 - distance / 2.4). */
    private static final double BURST_REACH = 2.4;
    private static final double PEARL_GRAVITY = 0.03;
    private static final double PEARL_DRAG = 0.99;

    private final NumberSetting maxTicks = new NumberSetting(EncryptedString.of("Max Flight Ticks"), 1.0, 6.0, 4.0, 1.0).getValue(EncryptedString.of("How many ticks the wind charge may fly before it has to burst"));
    private final NumberSetting minBoost = new NumberSetting(EncryptedString.of("Min Boost"), 5.0, 100.0, 20.0, 5.0).getValue(EncryptedString.of("Only throws the wind charge when the pearl gets at least this much push (percent of a point blank burst)"));
    private final NumberSetting maxPitch = new NumberSetting(EncryptedString.of("Max Pitch"), -90.0, 90.0, 30.0, 1.0).getValue(EncryptedString.of("Only boosts pearls thrown at least this far up (90 = any direction, negative = only upwards)"));
    private final BooleanSetting forwardOnly = new BooleanSetting(EncryptedString.of("Push Forward Only"), true).setDescription(EncryptedString.of("Only bursts that push the pearl along its flight, never sideways or backwards"));
    private final BooleanSetting silent = new BooleanSetting(EncryptedString.of("Silent Rotation"), true).setDescription(EncryptedString.of("Aims the wind charge without moving your camera"));
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true).setDescription(EncryptedString.of("Returns to the pearl slot right away"));

    private boolean pearlArmed;

    public PearlCatchModule() {
        super(EncryptedString.of("Pearl Catch"), EncryptedString.of("Boosts your thrown ender pearl further with a wind charge burst"), -1, Category.COMBAT);
        this.addSettings(this.maxTicks, this.minBoost, this.maxPitch, this.forwardOnly, this.silent, this.switchBack);
    }

    @Override
    public void onDisable() {
        this.pearlArmed = false;
        super.onDisable();
    }

    @EventListener
    public void onPreItemUse(PreItemUseEvent event) {
        this.pearlArmed = false;
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1755 != null) return;
        class_1799 hand = this.mc.field_1724.method_5998(class_1268.field_5808);
        if (hand.method_7909() != class_1802.field_8634) return;
        if (this.mc.field_1724.method_7357().method_7904(hand)) return; // still on cooldown, nothing gets thrown
        if (this.mc.field_1724.method_36455() > this.maxPitch.getFloatValue()) return;
        this.pearlArmed = true;
    }

    @EventListener
    public void onPostItemUse(PostItemUseEvent event) {
        if (!this.pearlArmed) return;
        this.pearlArmed = false;
        if (this.mc.field_1724 == null || this.mc.field_1687 == null || this.mc.field_1761 == null) return;
        // the pearl was really thrown only if it went on cooldown
        if (!this.mc.field_1724.method_7357().method_7904(new class_1799(class_1802.field_8634))) return;
        int windSlot = this.findWindChargeSlot();
        if (windSlot == -1) return;
        if (this.mc.field_1724.method_7357().method_7904(this.mc.field_1724.method_31548().method_5438(windSlot))) return;

        class_243 eye = this.mc.field_1724.method_33571();
        class_243 pearlPos = new class_243(eye.field_1352, eye.field_1351 - 0.1, eye.field_1350);
        class_243 motion = this.mc.field_1724.method_18798();
        class_243 pearlVel = this.mc.field_1724.method_5828(1.0f).method_1021(THROW_SPEED)
                .method_1031(motion.field_1352, this.mc.field_1724.method_24828() ? 0.0 : motion.field_1351, motion.field_1350);

        float[] aim = this.findBestAim(eye, pearlPos, pearlVel);
        if (aim == null) return;

        int previousSlot = this.mc.field_1724.method_31548().method_67532();
        float yaw = this.mc.field_1724.method_36454();
        float pitch = this.mc.field_1724.method_36455();
        InventoryUtil.swap(windSlot);
        this.mc.field_1724.method_36456(aim[0]);
        this.mc.field_1724.method_36457(aim[1]);
        // the use packet carries this rotation, so the server throws the charge exactly where we aimed
        this.mc.field_1761.method_2919((class_1657)this.mc.field_1724, class_1268.field_5808);
        if (this.silent.getValue()) {
            this.mc.field_1724.method_36456(yaw);
            this.mc.field_1724.method_36457(pitch);
        }
        if (this.switchBack.getValue()) InventoryUtil.swap(previousSlot);
    }

    /** Returns {yaw, pitch} of the throw whose burst pushes the pearl hardest, or null if none is good enough. */
    private float[] findBestAim(class_243 eye, class_243 pearlStart, class_243 pearlVel0) {
        int ticks = this.maxTicks.getIntValue();
        double reach = THROW_SPEED * ticks + 0.5;
        // pearl path, index = ticks after the throw
        class_243[] pearl = new class_243[ticks + 2];
        class_243 p = pearlStart;
        class_243 v = pearlVel0;
        pearl[0] = p;
        for (int t = 1; t < pearl.length; t++) {
            p = p.method_1019(v);
            v = v.method_1031(0.0, -PEARL_GRAVITY, 0.0).method_1021(PEARL_DRAG);
            pearl[t] = p;
        }
        class_243 flight = pearlVel0.method_1029();

        double best = this.minBoost.getValue() / 100.0;
        float[] result = null;
        float baseYaw = this.mc.field_1724.method_36454();
        for (int pitchStep = -30; pitchStep <= 30; pitchStep++) {
            float pitch = pitchStep * 3.0f;
            for (int yawStep = 0; yawStep < 72; yawStep++) {
                float yaw = baseYaw + yawStep * 5.0f;
                class_243 dir = directionOf(yaw, pitch);
                class_243 end = eye.method_1019(dir.method_1021(reach));
                class_3965 hit = this.mc.field_1687.method_17742(new class_3959(eye, end, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, (class_1297)this.mc.field_1724));
                if (hit == null || hit.method_17783() != class_239.class_240.field_1332) continue;
                class_243 at = hit.method_17784();
                double travelled = at.method_1022(eye);
                if (travelled < 0.3) continue;
                // the charge bursts in the tick it reaches the block; the pearl has moved that many ticks too
                int t = Math.max(1, (int)Math.ceil(travelled / THROW_SPEED));
                if (t >= pearl.length) continue;
                class_2350 side = hit.method_17780();
                class_243 burst = at.method_1031(side.method_10148() * 0.25, side.method_10164() * 0.25, side.method_10165() * 0.25);
                double score = this.pushScore(burst, pearl[t], flight);
                if (score > best) {
                    best = score;
                    result = new float[]{class_3532.method_15393(yaw), pitch};
                }
            }
        }
        return result;
    }

    /** How much the burst pushes the pearl along its flight (1 = point blank, straight behind it). */
    private double pushScore(class_243 burst, class_243 pearlPos, class_243 flight) {
        class_243 away = pearlPos.method_1020(burst);
        double distance = away.method_1033();
        if (distance >= BURST_REACH) return 0.0;
        double strength = 1.0 - distance / BURST_REACH;
        if (distance < 1.0E-4) return strength;
        double along = away.method_1021(1.0 / distance).method_1026(flight);
        if (this.forwardOnly.getValue() && along < 0.3) return 0.0;
        return strength * along;
    }

    private static class_243 directionOf(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double x = Math.toRadians(pitch);
        double cos = Math.cos(x);
        return new class_243(-Math.sin(y) * cos, -Math.sin(x), Math.cos(y) * cos);
    }

    private int findWindChargeSlot() {
        for (int slot = 0; slot < 9; ++slot) {
            if (this.mc.field_1724.method_31548().method_5438(slot).method_7909() == class_1802.field_49098) return slot;
        }
        return -1;
    }
}
