package hook.feature;

import hook.Hook;
import hook.event.EventListener;
import hook.event.events.Render2DEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ModeSetting;
import hook.module.setting.NumberSetting;
import hook.utils.EncryptedString;
import hook.utils.RenderVisibility;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.class_1041;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1542;
import net.minecraft.class_1569;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_1934;
import net.minecraft.class_243;
import net.minecraft.class_332;
import net.minecraft.class_640;

/**
 * Detailed tags above players (and optionally mobs and items) in the client's glass style:
 * name, health, ping, distance, game mode and equipment. Tags shrink with distance and the
 * overall size is adjustable. (Ported from SilentClient.)
 */
public final class NametagsModule extends Module {
    public enum HealthMode { Off, Number, Bar, Both }

    private static NametagsModule instance;
    private final BooleanSetting players = new BooleanSetting(EncryptedString.of("Players"), true).setDescription(EncryptedString.of("Tags above other players"));
    private final BooleanSetting hostiles = new BooleanSetting(EncryptedString.of("Hostile Mobs"), false).setDescription(EncryptedString.of("Zombies, skeletons, creepers..."));
    private final BooleanSetting passives = new BooleanSetting(EncryptedString.of("Passive Mobs"), false).setDescription(EncryptedString.of("Animals, villagers..."));
    private final BooleanSetting items = new BooleanSetting(EncryptedString.of("Dropped Items"), false).setDescription(EncryptedString.of("Item name and stack size"));
    private final BooleanSetting self = new BooleanSetting(EncryptedString.of("Yourself In Freecam"), true).setDescription(EncryptedString.of("Shows your own tag while Freecam is on"));
    private final NumberSetting size = new NumberSetting(EncryptedString.of("Size"), 50.0, 200.0, 100.0, 5.0).getValue(EncryptedString.of("Overall tag size in percent"));
    private final BooleanSetting shrink = new BooleanSetting(EncryptedString.of("Shrink With Distance"), true).setDescription(EncryptedString.of("Farther targets get smaller tags"));
    private final NumberSetting minSize = new NumberSetting(EncryptedString.of("Smallest Size"), 20.0, 100.0, 50.0, 5.0).getValue(EncryptedString.of("Size limit for far away targets in percent"));
    private final BooleanSetting zoomScaling = new BooleanSetting(EncryptedString.of("Zoom Aware"), true).setDescription(EncryptedString.of("Zooming in (Zoomify, OptiFine zoom...) makes tags bigger like walking closer instead of keeping them tiny"));
    private final BooleanSetting shrinkClose = new BooleanSetting(EncryptedString.of("Shrink Up Close"), true).setDescription(EncryptedString.of("Tags get smaller when you walk right up to someone so they do not cover them"));
    private final NumberSetting closeRange = new NumberSetting(EncryptedString.of("Close Range"), 2.0, 12.0, 6.0, 0.5).getValue(EncryptedString.of("Distance in blocks where tags start shrinking"));
    private final NumberSetting closeSize = new NumberSetting(EncryptedString.of("Close Size"), 30.0, 100.0, 60.0, 5.0).getValue(EncryptedString.of("Tag size in percent when right next to the target"));
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 16.0, 256.0, 128.0, 8.0).getValue(EncryptedString.of("Maximum distance in blocks"));
    private final BooleanSetting throughWalls = new BooleanSetting(EncryptedString.of("Through Walls"), true).setDescription(EncryptedString.of("Also shows targets you cannot see"));
    private final ModeSetting<HealthMode> health = new ModeSetting<HealthMode>(EncryptedString.of("Health"), HealthMode.Number, HealthMode.class).setDescription(EncryptedString.of("How health is shown"));
    private final BooleanSetting ping = new BooleanSetting(EncryptedString.of("Ping"), true).setDescription(EncryptedString.of("Connection latency of players"));
    private final BooleanSetting distance = new BooleanSetting(EncryptedString.of("Distance"), false).setDescription(EncryptedString.of("How far away the target is"));
    private final BooleanSetting gameMode = new BooleanSetting(EncryptedString.of("Game Mode"), false).setDescription(EncryptedString.of("S / C / A / SP badge for players"));
    private final BooleanSetting equipment = new BooleanSetting(EncryptedString.of("Equipment"), true).setDescription(EncryptedString.of("Held items and armor above the tag"));
    private final BooleanSetting durability = new BooleanSetting(EncryptedString.of("Durability"), true).setDescription(EncryptedString.of("Durability bars on the equipment"));
    private final NumberSetting maxTags = new NumberSetting(EncryptedString.of("Max Tags"), 1.0, 64.0, 24.0, 1.0).getValue(EncryptedString.of("Only the closest targets get a tag"));
    private final NumberSetting background = new NumberSetting(EncryptedString.of("Background"), 0.0, 100.0, 85.0, 5.0).getValue(EncryptedString.of("Opacity of the tag panel in percent"));

    private static final class_1304[] SLOTS = {class_1304.field_6173, class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166, class_1304.field_6171};
    private final float[] screen = new float[2];

    public NametagsModule() {
        super(EncryptedString.of("Nametags"), EncryptedString.of("Detailed tags with health, ping and equipment above players"), -1, Category.RENDER);
        this.addSettings(this.players, this.hostiles, this.passives, this.items, this.self, this.size, this.shrink, this.minSize, this.zoomScaling, this.shrinkClose, this.closeRange, this.closeSize, this.range, this.throughWalls,
                this.health, this.ping, this.distance, this.gameMode, this.equipment, this.durability, this.maxTags, this.background);
        hook.module.setting.SettingVisibility.set(this.minSize, this.shrink::getValue);
        hook.module.setting.SettingVisibility.set(this.closeRange, this.shrinkClose::getValue);
        hook.module.setting.SettingVisibility.set(this.closeSize, this.shrinkClose::getValue);
        instance = this;
    }

    /** Whether this entity gets one of our tags (and loses its vanilla one). */
    public static boolean targets(class_1297 entity) {
        NametagsModule m = instance;
        if (m == null || !m.isEnabled() || entity == null || !entity.method_5805()) return false;
        if (entity == m.mc.field_1724) {
            return m.self.getValue() && freecamOn();
        }
        if (entity instanceof class_1657 player) return m.players.getValue() && !player.method_7325();
        if (entity instanceof class_1542) return m.items.getValue();
        if (entity instanceof class_1569) return m.hostiles.getValue();
        if (entity instanceof class_1309) return m.passives.getValue();
        return false;
    }

    private static boolean freecamOn() {
        try {
            Module freecam = Hook.INSTANCE.getModuleManager().getModuleByClass(FreecamModule.class);
            return freecam != null && freecam.isEnabled();
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * How much the view is zoomed in compared to the normal FOV setting (1 = no zoom). Read from this frame's
     * projection, so it works with any zoom mod. Wider views (sprinting, speed) count as 1.
     */
    private float zoomFactor() {
        float scale = RenderVisibility.projectionScaleY();
        if (scale <= 0.0f) return 1.0f;
        int fov = (Integer) this.mc.field_1690.method_41808().method_41753();
        float normal = (float) (1.0 / Math.tan(Math.toRadians(fov) / 2.0));
        return Math.max(1.0f, scale / normal);
    }

    private float sizeAt(double blocks, float zoom) {
        float factor = (float) (this.size.getValue() / 100.0);
        if (this.shrink.getValue()) {
            // zoomed in, the target looks as if it was this much closer
            double seen = Math.max(blocks, 0.0) / (this.zoomScaling.getValue() ? zoom : 1.0f);
            factor *= (float) Math.max(this.minSize.getValue() / 100.0, Math.min(1.0, 10.0 / (seen + 4.0)));
        } else if (this.zoomScaling.getValue()) {
            factor *= (float) Math.min(4.0, zoom);
        }
        if (this.shrinkClose.getValue()) {
            // uses the real distance, so zooming never makes tags smaller
            double near = this.closeRange.getValue();
            if (blocks < near) {
                double t = Math.max(0.0, Math.min(1.0, (blocks - 1.0) / Math.max(0.5, near - 1.0)));
                double low = this.closeSize.getValue() / 100.0;
                factor *= (float) (low + (1.0 - low) * t);
            }
        }
        return factor;
    }

    private record Tag(class_1297 entity, double distance, float x, float y, float zoom) {
    }

    @EventListener
    public void onRender2D(Render2DEvent event) {
        if (this.mc.field_1687 == null || this.mc.field_1724 == null || this.mc.field_1690.field_1842) return;
        class_1041 window = this.mc.method_22683();
        int ww = window.method_4489();
        int wh = window.method_4506();
        double max = this.range.getValue();
        float zoom = this.zoomFactor();
        List<Tag> tags = new ArrayList<>();
        for (class_1297 entity : this.mc.field_1687.method_18112()) {
            if (!targets(entity)) continue;
            class_243 feet = entity.method_30950(event.tickDelta);
            double hx = feet.field_1352, hy = feet.field_1351 + entity.method_17682() + (entity instanceof class_1542 ? 0.2 : 0.35), hz = feet.field_1350;
            double dist = RenderVisibility.cameraDistance(hx, hy, hz);
            if (dist > max) continue;
            if (!this.throughWalls.getValue() && entity instanceof class_1309 living && !this.mc.field_1724.method_6057(living)) continue;
            if (!RenderVisibility.project(hx, hy, hz, ww, wh, this.screen)) continue;
            if (this.screen[0] < -300 || this.screen[0] > ww + 300 || this.screen[1] < -300 || this.screen[1] > wh + 300) continue;
            tags.add(new Tag(entity, dist, this.screen[0], this.screen[1], zoom));
        }
        if (tags.isEmpty()) return;
        tags.sort(Comparator.comparingDouble(Tag::distance).reversed());
        int start = Math.max(0, tags.size() - this.maxTags.getIntValue());
        class_332 ctx = event.context;
        float gui = Math.max(1.0f, window.method_4495());
        ctx.method_51448().pushMatrix();
        ctx.method_51448().scale(1.0f / gui, 1.0f / gui);
        try {
            MinecraftUiRenderer ui = new MinecraftUiRenderer(ctx, gui);
            for (int i = start; i < tags.size(); i++) {
                this.drawTag(ctx, ui, gui, tags.get(i));
            }
        } finally {
            ctx.method_51448().popMatrix();
        }
    }

    private void drawTag(class_332 ctx, UiRenderer ui, float gui, Tag tag) {
        class_1297 entity = tag.entity();
        float unit = gui * this.sizeAt(tag.distance(), tag.zoom());
        float textPx = 7.5f * unit;
        float smallPx = 5.5f * unit;
        float gap = 4.0f * unit;
        float pad = 5.0f * unit;

        List<String> texts = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();
        class_640 info = entity instanceof class_1657 p && this.mc.method_1562() != null ? this.mc.method_1562().method_2871(p.method_5667()) : null;
        String badge = null;
        if (this.gameMode.getValue() && info != null) {
            class_1934 mode = info.method_2958();
            badge = mode == class_1934.field_9220 ? "C" : mode == class_1934.field_9216 ? "A" : mode == class_1934.field_9219 ? "SP" : "S";
        }
        if (entity instanceof class_1542 item) {
            class_1799 stack = item.method_6983();
            texts.add(stack.method_7964().getString()); colors.add(UiRenderer.white(0.96f)); weights.add(1);
            if (stack.method_7947() > 1) { texts.add("x" + stack.method_7947()); colors.add(UiRenderer.white(0.55f)); weights.add(0); }
        } else {
            int team = entity.method_22861() & 0xFFFFFF;
            texts.add(entity.method_5477().getString()); colors.add(team == 0xFFFFFF ? UiRenderer.white(0.96f) : 0xFF000000 | team); weights.add(1);
        }
        float ratio = -1.0f;
        boolean absorbing = false;
        HealthMode mode = (HealthMode) this.health.getValue();
        if (entity instanceof class_1309 living && mode != HealthMode.Off) {
            float hp = living.method_6032();
            float absorption = living.method_6067();
            absorbing = absorption > 0.0f;
            ratio = Math.max(0.0f, Math.min(1.0f, hp / Math.max(1.0f, living.method_6063())));
            if (mode == HealthMode.Number || mode == HealthMode.Both) {
                float total = hp + absorption;
                String text = Math.abs(total - Math.round(total)) < 0.05f ? Integer.toString(Math.round(total)) : String.format("%.1f", total);
                texts.add(text); colors.add(absorbing ? 0xFFFFC74D : healthColor(ratio)); weights.add(1);
            }
        }
        if (this.ping.getValue() && info != null) { texts.add(info.method_2959() + "ms"); colors.add(UiRenderer.white(0.55f)); weights.add(0); }
        if (this.distance.getValue()) { texts.add(Math.round(tag.distance()) + "m"); colors.add(UiRenderer.white(0.55f)); weights.add(0); }

        float content = 0.0f;
        float[] widths = new float[texts.size()];
        for (int i = 0; i < texts.size(); i++) {
            widths[i] = ui.textWidth(texts.get(i), textPx, weights.get(i));
            content += widths[i] + (i > 0 ? gap : 0.0f);
        }
        float badgeW = 0.0f;
        if (badge != null) {
            badgeW = ui.textWidth(badge, smallPx, 1) + 6.0f * unit;
            content += badgeW + gap;
        }
        boolean bar = ratio >= 0.0f && (mode == HealthMode.Bar || mode == HealthMode.Both);
        float w = content + pad * 2.0f;
        float h = (bar ? 16.5f : 13.5f) * unit;
        float x = tag.x() - w / 2.0f;
        float y = tag.y() - h;
        float r = 5.0f * unit;
        float opacity = (float) (this.background.getValue() / 100.0);

        ui.shadow(x, y, w, h, r, 4.0f * unit, UiRenderer.argb(0.3f * opacity, 0, 0, 0));
        ui.round(x, y, w, h, r, UiEasing.mix(UiPalette.glass(0.88f * opacity), UiRenderer.argb(0.88f * opacity, 10, 11, 14), 0.4f));
        ui.outline(x, y, w, h, r, Math.max(1.0f, unit * 0.35f), UiRenderer.white(0.12f * opacity));

        float rowY = y + 6.75f * unit - textPx / 2.0f;
        float cursor = x + pad;
        if (badge != null) {
            float bh = 9.0f * unit;
            ui.round(cursor, y + 6.75f * unit - bh / 2.0f, badgeW, bh, 3.0f * unit, UiPalette.accent(0.3f));
            ui.textCentered(badge, cursor + badgeW / 2.0f, y + 6.75f * unit - smallPx / 2.0f, smallPx, 1, UiPalette.accent(1.0f));
            cursor += badgeW + gap;
        }
        for (int i = 0; i < texts.size(); i++) {
            ui.text(texts.get(i), cursor, rowY, textPx, weights.get(i), colors.get(i));
            cursor += widths[i] + gap;
        }
        if (bar) {
            float barH = Math.max(1.0f, 2.0f * unit);
            float barX = x + pad, barW = w - pad * 2.0f, barY = y + 12.5f * unit;
            ui.round(barX, barY, barW, barH, barH / 2.0f, UiRenderer.white(0.16f));
            ui.round(barX, barY, Math.max(barH, barW * ratio), barH, barH / 2.0f, absorbing ? 0xFFFFC74D : healthColor(ratio));
        }
        if (this.equipment.getValue() && entity instanceof class_1309 living) {
            this.drawEquipment(ctx, living, tag.x(), y - 2.0f * unit, unit);
        }
    }

    private void drawEquipment(class_332 ctx, class_1309 living, float centerX, float bottom, float unit) {
        List<class_1799> stacks = new ArrayList<>();
        for (class_1304 slot : SLOTS) {
            class_1799 stack = living.method_6118(slot);
            if (!stack.method_7960()) stacks.add(stack);
        }
        if (stacks.isEmpty()) return;
        float itemPx = 12.0f * unit;
        float spacing = unit;
        float total = stacks.size() * itemPx + (stacks.size() - 1) * spacing;
        float left = centerX - total / 2.0f;
        float top = bottom - itemPx;
        for (int i = 0; i < stacks.size(); i++) {
            class_1799 stack = stacks.get(i);
            ctx.method_51448().pushMatrix();
            ctx.method_51448().translate(left + i * (itemPx + spacing), top);
            ctx.method_51448().scale(itemPx / 16.0f);
            ctx.method_51427(stack, 0, 0);
            if (this.durability.getValue()) {
                ctx.method_51431(this.mc.field_1772, stack, 0, 0);
            }
            ctx.method_51448().popMatrix();
        }
    }

    private static int healthColor(float ratio) {
        return ratio > 0.5f ? lerp(0xFFFFD95A, 0xFF7CE38B, (ratio - 0.5f) * 2.0f) : lerp(0xFFFF5A5A, 0xFFFFD95A, ratio * 2.0f);
    }

    private static int lerp(int from, int to, float t) {
        int r = Math.round((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int g = Math.round((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
