package hook.feature;

import hook.Hook;
import hook.module.setting.StorageBlocksSetting;
import hook.utils.RenderUtils;
import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_1935;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;

/**
 * Block / item picker for Block ESP, Storage ESP, Block Notifier ... in the client's glass style:
 * scales with the window, category tabs, search, smooth scrolling, name tooltips, select/clear
 * visible, right-click for the colour picker. Enter saves, Esc cancels, Ctrl+A selects visible.
 */
public final class ColorListEditorScreen extends class_437 {
    private enum Filter {
        ALL("All"), SELECTED("Selected"), ORES("Ores"), STORAGE("Storage"), BUILDING("Building"), NATURE("Nature"), REDSTONE("Redstone"), OTHER("Other");

        final String label;

        Filter(String label) {
            this.label = label;
        }
    }

    private final StorageBlocksSetting setting;
    private final Map<String, Object[]> original = new LinkedHashMap<>();
    private String search = "";
    private Filter filter = Filter.ALL;
    private float scroll;
    private float scrollTarget;
    private final Map<String, float[]> anims = new java.util.HashMap<>();
    private long last;
    private float dt = 0.016f;
    private final List<float[]> hits = new ArrayList<>();
    private final List<Runnable> actions = new ArrayList<>();
    private final List<Integer> buttons = new ArrayList<>();

    public ColorListEditorScreen(StorageBlocksSetting setting) {
        super((class_2561) class_2561.method_43473());
        this.setting = setting;
        for (StorageBlocksSetting.Entry e : setting.entries()) {
            this.original.put(e.id(), new Object[]{e.enabled(), e.color()});
        }
    }

    // ------------------------------------------------------------------ layout helpers

    private float scale() {
        return Math.max(1.0f, class_310.method_1551().method_22683().method_4495());
    }

    private float u() {
        int h = class_310.method_1551().method_22683().method_4507();
        return Math.max(0.75f, h / 1080.0f);
    }

    private float d(float v) {
        return v * this.u();
    }

    private float anim(String key, float target, float speed) {
        float[] a = this.anims.computeIfAbsent(key, k -> new float[]{target});
        a[0] += (target - a[0]) * (1.0f - (float) Math.exp(-speed * this.dt));
        return a[0];
    }

    private static boolean inside(double x, double y, float rx, float ry, float rw, float rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }

    private static Filter classify(StorageBlocksSetting.Entry e) {
        String id = e.id().toLowerCase(Locale.ROOT);
        if (id.contains("_ore") || id.contains("ancient_debris") || id.contains("raw_") && id.contains("_block") || id.contains("amethyst")) return Filter.ORES;
        if (id.matches(".*(chest|barrel|shulker|hopper|furnace|smoker|dispenser|dropper|crafter|spawner|ender_chest|bookshelf|vault|decorated_pot|brewing|anvil|beacon|lectern|crafting_table|enchanting).*")) return Filter.STORAGE;
        if (id.matches(".*(redstone|piston|observer|lever|button|pressure_plate|rail|comparator|repeater|lamp|target|daylight|tripwire|note_block|tnt|sculk_sensor|trapped).*")) return Filter.REDSTONE;
        if (id.matches(".*(log|wood|leaves|sapling|grass|flower|fern|mushroom|vine|moss|azalea|bamboo|cactus|kelp|coral|dirt|sand|gravel|clay|snow|ice|crop|wheat|carrot|potato|beetroot|melon|pumpkin|berry|root|tulip|orchid|allium|daisy|poppy|lily|sugar_cane|dripleaf|nylium|wart|fungus|stem|hyphae|mangrove|cherry|sculk|spore).*")) return Filter.NATURE;
        if (id.matches(".*(planks|stone|brick|glass|wool|concrete|terracotta|slab|stairs|wall|fence|door|quartz|deepslate|tuff|basalt|blackstone|andesite|diorite|granite|prismarine|purpur|copper|iron_block|gold_block|diamond_block|emerald_block|netherite_block|obsidian|calcite|mud|sandstone|end_stone|tile).*")) return Filter.BUILDING;
        return Filter.OTHER;
    }

