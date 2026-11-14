package hook.feature;

import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ModeSetting;
import hook.module.setting.NumberSetting;
import hook.module.setting.RgbSetting;
import hook.module.setting.SettingVisibility;
import hook.utils.EncryptedString;
import net.minecraft.class_1294;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_4184;
import net.minecraft.class_5636;
import org.joml.Vector4f;

/**
 * Removes or reshapes fog. Every fog source can be switched separately (render distance edge, weather/biome,
 * sky, clouds, water, lava, powder snow, blindness, darkness); "Custom" mode moves the fog to a chosen distance
 * instead of removing it, and the fog colour can be tinted.
 */
public final class NoFogModule extends Module {
    public enum Mode { Remove, Custom }

    private static final float FAR = 1.0E7f;
    private static NoFogModule instance;

    private final ModeSetting<Mode> mode = new ModeSetting<Mode>(EncryptedString.of("Mode"), Mode.Remove, Mode.class).setDescription(EncryptedString.of("Remove fog completely or move it to a custom distance"));
    private final NumberSetting start = new NumberSetting(EncryptedString.of("Fog Start"), 0.0, 300.0, 90.0, 5.0).getValue(EncryptedString.of("Where fog begins, in percent of your render distance"));
    private final NumberSetting end = new NumberSetting(EncryptedString.of("Fog End"), 10.0, 400.0, 150.0, 5.0).getValue(EncryptedString.of("Where fog is fully thick, in percent of your render distance"));
    private final BooleanSetting distance = new BooleanSetting(EncryptedString.of("Render Distance Fog"), true).setDescription(EncryptedString.of("The fog at the edge of loaded chunks"));
    private final BooleanSetting environment = new BooleanSetting(EncryptedString.of("Weather & Biome Fog"), true).setDescription(EncryptedString.of("Rain, thunder, nether and end fog"));
    private final BooleanSetting sky = new BooleanSetting(EncryptedString.of("Sky Fog"), true).setDescription(EncryptedString.of("Fog blending the sky into the horizon"));
    private final BooleanSetting clouds = new BooleanSetting(EncryptedString.of("Cloud Fog"), true).setDescription(EncryptedString.of("Fog that fades far away clouds"));
    private final BooleanSetting water = new BooleanSetting(EncryptedString.of("Water"), true).setDescription(EncryptedString.of("Clear view underwater"));
    private final BooleanSetting lava = new BooleanSetting(EncryptedString.of("Lava"), true).setDescription(EncryptedString.of("Clear view inside lava"));
    private final BooleanSetting snow = new BooleanSetting(EncryptedString.of("Powder Snow"), true).setDescription(EncryptedString.of("Clear view inside powder snow"));
    private final BooleanSetting blindness = new BooleanSetting(EncryptedString.of("Blindness"), false).setDescription(EncryptedString.of("Removes the blindness effect fog"));
    private final BooleanSetting darkness = new BooleanSetting(EncryptedString.of("Darkness"), false).setDescription(EncryptedString.of("Removes the warden darkness fog"));
    private final BooleanSetting tint = new BooleanSetting(EncryptedString.of("Custom Color"), false).setDescription(EncryptedString.of("Tints the remaining fog"));
    private final RgbSetting color = new RgbSetting(EncryptedString.of("Fog Color"), 0xFF9FB8D8);
    private final NumberSetting tintStrength = new NumberSetting(EncryptedString.of("Color Strength"), 0.0, 100.0, 60.0, 5.0).getValue(EncryptedString.of("How strongly the fog color is replaced"));

    public NoFogModule() {
        super(EncryptedString.of("No Fog"), EncryptedString.of("Removes or customizes every kind of fog"), -1, Category.RENDER);
        this.addSettings(this.mode, this.start, this.end, this.distance, this.environment, this.sky, this.clouds,
                this.water, this.lava, this.snow, this.blindness, this.darkness, this.tint, this.color, this.tintStrength);
        SettingVisibility.set(this.start, () -> this.mode.getValue() == Mode.Custom);
        SettingVisibility.set(this.end, () -> this.mode.getValue() == Mode.Custom);
        SettingVisibility.set(this.color, this.tint::getValue);
        SettingVisibility.set(this.tintStrength, this.tint::getValue);
        instance = this;
    }

    /**
     * Rewrites the fog values vanilla is about to upload.
     * @param fog environmentalStart, environmentalEnd, renderDistanceStart, renderDistanceEnd, skyEnd, cloudEnd
     */
    public static void apply(class_4184 camera, float[] fog, Vector4f fogColor) {
        NoFogModule m = instance;
        if (m == null || !m.isEnabled() || camera == null) return;
        class_5636 medium = camera.method_19334();
        boolean air = medium == class_5636.field_27888 || medium == class_5636.field_60563;
        boolean effect = false;
        class_1297 viewer = camera.method_19331();
        if (viewer instanceof class_1309 living) {
            if (living.method_6059(class_1294.field_5919)) {
                if (!m.blindness.getValue()) return;
                effect = true;
            }
            if (living.method_6059(class_1294.field_38092)) {
                if (!m.darkness.getValue()) return;
                effect = true;
            }
        }
        float base = Math.max(16.0f, fog[3]);
        boolean custom = m.mode.getValue() == Mode.Custom;
        float s = custom ? base * (float) (m.start.getValue() / 100.0) : FAR;
        float e = custom ? Math.max(s + 1.0f, base * (float) (m.end.getValue() / 100.0)) : FAR + 1.0f;

        boolean env;
        boolean edge;
        if (air) {
            env = m.environment.getValue() || effect;
            edge = m.distance.getValue();
        } else {
            boolean on = medium == class_5636.field_27886 ? m.water.getValue()
                    : medium == class_5636.field_27885 ? m.lava.getValue()
                    : medium == class_5636.field_27887 && m.snow.getValue();
            env = on;
            edge = on || m.distance.getValue();
        }
        if (env) { fog[0] = s; fog[1] = e; }
        if (edge) { fog[2] = s; fog[3] = e; }
        if (air && m.sky.getValue()) fog[4] = custom ? e : FAR;
        if (air && m.clouds.getValue()) fog[5] = custom ? Math.max(fog[5], e) : FAR;

        if (m.tint.getValue() && fogColor != null) {
            int c = m.color.getArgb();
            float k = (float) (m.tintStrength.getValue() / 100.0);
            fogColor.x += ((c >> 16 & 0xFF) / 255.0f - fogColor.x) * k;
            fogColor.y += ((c >> 8 & 0xFF) / 255.0f - fogColor.y) * k;
            fogColor.z += ((c & 0xFF) / 255.0f - fogColor.z) * k;
        }
    }
}
