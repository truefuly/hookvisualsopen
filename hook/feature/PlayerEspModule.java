package hook.feature;

import hook.event.EventListener;
import hook.event.events.Render3DEvent;
import hook.module.Category;
import hook.module.Module;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ModeSetting;
import hook.module.setting.NumberSetting;
import hook.utils.EncryptedString;
import hook.utils.EspStyle;
import hook.utils.RenderUtils;
import java.awt.Color;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_4184;
import net.minecraft.class_4587;

/** Shows players through walls with a configurable box, glow and tracers. */
public final class PlayerEspModule extends Module {
    public enum PlayerColor { Theme, Health, Distance }

    private final EspStyle style = new EspStyle(EspStyle.Shape.Both, 45, 0xFFFF5A6E);
    private final ModeSetting<PlayerColor> playerColor = new ModeSetting<PlayerColor>(EncryptedString.of("Default Colors"), PlayerColor.Theme, PlayerColor.class).setDescription(EncryptedString.of("Colours used in Default colour mode: client theme, by health (green to red) or by distance (red close, green far)"));
    private final NumberSetting expand = new NumberSetting(EncryptedString.of("Expand"), 0.0, 0.5, 0.05, 0.01).getValue(EncryptedString.of("Makes the box a bit bigger than the player"));
    private final BooleanSetting invisible = new BooleanSetting(EncryptedString.of("Show Invisible"), true).setDescription(EncryptedString.of("Also shows players with the invisibility effect"));

    public PlayerEspModule() {
        super(EncryptedString.of("Player ESP"), EncryptedString.of("Renders players through walls"), -1, Category.RENDER);
        this.addSettings(this.style.settings());
        this.addSettings(this.playerColor, this.expand, this.invisible);
    }

    @EventListener
    public void onRender3D(Render3DEvent event) {
        if (this.mc.field_1687 == null || this.mc.field_1724 == null) return;
        class_4184 camera = RenderUtils.getCamera();
        if (camera == null || !camera.method_19332() || !this.style.begin(camera)) return;
        float tickProgress = camera.method_55437();
        double grow = this.expand.getValue();
        int index = 0;
        for (class_1657 player : this.mc.field_1687.method_18456()) {
            if (player == this.mc.field_1724 || player.method_31481() || !player.method_5805()) continue;
            if (!this.invisible.getValue() && player.method_5767()) continue;
            class_243 pos = player.method_30950(tickProgress);
            double half = player.method_17681() * 0.5 + grow;
            this.style.box(pos.field_1352 - half, pos.field_1351 - grow, pos.field_1350 - half,
                    pos.field_1352 + half, pos.field_1351 + player.method_17682() + grow, pos.field_1350 + half,
                    this.defaultColor(player, pos), index++);
        }
        class_4587 matrices = event.matrixStack;
        matrices.method_22903();
        RenderUtils.applyCameraTransform(matrices, camera);
        this.style.render(matrices);
        matrices.method_22909();
    }

    private Color defaultColor(class_1657 player, class_243 pos) {
        switch ((PlayerColor) this.playerColor.getValue()) {
            case Health: {
                float max = Math.max(1.0f, player.method_6063());
                float t = Math.max(0.0f, Math.min(1.0f, (player.method_6032() + player.method_6067()) / max));
                return Color.getHSBColor(t * 0.33f, 0.75f, 1.0f);
            }
            case Distance: {
                double d = Math.sqrt(this.mc.field_1724.method_5707(pos));
                float t = (float) Math.max(0.0, Math.min(1.0, d / 48.0));
                return Color.getHSBColor(t * 0.33f, 0.75f, 1.0f);
            }
            default: {
                Color c = UiPalette.accentColor(255);
                return new Color(c.getRed(), c.getGreen(), c.getBlue());
            }
        }
    }
}
