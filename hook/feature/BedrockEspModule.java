/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1802
 *  net.minecraft.class_1923
 *  net.minecraft.class_2246
 *  net.minecraft.class_2338
 *  net.minecraft.class_2350
 *  net.minecraft.class_2382
 *  net.minecraft.class_243
 *  net.minecraft.class_2561
 *  net.minecraft.class_2818
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 */
package hook.feature;

import hook.event.EventListener;
import hook.event.events.ChunkDataEvent;
import hook.event.events.Render3DEvent;
import hook.event.events.TickEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ModeSetting;
import hook.module.setting.NumberSetting;
import hook.module.setting.StorageBlocksSetting;
import hook.utils.BlockUtil;
import hook.utils.EncryptedString;
import hook.utils.RenderUtils;
import hook.utils.RenderVisibility;
import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.minecraft.class_1802;
import net.minecraft.class_1923;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_2382;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2818;
import net.minecraft.class_4184;
import net.minecraft.class_4587;

public final class BedrockEspModule
extends Module {
    private static final List<Integer> OVERWORLD_LEVELS = List.of(Integer.valueOf(-64), Integer.valueOf(-63), Integer.valueOf(-62), Integer.valueOf(-61), Integer.valueOf(-60));
    private static final List<Integer> NETHER_LEVELS = List.of(Integer.valueOf(0), Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3), Integer.valueOf(4), Integer.valueOf(123), Integer.valueOf(124), Integer.valueOf(125), Integer.valueOf(126), Integer.valueOf(127));
    private final NumberSetting minimumSize = new NumberSetting(EncryptedString.of("Minimum Void Size"), 1.0, 50.0, 2.0, 1.0);
    private final hook.utils.EspStyle style = new hook.utils.EspStyle(hook.utils.EspStyle.Shape.Both, 70, 0xFFF05550);
    private final StorageBlocksSetting color = new StorageBlocksSetting(EncryptedString.of("Color")).add("void", "Void", class_1802.field_8542, true, new Color(240, 85, 80)).colorOnly();
    private final BooleanSetting chatFeedback = new BooleanSetting(EncryptedString.of("Chat Feedback"), true);
    private final Set<class_2338> voidBlocks = new HashSet<class_2338>();
    private final Map<Long, Set<class_2338>> voidsByChunk = new HashMap<Long, Set<class_2338>>();
    private final Set<class_2338> announcedGroups = new HashSet<class_2338>();
    private final Queue<Long> scanQueue = new ArrayDeque<Long>();
    private final Set<Long> queuedChunks = new HashSet<Long>();
    private final Set<Long> scannedChunks = new HashSet<Long>();
    private final List<RenderUtils.ColoredBox> renderBoxes = new ArrayList<RenderUtils.ColoredBox>();
    private final List<RenderUtils.ColoredLine> renderLines = new ArrayList<RenderUtils.ColoredLine>();
    private Object scannedWorld;
    private int ticks;

    public BedrockEspModule() {
        super(EncryptedString.of("Bedrock ESP"), EncryptedString.of("Finds enclosed gaps in the overworld and nether bedrock layers"), -1, Category.RENDER);
        this.addSettings(this.minimumSize);
        this.addSettings(this.style.settings());
        this.addSettings(this.color, this.chatFeedback);
    }

    @Override
    public void onEnable() {
        this.reset();
        this.scannedWorld = this.mc.field_1687;
        this.enqueueLoadedChunks();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.reset();
        super.onDisable();
    }

    private void reset() {
        this.voidBlocks.clear();
        this.voidsByChunk.clear();
        this.announcedGroups.clear();
        this.scanQueue.clear();
        this.queuedChunks.clear();
        this.scannedChunks.clear();
        this.renderBoxes.clear();
        this.renderLines.clear();
        this.scannedWorld = null;
        this.ticks = 0;
    }

    @EventListener
    public void onChunkData(ChunkDataEvent var1) {
        this.enqueue(var1.packet.method_11523(), var1.packet.method_11524());
    }

    @EventListener
    public void onTick(TickEvent var1) {
        if (this.mc.field_1687 != null && this.mc.field_1724 != null) {
            if (this.scannedWorld != this.mc.field_1687) {
                this.voidBlocks.clear();
                this.voidsByChunk.clear();
                this.announcedGroups.clear();
                this.scanQueue.clear();
                this.queuedChunks.clear();
                this.scannedChunks.clear();
                this.scannedWorld = this.mc.field_1687;
                this.ticks = 0;
            }
            ++this.ticks;
            if (this.ticks % 20 == 1) {
                this.enqueueLoadedChunks();
            }
            if (this.ticks % 200 == 0 && this.scanQueue.isEmpty()) {
                this.scannedChunks.clear();
                this.enqueueLoadedChunks();
            }
            this.scanNextChunk();
            if (this.ticks % 40 == 0) {
                this.pruneUnloadedChunks();
            }
        } else {
            this.reset();
        }
    }

    private void enqueueLoadedChunks() {
        if (this.mc.field_1687 != null && this.mc.field_1724 != null) {
            class_1923 var1 = this.mc.field_1724.method_31476();
            List<class_2818> var2 = BlockUtil.getLoadedChunksList();
            var2.sort(Comparator.comparingInt(var1x -> Math.max(Math.abs(var1x.method_12004().field_9181 - var1.field_9181), Math.abs(var1x.method_12004().field_9180 - var1.field_9180))));
            Iterator<class_2818> iterator = var2.iterator();
            while (iterator.hasNext()) {
                class_2818 var4Object;
                class_2818 var4 = var4Object = iterator.next();
                this.enqueue(var4.method_12004().field_9181, var4.method_12004().field_9180);
            }
        }
    }

    private void enqueue(int var1, int var2) {
        long var3 = class_1923.method_8331((int)var1, (int)var2);
        if (!this.scannedChunks.contains(var3) && this.queuedChunks.add(var3)) {
            this.scanQueue.offer(var3);
        }
    }

    private void scanNextChunk() {
        Long var1 = this.scanQueue.poll();
        if (var1 != null) {
            this.queuedChunks.remove(var1);
            this.scannedChunks.add(var1);
            int var2 = class_1923.method_8325((long)var1);
            int var3 = class_1923.method_8332((long)var1);
            if (!this.mc.field_1687.method_8393(var2, var3)) {
                this.removeChunk(var1);
            } else {
                this.scanChunk(this.mc.field_1687.method_8497(var2, var3), var1);
            }
        }
    }

    private void scanChunk(class_2818 var1, long var2) {
        List<Integer> var5;
        Set<class_2338> var4 = this.voidsByChunk.remove(var2);
        if (var4 != null) {
            this.voidBlocks.removeAll(var4);
        }
        if (!(var5 = this.levelsForDimension()).isEmpty()) {
            HashSet<class_2338> var6 = new HashSet<class_2338>();
            int var7 = var1.method_12004().method_8326();
            int var8 = var1.method_12004().method_8328();
            for (Integer var10Object : var5) {
                int var10 = var10Object;
                for (int i = 0; i < 16; ++i) {
                    for (int var12 = 0; var12 < 16; ++var12) {
                        class_2338 var13 = new class_2338(var7 + i, var10, var8 + var12);
                        if (var1.method_8320(var13).method_26204() == class_2246.field_9987) continue;
                        var6.add(var13);
                    }
                }
            }
            HashSet<class_2338> var21 = new HashSet<class_2338>();
            HashSet<class_2338> var22 = new HashSet<class_2338>();
            for (Object e : var6) {
                class_2338 var24 = (class_2338)e;
                if (!var22.add(var24)) continue;
                HashSet<class_2338> var25 = new HashSet<class_2338>();
                ArrayDeque<class_2338> var14 = new ArrayDeque<class_2338>();
                var14.add(var24);
                while (!var14.isEmpty() && var25.size() < 200) {
                    class_2338 var15 = (class_2338)var14.remove();
                    var25.add(var15);
                    for (class_2350 var19 : class_2350.values()) {
                        class_2338 var20 = var15.method_10093(var19);
                        if (!var6.contains(var20) || !var22.add(var20)) continue;
                        var14.add(var20);
                    }
                }
                if (!var14.isEmpty() || var25.size() < this.minimumSize.getIntValue() || !this.isEnclosed(var25)) continue;
                var21.addAll(var25);
                class_2338 var26 = var25.iterator().next();
                if (!this.chatFeedback.getValue() || !this.announcedGroups.add(var26)) continue;
                this.mc.field_1724.method_7353((class_2561)class_2561.method_43470((String)("\u00a7d[Bedrock ESP] \u00a7fVoid found: " + var25.size() + " blocks at " + var26.method_23854())), false);
            }
            if (!var21.isEmpty()) {
                this.voidsByChunk.put(var2, Set.copyOf(var21));
            }
            this.voidBlocks.addAll(var21);
            this.announcedGroups.retainAll(this.voidBlocks);
        }
    }

    private boolean isEnclosed(Set<class_2338> var1) {
        for (class_2338 var3 : var1) {
            for (class_2350 var7 : class_2350.values()) {
                class_2338 var8 = var3.method_10093(var7);
                if (var1.contains(var8) || this.mc.field_1687.method_8320(var8).method_26204() == class_2246.field_9987) continue;
                return false;
            }
        }
        return true;
    }

    private void pruneUnloadedChunks() {
        for (Long var2 : new ArrayList<Long>(this.voidsByChunk.keySet())) {
            int var4;
            int var3 = class_1923.method_8325((long)var2);
            if (this.mc.field_1687.method_8393(var3, var4 = class_1923.method_8332((long)var2))) continue;
            this.removeChunk(var2);
        }
    }

    private void removeChunk(long var1) {
        Set<class_2338> var3 = this.voidsByChunk.remove(var1);
        if (var3 != null) {
            this.voidBlocks.removeAll(var3);
        }
        this.scannedChunks.remove(var1);
        this.queuedChunks.remove(var1);
    }

    private List<Integer> levelsForDimension() {
        String var1 = this.mc.field_1687.method_27983().method_29177().toString();
        if (var1.equals("minecraft:overworld")) {
            return OVERWORLD_LEVELS;
        }
        return var1.equals("minecraft:the_nether") ? NETHER_LEVELS : List.of();
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (this.mc.field_1687 == null || this.voidBlocks.isEmpty()) {
            return;
        }
        class_4184 camera = RenderUtils.getCamera();
        if (camera == null || !camera.method_19332()) {
            return;
        }
        class_4587 matrices = event.matrixStack;
        matrices.method_22903();
        RenderUtils.applyCameraTransform(matrices, camera);
        Color base = this.color.entries().get(0).color();
        if (!this.style.begin(camera)) {
            matrices.method_22909();
            return;
        }
        int index = 0;
        for (class_2338 pos : this.voidBlocks) {
            double x0 = (double)pos.method_10263() + 0.03;
            double y0 = (double)pos.method_10264() + 0.03;
            double z0 = (double)pos.method_10260() + 0.03;
            double x1 = (double)pos.method_10263() + 0.97;
            double y1 = (double)pos.method_10264() + 0.97;
            double z1 = (double)pos.method_10260() + 0.97;
            this.style.box(x0, y0, z0, x1, y1, z1, base, index++);
        }
        this.style.render(matrices);
        matrices.method_22909();
    }

    public static enum EspStyle {
        Box,
        Outline,
        Both;

    }
}

