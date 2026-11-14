/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1802
 *  net.minecraft.class_1923
 *  net.minecraft.class_2281
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_2350$class_2353
 *  net.minecraft.class_243
 *  net.minecraft.class_2586
 *  net.minecraft.class_2589
 *  net.minecraft.class_2595
 *  net.minecraft.class_2601
 *  net.minecraft.class_2605
 *  net.minecraft.class_2608
 *  net.minecraft.class_2609
 *  net.minecraft.class_2611
 *  net.minecraft.class_2614
 *  net.minecraft.class_2627
 *  net.minecraft.class_2636
 *  net.minecraft.class_2669
 *  net.minecraft.class_2680
 *  net.minecraft.class_2745
 *  net.minecraft.class_2769
 *  net.minecraft.class_2818
 *  net.minecraft.class_3719
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_7716
 *  net.minecraft.class_8172
 *  net.minecraft.class_8887
 */
package hook.feature;

import hook.event.EventListener;
import hook.event.events.Render3DEvent;
import hook.event.events.TickEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.NumberSetting;
import hook.module.setting.StorageBlocksSetting;
import hook.utils.EncryptedString;
import hook.utils.RenderUtils;
import hook.utils.RenderVisibility;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import net.minecraft.class_1802;
import net.minecraft.class_1923;
import net.minecraft.class_2281;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2586;
import net.minecraft.class_2589;
import net.minecraft.class_2595;
import net.minecraft.class_2601;
import net.minecraft.class_2605;
import net.minecraft.class_2608;
import net.minecraft.class_2609;
import net.minecraft.class_2611;
import net.minecraft.class_2614;
import net.minecraft.class_2627;
import net.minecraft.class_2636;
import net.minecraft.class_2669;
import net.minecraft.class_2680;
import net.minecraft.class_2745;
import net.minecraft.class_2769;
import net.minecraft.class_2818;
import net.minecraft.class_3719;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_7716;
import net.minecraft.class_8172;
import net.minecraft.class_8887;

