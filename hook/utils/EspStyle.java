package hook.utils;

import hook.feature.UiPalette;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ModeSetting;
import hook.module.setting.NumberSetting;
import hook.module.setting.RgbSetting;
import hook.module.setting.Setting;
import hook.module.setting.SettingVisibility;
import net.minecraft.class_243;
import net.minecraft.class_4184;
import net.minecraft.class_4587;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared look for all ESP modules: shape (fill, outline, both, corners), opacity, outline width, glow, pulse,
 * distance fade, colour mode, max distance and tracer style. A module adds its settings, then each frame calls
 * {@link #begin}, {@link #box} for every target and {@link #render}.
 */
public final class EspStyle {
    public enum Shape { Both, Fill, Outline, Corners }
    public enum ColorMode { Default, Static, Theme, Rainbow }

    public final ModeSetting<Shape> shape;
    public final ModeSetting<ColorMode> colorMode;
    public final RgbSetting color;
    public final NumberSetting fillOpacity;
    public final NumberSetting outlineOpacity;
    public final NumberSetting outlineWidth;
    public final BooleanSetting glow;
    public final NumberSetting glowStrength;
    public final BooleanSetting pulse;
    public final BooleanSetting distanceFade;
    public final NumberSetting maxDistance;
    public final BooleanSetting tracers;
    public final NumberSetting tracerWidth;
    public final NumberSetting tracerOpacity;

    private final List<RenderUtils.ColoredBox> fills = new ArrayList<>();
    private final List<RenderUtils.ColoredLine> lines = new ArrayList<>();
    private final List<Target> targets = new ArrayList<>();
    private static final int MAX_OUTLINED = 1500;
    private static final int MAX_GLOW = 200;
    private static final int MAX_TRACERS = 1000;
    private int tracerCount;
    private class_243 camera = class_243.field_1353;
    private class_243 tracerStart = class_243.field_1353;
    private double pulseFactor = 1.0;
    private long now;

    public EspStyle(Shape defaultShape, int defaultFill, int defaultColor) {
        this.shape = new ModeSetting<Shape>(EncryptedString.of("Shape"), defaultShape, Shape.class).setDescription(EncryptedString.of("Filled box, outline, both, or only the corners"));
        this.colorMode = new ModeSetting<ColorMode>(EncryptedString.of("Color Mode"), ColorMode.Default, ColorMode.class).setDescription(EncryptedString.of("Default: the module's own colours. Static: one colour. Theme: client accent. Rainbow: animated"));
        this.color = new RgbSetting(EncryptedString.of("Color"), defaultColor);
        this.fillOpacity = new NumberSetting(EncryptedString.of("Fill Opacity"), 0.0, 255.0, defaultFill, 1.0).getValue(EncryptedString.of("How see-through the filled box is"));
        this.outlineOpacity = new NumberSetting(EncryptedString.of("Outline Opacity"), 0.0, 255.0, 230.0, 1.0).getValue(EncryptedString.of("How strong the outline is"));
        this.outlineWidth = new NumberSetting(EncryptedString.of("Outline Width"), 1.0, 8.0, 2.0, 0.5).getValue(EncryptedString.of("Thickness of the outline in pixels"));
        this.glow = new BooleanSetting(EncryptedString.of("Glow"), true).setDescription(EncryptedString.of("Soft glowing halo around every box"));
        this.glowStrength = new NumberSetting(EncryptedString.of("Glow Strength"), 10.0, 100.0, 45.0, 5.0).getValue(EncryptedString.of("How bright the glow is"));
        this.pulse = new BooleanSetting(EncryptedString.of("Pulse"), false).setDescription(EncryptedString.of("Gently breathes the opacity"));
        this.distanceFade = new BooleanSetting(EncryptedString.of("Distance Fade"), true).setDescription(EncryptedString.of("Far away targets are drawn fainter so close ones stand out"));
        this.maxDistance = new NumberSetting(EncryptedString.of("Max Distance"), 0.0, 512.0, 0.0, 8.0).getValue(EncryptedString.of("Only shows targets closer than this (0 = no limit)"));
        this.tracers = new BooleanSetting(EncryptedString.of("Tracers"), false).setDescription(EncryptedString.of("Draws a line from your crosshair to every target"));
        this.tracerWidth = new NumberSetting(EncryptedString.of("Tracer Width"), 1.0, 6.0, 1.5, 0.5).getValue(EncryptedString.of("Thickness of the tracer lines"));
        this.tracerOpacity = new NumberSetting(EncryptedString.of("Tracer Opacity"), 20.0, 255.0, 200.0, 5.0).getValue(EncryptedString.of("How strong the tracer lines are"));

        SettingVisibility.set(this.color, () -> this.colorMode.getValue() == ColorMode.Static);
        SettingVisibility.set(this.fillOpacity, () -> this.shape.getValue() == Shape.Both || this.shape.getValue() == Shape.Fill);
        SettingVisibility.set(this.outlineOpacity, () -> this.shape.getValue() != Shape.Fill);
        SettingVisibility.set(this.outlineWidth, () -> this.shape.getValue() != Shape.Fill);
        SettingVisibility.set(this.glowStrength, this.glow::getValue);
        SettingVisibility.set(this.tracerWidth, this.tracers::getValue);
        SettingVisibility.set(this.tracerOpacity, this.tracers::getValue);
    }

    public Setting[] settings() {
        return new Setting[]{this.shape, this.colorMode, this.color, this.fillOpacity, this.outlineOpacity, this.outlineWidth,
                this.glow, this.glowStrength, this.pulse, this.distanceFade, this.maxDistance, this.tracers, this.tracerWidth, this.tracerOpacity};
    }

    /** Starts a frame; returns false when nothing can be drawn. */
    public boolean begin(class_4184 cam) {
        this.fills.clear();
        this.lines.clear();
        this.targets.clear();
        this.tracerCount = 0;
        if (cam == null) return false;
        this.camera = cam.method_71156();
        this.tracerStart = RenderUtils.getTracerStart(cam);
        this.now = System.currentTimeMillis();
        this.pulseFactor = this.pulse.getValue() ? 0.65 + 0.35 * (0.5 + 0.5 * Math.sin(this.now / 320.0)) : 1.0;
        return true;
    }

    /** Whether a target at this point is inside Max Distance (cheap pre-check before building boxes). */
    public boolean inRange(double x, double y, double z) {
        double max = this.maxDistance.getValue();
        if (max <= 0.0) return true;
        double dx = x - this.camera.field_1352, dy = y - this.camera.field_1351, dz = z - this.camera.field_1350;
        return dx * dx + dy * dy + dz * dz <= max * max;
    }

    public boolean wantsTracers() {
        return this.tracers.getValue();
    }

    /** Adds one target. {@code base} is the module's own colour for it (alpha ignored). */
    public void box(double x1, double y1, double z1, double x2, double y2, double z2, Color base, int index) {
        double cx = (x1 + x2) * 0.5, cy = (y1 + y2) * 0.5, cz = (z1 + z2) * 0.5;
        if (!this.inRange(cx, cy, cz)) return;
        boolean onScreen = RenderVisibility.box(x1, y1, z1, x2, y2, z2);
        if (!onScreen && !this.tracers.getValue()) return;
        Color rgb = this.resolve(base, index, cx, cy, cz);
        double fade = this.fade(cx, cy, cz) * this.pulseFactor;
        if (this.tracers.getValue() && this.tracerCount < MAX_TRACERS) {
            this.tracerCount++;
            this.lines.add(new RenderUtils.ColoredLine(this.tracerStart, new class_243(cx, cy, cz), withAlpha(rgb, this.tracerOpacity.getValue() * fade), this.tracerWidth.getFloatValue()));
        }
        if (onScreen) this.targets.add(new Target(x1, y1, z1, x2, y2, z2, rgb, fade, this.camera.method_1028(cx, cy, cz)));
    }

    /** Draws everything added since {@link #begin}. The caller has applied the camera transform. */
    public void render(class_4587 matrices) {
        // Outlines and glow are many lines per box; with thousands of targets (e.g. Tunnel ESP) only the closest get
        // them, so the frame never floods the vertex buffer. Fills are cheap and stay on every target.
        if (this.targets.size() > MAX_GLOW) this.targets.sort((a, b) -> Double.compare(a.dist, b.dist));
        Shape s = (Shape) this.shape.getValue();
        float width = this.outlineWidth.getFloatValue();
        double g = this.glowStrength.getValue() / 100.0;
        for (int i = 0; i < this.targets.size(); i++) {
            Target t = this.targets.get(i);
            if (s == Shape.Both || s == Shape.Fill || (i >= MAX_OUTLINED && s != Shape.Fill)) {
                double fa = s == Shape.Both || s == Shape.Fill ? this.fillOpacity.getValue() : this.outlineOpacity.getValue() * 0.35;
                this.fills.add(new RenderUtils.ColoredBox(t.x1, t.y1, t.z1, t.x2, t.y2, t.z2, withAlpha(t.rgb, fa * t.fade)));
            }
            if (i >= MAX_OUTLINED) continue;
            double outline = this.outlineOpacity.getValue() * t.fade;
            if (s == Shape.Both || s == Shape.Outline) {
                RenderUtils.addBoxOutline(this.lines, t.x1, t.y1, t.z1, t.x2, t.y2, t.z2, withAlpha(t.rgb, outline), width);
            } else if (s == Shape.Corners) {
                this.corners(t.x1, t.y1, t.z1, t.x2, t.y2, t.z2, withAlpha(t.rgb, outline), width);
            }
            if (!this.glow.getValue() || i >= MAX_GLOW) continue;
            double size = Math.max(t.x2 - t.x1, Math.max(t.y2 - t.y1, t.z2 - t.z1));
            for (int layer = 1; layer <= 3; layer++) {
                double grow = size * 0.025 * layer;
                double a = (s == Shape.Fill ? this.fillOpacity.getValue() + 60.0 : outline) * g * t.fade / (layer * 1.6);
                RenderUtils.addBoxOutline(this.lines, t.x1 - grow, t.y1 - grow, t.z1 - grow, t.x2 + grow, t.y2 + grow, t.z2 + grow, withAlpha(t.rgb, a), width + layer * 1.5f);
            }
            double e = size * 0.04;
            this.fills.add(new RenderUtils.ColoredBox(t.x1 - e, t.y1 - e, t.z1 - e, t.x2 + e, t.y2 + e, t.z2 + e, withAlpha(t.rgb, 22.0 * g * t.fade)));
        }
        RenderUtils.renderFilledBoxes(matrices, this.fills);
        RenderUtils.renderLines(matrices, this.lines);
    }

    private record Target(double x1, double y1, double z1, double x2, double y2, double z2, Color rgb, double fade, double dist) {
    }

    private Color resolve(Color base, int index, double x, double y, double z) {
        switch ((ColorMode) this.colorMode.getValue()) {
            case Static: {
                return new Color(this.color.getArgb() & 0xFFFFFF);
            }
            case Theme: {
                Color c = UiPalette.accentColor(255);
                return new Color(c.getRed(), c.getGreen(), c.getBlue());
            }
            case Rainbow: {
                float hue = (float) ((this.now % 6000L) / 6000.0 + (x + z) * 0.004 + y * 0.002 + index * 0.07);
                return Color.getHSBColor(hue - (float) Math.floor(hue), 0.65f, 1.0f);
            }
            default: {
                return base == null ? Color.WHITE : base;
            }
        }
    }

    private double fade(double x, double y, double z) {
        if (!this.distanceFade.getValue()) return 1.0;
        double d = Math.sqrt(this.camera.method_1028(x, y, z));
        // full strength up to 16 blocks, down to 35% at 160 blocks
        return Math.max(0.35, 1.0 - Math.max(0.0, d - 16.0) / 220.0);
    }

    private void corners(double x1, double y1, double z1, double x2, double y2, double z2, Color c, float w) {
        double lx = (x2 - x1) * 0.28, ly = (y2 - y1) * 0.28, lz = (z2 - z1) * 0.28;
        double[] xs = {x1, x2}, ys = {y1, y2}, zs = {z1, z2};
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) for (int k = 0; k < 2; k++) {
            double x = xs[i], y = ys[j], z = zs[k];
            double sx = i == 0 ? lx : -lx, sy = j == 0 ? ly : -ly, sz = k == 0 ? lz : -lz;
            class_243 p = new class_243(x, y, z);
            this.lines.add(new RenderUtils.ColoredLine(p, new class_243(x + sx, y, z), c, w));
            this.lines.add(new RenderUtils.ColoredLine(p, new class_243(x, y + sy, z), c, w));
            this.lines.add(new RenderUtils.ColoredLine(p, new class_243(x, y, z + sz), c, w));
        }
    }

    private static Color withAlpha(Color c, double alpha) {
        int a = (int) Math.max(0, Math.min(255, Math.round(alpha)));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