    private List<StorageBlocksSetting.Entry> visibleEntries() {
        List<StorageBlocksSetting.Entry> out = new ArrayList<>();
        String q = this.search.toLowerCase(Locale.ROOT).trim();
        for (StorageBlocksSetting.Entry e : this.setting.entries()) {
            if (this.filter == Filter.SELECTED && !e.enabled()) continue;
            if (this.filter != Filter.ALL && this.filter != Filter.SELECTED && classify(e) != this.filter) continue;
            if (!q.isEmpty() && !e.displayName().toLowerCase(Locale.ROOT).contains(q) && !e.id().toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(e);
        }
        return out;
    }

    private void hit(float x, float y, float w, float h, int button, Runnable action) {
        this.hits.add(new float[]{x, y, w, h});
        this.buttons.add(button);
        this.actions.add(action);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        this.dt = this.last == 0L ? 0.016f : Math.min(0.1f, (now - this.last) / 1.0E9f);
        this.last = now;
        this.hits.clear();
        this.actions.clear();
        this.buttons.clear();
        float s = this.scale();
        double mx = mouseX * s, my = mouseY * s;
        int sw = class_310.method_1551().method_22683().method_4489();
        int sh = class_310.method_1551().method_22683().method_4506();

        ctx.method_51448().pushMatrix();
        ctx.method_51448().scale(1.0f / s, 1.0f / s);
        try {
            MinecraftUiRenderer ui = new MinecraftUiRenderer(ctx, s);
            ui.rect(0, 0, sw, sh, UiRenderer.argb(0.45f, 0, 0, 0));
            float pw = Math.min(sw - this.d(80), this.d(1020));
            float ph = Math.min(sh - this.d(80), this.d(760));
            float px = (sw - pw) / 2.0f, py = (sh - ph) / 2.0f;
            ui.shadow(px, py, pw, ph, this.d(22), this.d(30), UiRenderer.argb(0.45f, 0, 0, 0));
            ui.round(px, py, pw, ph, this.d(22), UiEasing.mix(UiPalette.glass(0.97f), UiRenderer.argb(0.97f, 14, 15, 19), 0.5f));
            ui.outline(px, py, pw, ph, this.d(22), Math.max(1.0f, this.u()), UiRenderer.white(0.08f));

            // header
            boolean picker = this.setting.isPickerOnly();
            String title = this.setting.getName().toString();
            ui.text(title, px + this.d(26), py + this.d(22), this.d(20), 1, UiRenderer.white(0.96f));
            String sub = this.setting.selectedCount() + (picker ? " selected" : " blocks highlighted") + "  ·  left-click to toggle" + (picker ? "" : ", right-click to change the colour");
            ui.text(sub, px + this.d(26), py + this.d(48), this.d(11), 0, UiRenderer.white(0.5f));

            // search field
            float fx = px + pw - this.d(26) - this.d(300), fy = py + this.d(22), fw = this.d(300), fh = this.d(36);
            ui.round(fx, fy, fw, fh, this.d(12), UiRenderer.argb(0.9f, 8, 9, 12));
            ui.outline(fx, fy, fw, fh, this.d(12), Math.max(1.0f, this.u()), this.search.isEmpty() ? UiRenderer.white(0.08f) : UiPalette.accent(0.6f));
            ui.icon(UiIcon.SEARCH, fx + this.d(12), fy + (fh - this.d(14)) / 2.0f, this.d(14), UiRenderer.white(0.5f));
            boolean caret = (System.currentTimeMillis() / 500L) % 2L == 0L;
            ui.text(this.search.isEmpty() ? (picker ? "Search..." : "Search blocks...") : this.search + (caret ? "|" : ""), fx + this.d(34), fy + (fh - this.d(13)) / 2.0f - this.d(1), this.d(13), 0, this.search.isEmpty() ? UiRenderer.white(0.35f) : UiRenderer.white(0.92f));
            if (!this.search.isEmpty()) {
                float cx = fx + fw - this.d(28);
                ui.icon(UiIcon.CLOSE, cx, fy + (fh - this.d(12)) / 2.0f, this.d(12), UiRenderer.white(0.5f));
                this.hit(cx - this.d(6), fy, this.d(28), fh, 0, () -> { this.search = ""; this.scrollTarget = 0; });
            }

            // filter tabs
            float tx = px + this.d(26), ty = py + this.d(76);
            for (Filter f : Filter.values()) {
                String label = f == Filter.SELECTED ? f.label + " (" + this.setting.selectedCount() + ")" : f.label;
                float tw = ui.textWidth(label, this.d(12), 1) + this.d(26);
                boolean active = this.filter == f;
                boolean hot = inside(mx, my, tx, ty, tw, this.d(30));
                float a = this.anim("tab:" + f, active ? 1 : 0, 12), hv = this.anim("tabh:" + f, hot ? 1 : 0, 14);
                ui.round(tx, ty, tw, this.d(30), this.d(15), UiEasing.mix(UiRenderer.white(0.04f + 0.04f * hv), UiPalette.accent(0.85f), a));
                ui.textCentered(label, tx + tw / 2.0f, ty + this.d(8), this.d(12), 1, active ? UiRenderer.argb(0.95f, 14, 15, 19) : UiRenderer.white(0.8f));
                this.hit(tx, ty, tw, this.d(30), 0, () -> { this.filter = f; this.scrollTarget = 0; });
                tx += tw + this.d(8);
            }

            // grid
            float gx = px + this.d(26), gy = py + this.d(122), gw = pw - this.d(52), gh = ph - this.d(122) - this.d(78);
            ui.round(gx, gy, gw, gh, this.d(14), UiRenderer.argb(0.6f, 6, 7, 10));
            float cell = this.d(58), gap = this.d(8);
            int cols = Math.max(1, (int) ((gw - this.d(20) + gap) / (cell + gap)));
            float gridW = cols * cell + (cols - 1) * gap;
            float startX = gx + (gw - gridW) / 2.0f;
            List<StorageBlocksSetting.Entry> entries = this.visibleEntries();
            int rows = (entries.size() + cols - 1) / cols;
            float contentH = rows * (cell + gap) + this.d(16);
            float maxScroll = Math.max(0, contentH - gh);
            this.scrollTarget = Math.max(0, Math.min(maxScroll, this.scrollTarget));
            this.scroll += (this.scrollTarget - this.scroll) * (1.0f - (float) Math.exp(-16f * this.dt));
            StorageBlocksSetting.Entry hovered = null;
            ui.pushClip(gx, gy, gw, gh);
            List<Object[]> items = new ArrayList<>();
            for (int i = 0; i < entries.size(); i++) {
                StorageBlocksSetting.Entry e = entries.get(i);
                float cx = startX + (i % cols) * (cell + gap);
                float cy = gy + this.d(10) + (i / cols) * (cell + gap) - this.scroll;
                if (cy + cell < gy || cy > gy + gh) continue;
                boolean hot = inside(mx, my, cx, cy, cell, cell) && inside(mx, my, gx, gy, gw, gh);
                if (hot) hovered = e;
                float sel = this.anim("sel:" + e.id(), e.enabled() ? 1 : 0, 14);
                float hv = this.anim("hov:" + e.id(), hot ? 1 : 0, 16);
                Color c = e.color();
                int tint = UiRenderer.argb(0.22f * sel, c.getRed(), c.getGreen(), c.getBlue());
                ui.round(cx, cy, cell, cell, this.d(12), UiRenderer.white(0.04f + 0.05f * hv));
                if (sel > 0.01f) ui.round(cx, cy, cell, cell, this.d(12), tint);
                ui.outline(cx, cy, cell, cell, this.d(12), Math.max(1.0f, this.u() * (1 + sel)), UiEasing.mix(UiRenderer.white(0.06f + 0.12f * hv), UiRenderer.argb(0.95f, c.getRed(), c.getGreen(), c.getBlue()), sel));
                if (e.item() == null || e.item() == class_1802.field_8162) {
                    String abbr = e.displayName().length() <= 3 ? e.displayName() : e.displayName().substring(0, 2);
                    ui.textCentered(abbr.toUpperCase(Locale.ROOT), cx + cell / 2.0f, cy + cell / 2.0f - this.d(8), this.d(15), 1, UiRenderer.white(0.6f));
                } else {
                    items.add(new Object[]{e, cx, cy});
                }
                ui.circle(cx + cell - this.d(9), cy + this.d(9), this.d(4), UiRenderer.argb(1.0f, c.getRed(), c.getGreen(), c.getBlue()));
                if (sel > 0.5f) ui.icon(UiIcon.CHECK, cx + this.d(5), cy + this.d(5), this.d(12), UiRenderer.white(0.9f * sel));
                StorageBlocksSetting.Entry entry = e;
                this.hit(cx, cy, cell, cell, 0, entry::toggle);
                if (!picker) this.hit(cx, cy, cell, cell, 1, () -> class_310.method_1551().method_1507((class_437) new StorageColorPickerScreen(this, entry)));
                else this.hit(cx, cy, cell, cell, 1, entry::toggle);
            }
            ui.popClip();
            // items are drawn with the vanilla item renderer (pixel coordinates in this matrix)
            for (Object[] it : items) {
                StorageBlocksSetting.Entry e = (StorageBlocksSetting.Entry) it[0];
                float cx = (Float) it[1], cy = (Float) it[2];
                if (cy < gy - this.d(4) || cy + cell > gy + gh + this.d(4)) continue;
                float icon = cell * 0.62f;
                ctx.method_51448().pushMatrix();
                ctx.method_51448().translate(cx + (cell - icon) / 2.0f, cy + (cell - icon) / 2.0f);
                ctx.method_51448().scale(icon / 16.0f, icon / 16.0f);
                ctx.method_51427(new class_1799((class_1935) e.item()), 0, 0);
                ctx.method_51448().popMatrix();
            }
            if (entries.isEmpty()) {
                ui.textCentered("No blocks match", gx + gw / 2.0f, gy + gh / 2.0f - this.d(8), this.d(14), 1, UiRenderer.white(0.4f));
            }
            // scrollbar
            if (maxScroll > 0) {
                float barH = Math.max(this.d(40), gh * gh / contentH);
                float barY = gy + (gh - barH) * (this.scroll / maxScroll);
                ui.round(gx + gw - this.d(8), barY, this.d(4), barH, this.d(2), UiRenderer.white(0.25f));
            }

            // footer buttons
            float by = py + ph - this.d(58), bh = this.d(36);
            float bx = px + pw - this.d(26);
            bx = this.footerButton(ui, mx, my, bx, by, bh, "Save", true, () -> { Hook.INSTANCE.getSettingsManager().saveNow(); this.returnToGui(); });
            bx = this.footerButton(ui, mx, my, bx, by, bh, "Cancel", false, () -> { this.restore(); this.returnToGui(); });
            float lx = px + this.d(26);
            lx = this.footerButtonLeft(ui, mx, my, lx, by, bh, "Select visible", () -> { for (StorageBlocksSetting.Entry e : this.visibleEntries()) e.setEnabled(true); });
            lx = this.footerButtonLeft(ui, mx, my, lx, by, bh, "Clear visible", () -> { for (StorageBlocksSetting.Entry e : this.visibleEntries()) e.setEnabled(false); });
            this.footerButtonLeft(ui, mx, my, lx, by, bh, "Clear all", this.setting::clear);

            // tooltip
            if (hovered != null) {
                String name = hovered.displayName();
                float tw = ui.textWidth(name, this.d(12), 1) + this.d(20);
                float ttx = (float) Math.min(mx + this.d(14), sw - tw - this.d(6)), tty = (float) my + this.d(16);
                ui.round(ttx, tty, tw, this.d(26), this.d(9), UiRenderer.argb(0.95f, 20, 21, 26));
                ui.outline(ttx, tty, tw, this.d(26), this.d(9), Math.max(1.0f, this.u()), UiRenderer.white(0.12f));
                ui.text(name, ttx + this.d(10), tty + this.d(6), this.d(12), 1, UiRenderer.white(0.95f));
            }
        } finally {
            ctx.method_51448().popMatrix();
        }
    }

    private float footerButton(UiRenderer ui, double mx, double my, float right, float y, float h, String text, boolean primary, Runnable action) {
        float w = ui.textWidth(text, this.d(13), 1) + this.d(36);
        float x = right - w;
        boolean hot = inside(mx, my, x, y, w, h);
        float a = this.anim("btn:" + text, hot ? 1 : 0, 14);
        ui.round(x, y, w, h, this.d(12), primary ? UiEasing.mix(UiPalette.accent(0.85f), UiPalette.accent(1.0f), a) : UiRenderer.white(0.06f + 0.05f * a));
        ui.textCentered(text, x + w / 2.0f, y + (h - this.d(13)) / 2.0f - this.d(1), this.d(13), 1, primary ? UiRenderer.argb(0.95f, 14, 15, 19) : UiRenderer.white(0.9f));
        this.hit(x, y, w, h, 0, action);
        return x - this.d(10);
    }

    private float footerButtonLeft(UiRenderer ui, double mx, double my, float left, float y, float h, String text, Runnable action) {
        float w = ui.textWidth(text, this.d(12), 1) + this.d(30);
        boolean hot = inside(mx, my, left, y, w, h);
        float a = this.anim("btnl:" + text, hot ? 1 : 0, 14);
        ui.round(left, y, w, h, this.d(12), UiRenderer.white(0.04f + 0.05f * a));
        ui.outline(left, y, w, h, this.d(12), Math.max(1.0f, this.u()), UiRenderer.white(0.07f + 0.08f * a));
        ui.textCentered(text, left + w / 2.0f, y + (h - this.d(12)) / 2.0f - this.d(1), this.d(12), 1, UiRenderer.white(0.85f));
        this.hit(left, y, w, h, 0, action);
        return left + w + this.d(10);
    }

    // ------------------------------------------------------------------ input

    public boolean mouseClicked(double x, double y, int button) {
        float s = this.scale();
        double mx = x * s, my = y * s;
        for (int i = this.hits.size() - 1; i >= 0; i--) {
            float[] r = this.hits.get(i);
            if (this.buttons.get(i) == button && inside(mx, my, r[0], r[1], r[2], r[3])) {
                this.actions.get(i).run();
                UiAudio.play(UiAudio.Cue.TAB);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean method_25401(double x, double y, double h, double v) {
        this.scrollTarget -= (float) v * this.d(70);
        return true;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) { // esc
            this.restore();
            this.returnToGui();
            return true;
        }
        if (key == 257 || key == 335) { // enter
            Hook.INSTANCE.getSettingsManager().saveNow();
            this.returnToGui();
            return true;
        }
        if (key == 65 && (mods & 2) != 0) { // ctrl+a
            for (StorageBlocksSetting.Entry e : this.visibleEntries()) e.setEnabled(true);
            return true;
        }
        if (key == 259 && !this.search.isEmpty()) {
            this.search = this.search.substring(0, this.search.length() - 1);
            this.scrollTarget = 0;
            return true;
        }
        return false;
    }

    public boolean charTyped(char c, int mods) {
        if (!Character.isISOControl(c) && this.search.length() < 32) {
            this.search = this.search + c;
            this.scrollTarget = 0;
        }
        return true;
    }

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        return this.mouseClicked(click.comp_4798(), click.comp_4799(), click.method_74245());
    }

    @Override
    public boolean method_25404(class_11908 key) {
        return this.keyPressed(key.comp_4795(), key.comp_4796(), key.comp_4797());
    }

    @Override
    public boolean method_25400(class_11905 input) {
        return input.method_74227() && this.charTyped((char) input.comp_4793(), input.comp_4794());
    }

    @Override
    public void method_25420(class_332 ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean method_25422() {
        return false;
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private void restore() {
        for (StorageBlocksSetting.Entry e : this.setting.entries()) {
            Object[] o = this.original.get(e.id());
            if (o == null) continue;
            e.setEnabled((Boolean) o[0]);
            e.setColor((Color) o[1]);
        }
    }

    private void returnToGui() {
        class_310.method_1551().method_1507((class_437) Hook.INSTANCE.GUI);
    }
}