public final class StorageEspModule
extends Module {
    private final hook.utils.EspStyle style = new hook.utils.EspStyle(hook.utils.EspStyle.Shape.Both, 45, 0xFFFFB347);
    private final NumberSetting boxSize = new NumberSetting(EncryptedString.of("Box Size"), 50.0, 100.0, 92.0, 1.0).getValue(EncryptedString.of("Size of the box in percent of the storage block"));
    private final StorageBlocksSetting storages = new StorageBlocksSetting(EncryptedString.of("Storages")).add("chest", "Chest", class_1802.field_8106, true, new Color(156, 91, 0)).add("ender_chest", "Ender Chest", class_1802.field_8466, true, new Color(127, 91, 255)).add("shulker", "Shulker Box", class_1802.field_8545, true, new Color(236, 85, 204)).add("spawner", "Spawner", class_1802.field_8849, true, new Color(255, 76, 98)).add("furnace", "Furnace", class_1802.field_8732, true, new Color(102, 168, 255)).add("barrel", "Barrel", class_1802.field_16307, true, new Color(232, 151, 70)).add("enchanting_table", "Enchanting Table", class_1802.field_8657, true, new Color(62, 224, 207)).add("piston", "Moving Piston", class_1802.field_8249, true, new Color(151, 224, 88)).add("hopper", "Hopper", class_1802.field_8239, true, new Color(0, 0, 255)).add("dispenser", "Dispenser", class_1802.field_8357, false, new Color(95, 188, 255)).add("dropper", "Dropper", class_1802.field_8878, false, new Color(118, 134, 162)).add("brewing_stand", "Brewing Stand", class_1802.field_8740, false, new Color(249, 115, 140)).add("crafter", "Crafter", class_1802.field_46791, false, new Color(255, 195, 72)).add("chiseled_bookshelf", "Chiseled Bookshelf", class_1802.field_40215, false, new Color(189, 126, 255)).add("decorated_pot", "Decorated Pot", class_1802.field_42699, false, new Color(223, 119, 92));
    private List<StoragePair> targets = List.of();
    private final List<RenderUtils.ColoredBox> renderBoxes = new ArrayList<RenderUtils.ColoredBox>();
    private final List<RenderUtils.ColoredLine> renderLines = new ArrayList<RenderUtils.ColoredLine>();
    private Object scannedWorld;
    private int scanCooldown;
    private final Color[] renderColors = new Color[StorageType.values().length];

    public StorageEspModule() {
        super(EncryptedString.of("Storage ESP"), EncryptedString.of("Highlights selected loaded storage blocks through walls"), -1, Category.RENDER);
        this.addSettings(this.style.settings());
        this.addSettings(this.boxSize, this.storages);
    }

    @Override
    public void onEnable() {
        this.targets = List.of();
        this.scannedWorld = null;
        this.scanCooldown = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.targets = List.of();
        this.scannedWorld = null;
        this.renderBoxes.clear();
        this.renderLines.clear();
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent var1) {
        if (this.mc.field_1687 != null && this.mc.field_1724 != null) {
            int var10000;
            if (this.scannedWorld != this.mc.field_1687) {
                this.targets = List.of();
                this.scannedWorld = this.mc.field_1687;
                this.scanCooldown = 0;
            }
            if ((var10000 = this.scanCooldown--) <= 0) {
                this.refreshTargets();
                this.scanCooldown = 9;
            }
        } else {
            this.targets = List.of();
            this.scannedWorld = null;
            this.scanCooldown = 0;
        }
    }

    private void refreshTargets() {
        int var1 = this.mc.field_1690.method_38521();
        class_1923 var2 = this.mc.field_1724.method_31476();
        ArrayList<StoragePair> var3 = new ArrayList<StoragePair>();
        HashSet<class_2338> var4 = new HashSet<class_2338>();
        for (int var5 = var2.field_9181 - var1; var5 <= var2.field_9181 + var1; ++var5) {
            for (int var6 = var2.field_9180 - var1; var6 <= var2.field_9180 + var1; ++var6) {
                if (!this.mc.field_1687.method_8393(var5, var6)) continue;
                class_2818 var7 = this.mc.field_1687.method_8497(var5, var6);
                for (class_2586 var9 : var7.method_12214().values()) {
                    StorageType var10 = StorageEspModule.typeOf(var9);
                    if (var10 == null) continue;
                    class_2338 var11 = var9.method_11016().method_10062();
                    if (var10 == StorageType.CHEST) {
                        if (var4.contains(var11)) continue;
                        class_2338 var12 = this.chestPartner(var11);
                        if (var12 != null) {
                            var4.add(var11);
                            var4.add(var12);
                            var3.add(new StoragePair(var11, var12, var10));
                            continue;
                        }
                    }
                    var3.add(new StoragePair(var11, var11, var10));
                }
            }
        }
        var3.sort(Comparator.comparingDouble(this::distanceSquared));
        this.targets = List.copyOf(var3);
    }

    private class_2338 chestPartner(class_2338 var1) {
        if (!(this.mc.field_1687.method_8321(var1) instanceof class_2595)) {
            return null;
        }
        class_2680 var2 = this.mc.field_1687.method_8320(var1);
        if (var2.method_26204() instanceof class_2281 && var2.method_11654((class_2769)class_2281.field_10770) != class_2745.field_12569) {
            for (class_2350 var4 : class_2350.class_2353.field_11062) {
                class_2338 var5 = var1.method_10093(var4);
                class_2680 var6 = this.mc.field_1687.method_8320(var5);
                if (var6.method_26204() != var2.method_26204() || !(this.mc.field_1687.method_8321(var5) instanceof class_2595) || var6.method_11654((class_2769)class_2281.field_10770) == class_2745.field_12569) continue;
                return var5.method_10062();
            }
            return null;
        }
        return null;
    }

    private boolean targetStillLive(StoragePair var1) {
        if (!this.mc.field_1687.method_8393(var1.first().method_10263() >> 4, var1.first().method_10260() >> 4)) {
            return false;
        }
        class_2586 var2 = this.mc.field_1687.method_8321(var1.first());
        if (var2 != null && !var2.method_11015() && StorageEspModule.typeOf(var2) == var1.type()) {
            if (!var1.first().equals((Object)var1.second())) {
                if (!this.mc.field_1687.method_8393(var1.second().method_10263() >> 4, var1.second().method_10260() >> 4)) {
                    return false;
                }
                class_2586 var3 = this.mc.field_1687.method_8321(var1.second());
                if (var3 == null || var3.method_11015() || StorageEspModule.typeOf(var3) != var1.type()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (this.mc.field_1687 == null || this.mc.field_1724 == null || this.targets.isEmpty()) {
            return;
        }
        class_4184 camera = RenderUtils.getCamera();
        if (camera == null || !camera.method_19332()) {
            return;
        }
        class_4587 matrices = event.matrixStack;
        matrices.method_22903();
        RenderUtils.applyCameraTransform(matrices, camera);
        if (!this.style.begin(camera)) {
            matrices.method_22909();
            return;
        }
        double inset = (1.0 - this.boxSize.getValue() / 100.0) * 0.5;
        int index = 0;
        for (StoragePair pair : this.targets) {
            StorageBlocksSetting.Entry entry = this.storages.get(pair.type().id);
            if (entry == null || !entry.enabled()) continue;
            int minX = Math.min(pair.first().method_10263(), pair.second().method_10263());
            int minY = Math.min(pair.first().method_10264(), pair.second().method_10264());
            int minZ = Math.min(pair.first().method_10260(), pair.second().method_10260());
            int maxX = Math.max(pair.first().method_10263(), pair.second().method_10263()) + 1;
            int maxY = Math.max(pair.first().method_10264(), pair.second().method_10264()) + 1;
            int maxZ = Math.max(pair.first().method_10260(), pair.second().method_10260()) + 1;
            if (!this.style.wantsTracers() && !RenderVisibility.box(minX, minY, minZ, maxX, maxY, maxZ) || !this.targetStillLive(pair)) continue;
            this.style.box(minX + inset, minY + inset, minZ + inset, maxX - inset, maxY - inset, maxZ - inset, entry.color(), index++);
        }
        this.style.render(matrices);
        matrices.method_22909();
    }

    private double distanceSquared(StoragePair var1) {
        class_243 var2 = var1.center();
        return this.mc.field_1724.method_5649(var2.field_1352, var2.field_1351, var2.field_1350);
    }

    private static class_243 tracerStart(class_4184 var0) {
        return RenderUtils.getTracerStart(var0);
    }

    private static StorageType typeOf(class_2586 var0) {
        if (var0 instanceof class_2595) {
            return StorageType.CHEST;
        }
        if (var0 instanceof class_2611) {
            return StorageType.ENDER_CHEST;
        }
        if (var0 instanceof class_2627) {
            return StorageType.SHULKER;
        }
        if (var0 instanceof class_2636) {
            return StorageType.SPAWNER;
        }
        if (var0 instanceof class_2609) {
            return StorageType.FURNACE;
        }
        if (var0 instanceof class_3719) {
            return StorageType.BARREL;
        }
        if (var0 instanceof class_2605) {
            return StorageType.ENCHANTING_TABLE;
        }
        if (var0 instanceof class_2669) {
            return StorageType.PISTON;
        }
        if (var0 instanceof class_2614) {
            return StorageType.HOPPER;
        }
        if (var0 instanceof class_2608) {
            return StorageType.DROPPER;
        }
        if (var0 instanceof class_2601) {
            return StorageType.DISPENSER;
        }
        if (var0 instanceof class_2589) {
            return StorageType.BREWING_STAND;
        }
        if (var0 instanceof class_8887) {
            return StorageType.CRAFTER;
        }
        if (var0 instanceof class_7716) {
            return StorageType.CHISELED_BOOKSHELF;
        }
        return var0 instanceof class_8172 ? StorageType.DECORATED_POT : null;
    }

    private static enum StorageType {
        CHEST("chest"),
        ENDER_CHEST("ender_chest"),
        SHULKER("shulker"),
        SPAWNER("spawner"),
        FURNACE("furnace"),
        BARREL("barrel"),
        ENCHANTING_TABLE("enchanting_table"),
        PISTON("piston"),
        HOPPER("hopper"),
        DISPENSER("dispenser"),
        DROPPER("dropper"),
        BREWING_STAND("brewing_stand"),
        CRAFTER("crafter"),
        CHISELED_BOOKSHELF("chiseled_bookshelf"),
        DECORATED_POT("decorated_pot");

        private final String id;

        private StorageType(String nullxx) {
            this.id = nullxx;
        }
    }

    private record StoragePair(class_2338 first, class_2338 second, StorageType type) {
        private class_243 center() {
            return new class_243((double)(this.first.method_10263() + this.second.method_10263() + 1) * 0.5, (double)(this.first.method_10264() + this.second.method_10264() + 1) * 0.5, (double)(this.first.method_10260() + this.second.method_10260() + 1) * 0.5);
        }
    }
}

