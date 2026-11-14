package hook.feature;

import hook.event.EventListener;
import hook.event.events.TickEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.utils.EncryptedString;

import java.lang.reflect.Field;

/**
 * Renders every chunk the server sent you inside your render distance, also ones that are hidden behind rock or
 * outside what the camera "should" be able to see. Turns off vanilla's and Sodium's occlusion culling (which only
 * builds chunks reachable through open air from the camera, so in Freecam far caves stayed empty until you flew
 * there) and Sodium's fog occlusion.
 */
public final class ForceRenderModule extends Module {
    private final BooleanSetting occlusion = new BooleanSetting(EncryptedString.of("Ignore Occlusion"), true).setDescription(EncryptedString.of("Renders chunks hidden behind blocks too, so caves and bases far away are already built"));
    private final BooleanSetting fog = new BooleanSetting(EncryptedString.of("Ignore Fog Culling"), true).setDescription(EncryptedString.of("Sodium: renders chunks behind the fog distance too"));
    private final BooleanSetting entities = new BooleanSetting(EncryptedString.of("Ignore Entity Culling"), true).setDescription(EncryptedString.of("Sodium: draws entities in hidden chunks as well"));

    private boolean savedSmartCull = true;
    private Boolean savedFog;
    private Boolean savedEntities;
    private boolean applied;
    private static ForceRenderModule instance;

    public ForceRenderModule() {
        super(EncryptedString.of("Force Render"), EncryptedString.of("Renders everything the server sent you, even when performance mods would skip it"), -1, Category.RENDER);
        this.addSettings(this.occlusion, this.fog, this.entities);
        instance = this;
    }

    /**
     * Sodium's chunk search radius around the camera. With Force Render on it reaches every chunk the server sent,
     * even when Freecam took the camera far away from the player: distance camera-to-player plus the loaded radius.
     */
    public static float searchDistance(float base) {
        ForceRenderModule m = instance;
        if (m == null || !m.isEnabled() || m.mc == null || m.mc.field_1724 == null || m.mc.field_1690 == null) return base;
        net.minecraft.class_4184 cam = m.mc.field_1773 == null ? null : m.mc.field_1773.method_19418();
        double away = 0.0;
        if (cam != null) {
            net.minecraft.class_243 c = cam.method_71156();
            double dx = c.field_1352 - m.mc.field_1724.method_23317(), dz = c.field_1350 - m.mc.field_1724.method_23321();
            away = Math.sqrt(dx * dx + dz * dz);
        }
        int loaded = Math.max(m.mc.field_1690.method_38521(), 2) + 2; // server view distance is at most this
        return (float) Math.max(base, away + loaded * 16.0 + 16.0);
    }

    @Override
    public void onEnable() {
        if (this.mc != null) this.savedSmartCull = this.mc.field_1730;
        this.savedFog = sodiumFlag("useFogOcclusion");
        this.savedEntities = sodiumFlag("useEntityCulling");
        this.applied = false;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (this.mc != null) {
            this.mc.field_1730 = this.savedSmartCull;
            if (this.savedFog != null) setSodiumFlag("useFogOcclusion", this.savedFog);
            if (this.savedEntities != null) setSodiumFlag("useEntityCulling", this.savedEntities);
            if (this.mc.field_1769 != null) this.mc.field_1769.method_3292();
        }
        super.onDisable();
    }

    @EventListener
    public void onTick(TickEvent event) {
        if (this.mc == null) return;
        boolean changed = false;
        boolean smartCull = !this.occlusion.getValue() && this.savedSmartCull;
        if (this.mc.field_1730 != smartCull) { this.mc.field_1730 = smartCull; changed = true; }
        if (this.savedFog != null) changed |= setSodiumFlag("useFogOcclusion", this.fog.getValue() ? false : this.savedFog);
        if (this.savedEntities != null) changed |= setSodiumFlag("useEntityCulling", this.entities.getValue() ? false : this.savedEntities);
        if ((changed || !this.applied) && this.mc.field_1769 != null) {
            this.mc.field_1769.method_3292(); // rebuild the visible chunk list with the new rules
            this.applied = true;
        }
    }

    /** Reads a boolean from Sodium's performance options, or null when Sodium isn't installed. */
    private static Boolean sodiumFlag(String name) {
        try {
            Field f = performance().getClass().getField(name);
            return f.getBoolean(performance());
        } catch (Throwable t) {
            return null;
        }
    }

    /** Sets a Sodium performance flag in memory (not saved to its config); returns whether it changed. */
    private static boolean setSodiumFlag(String name, boolean value) {
        try {
            Object perf = performance();
            Field f = perf.getClass().getField(name);
            if (f.getBoolean(perf) == value) return false;
            f.setBoolean(perf, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object performance() throws Exception {
        Class<?> mod = Class.forName("net.caffeinemc.mods.sodium.client.SodiumClientMod");
        Object options = mod.getMethod("options").invoke(null);
        return options.getClass().getField("performance").get(options);
    }
}
