/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1923
 *  net.minecraft.class_2246
 *  net.minecraft.class_2248
 *  net.minecraft.class_2338
 *  net.minecraft.class_243
 *  net.minecraft.class_2818
 *  net.minecraft.class_2826
 *  net.minecraft.class_2960
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_7923
 */
package hook.feature;

import hook.event.EventListener;
import hook.event.events.ChunkDataEvent;
import hook.event.events.Render3DEvent;
import hook.event.events.TickEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
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
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import net.minecraft.class_1923;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2818;
import net.minecraft.class_2826;
import net.minecraft.class_2960;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_7923;

public class BlockEspModule
extends Module {
    private static final int MAX_RENDERED_BLOCKS = 4096;
    private final hook.utils.EspStyle style = new hook.utils.EspStyle(hook.utils.EspStyle.Shape.Both, 45, 0xFF5AC8FF);
    private final NumberSetting boxSize = new NumberSetting(EncryptedString.of("Box Size"), 50.0, 100.0, 96.0, 1.0).getValue(EncryptedString.of("Size of the box in percent of a full block"));
    private final StorageBlocksSetting blocks = BlockEspModule.createBlockPalette();
    private final boolean rotatedOnly;
    private final Map<Long, List<BlockEntry>> results = new HashMap<Long, List<BlockEntry>>();
    private final Queue<Long> scanQueue = new ArrayDeque<Long>();
    private final Set<Long> queued = new HashSet<Long>();
    private final Set<Long> scanned = new HashSet<Long>();
    private final Map<class_2248, StorageBlocksSetting.Entry> selected = new HashMap<class_2248, StorageBlocksSetting.Entry>();
    private final List<RenderUtils.ColoredBox> renderBoxes = new ArrayList<RenderUtils.ColoredBox>();
    private final List<RenderUtils.ColoredLine> renderLines = new ArrayList<RenderUtils.ColoredLine>();
    private Object trackedWorld;
    private int selectionSignature;
    private int ticks;

    public BlockEspModule() {
        super(EncryptedString.of("Block ESP"), EncryptedString.of("Searches and highlights any selected Minecraft block"), -1, Category.RENDER);
        this.rotatedOnly = false;
        this.addSettings(this.style.settings());
        this.addSettings(this.boxSize, this.blocks);
    }

    protected BlockEspModule(CharSequence name, CharSequence description, Set<String> defaultBlocks, boolean rotatedOnly) {
        this(name, description, defaultBlocks, rotatedOnly, Category.RENDER);
    }

    protected BlockEspModule(CharSequence name, CharSequence description, Set<String> defaultBlocks, boolean rotatedOnly, Category category) {
        super(name, description, -1, category);
        this.blocks.clear();
        for (class_2248 block : class_7923.field_41175) {
            if (block == class_2246.field_10124 || block == class_2246.field_10543 || block == class_2246.field_10243) continue;
            class_2960 id = class_7923.field_41175.method_10221(block);
            Color color = Color.getHSBColor((float)(id.toString().hashCode() & 0xFFFF) / 65535.0f, 0.58f, 1.0f);
            boolean selectedByDefault = defaultBlocks.contains(id.toString())
                || defaultBlocks.contains("@deepslate") && (id.toString().contains("deepslate") || id.toString().equals("minecraft:stone"));
            this.blocks.add(id.toString(), block.method_9518().getString(), block.method_8389(), selectedByDefault, color);
        }
        this.rotatedOnly = rotatedOnly;
        this.addSettings(this.style.settings());
        this.addSettings(this.boxSize, this.blocks);
    }

    private static StorageBlocksSetting createBlockPalette() {
        StorageBlocksSetting palette = new StorageBlocksSetting(EncryptedString.of("Blocks"));
        for (class_2248 block : class_7923.field_41175) {
            if (block == class_2246.field_10124 || block == class_2246.field_10543 || block == class_2246.field_10243) continue;
            class_2960 id = class_7923.field_41175.method_10221(block);
            Color color = Color.getHSBColor((float)(id.toString().hashCode() & 0xFFFF) / 65535.0f, 0.58f, 1.0f);
            palette.add(id.toString(), block.method_9518().getString(), block.method_8389(), false, color);
        }
        return palette;
    }

    @Override
    public void onEnable() {
        this.reset();
        this.trackedWorld = this.mc.field_1687;
        this.refreshSelection();
        this.enqueueLoadedChunks();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        this.reset();
        super.onDisable();
    }

    private void reset() {
        this.results.clear();
        this.scanQueue.clear();
        this.queued.clear();
        this.scanned.clear();
        this.selected.clear();
        this.renderBoxes.clear();
        this.renderLines.clear();
        this.trackedWorld = null;
        this.selectionSignature = 0;
        this.ticks = 0;
    }

    @EventListener
    public void onChunkData(ChunkDataEvent event) {
        this.enqueue(event.packet.method_11523(), event.packet.method_11524());
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (this.mc.field_1687 == null || this.mc.field_1724 == null) {
            this.reset();
            return;
        }
        if (this.trackedWorld != this.mc.field_1687) {
            this.reset();
            this.trackedWorld = this.mc.field_1687;
        }
        if (this.computeSelectionSignature() != this.selectionSignature) {
            this.results.clear();
            this.scanQueue.clear();
            this.queued.clear();
            this.scanned.clear();
            this.refreshSelection();
            this.enqueueLoadedChunks();
        }
        if (this.selected.isEmpty()) {
            return;
        }
        ++this.ticks;
        if (this.ticks % 100 == 1) {
            this.enqueueLoadedChunks();
        }
        if (this.ticks % 400 == 0) {
            this.scanned.clear();
        }
        this.scanNextChunk();
        if (this.ticks % 20 == 0) {
            this.pruneUnloaded();
        }
    }

    private int computeSelectionSignature() {
        int signature = 1;
        for (StorageBlocksSetting.Entry entry : this.blocks.entries()) {
            if (!entry.enabled()) continue;
            signature = signature * 31 + entry.id().hashCode();
        }
        return signature;
    }

    private void refreshSelection() {
        this.selected.clear();
        for (StorageBlocksSetting.Entry entry : this.blocks.entries()) {
            if (!entry.enabled()) continue;
            class_7923.field_41175.method_10223(class_2960.method_60654((String)entry.id())).ifPresent(block -> this.selected.put((class_2248)block.comp_349(), entry));
        }
        this.selectionSignature = this.computeSelectionSignature();
    }

    private void enqueueLoadedChunks() {
        if (this.mc.field_1687 == null || this.mc.field_1724 == null || this.selected.isEmpty()) {
            return;
        }
        class_1923 playerChunk = this.mc.field_1724.method_31476();
        List<class_2818> chunks = BlockUtil.getLoadedChunksList();
        chunks.sort(Comparator.comparingInt(chunk -> {
            class_1923 pos = chunk.method_12004();
            return Math.max(Math.abs(pos.field_9181 - playerChunk.field_9181), Math.abs(pos.field_9180 - playerChunk.field_9180));
        }));
        for (class_2818 chunk2 : chunks) {
            this.enqueue(chunk2.method_12004().field_9181, chunk2.method_12004().field_9180);
        }
    }

    private void enqueue(int chunkX, int chunkZ) {
        long key = class_1923.method_8331((int)chunkX, (int)chunkZ);
        if (!this.selected.isEmpty() && !this.scanned.contains(key) && this.queued.add(key)) {
            this.scanQueue.offer(key);
        }
    }

    private void scanNextChunk() {
        Long key = this.scanQueue.poll();
        if (key == null) {
            return;
        }
        this.queued.remove(key);
        this.scanned.add(key);
        int chunkX = class_1923.method_8325((long)key);
        int chunkZ = class_1923.method_8332((long)key);
        if (!this.mc.field_1687.method_8393(chunkX, chunkZ)) {
            this.results.remove(key);
            return;
        }
        class_2818 chunk = this.mc.field_1687.method_8497(chunkX, chunkZ);
        class_2826[] sections = chunk.method_12006();
        int bottomSection = chunk.method_32891();
        int startX = chunk.method_12004().method_8326();
        int startZ = chunk.method_12004().method_8328();
        ArrayList<BlockEntry> found = new ArrayList<BlockEntry>();
        for (int sectionIndex = 0; sectionIndex < sections.length && found.size() < 4096; ++sectionIndex) {
            class_2826 section = sections[sectionIndex];
            if (section == null || !section.method_12265().method_19526(state -> this.selected.containsKey(state.method_26204()))) continue;
            int startY = bottomSection + sectionIndex << 4;
            for (int y = 0; y < 16 && found.size() < 4096; ++y) {
                for (int z = 0; z < 16 && found.size() < 4096; ++z) {
                    for (int x = 0; x < 16 && found.size() < 4096; ++x) {
                        var state = section.method_12254(x, y, z);
                        class_2248 block = state.method_26204();
                        if (!this.selected.containsKey(block)) continue;
                        if (this.rotatedOnly && !BlockEspModule.hasUnnaturalAxis(state.toString())) continue;
                        found.add(new BlockEntry(new class_2338(startX + x, startY + y, startZ + z), block));
                    }
                }
            }
        }
        if (found.isEmpty()) {
            this.results.remove(key);
        } else {
            this.results.put(key, List.copyOf(found));
        }
    }

    private void pruneUnloaded() {
        this.results.keySet().removeIf(key -> !this.mc.field_1687.method_8393(class_1923.method_8325((long)key), class_1923.method_8332((long)key)));
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (this.mc.field_1687 == null || this.results.isEmpty()) {
            return;
        }
        class_4184 camera = RenderUtils.getCamera();
        if (camera == null || !camera.method_19332()) {
            return;
        }
        if (!this.style.begin(camera)) {
            return;
        }
        double inset = (1.0 - this.boxSize.getValue() / 100.0) * 0.5;
        int rendered = 0;
        int index = 0;
        block0: for (List<BlockEntry> chunkEntries : this.results.values()) {
            for (BlockEntry blockEntry : chunkEntries) {
                class_2338 pos = blockEntry.pos();
                StorageBlocksSetting.Entry entry = this.selected.get(blockEntry.block());
                if (entry == null) continue;
                if (!this.style.inRange(pos.method_10263() + 0.5, pos.method_10264() + 0.5, pos.method_10260() + 0.5)) continue;
                if (!this.style.wantsTracers() && !RenderVisibility.box(pos.method_10263(), pos.method_10264(), pos.method_10260(), pos.method_10263() + 1, pos.method_10264() + 1, pos.method_10260() + 1)) continue;
                this.style.box(pos.method_10263() + inset, pos.method_10264() + inset, pos.method_10260() + inset,
                        pos.method_10263() + 1 - inset, pos.method_10264() + 1 - inset, pos.method_10260() + 1 - inset, entry.color(), index++);
                if (++rendered < 4096) continue;
                break block0;
            }
        }
        class_4587 matrices = event.matrixStack;
        matrices.method_22903();
        RenderUtils.applyCameraTransform(matrices, camera);
        this.style.render(matrices);
        matrices.method_22909();
    }

    private record BlockEntry(class_2338 pos, class_2248 block) {
    }

    private static boolean hasUnnaturalAxis(String state) {
        String value = state.toLowerCase(java.util.Locale.ROOT);
        return value.contains("axis=x") || value.contains("axis=z")
            || value.contains("facing=north") || value.contains("facing=east")
            || value.contains("facing=south") || value.contains("facing=west");
    }
}

