package hook.feature;

import hook.module.Category;
import hook.module.Module;
import hook.module.setting.ModeSetting;
import hook.module.setting.StorageBlocksSetting;
import hook.utils.EncryptedString;
import net.minecraft.class_1792;
import net.minecraft.class_1802;
import net.minecraft.class_2394;
import net.minecraft.class_2396;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

import java.awt.Color;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Hides particles. "Hide All" removes every particle except the ones you tick in the list; "Hide Selected" only
 * removes the ticked ones. The list contains every particle type in the game.
 */
public final class NoParticlesModule extends Module {
    public enum Mode { HideAll, HideSelected }

    private static NoParticlesModule instance;

    private final ModeSetting<Mode> mode = new ModeSetting<Mode>(EncryptedString.of("Mode"), Mode.HideAll, Mode.class).setDescription(EncryptedString.of("HideAll: no particles except the ticked ones. HideSelected: only the ticked particles are hidden"));
    private final StorageBlocksSetting particles = createList();

    public NoParticlesModule() {
        super(EncryptedString.of("No Particles"), EncryptedString.of("Hides all particles or just the ones you choose"), -1, Category.RENDER);
        this.particles.setDescription(EncryptedString.of("HideAll: ticked particles stay visible. HideSelected: ticked particles get hidden"));
        this.addSettings(this.mode, this.particles);
        instance = this;
    }

    /** Whether a particle of this type should not be spawned. */
    public static boolean hides(class_2396<?> type) {
        NoParticlesModule m = instance;
        if (m == null || !m.isEnabled() || type == null) return false;
        class_2960 id = class_7923.field_41180.method_10221(type);
        return m.hidesId(id == null ? null : id.toString());
    }

    public static boolean hides(class_2394 effect) {
        return effect != null && hides(effect.method_10295());
    }

    /** Block breaking/destroy particles, which vanilla creates without going through a particle type. */
    public static boolean hidesBlockCrumbs() {
        NoParticlesModule m = instance;
        return m != null && m.isEnabled() && m.hidesId("minecraft:block");
    }

    private boolean hidesId(String id) {
        StorageBlocksSetting.Entry entry = id == null ? null : this.particles.get(id);
        boolean ticked = entry != null && entry.enabled();
        return this.mode.getValue() == Mode.HideAll ? !ticked : ticked;
    }

    private static StorageBlocksSetting createList() {
        StorageBlocksSetting list = new StorageBlocksSetting(EncryptedString.of("Particles")).pickerOnly();
        for (class_2396<?> type : class_7923.field_41180) {
            class_2960 id = class_7923.field_41180.method_10221(type);
            if (id == null) continue;
            list.add(id.toString(), prettyName(id.method_12832()), iconFor(id.method_12832()), false, Color.WHITE);
        }
        return list;
    }

    private static String prettyName(String path) {
        StringBuilder out = new StringBuilder();
        for (String part : path.split("_")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    private static final Map<String, class_1792> ICONS = new HashMap<>();

    static {
        ICONS.put("block", class_1802.field_20391);
        ICONS.put("block_marker", class_1802.field_8077);
        ICONS.put("falling_dust", class_1802.field_8858);
        ICONS.put("dust", class_1802.field_8725);
        ICONS.put("dust_color_transition", class_1802.field_8725);
        ICONS.put("flame", class_1802.field_8810);
        ICONS.put("small_flame", class_1802.field_8810);
        ICONS.put("soul_fire_flame", class_1802.field_22001);
        ICONS.put("lava", class_1802.field_8187);
        ICONS.put("dripping_lava", class_1802.field_8187);
        ICONS.put("falling_lava", class_1802.field_8187);
        ICONS.put("landing_lava", class_1802.field_8187);
        ICONS.put("smoke", class_1802.field_17346);
        ICONS.put("large_smoke", class_1802.field_17346);
        ICONS.put("campfire_cosy_smoke", class_1802.field_17346);
        ICONS.put("campfire_signal_smoke", class_1802.field_17346);
        ICONS.put("explosion", class_1802.field_8626);
        ICONS.put("explosion_emitter", class_1802.field_8626);
        ICONS.put("heart", class_1802.field_8264);
        ICONS.put("damage_indicator", class_1802.field_8264);
        ICONS.put("crit", class_1802.field_8802);
        ICONS.put("enchanted_hit", class_1802.field_8598);
        ICONS.put("enchant", class_1802.field_8657);
        ICONS.put("totem_of_undying", class_1802.field_8288);
        ICONS.put("bubble", class_1802.field_8705);
        ICONS.put("bubble_pop", class_1802.field_8705);
        ICONS.put("splash", class_1802.field_8705);
        ICONS.put("rain", class_1802.field_8705);
        ICONS.put("dripping_water", class_1802.field_8705);
        ICONS.put("falling_water", class_1802.field_8705);
        ICONS.put("portal", class_1802.field_8634);
        ICONS.put("reverse_portal", class_1802.field_8634);
        ICONS.put("note", class_1802.field_8643);
        ICONS.put("firework", class_1802.field_8639);
        ICONS.put("happy_villager", class_1802.field_8687);
        ICONS.put("angry_villager", class_1802.field_8687);
        ICONS.put("snowflake", class_1802.field_8543);
        ICONS.put("item_snowball", class_1802.field_8543);
        ICONS.put("item_slime", class_1802.field_8777);
        ICONS.put("item", class_1802.field_8143);
        ICONS.put("sweep_attack", class_1802.field_8802);
        ICONS.put("cloud", class_1802.field_19044);
        ICONS.put("poof", class_1802.field_19044);
        ICONS.put("effect", class_1802.field_8574);
        ICONS.put("entity_effect", class_1802.field_8574);
        ICONS.put("instant_effect", class_1802.field_8574);
        ICONS.put("witch", class_1802.field_8574);
        ICONS.put("gust", class_1802.field_49098);
        ICONS.put("small_gust", class_1802.field_49098);
        ICONS.put("gust_emitter_large", class_1802.field_49098);
        ICONS.put("gust_emitter_small", class_1802.field_49098);
    }

    private static class_1792 iconFor(String path) {
        class_1792 icon = ICONS.get(path.toLowerCase(Locale.ROOT));
        return icon != null ? icon : class_1802.field_8601;
    }
}
