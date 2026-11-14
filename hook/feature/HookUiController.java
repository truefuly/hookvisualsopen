/*
 * Decompiled with CFR 0.152.
 */
package hook.feature;

import hook.Hook;
import hook.feature.ConfigUiScreen;
import hook.feature.HalloweenGhostOverlay;
import hook.feature.ModuleCatalog;
import hook.feature.UiAudio;
import hook.feature.UiEasing;
import hook.feature.UiIcon;
import hook.feature.UiPalette;
import hook.feature.UiRenderer;
import hook.module.Category;
import hook.module.Module;
import hook.module.NewFeature;
import hook.module.setting.BindSetting;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ButtonSetting;
import hook.module.setting.ChoiceSetting;
import hook.module.setting.ItemSetting;
import hook.module.setting.MinMaxSetting;
import hook.module.setting.ModeSetting;
import hook.module.setting.NumberSetting;
import hook.module.setting.RgbSetting;
import hook.module.setting.Setting;
import hook.module.setting.SettingVisibility;
import hook.module.setting.StorageBlocksSetting;
import hook.module.setting.StringSetting;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class HookUiController {
    private static final float WIN_W = 1000.0f;
    private static final float WIN_H = 700.0f;
    private static final float SIDE_W = 236.0f;
    private static final float GAP = 14.0f;
    private static final float SEARCH_H = 56.0f;
    private static final float PLAYER_H = 108.0f;
    private static final float HEADER_H = 78.0f;
    private static final float CARD_H = 90.0f;
    private static final float CARD_GAP = 12.0f;
    private static final float ROW_GAP = 8.0f;
    private static final float PANEL_R = 22.0f;
    private static final int TEXT = UiRenderer.argb(1.0f, 244, 245, 247);
    private static final int SUB = UiRenderer.white(0.56f);
    private static final int FAINT = UiRenderer.white(0.34f);
    private static final int INK = UiRenderer.argb(1.0f, 22, 23, 27);
    private final UiDataSource host;
    private final ConfigUiScreen configs = new ConfigUiScreen();
    private final Map<String, float[]> anims = new HashMap<String, float[]>();
    private final List<ClickRegion> hits = new ArrayList<ClickRegion>();
    private final List<UiParticleState> particles = new ArrayList<UiParticleState>();
    private final Map<Setting, float[]> tracks = new IdentityHashMap<Setting, float[]>();
    private final Random random = new Random();
    private final HalloweenGhostOverlay halloween = new HalloweenGhostOverlay();
    private float hw;
    private Category tab = Category.values()[0];
    private Module page;
    private Module pageShown;
    private float tabT = 10.0f;
    private float vis;
    private boolean closing;
    private long last;
    private float dt;
    private float time = (float)(Math.random() * 1000.0);
    private double mx;
    private double my;
    private int sw;
    private int sh;
    private float u = 1.0f;
    private float wx;
    private float wy;
    private float offX;
    private float offY;
    private boolean posLoaded;
    private boolean dragging;
    private double grabX;
    private double grabY;
    private String query = "";
    private boolean searchFocus;
    private float caretT;
    private StringSetting editing;
    private BindSetting listening;
    private Module listeningModule;
    private Setting sliding;
    private int slideKnob;
    private final float[] gridScroll = new float[2];
    private final float[] pageScroll = new float[2];
    private final float[] pickScroll = new float[2];
    private float gridMax;
    private float pageMax;
    private float pickMax;
    private final float[] gridRect = new float[4];
    private final float[] pageRect = new float[4];
    private final float[] pickRect = new float[4];
    private ItemSetting picking;
    private RgbSetting coloring;
    private int colorDrag;
    private final float[] hsv = new float[3];
    private float alphaPick = 1.0f;
    private final float[] svRect = new float[4];
    private final float[] hueRect = new float[4];
    private final float[] alphaRect = new float[4];
    private String pickQuery = "";
    private List<?> pickItems;
    private int layerNow;
    private float[] hitClip;
    private String tip;
    private float tipX;
    private float tipY;
    private static final Object THEME_GRID = new Object();
    private static final Object SKY_PREVIEWS = new Object();
    private static final Object LOBBY_GRID = new Object();

    public HookUiController(UiDataSource var1) {
        this.host = var1;
    }

    public void onOpen(boolean var1) {
        this.closing = false;
        this.last = 0L;
        if (var1) {
            this.vis = 0.0f;
            this.dragging = false;
            this.searchFocus = false;
            this.editing = null;
            this.listening = null;
            this.sliding = null;
            this.picking = null;
            this.tabT = 0.0f;
            UiAudio.play(UiAudio.Cue.GUI_OPEN);
        } else {
            this.vis = Math.max(this.vis, 0.6f);
        }
    }

    public void requestClose() {
        if (!this.closing) {
            this.closing = true;
            UiAudio.play(UiAudio.Cue.GUI_CLOSE);
            this.dragging = false;
            this.sliding = null;
            this.searchFocus = false;
            this.editing = null;
            this.listening = null;
        }
    }

    public boolean isClosing() {
        return this.closing;
    }

    public float visibility() {
        return UiEasing.out(this.vis);
    }

    public boolean isTyping() {
        return this.configs.active() || this.searchFocus || this.editing != null || this.listening != null || this.picking != null || this.coloring != null;
    }

    public void render(UiRenderer var1, int var2, int var3, double var4, double var6, float var8) {
        long var9 = System.nanoTime();
        this.dt = this.last == 0L ? 0.016f : UiEasing.clamp((float)(var9 - this.last) / 1.0E9f, 0.0f, 0.1f);
        this.last = var9;
        this.time += this.dt;
        this.tabT += this.dt;
        this.caretT += this.dt;
        this.mx = var4;
        this.my = var6;
        this.sw = var2;
        this.sh = var3;
        this.hits.clear();
        this.tip = null;
        this.loadPosition();
        float var11 = (float)var3 / 1080.0f * var8;
        this.u = Math.max(0.45f, Math.min(var11, Math.min((float)(var2 - 40) / WIN_W, (float)(var3 - 40) / WIN_H)));
        if (this.closing) {
            this.vis -= this.dt / 0.22f;
            if (this.vis <= 0.0f) {
                this.vis = 0.0f;
                this.closing = false;
                this.host.closeNow();
            }
        } else {
            this.vis = Math.min(1.0f, this.vis + this.dt / 0.34f);
        }
        float var12 = UiEasing.out(this.vis);
        if (this.dragging) {
            this.offX = (float)((this.mx - this.grabX) / (double)this.u);
            this.offY = (float)((this.my - this.grabY) / (double)this.u);
        }
        this.clampPosition();
        this.wx = Math.round(((float)var2 - WIN_W * this.u) / 2.0f + this.offX * this.u);
        this.wy = Math.round(((float)var3 - WIN_H * this.u) / 2.0f + this.offY * this.u);
        this.applySlide();
        this.applyColorDrag();
        this.hw = UiPalette.halloween();
        float[] var13 = new float[]{0.0f, 0.0f};
        float var14 = this.host.shakeStrength();
        if (this.hw > 0.003f && var14 > 0.0f) {
            var13 = this.halloween.shake(this.dt, this.host.beat() * this.hw, var14, var3, this.time);
        } else {
            this.halloween.shake(this.dt, 0.0f, 0.0f, var3, this.time);
            this.halloween.calm();
        }
        var1.pushTransform(var13[0], var13[1], 1.0f);
        this.drawBackground(var1, var12);
        this.drawParticles(var1, var12);
        if (this.hw > 0.003f) {
            var1.pushAlpha(var12);
            this.halloween.background(var1, var2, var3, this.time, this.dt, this.hw, new float[]{this.wx, this.wy, this.d(WIN_W), this.d(WIN_H)});
            var1.popAlpha();
        }
        var1.popTransform();
        float var15 = 0.955f + 0.045f * var12;
        float var16 = this.wx + WIN_W * this.u / 2.0f;
        float var17 = this.wy + WIN_H * this.u / 2.0f;
        var1.pushTransform(var16 * (1.0f - var15), var17 * (1.0f - var15) + (1.0f - var12) * 14.0f * this.u, var15);
        var1.pushAlpha(var12);
        this.layerNow = 0;
        this.drawSidebar(var1);
        this.drawPlayer(var1);
        this.drawMain(var1);
        this.drawSearchBar(var1);
        if (this.tip != null) {
            this.drawTip(var1);
        }
        this.layerNow = 1;
        this.drawPicker(var1);
        this.drawColorPicker(var1);
        this.layerNow = 2;
        this.configs.render(var1, this.mainX(), this.wy, this.mainW(), this.mainH(), this.u, this.dt, this.mx, this.my, !this.closing && this.vis >= 0.6f);
        var1.popAlpha();
        var1.popTransform();
    }

    private float d(float var1) {
        return var1 * this.u;
    }

    private float an(String var1, float var2, float var3) {
        float[] var4 = this.anims.computeIfAbsent(var1, var1x -> new float[]{var2});
        var4[0] = var4[0] + (var2 - var4[0]) * (1.0f - (float)Math.exp(-var3 * this.dt));
        if (Math.abs(var2 - var4[0]) < 5.0E-4f) {
            var4[0] = var2;
        }
        return var4[0];
    }

    private void drawBackground(UiRenderer var1, float var2) {
        var1.pushAlpha(var2);
        float var3 = (float)this.sh * 0.04f;
        var1.rect(-var3, -var3, (float)this.sw + 2.0f * var3, (float)this.sh + 2.0f * var3, UiRenderer.argb(0.38f, 5, 6, 8));
        var1.gradient(-var3, -var3, (float)this.sw + 2.0f * var3, (float)this.sh + 2.0f * var3, UiPalette.bgTop(0.2f), UiPalette.bgBottom(0.5f));
        float var4 = Math.max(this.sw, this.sh);
        float var5 = UiPalette.lightAmount();
        float var6 = this.time;
        float var7 = UiPalette.selected() == UiPalette.Hook ? 0.35f : 1.0f;
        var1.glow((float)this.sw * (0.28f + 0.16f * (float)Math.sin((double)var6 * 0.031)), (float)this.sh * (0.3f + 0.14f * (float)Math.cos((double)var6 * 0.026)), var4 * 0.62f, UiPalette.light(0.05f * var5 * var7));
        var1.glow((float)this.sw * (0.74f + 0.14f * (float)Math.cos((double)var6 * 0.022 + 1.7)), (float)this.sh * (0.66f + 0.16f * (float)Math.sin((double)var6 * 0.028 + 0.6)), var4 * 0.58f, UiEasing.mix(UiRenderer.argb(0.06f * var7, 190, 195, 205), UiPalette.light(0.06f * var5 * var7), 0.75f));
        var1.glow((float)this.sw * (0.5f + 0.3f * (float)Math.sin((double)var6 * 0.017 + 3.1)), (float)this.sh * (0.5f + 0.24f * (float)Math.sin((double)var6 * 0.021 + 2.2)), var4 * 0.4f, UiRenderer.white(0.025f * var7));
        var1.popAlpha();
    }

    private void drawParticles(UiRenderer var1, float var2) {
        if (this.particles.isEmpty()) {
            int var18 = UiPalette.selected() == UiPalette.Hook ? 26 : 48;
            for (int var3 = 0; var3 < var18; ++var3) {
                UiParticleState var4 = new UiParticleState();
                var4.x = this.random.nextFloat();
                var4.y = this.random.nextFloat();
                var4.speed = 0.004f + this.random.nextFloat() * 0.01f;
                var4.size = 0.8f + this.random.nextFloat() * 1.7f;
                var4.phase = this.random.nextFloat() * 100.0f;
                var4.fade = 0.18f + this.random.nextFloat() * 0.35f;
                var4.sway = 0.004f + this.random.nextFloat() * 0.01f;
                var4.base = 0.25f + this.random.nextFloat() * 0.55f;
                this.particles.add(var4);
            }
        }
        var1.pushAlpha(var2);
        float var17 = this.hw;
        for (UiParticleState var5 : this.particles) {
            var5.y -= var5.speed * this.dt * (1.0f + 0.5f * var17);
            var5.x += (float)Math.sin(this.time * 0.35f + var5.phase) * var5.sway * this.dt;
            if (var5.y < -0.05f) {
                var5.y = 1.05f;
                var5.x = this.random.nextFloat();
            }
            if (var5.x < -0.05f) {
                var5.x = 1.05f;
            } else if (var5.x > 1.05f) {
                var5.x = -0.05f;
            }
            float var6 = 0.5f + 0.5f * (float)Math.sin(this.time * var5.fade + var5.phase);
            float var7 = var5.base * var6 * var6;
            float var8 = var5.x * (float)this.sw;
            float var9 = var5.y * (float)this.sh;
            float var10 = var5.size * Math.max(1.0f, this.u);
            int var11 = UiEasing.mix(UiRenderer.white(0.045f * var7), UiPalette.light(0.06f * var7), 0.7f);
            int var12 = UiEasing.mix(UiRenderer.white(0.42f * var7), UiPalette.light(0.42f * var7), 0.35f);
            if (var17 > 0.003f) {
                float var13 = 0.75f + 0.25f * (float)Math.sin(this.time * 9.0f + var5.phase * 3.0f);
                boolean var14 = (int)(var5.phase * 10.0f) % 5 == 0;
                int var15 = var14 ? UiRenderer.argb(0.06f * var7, 170, 110, 255) : UiRenderer.argb(0.07f * var7 * var13, 255, 120, 40);
                int var16 = var14 ? UiRenderer.argb(0.45f * var7, 225, 200, 255) : UiRenderer.argb(0.55f * var7 * var13, 255, 196, 120);
                var11 = UiEasing.mix(var11, var15, var17);
                var12 = UiEasing.mix(var12, var16, var17);
            }
            var1.glow(var8, var9, var10 * 9.0f, var11);
            var1.glow(var8, var9, var10 * 2.4f, var12);
        }
        var1.popAlpha();
    }

    private void glass(UiRenderer var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        var1.shadow(var2, var3, var4, var5, var6, this.d(30.0f), UiRenderer.argb(0.42f, 0, 0, 0));
        var1.round(var2, var3, var4, var5, var6, UiPalette.glass(0.6f));
        var1.roundGradient(var2, var3, var4, Math.min(var5, Math.max(var5 * 0.5f, Math.min(var5, this.d(150.0f)))), var6, UiRenderer.white(0.05f), UiRenderer.white(0.0f));
        float var8 = Math.min(var4, var5) * 0.5f;
        float var9 = var2 + var8 + (var4 - 2.0f * var8) * (0.5f + 0.5f * (float)Math.sin(this.time * 0.09f + var7));
        float var10 = var3 + var8 + (var5 - 2.0f * var8) * (0.35f + 0.35f * (float)Math.cos(this.time * 0.07f + var7 * 1.3f));
        var1.glow(var9, var10, var8, UiEasing.mix(UiRenderer.white(0.04f), UiPalette.light(0.05f * UiPalette.lightAmount()), 0.7f));
        var1.outline(var2, var3, var4, var5, var6, Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.085f));
        var1.rect(var2 + var6, var3 + Math.max(1.0f, this.u), var4 - 2.0f * var6, Math.max(1.0f, this.u * 0.8f), UiRenderer.white(0.05f));
    }

    private float sideX() {
        return this.wx;
    }

    private float mainX() {
        return this.wx + this.d(250.0f);
    }

    private float mainW() {
        return this.d(750.0f);
    }

    private float mainH() {
        return this.d(630.0f);
    }

    private void drawSidebar(UiRenderer var1) {
        float var2 = this.sideX();
        float var3 = this.wy;
        float var4 = this.d(236.0f);
        float var5 = this.d(502.0f);
        this.glass(var1, var2, var3, var4, var5, this.d(22.0f), 0.4f);
        this.halloween.panelWeb(var1, var2, var3, var4, var5, this.u, this.time, this.hw, true);
        float var6 = this.d(86.0f);
        this.hit(var2, var3, var4, var6, var1x -> this.startDrag());
        float var8 = var2 + this.d(20.0f);
        float var9 = var3 + this.d(23.0f);
        var1.glow(var8 + this.d(48.0f), var9 + this.d(20.0f), this.d(54.0f), UiEasing.mix(UiRenderer.white(0.035f), UiPalette.light(0.045f), 0.6f));
        float var11 = var1.text("HookVisuals", var8, var9 + this.d(2.0f), this.d(23.0f), 1, TEXT);
        HalloweenGhostOverlay.lantern(var1, var8 + this.d(6.0f) + var11, var9 + this.d(4.0f), this.d(19.0f), this.time, this.hw);
        var1.text("Minecraft 1.21.11", var8 + this.d(1.0f), var9 + this.d(27.0f), this.d(10.5f), 0, FAINT);
        var1.rect(var2 + this.d(18.0f), var3 + var6, var4 - this.d(36.0f), Math.max(1.0f, this.u), UiRenderer.white(0.06f));
        Category[] var12 = Category.values();
        float var13 = var3 + var6 + this.d(14.0f);
        float var14 = this.d(46.0f);
        boolean var15 = !this.query.isEmpty();
        for (Category var19 : var12) {
            if (PanicButtonModule.isPanicActive() && var19 != Category.VISUALS) continue;
            if (var19 != Category.CLIENT && this.modules(var19).isEmpty()) continue; // hide emptied categories
            String var20 = "tab:" + var19.name();
            float var21 = var2 + this.d(12.0f);
            float var22 = var4 - this.d(24.0f);
            boolean var23 = var19 == this.tab && !var15;
            boolean var24 = this.hot(var21, var13, var22, var14);
            float var25 = this.an(var20 + ":h", var24 ? 1.0f : 0.0f, 14.0f);
            float var26 = this.an(var20 + ":s", var23 ? 1.0f : 0.0f, 11.0f);
            if (var26 > 0.01f) {
                var1.glow(var21 + this.d(30.0f), var13 + var14 / 2.0f, this.d(70.0f), UiEasing.mix(UiRenderer.white(0.045f * var26), UiPalette.accent(0.07f * var26), 0.6f));
            }
            var1.round(var21, var13, var22, var14, this.d(14.0f), UiEasing.mix(UiRenderer.white(0.035f * var25 + 0.085f * var26), UiPalette.accent(0.035f * var25 + 0.085f * var26), 0.35f * var26));
            if (var26 > 0.01f) {
                var1.outline(var21, var13, var22, var14, this.d(14.0f), Math.max(1.0f, this.u * 0.9f), UiEasing.mix(UiRenderer.white(0.1f * var26), UiPalette.accent(0.16f * var26), 0.6f));
                float var27 = this.d(18.0f) * var26;
                var1.round(var21 + this.d(3.0f), var13 + (var14 - var27) / 2.0f, this.d(3.0f), var27, this.d(1.5f), UiPalette.accent(0.95f * var26));
            }
            int var34 = UiEasing.mix(UiRenderer.white(0.66f), TEXT, Math.max(var25 * 0.6f, var26));
            float var28 = this.d(19.0f);
            var1.icon(HookUiController.iconFor(var19), var21 + this.d(16.0f), var13 + (var14 - var28) / 2.0f, var28, var34);
            var1.text(var19.name.toString(), var21 + this.d(46.0f), var13 + (var14 - this.d(15.0f)) / 2.0f - this.d(0.5f), this.d(14.5f), 0, var34);
            int var29 = this.modules(var19).size();
            String var30 = String.valueOf(var29);
            float var31 = Math.max(this.d(24.0f), var1.textWidth(var30, this.d(11.0f), 1) + this.d(14.0f));
            float var32 = var21 + var22 - var31 - this.d(10.0f);
            var1.round(var32, var13 + (var14 - this.d(22.0f)) / 2.0f, var31, this.d(22.0f), this.d(11.0f), UiRenderer.white(0.05f + 0.05f * var26));
            var1.textCentered(var30, var32 + var31 / 2.0f, var13 + (var14 - this.d(11.0f)) / 2.0f - this.d(0.5f), this.d(11.0f), 1, UiRenderer.white(0.45f + 0.4f * var26));
            this.hit(var21, var13, var22, var14, var2x -> this.selectTab(var19));
            var13 += var14 + this.d(4.0f);
        }
    }

    private void selectTab(Category var1) {
        boolean var2 = var1 != this.tab || !this.query.isEmpty() || this.page != null;
        this.tab = var1;
        this.query = "";
        this.searchFocus = false;
        this.page = null;
        if (var2) {
            this.tabT = 0.0f;
            this.gridScroll[0] = 0.0f;
            this.gridScroll[1] = 0.0f;
            UiAudio.play(UiAudio.Cue.TAB);
        }
    }

    private static UiIcon iconFor(Category var0) {
        return switch (var0) {
            default -> throw new MatchException(null, null);
            case Category.COMBAT -> UiIcon.SWORD;
            case Category.MISC -> UiIcon.BOX;
            case Category.MACROS -> UiIcon.BOLT;
            case Category.DONUT -> UiIcon.DONUT;
            case Category.GLAZED -> UiIcon.ACTIVITY;
            case Category.RENDER -> UiIcon.EYE;
            case Category.VISUALS -> UiIcon.LAYOUT;
            case Category.AUTOMATION -> UiIcon.ACTIVITY;
            case Category.CLIENT -> UiIcon.MONITOR;
        };
    }

    private void drawPlayer(UiRenderer var1) {
        float var2 = this.sideX();
        float var3 = this.d(108.0f);
        float var4 = this.wy + this.d(WIN_H) - var3;
        float var5 = this.d(236.0f);
        this.glass(var1, var2, var4, var5, var3, this.d(22.0f), 2.2f);
        float var6 = this.d(52.0f);
        float var7 = var2 + this.d(18.0f);
        float var8 = var4 + (var3 - var6) / 2.0f;
        var1.round(var7, var8, var6, var6, this.d(16.0f), UiRenderer.white(0.07f));
        var1.outline(var7, var8, var6, var6, this.d(16.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.1f));
        float var9 = this.d(32.0f);
        var1.head(var7 + (var6 - var9) / 2.0f, var8 + (var6 - var9) / 2.0f, var9);
        float var10 = var7 + var6 + this.d(14.0f);
        float var11 = var2 + var5 - var10 - this.d(14.0f);
        var1.text(var1.ellipsize(this.host.playerName(), var11, this.d(15.0f), 1), var10, var4 + this.d(25.0f), this.d(15.0f), 1, TEXT);
        var1.text(var1.ellipsize(this.host.serverLine(), var11, this.d(11.5f), 0), var10, var4 + this.d(47.0f), this.d(11.5f), 0, SUB);
        int var12 = 0;
        for (Module var14 : this.allModules()) {
            if (!var14.isEnabled()) continue;
            ++var12;
        }
        float var15 = 0.55f + 0.45f * (float)Math.sin(this.time * 2.2f);
        float var16 = var4 + this.d(71.0f);
        var1.glow(var10 + this.d(3.5f), var16 + this.d(3.5f), this.d(9.0f), UiPalette.accent(0.22f * var15));
        var1.circle(var10 + this.d(3.5f), var16 + this.d(3.5f), this.d(3.0f), UiPalette.accent(0.9f));
        var1.text(var12 + (var12 == 1 ? " module active" : " modules active"), var10 + this.d(13.0f), var16 - this.d(2.0f), this.d(11.0f), 0, FAINT);
    }

    private void drawMain(UiRenderer var1) {
        float var2 = this.mainX();
        float var3 = this.wy;
        float var4 = this.mainW();
        float var5 = this.mainH();
        this.glass(var1, var2, var3, var4, var5, this.d(22.0f), 4.1f);
        this.halloween.panelWeb(var1, var2, var3, var4, var5, this.u, this.time, this.hw * 0.8f, false);
        float var6 = this.configs.visibility();
        if (!(var6 >= 0.995f)) {
            var1.pushAlpha(1.0f - var6);
            float var7 = this.an("page", this.page != null ? 1.0f : 0.0f, 10.0f);
            if (this.page != null) {
                this.pageShown = this.page;
            }
            float var8 = this.d(36.0f);
            if (var7 < 0.995f) {
                var1.pushAlpha(1.0f - var7);
                var1.pushTransform(-var8 * UiEasing.inOut(var7), 0.0f, 1.0f);
                this.drawGridPage(var1, var2, var3, var4, var5, var7 < 0.5f);
                var1.popTransform();
                var1.popAlpha();
            }
            if (var7 > 0.005f && this.pageShown != null) {
                var1.pushAlpha(var7);
                var1.pushTransform(var8 * (1.0f - UiEasing.inOut(var7)), 0.0f, 1.0f);
                this.drawSettingsPage(var1, this.pageShown, var2, var3, var4, var5, var7 >= 0.5f);
                var1.popTransform();
                var1.popAlpha();
            }
            var1.popAlpha();
        }
    }

    private void header(UiRenderer var1, float var2, float var3, float var4, UiIcon var5, String var6, String var7, boolean var8) {
        if (var8) {
            this.hit(var2, var3, var4, this.d(78.0f), var1x -> this.startDrag());
        }
        float var9 = this.d(44.0f);
        float var10 = var2 + this.d(22.0f);
        float var11 = var3 + this.d(18.0f);
        var1.round(var10, var11, var9, var9, this.d(14.0f), UiRenderer.white(0.07f));
        var1.outline(var10, var11, var9, var9, this.d(14.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.09f));
        float var12 = this.d(21.0f);
        var1.icon(var5, var10 + (var9 - var12) / 2.0f, var11 + (var9 - var12) / 2.0f, var12, TEXT);
        var1.text(var6, var10 + var9 + this.d(16.0f), var11 + this.d(1.0f), this.d(21.0f), 1, TEXT);
        var1.text(var1.ellipsize(var7, var4 - this.d(260.0f), this.d(12.0f), 0), var10 + var9 + this.d(17.0f), var11 + this.d(27.0f), this.d(12.0f), 0, SUB);
        float var13 = this.d(18.0f);
        var1.icon(UiIcon.GRIP, var2 + var4 - this.d(22.0f) - var13, var3 + this.d(30.0f), var13, UiRenderer.white(0.22f));
        var1.rect(var2 + this.d(20.0f), var3 + this.d(78.0f), var4 - this.d(40.0f), Math.max(1.0f, this.u), UiRenderer.white(0.055f));
    }

    private void drawGridPage(UiRenderer var1, float var2, float var3, float var4, float var5, boolean var6) {
        String var29;
        boolean var7 = !this.query.isEmpty();
        List<Module> var8 = var7 ? this.searchResults() : this.modules(this.tab);
        int var9 = 0;
        for (Module var11Raw : var8) {
            Module var11 = var11Raw;
            if (!var11.isEnabled()) continue;
            ++var9;
        }
        String string = var29 = var7 ? "Search" : this.tab.name.toString();
        String var30 = var7 ? var8.size() + (var8.size() == 1 ? " result for \"" : " results for \"") + this.query + "\"" : var8.size() + (var8.size() == 1 ? " module" : " modules") + "  \u00b7  " + var9 + " enabled";
        this.header(var1, var2, var3, var4, var7 ? UiIcon.SEARCH : HookUiController.iconFor(this.tab), var29, var30, var6);
        float var12 = var2 + this.d(18.0f);
        float var13 = var3 + this.d(92.0f);
        float var14 = var4 - this.d(36.0f);
        float var15 = var5 - this.d(92.0f) - this.d(14.0f);
        this.gridRect[0] = var12;
        this.gridRect[1] = var13;
        this.gridRect[2] = var14;
        this.gridRect[3] = var15;
        int var16 = var14 >= this.d(600.0f) ? 2 : 1;
        float var17 = (var14 - this.d(12.0f) * (float)(var16 - 1)) / (float)var16;
        float var18 = this.d(90.0f);
        int var19 = (var8.size() + var16 - 1) / var16;
        float var20 = (float)var19 * var18 + (float)Math.max(0, var19 - 1) * this.d(12.0f);
        this.gridMax = Math.max(0.0f, var20 - var15);
        float var21 = this.scroll(this.gridScroll, this.gridMax);
        if (var8.isEmpty()) {
            var1.icon(var7 ? UiIcon.SEARCH : HookUiController.iconFor(this.tab), var12 + var14 / 2.0f - this.d(16.0f), var13 + var15 / 2.0f - this.d(46.0f), this.d(32.0f), UiRenderer.white(0.22f));
            var1.textCentered(var7 ? "No modules match" : "Nothing here yet", var12 + var14 / 2.0f, var13 + var15 / 2.0f, this.d(15.0f), 1, SUB);
        } else {
            // Keep cards, shadows and hover hit areas inside the module viewport.
            var1.pushClip(var12, var13, var14, var15);
            this.hitClip = new float[]{var12, var13, var14, var15};
            for (int var22 = 0; var22 < var8.size(); ++var22) {
                Module var23 = var8.get(var22);
                int var24 = var22 % var16;
                int var25 = var22 / var16;
                float var26 = var12 + (float)var24 * (var17 + this.d(12.0f));
                float var27 = var13 + (float)var25 * (var18 + this.d(12.0f)) - var21;
                if (var27 + var18 < var13 - this.d(20.0f) || var27 > var13 + var15 + this.d(20.0f)) continue;
                float var28 = UiEasing.out((this.tabT - (float)var22 * 0.028f) / 0.34f);
                var1.pushAlpha(var28);
                var1.pushTransform(0.0f, (1.0f - var28) * this.d(12.0f), 1.0f);
                this.drawCard(var1, var23, var26, var27, var17, var18, var6 && var28 > 0.9f);
                var1.popTransform();
                var1.popAlpha();
            }
            this.hitClip = null;
            var1.popClip();
            this.scrollbar(var1, var12 + var14 + this.d(4.0f), var13, var15, var21, this.gridMax, var20);
        }
    }

    private void drawCard(UiRenderer var1, Module var2, float var3, float var4, float var5, float var6, boolean var7) {
        String var8 = var2.getName().toString();
        String var9 = "card:" + var8;
        boolean var10 = var7 && this.hot(var3, var4, var5, var6);
        float var11 = this.an(var9 + ":h", var10 ? 1.0f : 0.0f, 13.0f);
        float var12 = this.an(var9 + ":on", var2.isEnabled() ? 1.0f : 0.0f, 10.0f);
        if (var11 > 0.01f) {
            var1.shadow(var3, var4, var5, var6, this.d(18.0f), this.d(16.0f), UiRenderer.white(0.035f * var11));
        }
        var1.round(var3, var4, var5, var6, this.d(18.0f), UiRenderer.white(0.035f + 0.03f * var11));
        if (var12 > 0.01f) {
            var1.round(var3, var4, var5, var6, this.d(18.0f), UiEasing.mix(UiRenderer.white(0.035f * var12), UiPalette.accent(0.05f * var12), 0.6f));
        }
        if (var11 > 0.01f) {
            var1.pushClip(var3, var4, var5, var6);
            var1.glow(UiEasing.clamp((float)this.mx, var3, var3 + var5), var4 + var6 * 0.15f, var5 * 0.45f, UiRenderer.white(0.045f * var11));
            var1.popClip();
        }
        var1.outline(var3, var4, var5, var6, this.d(18.0f), Math.max(1.0f, this.u * 0.9f), UiEasing.mix(UiRenderer.white(0.06f + 0.08f * var11 + 0.06f * var12), UiPalette.accent(0.06f + 0.08f * var11 + 0.1f * var12), 0.5f * var12));
        float var13 = this.d(20.0f);
        float var14 = this.d(44.0f);
        float var15 = this.d(24.0f);
        float var16 = var3 + var5 - var13 - var14;
        float var17 = var4 + (var6 - var15) / 2.0f;
        float var18 = this.d(32.0f);
        float var19 = var16 - this.d(12.0f) - var18;
        float var20 = var4 + (var6 - var18) / 2.0f;
        float var21 = var19 - this.d(10.0f) - (var3 + var13);
        int var22 = UiEasing.mix(UiRenderer.white(0.86f), TEXT, var12);
        if (var12 > 0.01f) {
            var1.glow(var3 + var13 + this.d(4.0f), var4 + this.d(29.0f), this.d(16.0f), UiPalette.accent(0.26f * var12));
            var1.circle(var3 + var13 + this.d(3.0f), var4 + this.d(29.0f), this.d(3.0f) * (0.6f + 0.4f * var12), UiPalette.accent(0.97f * var12));
        }
        float var23 = var3 + var13 + this.d(13.0f) * var12;
        boolean var24 = var2 instanceof NewFeature;
        float var25 = var24 ? this.d(52.0f) : 0.0f;
        String var26 = var1.ellipsize(var8, var21 - this.d(13.0f) * var12 - var25, this.d(15.0f), 1);
        float var27 = var1.text(var26, var23, var4 + this.d(20.0f), this.d(15.0f), 1, var22);
        if (var24) {
            float var28 = 0.5f + 0.5f * (float)Math.sin(this.time * 1.8f);
            var1.outline(var3, var4, var5, var6, this.d(18.0f), Math.max(1.0f, this.u * 1.1f), UiEasing.mix(UiRenderer.argb(0.2f + 0.12f * var28, 255, 150, 70), UiRenderer.argb(0.2f + 0.12f * var28, 170, 110, 255), var28));
            HalloweenGhostOverlay.newBadge(var1, var23 + var27 + this.d(8.0f), var4 + this.d(19.0f), this.d(17.0f), this.time, 1.0f);
        }
        List<String> var32 = this.wrap(var1, var2.getDescription() == null ? "" : var2.getDescription().toString(), var21, this.d(11.8f), 2);
        for (int var29 = 0; var29 < var32.size(); ++var29) {
            var1.text(var32.get(var29), var3 + var13, var4 + this.d(45.0f) + (float)var29 * this.d(16.0f), this.d(11.8f), 0, SUB);
        }
        boolean var33 = var7 && this.hot(var19, var20, var18, var18);
        float var30 = this.an(var9 + ":g", var33 ? 1.0f : 0.0f, 16.0f);
        var1.round(var19, var20, var18, var18, this.d(11.0f), UiRenderer.white(0.035f + 0.03f * var11 + 0.07f * var30));
        float var31 = this.d(17.0f);
        var1.icon(UiIcon.GEAR, var19 + (var18 - var31) / 2.0f, var20 + (var18 - var31) / 2.0f, var31, UiRenderer.white(0.42f + 0.25f * var11 + 0.33f * var30));
        this.toggle(var1, var16, var17, var14, var15, var12, var11);
        if (var7) {
            this.hit(var3, var4, var5, var6, var2x -> {
                if (var2x == 1) {
                    this.openPage(var2);
                } else if (var2x == 0) {
                    this.toggleModule(var2);
                }
            });
            this.hit(var19, var20, var18, var18, var2x -> this.openPage(var2));
        }
    }

    private void toggle(UiRenderer var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        float var8 = UiEasing.inOut(var6);
        int var9 = UiEasing.mix(UiRenderer.white(0.1f + 0.05f * var7), UiPalette.accent(0.93f), var8);
        var1.round(var2, var3, var4, var5, var5 / 2.0f, var9);
        if (var8 < 0.98f) {
            var1.outline(var2, var3, var4, var5, var5 / 2.0f, Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.1f * (1.0f - var8)));
        }
        float var10 = var5 - this.d(6.0f);
        float var11 = UiEasing.lerp(var2 + this.d(3.0f), var2 + var4 - this.d(3.0f) - var10, var8);
        float var12 = var3 + this.d(3.0f);
        if (var8 > 0.02f) {
            var1.glow(var2 + var4 / 2.0f, var3 + var5 / 2.0f, var4 * 0.9f, UiPalette.accent(0.08f * var8));
        }
        var1.shadow(var11, var12, var10, var10, var10 / 2.0f, this.d(4.0f), UiRenderer.argb(0.35f, 0, 0, 0));
        var1.round(var11, var12, var10, var10, var10 / 2.0f, UiEasing.mix(UiRenderer.white(0.92f), INK, var8));
    }

    private void toggleModule(Module var1) {
        var1.toggle();
        UiAudio.play(var1.isEnabled() ? UiAudio.Cue.ON : UiAudio.Cue.OFF);
    }

    private void openPage(Module var1) {
        this.page = var1;
        this.pageScroll[0] = 0.0f;
        this.pageScroll[1] = 0.0f;
        this.editing = null;
        this.listening = null;
        UiAudio.play(UiAudio.Cue.OPEN);
    }

    private void closePage() {
        this.page = null;
        this.editing = null;
        this.listening = null;
        UiAudio.play(UiAudio.Cue.BACK);
    }

    private void drawSettingsPage(UiRenderer var1, Module var2, float var3, float var4, float var5, float var6, boolean var7) {
        String var8;
        String string = var8 = var2.getDescription() == null ? "" : var2.getDescription().toString();
        if (var7) {
            this.hit(var3, var4, var5, this.d(78.0f), var1x -> this.startDrag());
        }
        float var9 = this.d(44.0f);
        float var10 = var3 + this.d(22.0f);
        float var11 = var4 + this.d(18.0f);
        boolean var12 = var7 && this.hot(var10, var11, var9, var9);
        float var13 = this.an("back", var12 ? 1.0f : 0.0f, 15.0f);
        var1.round(var10, var11, var9, var9, this.d(14.0f), UiRenderer.white(0.07f + 0.07f * var13));
        var1.outline(var10, var11, var9, var9, this.d(14.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.09f + 0.08f * var13));
        float var14 = this.d(22.0f);
        var1.icon(UiIcon.CHEVRON_LEFT, var10 + (var9 - var14) / 2.0f - this.d(2.0f) * var13, var11 + (var9 - var14) / 2.0f, var14, TEXT);
        if (var7) {
            this.hit(var10, var11, var9, var9, var1x -> this.closePage());
        }
        String var15 = var2.getName().toString();
        boolean var16 = this.isClient(var2);
        String var17 = var16 ? "Client settings" : var15;
        String var18 = var16 ? "Themes, sound and interface" : var8;
        float var19 = var1.text(var17, var10 + var9 + this.d(16.0f), var11 + this.d(1.0f), this.d(21.0f), 1, TEXT);
        if (var2 instanceof NewFeature) {
            HalloweenGhostOverlay.newBadge(var1, var10 + var9 + this.d(26.0f) + var19, var11 + this.d(3.0f), this.d(19.0f), this.time, 1.0f);
        }
        var1.text(var1.ellipsize(var18, var5 - this.d(300.0f), this.d(12.0f), 0), var10 + var9 + this.d(17.0f), var11 + this.d(27.0f), this.d(12.0f), 0, SUB);
        float var20 = this.d(52.0f);
        float var21 = this.d(28.0f);
        float var22 = var3 + var5 - this.d(24.0f) - var20;
        float var23 = var11 + (var9 - var21) / 2.0f;
        if (var16) {
            float var24 = this.d(18.0f);
            var1.icon(UiIcon.GRIP, var3 + var5 - this.d(22.0f) - var24, var4 + this.d(30.0f), var24, UiRenderer.white(0.22f));
        } else {
            boolean var36 = var7 && this.hot(var22, var23, var20, var21);
            float var25 = this.an("page:on:" + var15, var2.isEnabled() ? 1.0f : 0.0f, 10.0f);
            this.toggle(var1, var22, var23, var20, var21, var25, this.an("page:toh", var36 ? 1.0f : 0.0f, 15.0f));
            var1.textRight(var2.isEnabled() ? "Enabled" : "Disabled", var22 - this.d(12.0f), var23 + (var21 - this.d(12.0f)) / 2.0f - this.d(1.0f), this.d(12.0f), 0, SUB);
            if (var7) {
                this.hit(var22, var23, var20, var21, var2x -> this.toggleModule(var2));
            }
        }
        var1.rect(var3 + this.d(20.0f), var4 + this.d(78.0f), var5 - this.d(40.0f), Math.max(1.0f, this.u), UiRenderer.white(0.055f));
        float var37 = var3 + this.d(18.0f);
        float var38 = var4 + this.d(92.0f);
        float var26 = var5 - this.d(36.0f);
        float var27 = var6 - this.d(92.0f) - this.d(14.0f);
        this.pageRect[0] = var37;
        this.pageRect[1] = var38;
        this.pageRect[2] = var26;
        this.pageRect[3] = var27;
        List<Object> var28 = this.pageItems(var2);
        float var29 = 0.0f;
        for (Object var31 : var28) {
            var29 += this.itemHeight(var31, var26) + this.d(8.0f);
        }
        var29 = Math.max(0.0f, var29 - this.d(8.0f));
        this.pageMax = Math.max(0.0f, var29 - var27);
        float var40 = this.scroll(this.pageScroll, this.pageMax);
        var1.pushClip(var37 - this.d(10.0f), var38, var26 + this.d(20.0f), var27);
        this.hitClip = new float[]{var37, var38, var26, var27};
        float var41 = var38 - var40;
        for (Object var33 : var28) {
            float var34 = this.itemHeight(var33, var26);
            if (!(var41 + var34 < var38 - this.d(20.0f)) && !(var41 > var38 + var27 + this.d(20.0f))) {
                if (var33 instanceof Setting) {
                    Setting var35 = (Setting)var33;
                    this.drawRow(var1, var2, var35, var37, var41, var26, var34, var7);
                } else if (var33 == THEME_GRID) {
                    this.drawThemes(var1, var37, var41, var26, var7);
                } else if (var33 == LOBBY_GRID) {
                    this.drawLobbyThemes(var1, var37, var41, var26, var7);
                } else if (var33 == SKY_PREVIEWS && var2 instanceof CustomSkysModule) {
                    this.drawSkyPreviews(var1, (CustomSkysModule)var2, var37, var41, var26);
                } else {
                    this.section(var1, (String)var33, var37, var41, var26);
                }
            }
            var41 += var34 + this.d(8.0f);
        }
        this.hitClip = null;
        var1.popClip();
        this.scrollbar(var1, var37 + var26 + this.d(4.0f), var38, var27, var40, this.pageMax, var29);
    }

    private boolean isClient(Module var1) {
        return var1 instanceof hook.module.modules.client.Hook;
    }

    private List<Object> pageItems(Module var1) {
        ArrayList<Object> var2 = new ArrayList<Object>();
        if (!this.isClient(var1)) {
            for (Setting var8 : var1.getSettings()) {
                if (!SettingVisibility.visible(var8)) continue;
                var2.add(var8);
            }
            if (var1 instanceof CustomSkysModule) var2.add(SKY_PREVIEWS);
            return var2;
        }
        Setting[] var3 = new Setting[]{hook.module.modules.client.Hook.uiSounds, hook.module.modules.client.Hook.uiVolume};
        Setting[] var4 = new Setting[]{hook.module.modules.client.Hook.halloweenMusic, hook.module.modules.client.Hook.halloweenVolume, hook.module.modules.client.Hook.beatShake, hook.module.modules.client.Hook.shakeStrength};
        var2.add("Theme");
        var2.add(THEME_GRID);
        var2.add("Lobby Themes");
        var2.add(LOBBY_GRID);
        var2.add("Sound");
        var2.addAll(List.of(var3));
        var2.add("Halloween");
        var2.addAll(List.of(var4));
        var2.add("Interface");
        for (Setting var6 : var1.getSettings()) {
            if (var6 == hook.module.modules.client.Hook.theme || var6 == hook.module.modules.client.Hook.lobbyTheme || List.of(var3).contains(var6) || List.of(var4).contains(var6)) continue;
            var2.add(var6);
        }
        return var2;
    }

    private float itemHeight(Object var1, float var2) {
        if (var1 instanceof Setting) {
            return this.d(62.0f);
        }
        if (var1 == THEME_GRID) {
            int var3 = (UiPalette.values().length + 3) / 4;
            return (float)var3 * this.d(108.0f) + (float)(var3 - 1) * this.d(12.0f);
        }
        if (var1 == SKY_PREVIEWS) return this.d(190.0f);
        if (var1 == LOBBY_GRID) {
            int rows = (LobbyTheme.values().length + 2) / 3;
            return rows * this.lobbyTileHeight(var2) + (rows - 1) * this.d(12.0f);
        }
        return this.d(26.0f);
    }

    private void drawSkyPreviews(UiRenderer renderer, CustomSkysModule module, float x, float y, float width) {
        String[] names = {"Borealis", "Nebula", "HookVisuals", "Frost", "Crimson"};
        String[] images = {"sky_borealis", "sky_nebula", "sky_hook", "sky_frost", "sky_crimson"};
        float gap = this.d(10.0f);
        float tile = (width - gap * 2.0f) / 3.0f;
        float imageSize = this.d(48.0f);
        renderer.text("SKY PREVIEWS", x + this.d(2.0f), y + this.d(6.0f), this.d(10.5f), 1, UiRenderer.white(0.42f));
        for (int i = 0; i < names.length; i++) {
            int col = i % 3;
            int row = i / 3;
            float tx = x + col * (tile + gap);
            float ty = y + this.d(24.0f) + row * this.d(80.0f);
            boolean selected = names[i].equals(module.selectedThemeName());
            renderer.round(tx, ty, tile, this.d(74.0f), this.d(8.0f), UiRenderer.white(selected ? 0.12f : 0.045f));
            renderer.image(images[i], tx + (tile - imageSize) / 2.0f, ty + this.d(4.0f), imageSize, UiRenderer.white(1.0f));
            renderer.textCentered(names[i], tx + tile / 2.0f, ty + this.d(66.0f), this.d(9.0f), 1, UiRenderer.white(selected ? 0.95f : 0.6f));
        }
    }

    private float lobbyTileHeight(float width) {
        float tile = (width - this.d(12.0f) * 2.0f) / 3.0f;
        return (tile - this.d(16.0f)) * 9.0f / 16.0f + this.d(52.0f);
    }

    /** Lobby theme picker: preview tiles like the colour themes; Standard keeps vanilla menus. */
    private void drawLobbyThemes(UiRenderer ui, float x, float y, float width, boolean interactive) {
        LobbyTheme[] themes = LobbyTheme.values();
        LobbyTheme selected = LobbyTheme.selected();
        float gap = this.d(12.0f);
        float tileW = (width - gap * 2.0f) / 3.0f;
        float tileH = this.lobbyTileHeight(width);
        for (int i = 0; i < themes.length; i++) {
            LobbyTheme theme = themes[i];
            float tx = x + (i % 3) * (tileW + gap);
            float ty = y + (i / 3) * (tileH + gap);
            String key = "lobby:" + theme.name();
            boolean hot = interactive && this.hot(tx, ty, tileW, tileH);
            float hover = this.an(key + ":h", hot ? 1.0f : 0.0f, 13.0f);
            float sel = this.an(key + ":s", theme == selected ? 1.0f : 0.0f, 9.0f);
            ty -= this.d(2.0f) * hover;
            if (sel > 0.01f) ui.shadow(tx, ty, tileW, tileH, this.d(16.0f), this.d(16.0f), UiPalette.accent(0.12f * sel));
            ui.round(tx, ty, tileW, tileH, this.d(16.0f), UiRenderer.white(0.035f + 0.03f * hover + 0.03f * sel));
            float px = tx + this.d(8.0f), py = ty + this.d(8.0f), pw = tileW - this.d(16.0f), ph = pw * 9.0f / 16.0f;
            UiTextureData preview = theme.folder == null ? null : UiImageCache.lobbyPreview(theme.folder);
            ui.pushClip(px, py, pw, ph);
            if (preview != null) {
                ui.texture(preview, px, py, pw, ph, UiRenderer.white(1.0f));
            } else {
                ui.gradient(px, py, pw, ph, UiRenderer.argb(1.0f, 92, 124, 182), UiRenderer.argb(1.0f, 50, 72, 40));
                ui.rect(px, py + ph * 0.72f, pw, ph * 0.28f, UiRenderer.argb(1.0f, 92, 64, 40));
                ui.rect(px, py + ph * 0.72f, pw, ph * 0.06f, UiRenderer.argb(1.0f, 96, 150, 64));
                ui.textCentered("VANILLA", px + pw / 2.0f, py + ph * 0.32f, this.d(11.0f), 1, UiRenderer.white(0.85f));
            }
            ui.popClip();
            ui.outline(px, py, pw, ph, this.d(4.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.1f));
            if (theme.limited) {
                float bw = ui.textWidth("LIMITED", this.d(8.0f), 1) + this.d(10.0f);
                ui.round(px + pw - bw - this.d(4.0f), py + this.d(4.0f), bw, this.d(14.0f), this.d(7.0f), UiRenderer.argb(0.85f, 230, 120, 40));
                ui.textCentered("LIMITED", px + pw - bw / 2.0f - this.d(4.0f), py + this.d(6.5f), this.d(8.0f), 1, UiRenderer.white(0.95f));
            }
            ui.text(theme.title, tx + this.d(12.0f), py + ph + this.d(9.0f), this.d(11.5f), 1, UiEasing.mix(UiRenderer.white(0.75f), TEXT, Math.max(sel, hover)));
            ui.text(ui.ellipsize(theme.description, tileW - this.d(24.0f), this.d(8.5f), 0), tx + this.d(12.0f), py + ph + this.d(25.0f), this.d(8.5f), 0, UiRenderer.white(0.42f));
            if (sel > 0.01f) {
                float r = this.d(8.0f) * (0.7f + 0.3f * sel);
                float cx = px + this.d(14.0f), cy = py + this.d(14.0f);
                ui.circle(cx, cy, r, UiPalette.accent(sel));
                ui.icon(UiIcon.CHECK, cx - r * 0.65f, cy - r * 0.65f, r * 1.3f, UiRenderer.argb(sel, 22, 23, 27));
            }
            ui.outline(tx, ty, tileW, tileH, this.d(16.0f), Math.max(1.0f, this.u * 0.9f), UiEasing.mix(UiRenderer.white(0.06f + 0.08f * hover), UiPalette.accent(0.6f), sel));
            if (!interactive) continue;
            int index = i;
            this.hit(tx, ty, tileW, tileH, click -> {
                if (hook.module.modules.client.Hook.lobbyTheme.getModeIndex() != index) {
                    hook.module.modules.client.Hook.lobbyTheme.setModeIndex(index);
                    UiAudio.play(UiAudio.Cue.THEME);
                }
            });
        }
    }

    private void section(UiRenderer var1, String var2, float var3, float var4, float var5) {
        float var6 = var4 + this.d(8.0f);
        var1.text(var2.toUpperCase(Locale.ROOT), var3 + this.d(6.0f), var6, this.d(10.5f), 1, UiRenderer.white(0.42f));
        float var7 = var1.textWidth(var2.toUpperCase(Locale.ROOT), this.d(10.5f), 1);
        var1.rect(var3 + this.d(16.0f) + var7, var6 + this.d(7.0f), var5 - var7 - this.d(22.0f), Math.max(1.0f, this.u), UiRenderer.white(0.06f));
    }

    private void drawThemes(UiRenderer var1, float var2, float var3, float var4, boolean var5) {
        UiPalette[] var6 = UiPalette.values();
        UiPalette var7 = UiPalette.selected();
        int var8 = 4;
        float var9 = this.d(12.0f);
        float var10 = (var4 - var9 * (float)(var8 - 1)) / (float)var8;
        float var11 = this.d(108.0f);
        for (int var12 = 0; var12 < var6.length; ++var12) {
            UiPalette var13 = var6[var12];
            float var14 = var2 + (float)(var12 % var8) * (var10 + var9);
            float var15 = var3 + (float)(var12 / var8) * (var11 + var9);
            String var16 = "theme:" + var13.name();
            boolean var17 = var5 && this.hot(var14, var15, var10, var11);
            float var18 = this.an(var16 + ":h", var17 ? 1.0f : 0.0f, 13.0f);
            float var19 = this.an(var16 + ":s", var13 == var7 ? 1.0f : 0.0f, 9.0f);
            float var20 = this.d(2.0f) * var18;
            var15 -= var20;
            if (var19 > 0.01f) {
                var1.shadow(var14, var15, var10, var11, this.d(18.0f), this.d(18.0f), var13.previewAccent() & 0xFFFFFF | Math.round(0.1f * var19 * 255.0f) << 24);
            }
            var1.round(var14, var15, var10, var11, this.d(18.0f), UiRenderer.white(0.035f + 0.03f * var18 + 0.03f * var19));
            float var21 = var14 + this.d(10.0f);
            float var22 = var15 + this.d(10.0f);
            float var23 = var10 - this.d(20.0f);
            float var24 = this.d(58.0f);
            int var25 = var13.previewTop();
            var1.roundGradient(var21, var22, var23, var24, this.d(12.0f), UiEasing.mix(var25, -14013648, 0.25f), UiEasing.mix(var25, -16448250, 0.55f));
            var1.pushClip(var21, var22, var23, var24);
            var1.glow(var21 + var23 * 0.72f, var22 + var24 * 0.35f, var23 * 0.55f, var13.previewLight() & 0xFFFFFF | Math.round(Math.min(1.0f, (0.12f + 0.06f * var18) * var13.lightAmount) * 255.0f) << 24);
            var1.popClip();
            var1.outline(var21, var22, var23, var24, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.08f));
            float var26 = this.d(26.0f);
            var1.imageGradient("logo", var21 + this.d(12.0f), var22 + (var24 - var26) / 2.0f, var26, -1, var13.previewLogo());
            float var27 = this.d(30.0f);
            float var28 = this.d(16.0f);
            float var29 = var21 + var23 - this.d(12.0f) - var27;
            float var30 = var22 + (var24 - var28) / 2.0f;
            var1.round(var29, var30, var27, var28, var28 / 2.0f, var13.previewAccent() & 0xFFFFFF | 0xED000000);
            float var31 = var28 - this.d(4.0f);
            var1.round(var29 + var27 - this.d(2.0f) - var31, var30 + this.d(2.0f), var31, var31, var31 / 2.0f, INK);
            var1.text(var13.name(), var14 + this.d(14.0f), var15 + var11 - this.d(31.0f), this.d(13.0f), 1, UiEasing.mix(UiRenderer.white(0.78f), TEXT, Math.max(var19, var18)));
            if (var19 > 0.01f) {
                float var32 = this.d(9.0f) * (0.7f + 0.3f * var19);
                float var33 = var14 + var10 - this.d(22.0f);
                float var34 = var15 + var11 - this.d(23.0f);
                var1.glow(var33, var34, var32 * 2.4f, var13.previewAccent() & 0xFFFFFF | Math.round(0.25f * var19 * 255.0f) << 24);
                var1.circle(var33, var34, var32, var13.previewAccent() & 0xFFFFFF | Math.round(var19 * 255.0f) << 24);
                float var35 = var32 * 1.3f;
                var1.icon(UiIcon.CHECK, var33 - var35 / 2.0f, var34 - var35 / 2.0f, var35, UiRenderer.argb(var19, 22, 23, 27));
            }
            var1.outline(var14, var15, var10, var11, this.d(18.0f), Math.max(1.0f, this.u * 0.9f), UiEasing.mix(UiRenderer.white(0.06f + 0.08f * var18), var13.previewAccent() & 0xFFFFFF | Math.round(153.0f) << 24, var19));
            if (!var5) continue;
            int var37 = var12;
            this.hit(var14, var15, var10, var11, var1x -> {
                if (hook.module.modules.client.Hook.theme.getModeIndex() != var37) {
                    hook.module.modules.client.Hook.theme.setModeIndex(var37);
                    UiAudio.play(UiAudio.Cue.THEME);
                }
            });
        }
    }

    private void drawRow(UiRenderer var1, Module var2, Setting var3, float var4, float var5, float var6, float var7, boolean var8) {
        String var9 = "row:" + System.identityHashCode(var3);
        boolean var10 = var8 && this.hot(var4, var5, var6, var7);
        float var11 = this.an(var9 + ":h", var10 ? 1.0f : 0.0f, 13.0f);
        var1.round(var4, var5, var6, var7, this.d(16.0f), UiRenderer.white(0.03f + 0.025f * var11));
        var1.outline(var4, var5, var6, var7, this.d(16.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.05f + 0.05f * var11));
        String var12 = var3.getName() == null ? "" : var3.getName().toString();
        String var13 = var3.getDescription() == null ? "" : var3.getDescription().toString();
        float var14 = this.d(20.0f);
        float var15 = this.d(300.0f);
        float var16 = var6 - var14 * 2.0f - var15 - this.d(12.0f);
        if (var13.isEmpty()) {
            var1.text(var1.ellipsize(var12, var16, this.d(14.0f), 1), var4 + var14, var5 + (var7 - this.d(14.0f)) / 2.0f - this.d(1.0f), this.d(14.0f), 1, TEXT);
        } else {
            var1.text(var1.ellipsize(var12, var16, this.d(14.0f), 1), var4 + var14, var5 + this.d(14.0f), this.d(14.0f), 1, TEXT);
            var1.text(var1.ellipsize(var13, var16, this.d(11.5f), 0), var4 + var14, var5 + this.d(35.0f), this.d(11.5f), 0, SUB);
        }
        float var17 = var4 + var6 - var14;
        float var18 = var5 + var7 / 2.0f;
        if (var3 instanceof BooleanSetting) {
            BooleanSetting var19 = (BooleanSetting)var3;
            float var30 = this.d(44.0f);
            float var31 = this.d(24.0f);
            float var32 = this.an(var9 + ":on", var19.getValue() ? 1.0f : 0.0f, 10.0f);
            this.toggle(var1, var17 - var30, var18 - var31 / 2.0f, var30, var31, var32, var11);
            if (var8) {
                this.hit(var4, var5, var6, var7, var1x -> {
                    var19.toggle();
                    UiAudio.play(var19.getValue() ? UiAudio.Cue.ON : UiAudio.Cue.OFF);
                });
            }
        } else if (var3 instanceof NumberSetting) {
            NumberSetting var20 = (NumberSetting)var3;
            float var40 = this.d(66.0f);
            float var50 = this.d(200.0f);
            float var60 = var17 - var40 - this.d(14.0f) - var50;
            double var33 = var20.getMax() - var20.getMin();
            float var35 = var33 <= 0.0 ? 0.0f : (float)((var20.getValue() - var20.getMin()) / var33);
            float var36 = this.an(var9 + ":v", var35, 18.0f);
            boolean var37 = this.sliding == var20;
            float var38 = this.an(var9 + ":a", !var37 && !var10 ? 0.0f : 1.0f, 14.0f);
            this.slider(var1, var20, var60, var18, var50, var36, -1.0f, var38);
            this.valuePill(var1, HookUiController.format(var20.getValue(), var20.getFormat()), var17 - var40, var18, var40);
            if (var8) {
                this.hit(var60 - this.d(8.0f), var18 - this.d(14.0f), var50 + this.d(16.0f), this.d(28.0f), var2x -> this.beginSlide(var20, 0));
            }
        } else if (var3 instanceof MinMaxSetting) {
            MinMaxSetting var21 = (MinMaxSetting)var3;
            float var41 = this.d(96.0f);
            float var51 = this.d(170.0f);
            float var61 = var17 - var41 - this.d(14.0f) - var51;
            double var70 = var21.getMaxValue() - var21.getMinValue();
            float var86 = var70 <= 0.0 ? 0.0f : (float)((var21.getCurrentMin() - var21.getMinValue()) / var70);
            float var95 = var70 <= 0.0 ? 1.0f : (float)((var21.getCurrentMax() - var21.getMinValue()) / var70);
            float var104 = this.an(var9 + ":v1", var86, 18.0f);
            float var112 = this.an(var9 + ":v2", var95, 18.0f);
            float var39 = this.an(var9 + ":a", this.sliding != var21 && !var10 ? 0.0f : 1.0f, 14.0f);
            this.slider(var1, var21, var61, var18, var51, var104, var112, var39);
            this.valuePill(var1, HookUiController.format(var21.getCurrentMin(), var21.getStep()) + " \u2013 " + HookUiController.format(var21.getCurrentMax(), var21.getStep()), var17 - var41, var18, var41);
            if (var8) {
                this.hit(var61 - this.d(8.0f), var18 - this.d(14.0f), var51 + this.d(16.0f), this.d(28.0f), var4x -> {
                    float[] var5x = this.tracks.get(var21);
                    float var6x = var5x == null ? 0.0f : (float)((this.mx - (double)var5x[0]) / (double)var5x[1]);
                    this.beginSlide(var21, Math.abs(var6x - var86) <= Math.abs(var6x - var95) && !(var6x > var95) ? 0 : 1);
                });
            }
        } else if (var3 instanceof ModeSetting) {
            ModeSetting var22 = (ModeSetting)var3;
            float var42 = this.d(200.0f);
            float var52 = this.d(34.0f);
            float var62 = var17 - var42;
            float var71 = var18 - var52 / 2.0f;
            boolean var34 = var8 && this.hot(var62, var71, var42, var52);
            float var87 = this.an(var9 + ":m", var34 ? 1.0f : 0.0f, 15.0f);
            var1.round(var62, var71, var42, var52, this.d(12.0f), UiRenderer.white(0.05f + 0.04f * var87));
            var1.outline(var62, var71, var42, var52, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.08f + 0.07f * var87));
            float var96 = this.d(16.0f);
            var1.icon(UiIcon.CHEVRON_LEFT, var62 + this.d(8.0f), var18 - var96 / 2.0f, var96, UiRenderer.white(0.45f + 0.4f * var87));
            var1.icon(UiIcon.CHEVRON_RIGHT, var62 + var42 - this.d(8.0f) - var96, var18 - var96 / 2.0f, var96, UiRenderer.white(0.45f + 0.4f * var87));
            String var105 = HookUiController.prettyEnum(String.valueOf(var22.getValue()));
            float var113 = this.an(var9 + ":idx", var22.getModeIndex(), 16.0f);
            float var117 = (var113 - (float)var22.getModeIndex()) * this.d(10.0f);
            var1.pushClip(var62 + this.d(28.0f), var71, var42 - this.d(56.0f), var52);
            var1.textCentered(var1.ellipsize(var105, var42 - this.d(64.0f), this.d(12.5f), 1), var62 + var42 / 2.0f + var117, var18 - this.d(7.5f), this.d(12.5f), 1, TEXT);
            var1.popClip();
            if (var8) {
                this.hit(var62, var71, var42 / 2.0f, var52, var1x -> {
                    if (var1x == 1) {
                        var22.cycleUp();
                    } else {
                        var22.cycleDown();
                    }
                    UiAudio.play(UiAudio.Cue.TICK);
                });
                this.hit(var62 + var42 / 2.0f, var71, var42 / 2.0f, var52, var1x -> {
                    if (var1x == 1) {
                        var22.cycleDown();
                    } else {
                        var22.cycleUp();
                    }
                    UiAudio.play(UiAudio.Cue.TICK);
                });
            }
        } else if (var3 instanceof ChoiceSetting) {
            ChoiceSetting var23 = (ChoiceSetting)var3;
            float var43 = this.d(200.0f);
            float var53 = this.d(34.0f);
            float var63 = var17 - var43;
            float var72 = var18 - var53 / 2.0f;
            boolean var79 = var8 && this.hot(var63, var72, var43, var53);
            float var88 = this.an(var9 + ":m", var79 ? 1.0f : 0.0f, 15.0f);
            var1.round(var63, var72, var43, var53, this.d(12.0f), UiRenderer.white(0.05f + 0.04f * var88));
            var1.outline(var63, var72, var43, var53, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.08f + 0.07f * var88));
            float var97 = this.d(16.0f);
            var1.icon(UiIcon.CHEVRON_LEFT, var63 + this.d(8.0f), var18 - var97 / 2.0f, var97, UiRenderer.white(0.45f + 0.4f * var88));
            var1.icon(UiIcon.CHEVRON_RIGHT, var63 + var43 - this.d(8.0f) - var97, var18 - var97 / 2.0f, var97, UiRenderer.white(0.45f + 0.4f * var88));
            float var106 = this.an(var9 + ":idx", var23.getIndex(), 16.0f);
            float var114 = (var106 - (float)var23.getIndex()) * this.d(10.0f);
            var1.pushClip(var63 + this.d(28.0f), var72, var43 - this.d(56.0f), var53);
            var1.textCentered(var1.ellipsize(var23.getMode(), var43 - this.d(64.0f), this.d(12.5f), 1), var63 + var43 / 2.0f + var114, var18 - this.d(7.5f), this.d(12.5f), 1, TEXT);
            var1.popClip();
            if (var8) {
                this.hit(var63, var72, var43 / 2.0f, var53, var1x -> {
                    if (var1x == 1) {
                        var23.cycleUp();
                    } else {
                        var23.cycleDown();
                    }
                    UiAudio.play(UiAudio.Cue.TICK);
                });
                this.hit(var63 + var43 / 2.0f, var72, var43 / 2.0f, var53, var1x -> {
                    if (var1x == 1) {
                        var23.cycleDown();
                    } else {
                        var23.cycleUp();
                    }
                    UiAudio.play(UiAudio.Cue.TICK);
                });
            }
        } else if (var3 instanceof ButtonSetting) {
            ButtonSetting var24 = (ButtonSetting)var3;
            float var44 = this.d(150.0f);
            float var54 = this.d(34.0f);
            float var64 = var17 - var44;
            float var73 = var18 - var54 / 2.0f;
            boolean var80 = var8 && this.hot(var64, var73, var44, var54);
            float var89 = this.an(var9 + ":b", var80 ? 1.0f : 0.0f, 15.0f);
            float var98 = this.an(var9 + ":press", 0.0f, 9.0f);
            var1.round(var64, var73, var44, var54, this.d(12.0f), UiEasing.mix(UiRenderer.white(0.06f + 0.06f * var89), UiPalette.accent(0.3f), var98));
            var1.outline(var64, var73, var44, var54, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiEasing.mix(UiRenderer.white(0.1f + 0.1f * var89), UiPalette.accent(0.6f), Math.max(var98, 0.4f * var89)));
            var1.textCentered("Run", var64 + var44 / 2.0f, var18 - this.d(7.5f), this.d(12.5f), 1, TEXT);
            if (var8) {
                String var107 = var9 + ":press";
                this.hit(var64, var73, var44, var54, var3x -> {
                    this.anims.put(var107, new float[]{1.0f});
                    UiAudio.play(UiAudio.Cue.CONFIRM);
                    var24.run();
                });
            }
        } else if (var3 instanceof RgbSetting) {
            RgbSetting var25 = (RgbSetting)var3;
            float var45 = this.d(150.0f);
            float var55 = this.d(34.0f);
            float var65 = var17 - var45;
            float var74 = var18 - var55 / 2.0f;
            boolean var81 = var8 && this.hot(var65, var74, var45, var55);
            float var90 = this.an(var9 + ":c", !var81 && this.coloring != var25 ? 0.0f : 1.0f, 15.0f);
            var1.round(var65, var74, var45, var55, this.d(12.0f), UiRenderer.white(0.05f + 0.05f * var90));
            var1.outline(var65, var74, var45, var55, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.08f + 0.1f * var90));
            float var99 = this.d(20.0f);
            float var108 = var65 + this.d(9.0f);
            this.checker(var1, var108, var18 - var99 / 2.0f, var99, var99, this.d(5.0f));
            var1.round(var108, var18 - var99 / 2.0f, var99, var99, this.d(7.0f), var25.getArgb());
            var1.outline(var108, var18 - var99 / 2.0f, var99, var99, this.d(7.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.3f));
            var1.text(HookUiController.hex(var25.getArgb()), var108 + var99 + this.d(10.0f), var18 - this.d(7.5f), this.d(12.5f), 1, TEXT);
            if (var8) {
                this.hit(var65, var74, var45, var55, var2x -> this.openColor(var25));
            }
        } else if (var3 instanceof BindSetting) {
            BindSetting var26 = (BindSetting)var3;
            float var46 = this.d(150.0f);
            float var56 = this.d(34.0f);
            float var66 = var17 - var46;
            float var75 = var18 - var56 / 2.0f;
            boolean var82 = this.listening == var26;
            boolean var91 = var8 && this.hot(var66, var75, var46, var56);
            float var100 = this.an(var9 + ":k", !var91 && !var82 ? 0.0f : 1.0f, 15.0f);
            float var109 = var82 ? 0.5f + 0.5f * (float)Math.sin(this.time * 6.0f) : 0.0f;
            var1.round(var66, var75, var46, var56, this.d(12.0f), UiRenderer.white(0.05f + 0.05f * var100 + 0.05f * var109));
            var1.outline(var66, var75, var46, var56, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.08f + 0.1f * var100 + 0.12f * var109));
            float var115 = this.d(16.0f);
            var1.icon(UiIcon.KEYBOARD, var66 + this.d(12.0f), var18 - var115 / 2.0f, var115, UiRenderer.white(0.55f + 0.3f * var100));
            String var118 = var82 ? "Press a key\u2026" : this.keyLabel(var26.getValue());
            var1.text(var1.ellipsize(var118, var46 - this.d(44.0f), this.d(12.5f), 1), var66 + this.d(36.0f), var18 - this.d(7.5f), this.d(12.5f), 1, var82 ? SUB : TEXT);
            if (var8) {
                this.hit(var66, var75, var46, var56, var3x -> {
                    if (var3x == 1) {
                        this.assignBind(var26, var2, -1);
                    } else {
                        this.listening = var26;
                        this.listeningModule = var2;
                        UiAudio.play(UiAudio.Cue.TICK);
                    }
                });
            }
        } else if (var3 instanceof StringSetting) {
            StringSetting var27 = (StringSetting)var3;
            float var47 = this.d(260.0f);
            float var57 = this.d(34.0f);
            float var67 = var17 - var47;
            float var76 = var18 - var57 / 2.0f;
            boolean var83 = this.editing == var27;
            boolean var92 = var8 && this.hot(var67, var76, var47, var57);
            float var101 = this.an(var9 + ":f", var83 ? 1.0f : (var92 ? 0.5f : 0.0f), 15.0f);
            this.field(var1, var67, var76, var47, var57, var27.getValue() == null ? "" : var27.getValue(), "Type here\u2026", var83, var101, null);
            if (var8) {
                this.hit(var67, var76, var47, var57, var2x -> {
                    this.editing = var27;
                    this.caretT = 0.0f;
                });
            }
        } else if (var3 instanceof ItemSetting) {
            ItemSetting var28 = (ItemSetting)var3;
            float var48 = this.d(220.0f);
            float var58 = this.d(38.0f);
            float var68 = var17 - var48;
            float var77 = var18 - var58 / 2.0f;
            boolean var84 = var8 && this.hot(var68, var77, var48, var58);
            float var93 = this.an(var9 + ":i", var84 ? 1.0f : 0.0f, 15.0f);
            var1.round(var68, var77, var48, var58, this.d(12.0f), UiRenderer.white(0.05f + 0.06f * var93));
            var1.outline(var68, var77, var48, var58, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.08f + 0.08f * var93));
            Object var102 = this.host.itemOf(var28);
            float var110 = this.d(24.0f);
            var1.item(var102, var68 + this.d(9.0f), var18 - var110 / 2.0f, var110);
            var1.text(var1.ellipsize(this.host.itemName(var102), var48 - this.d(70.0f), this.d(12.5f), 0), var68 + this.d(42.0f), var18 - this.d(7.5f), this.d(12.5f), 0, TEXT);
            float var116 = this.d(16.0f);
            var1.icon(UiIcon.CHEVRON_RIGHT, var68 + var48 - this.d(10.0f) - var116, var18 - var116 / 2.0f, var116, UiRenderer.white(0.4f + 0.4f * var93));
            if (var8) {
                this.hit(var68, var77, var48, var58, var2x -> this.openPicker(var28));
            }
        } else if (var3 instanceof StorageBlocksSetting) {
            StorageBlocksSetting var29 = (StorageBlocksSetting)var3;
            float var49 = this.d(170.0f);
            float var59 = this.d(34.0f);
            float var69 = var17 - var49;
            float var78 = var18 - var59 / 2.0f;
            boolean var85 = var8 && this.hot(var69, var78, var49, var59);
            float var94 = this.an(var9 + ":s", var85 ? 1.0f : 0.0f, 15.0f);
            var1.round(var69, var78, var49, var59, this.d(12.0f), UiRenderer.white(0.05f + 0.06f * var94));
            var1.outline(var69, var78, var49, var59, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.08f + 0.08f * var94));
            String var103 = var29.isColorOnly() ? "Edit color" : var29.selectedCount() + " selected";
            float var111 = this.d(16.0f);
            var1.icon(UiIcon.BOX, var69 + this.d(12.0f), var18 - var111 / 2.0f, var111, UiRenderer.white(0.6f + 0.3f * var94));
            var1.text(var103, var69 + this.d(36.0f), var18 - this.d(7.5f), this.d(12.5f), 1, TEXT);
            var1.icon(UiIcon.CHEVRON_RIGHT, var69 + var49 - this.d(10.0f) - var111, var18 - var111 / 2.0f, var111, UiRenderer.white(0.4f + 0.4f * var94));
            if (var8) {
                this.hit(var69, var78, var49, var59, var2x -> this.host.openStorage(var29));
            }
        }
    }

    private void slider(UiRenderer var1, Setting var2, float var3, float var4, float var5, float var6, float var7, float var8) {
        float var12;
        this.tracks.put(var2, new float[]{var3, var5});
        float var9 = this.d(6.0f);
        var1.round(var3, var4 - var9 / 2.0f, var5, var9, var9 / 2.0f, UiRenderer.white(0.12f));
        boolean var10 = var7 >= 0.0f;
        float var11 = var10 ? UiEasing.clamp(var6, 0.0f, 1.0f) : 0.0f;
        float f = var12 = var10 ? UiEasing.clamp(var7, 0.0f, 1.0f) : UiEasing.clamp(var6, 0.0f, 1.0f);
        if (var12 - var11 > 0.0f) {
            var1.round(var3 + var5 * var11, var4 - var9 / 2.0f, Math.max(var9, var5 * (var12 - var11)), var9, var9 / 2.0f, UiPalette.accent(0.92f));
        }
        float var13 = this.d(16.0f) + this.d(2.0f) * var8;
        if (var10) {
            this.knob(var1, var3 + var5 * var11, var4, var13, var8);
        }
        this.knob(var1, var3 + var5 * var12, var4, var13, var8);
    }

    private void knob(UiRenderer var1, float var2, float var3, float var4, float var5) {
        var1.glow(var2, var3, var4 * 1.4f, UiPalette.accent(0.14f * var5));
        var1.shadow(var2 - var4 / 2.0f, var3 - var4 / 2.0f, var4, var4, var4 / 2.0f, this.d(5.0f), UiRenderer.argb(0.45f, 0, 0, 0));
        var1.round(var2 - var4 / 2.0f, var3 - var4 / 2.0f, var4, var4, var4 / 2.0f, UiRenderer.white(0.97f));
        var1.round(var2 - var4 * 0.18f, var3 - var4 * 0.18f, var4 * 0.36f, var4 * 0.36f, var4 * 0.18f, UiEasing.mix(UiRenderer.argb(0.9f, 40, 41, 46), UiPalette.accent(0.95f), 0.55f));
    }

    private void valuePill(UiRenderer var1, String var2, float var3, float var4, float var5) {
        float var6 = this.d(30.0f);
        var1.round(var3, var4 - var6 / 2.0f, var5, var6, this.d(10.0f), UiRenderer.white(0.06f));
        var1.textCentered(var1.ellipsize(var2, var5 - this.d(10.0f), this.d(12.0f), 1), var3 + var5 / 2.0f, var4 - this.d(7.0f), this.d(12.0f), 1, TEXT);
    }

    private void field(UiRenderer var1, float var2, float var3, float var4, float var5, String var6, String var7, boolean var8, float var9, UiIcon var10) {
        var1.round(var2, var3, var4, var5, Math.min(var5 / 2.0f, this.d(12.0f)), UiRenderer.white(0.04f + 0.04f * var9));
        var1.outline(var2, var3, var4, var5, Math.min(var5 / 2.0f, this.d(12.0f)), Math.max(1.0f, this.u * 0.9f), UiEasing.mix(UiRenderer.white(0.08f + 0.16f * var9), UiPalette.accent(0.1f + 0.3f * var9), var9));
        float var11 = var2 + this.d(12.0f);
        if (var10 != null) {
            float var12 = this.d(17.0f);
            var1.icon(var10, var11, var3 + (var5 - var12) / 2.0f, var12, UiRenderer.white(0.5f + 0.4f * var9));
            var11 += var12 + this.d(10.0f);
        }
        float var17 = var2 + var4 - var11 - this.d(12.0f);
        float var13 = var3 + (var5 - this.d(13.0f)) / 2.0f - this.d(1.0f);
        var1.pushClip(var11, var3, var17 + this.d(4.0f), var5);
        float var14 = 0.0f;
        if (var6.isEmpty()) {
            var1.text(var7, var11, var13, this.d(13.0f), 0, FAINT);
        } else {
            float var15 = var1.textWidth(var6, this.d(13.0f), 0);
            float var16 = Math.max(0.0f, var15 - var17);
            var14 = var1.text(var6, var11 - var16, var13, this.d(13.0f), 0, TEXT) - var16;
        }
        if (var8) {
            float var18 = 0.5f + 0.5f * (float)Math.cos(this.caretT * 6.0f);
            var1.rect(var11 + var14 + this.d(1.0f), var3 + var5 * 0.26f, Math.max(1.0f, this.u * 1.4f), var5 * 0.48f, UiRenderer.white(0.9f * var18));
        }
        var1.popClip();
    }

    private void beginSlide(Setting var1, int var2) {
        this.sliding = var1;
        this.slideKnob = var2;
        this.applySlide();
    }

    private void applySlide() {
        float[] var1;
        if (this.sliding != null && (var1 = this.tracks.get(this.sliding)) != null && var1[1] > 0.0f) {
            double var2 = UiEasing.clamp((float)((this.mx - (double)var1[0]) / (double)var1[1]), 0.0f, 1.0f);
            Setting setting = this.sliding;
            if (setting instanceof NumberSetting) {
                NumberSetting var4 = (NumberSetting)setting;
                double var10 = HookUiController.snap(var4.getMin() + var2 * (var4.getMax() - var4.getMin()), var4.getFormat());
                double var8 = Math.max(var4.getMin(), Math.min(var4.getMax(), var10));
                if (var8 != var4.getValue()) {
                    var4.getValue(var8);
                    UiAudio.play(UiAudio.Cue.SLIDE);
                }
            } else {
                Setting var10 = this.sliding;
                if (var10 instanceof MinMaxSetting) {
                    MinMaxSetting var5 = (MinMaxSetting)var10;
                    double var12 = HookUiController.snap(var5.getMinValue() + var2 * (var5.getMaxValue() - var5.getMinValue()), var5.getStep());
                    var12 = Math.max(var5.getMinValue(), Math.min(var5.getMaxValue(), var12));
                    if (this.slideKnob == 0) {
                        var5.setCurrentMin(Math.min(var12, var5.getCurrentMax()));
                    } else {
                        var5.setCurrentMax(Math.max(var12, var5.getCurrentMin()));
                    }
                }
            }
        }
    }

    private static double snap(double var0, double var2) {
        return var2 > 0.0 ? (double)Math.round(var0 / var2) * var2 : var0;
    }

    static String format(double var0, double var2) {
        if (!(var2 >= 1.0) && !(var2 <= 0.0)) {
            int var4 = Math.max(1, Math.min(4, (int)Math.ceil(-Math.log10(var2) - 1.0E-9)));
            return String.format(Locale.ROOT, "%." + var4 + "f", var0);
        }
        return String.valueOf(Math.round(var0));
    }

    private static String prettyEnum(String var0) {
        if (var0.isEmpty()) {
            return var0;
        }
        String var1 = var0.replace('_', ' ').toLowerCase(Locale.ROOT);
        StringBuilder var2 = new StringBuilder();
        boolean var3 = true;
        for (char var7 : var1.toCharArray()) {
            var2.append(var3 ? Character.toUpperCase(var7) : var7);
            var3 = var7 == ' ' || Character.isDigit(var7);
        }
        return var2.toString();
    }

    private String keyLabel(int var1) {
        if (var1 < 0) {
            return "None";
        }
        String var2 = this.host.keyName(var1);
        return var2 != null && !var2.isBlank() ? var2 : "Key " + var1;
    }

    private void assignBind(BindSetting var1, Module var2, int var3) {
        var1.setValue(var3);
        if (var1.isModuleKey() && var2 != null) {
            var2.setKeybind(var3);
        }
        var1.setListening(false);
        this.listening = null;
        UiAudio.play(UiAudio.Cue.CONFIRM);
    }

    private void drawSearchBar(UiRenderer var1) {
        float var2 = this.mainX();
        float var3 = this.wy + this.d(644.0f);
        float var4 = this.d(56.0f);
        float var6 = this.d(10.0f);
        float var7 = this.mainW() - (var4 + var6) * 3.0f;
        boolean var8 = this.hot(var2, var3, var7, var4);
        float var9 = this.an("search:f", this.searchFocus ? 1.0f : (var8 ? 0.45f : 0.0f), 13.0f);
        this.glass(var1, var2, var3, var7, var4, var4 / 2.0f, 6.3f);
        var1.round(var2, var3, var7, var4, var4 / 2.0f, UiRenderer.white(0.035f * var9));
        var1.outline(var2, var3, var7, var4, var4 / 2.0f, Math.max(1.0f, this.u * 0.9f), UiEasing.mix(UiRenderer.white(0.14f * var9), UiPalette.accent(0.32f * var9), var9));
        float var10 = this.d(19.0f);
        var1.icon(UiIcon.SEARCH, var2 + this.d(22.0f), var3 + (var4 - var10) / 2.0f, var10, UiRenderer.white(0.55f + 0.4f * var9));
        float var11 = var2 + this.d(54.0f);
        float var12 = var3 + (var4 - this.d(14.5f)) / 2.0f - this.d(1.0f);
        float var13 = var7 - this.d(54.0f) - this.d(80.0f);
        var1.pushClip(var11, var3, var13, var4);
        float var14 = 0.0f;
        if (this.query.isEmpty()) {
            var1.text("Search modules\u2026", var11, var12, this.d(14.5f), 0, FAINT);
        } else {
            float var15 = var1.textWidth(this.query, this.d(14.5f), 0);
            float var16 = Math.max(0.0f, var15 - var13 + this.d(6.0f));
            var14 = var1.text(this.query, var11 - var16, var12, this.d(14.5f), 0, TEXT) - var16;
        }
        if (this.searchFocus) {
            float var21 = 0.5f + 0.5f * (float)Math.cos(this.caretT * 6.0f);
            var1.rect(var11 + var14 + this.d(1.5f), var3 + var4 * 0.3f, Math.max(1.0f, this.u * 1.4f), var4 * 0.4f, UiRenderer.white(0.9f * var21));
        }
        var1.popClip();
        if (!this.query.isEmpty()) {
            float var17;
            float var22 = this.d(28.0f);
            float var27 = var2 + var7 - this.d(16.0f) - var22;
            boolean var18 = this.hot(var27, var17 = var3 + (var4 - var22) / 2.0f, var22, var22);
            float var19 = this.an("search:clear", var18 ? 1.0f : 0.0f, 15.0f);
            var1.round(var27, var17, var22, var22, var22 / 2.0f, UiRenderer.white(0.06f + 0.08f * var19));
            float var20 = this.d(14.0f);
            var1.icon(UiIcon.CLOSE, var27 + (var22 - var20) / 2.0f, var17 + (var22 - var20) / 2.0f, var20, UiRenderer.white(0.6f + 0.4f * var19));
            this.hit(var2, var3, var7, var4, var1x -> {
                this.searchFocus = true;
                this.caretT = 0.0f;
            });
            this.hit(var27, var17, var22, var22, var1x -> {
                this.query = "";
                this.tabT = 0.0f;
            });
        } else {
            String var23 = "CTRL F";
            float var28 = var1.textWidth(var23, this.d(10.0f), 1) + this.d(16.0f);
            float var30 = var2 + var7 - this.d(18.0f) - var28;
            var1.round(var30, var3 + (var4 - this.d(24.0f)) / 2.0f, var28, this.d(24.0f), this.d(8.0f), UiRenderer.white(0.05f * (1.0f - var9)));
            var1.textCentered(var23, var30 + var28 / 2.0f, var3 + (var4 - this.d(10.0f)) / 2.0f - this.d(0.5f), this.d(10.0f), 1, UiRenderer.white(0.3f * (1.0f - var9)));
            this.hit(var2, var3, var7, var4, var1x -> {
                this.searchFocus = true;
                this.caretT = 0.0f;
            });
        }
        float var24 = var2 + var7 + var6;
        this.utilityButton(var1, "profiles", var24, var3, var4, UiIcon.FOLDER, "Configs", true, var1x -> {
            this.searchFocus = false;
            this.editing = null;
            this.listening = null;
            this.sliding = null;
            this.configs.open();
        });
        boolean var29 = this.host.inWorld();
        this.utilityButton(var1, "hud", var24 += var4 + var6, var3, var4, UiIcon.LAYOUT, var29 ? "Edit HUD" : "Edit HUD (join a world)", var29, var1x -> this.host.openHudEditor());
        this.utilityButton(var1, "cfg", var24 += var4 + var6, var3, var4, UiIcon.GEAR, "Client settings", true, var1x -> {
            Module var2x = this.clientModule();
            if (var2x != null) {
                this.query = "";
                this.searchFocus = false;
                this.openPage(var2x);
            }
        });
    }

    private void utilityButton(UiRenderer var1, String var2, float var3, float var4, float var5, UiIcon var6, String var7, boolean var8, ClickAction var9) {
        boolean var10 = this.hot(var3, var4, var5, var5);
        float var11 = this.an("btn:" + var2, var10 ? 1.0f : 0.0f, 14.0f);
        this.glass(var1, var3, var4, var5, var5, this.d(18.0f), var3 * 0.01f);
        var1.round(var3, var4, var5, var5, this.d(18.0f), UiRenderer.white(0.05f * var11));
        var1.outline(var3, var4, var5, var5, this.d(18.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.12f * var11));
        if (var11 > 0.01f) {
            var1.glow(var3 + var5 / 2.0f, var4 + var5 / 2.0f, var5 * 0.8f, UiRenderer.white(0.05f * var11));
        }
        float var12 = this.d(20.0f);
        var1.icon(var6, var3 + (var5 - var12) / 2.0f, var4 + (var5 - var12) / 2.0f - this.d(1.0f) * var11, var12, UiRenderer.white(var8 ? 0.7f + 0.3f * var11 : 0.28f));
        if (var10) {
            this.tip = var7;
            this.tipX = var3 + var5 / 2.0f;
            this.tipY = var4 - this.d(10.0f);
        }
        if (var8) {
            this.hit(var3, var4, var5, var5, var9);
        } else {
            this.hit(var3, var4, var5, var5, var0 -> {});
        }
    }

    private void drawTip(UiRenderer var1) {
        float var2 = this.an("tip", 1.0f, 12.0f);
        float var3 = var1.textWidth(this.tip, this.d(11.5f), 0) + this.d(22.0f);
        float var4 = this.d(28.0f);
        float var5 = Math.min(this.tipX - var3 / 2.0f, this.wx + this.d(1000.0f) - var3);
        float var6 = this.tipY - var4;
        var1.pushAlpha(var2);
        var1.shadow(var5, var6, var3, var4, this.d(10.0f), this.d(12.0f), UiRenderer.argb(0.4f, 0, 0, 0));
        var1.round(var5, var6, var3, var4, this.d(10.0f), UiRenderer.argb(0.92f, 28, 29, 33));
        var1.outline(var5, var6, var3, var4, this.d(10.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.1f));
        var1.textCentered(this.tip, var5 + var3 / 2.0f, var6 + (var4 - this.d(11.5f)) / 2.0f - this.d(1.0f), this.d(11.5f), 0, TEXT);
        var1.popAlpha();
    }

    private void openColor(RgbSetting var1) {
        this.coloring = var1;
        this.colorDrag = 0;
        int var2 = var1.getArgb();
        Color.RGBtoHSB(var2 >> 16 & 0xFF, var2 >> 8 & 0xFF, var2 & 0xFF, this.hsv);
        this.alphaPick = (float)(var2 >>> 24) / 255.0f;
        UiAudio.play(UiAudio.Cue.OPEN);
    }

    private void closeColor() {
        this.coloring = null;
        this.colorDrag = 0;
        UiAudio.play(UiAudio.Cue.BACK);
    }

    private void writeColor() {
        if (this.coloring != null) {
            int var1 = Color.HSBtoRGB(this.hsv[0], this.hsv[1], this.hsv[2]) & 0xFFFFFF;
            this.coloring.setArgb(Math.round(UiEasing.clamp(this.alphaPick, 0.0f, 1.0f) * 255.0f) << 24 | var1);
        }
    }

    private void applyColorDrag() {
        if (this.coloring != null && this.colorDrag != 0) {
            if (this.colorDrag == 1) {
                this.hsv[1] = UiEasing.clamp((float)((this.mx - (double)this.svRect[0]) / (double)this.svRect[2]), 0.0f, 1.0f);
                this.hsv[2] = 1.0f - UiEasing.clamp((float)((this.my - (double)this.svRect[1]) / (double)this.svRect[3]), 0.0f, 1.0f);
            } else if (this.colorDrag == 2) {
                this.hsv[0] = UiEasing.clamp((float)((this.my - (double)this.hueRect[1]) / (double)this.hueRect[3]), 0.0f, 0.999f);
            } else if (this.colorDrag == 3) {
                this.alphaPick = 1.0f - UiEasing.clamp((float)((this.my - (double)this.alphaRect[1]) / (double)this.alphaRect[3]), 0.0f, 1.0f);
            }
            this.writeColor();
        }
    }

    private static String hex(int var0) {
        return var0 >>> 24 >= 255 ? String.format("#%06X", var0 & 0xFFFFFF) : String.format("#%08X", var0);
    }

    private void checker(UiRenderer var1, float var2, float var3, float var4, float var5, float var6) {
        var1.round(var2, var3, var4, var5, var6, UiRenderer.argb(1.0f, 205, 205, 210));
        float var7 = Math.max(2.0f, this.d(5.0f));
        var1.pushClip(var2 + var6 * 0.3f, var3 + var6 * 0.3f, var4 - var6 * 0.6f, var5 - var6 * 0.6f);
        for (float var8 = var3; var8 < var3 + var5; var8 += var7) {
            for (float var9 = var2; var9 < var2 + var4; var9 += var7) {
                if (((int)((var9 - var2) / var7) + (int)((var8 - var3) / var7)) % 2 != 0) continue;
                var1.rect(var9, var8, var7, var7, UiRenderer.argb(1.0f, 150, 150, 158));
            }
        }
        var1.popClip();
    }

    private void drawColorPicker(UiRenderer var1) {
        float var2 = this.an("colorpicker", this.coloring != null ? 1.0f : 0.0f, 13.0f);
        if (!(var2 < 0.005f || this.coloring == null && var2 < 0.02f)) {
            float var46;
            RgbSetting var3 = this.coloring;
            var1.pushAlpha(var2);
            float var4 = this.mainX();
            var1.round(var4, this.wy, this.mainW(), this.mainH(), this.d(22.0f), UiRenderer.argb(0.45f, 4, 4, 6));
            float var5 = this.d(470.0f);
            float var6 = this.d(284.0f);
            float var7 = var4 + (this.mainW() - var5) / 2.0f;
            float var8 = this.wy + (this.mainH() - var6) / 2.0f;
            float var9 = 0.94f + 0.06f * UiEasing.out(var2);
            var1.pushTransform((var7 + var5 / 2.0f) * (1.0f - var9), (var8 + var6 / 2.0f) * (1.0f - var9), var9);
            var1.shadow(var7, var8, var5, var6, this.d(20.0f), this.d(34.0f), UiRenderer.argb(0.55f, 0, 0, 0));
            var1.round(var7, var8, var5, var6, this.d(20.0f), UiRenderer.argb(0.94f, 24, 25, 29));
            var1.outline(var7, var8, var5, var6, this.d(20.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.1f));
            String var10 = var3 == null ? "" : var3.getName().toString();
            var1.text(var1.ellipsize(var10, var5 - this.d(90.0f), this.d(17.0f), 1), var7 + this.d(22.0f), var8 + this.d(20.0f), this.d(17.0f), 1, TEXT);
            float var11 = this.d(30.0f);
            float var12 = var7 + var5 - this.d(18.0f) - var11;
            float var13 = var8 + this.d(16.0f);
            boolean var14 = var3 != null && this.hot(var12, var13, var11, var11);
            float var15 = this.an("colorpicker:x", var14 ? 1.0f : 0.0f, 15.0f);
            var1.round(var12, var13, var11, var11, var11 / 2.0f, UiRenderer.white(0.05f + 0.08f * var15));
            float var16 = this.d(14.0f);
            var1.icon(UiIcon.CLOSE, var12 + (var11 - var16) / 2.0f, var13 + (var11 - var16) / 2.0f, var16, UiRenderer.white(0.6f + 0.4f * var15));
            if (var3 != null) {
                this.hit(var7, var8, var5, var6, var0 -> {});
                this.hit(var12, var13, var11, var11, var1x -> this.closeColor());
            }
            float var17 = var7 + this.d(22.0f);
            float var18 = var8 + this.d(62.0f);
            float var19 = this.d(240.0f);
            float var20 = this.d(200.0f);
            this.svRect[0] = var17;
            this.svRect[1] = var18;
            this.svRect[2] = var19;
            this.svRect[3] = var20;
            int var21 = Math.max(24, Math.round(var19 / Math.max(1.0f, this.u * 2.0f)));
            float var22 = var19 / (float)var21;
            for (int var23 = 0; var23 < var21; ++var23) {
                float var24 = ((float)var23 + 0.5f) / (float)var21;
                int var25 = 0xFF000000 | Color.HSBtoRGB(this.hsv[0], var24, 1.0f) & 0xFFFFFF;
                var1.gradient(var17 + (float)var23 * var22, var18, var22 + 1.0f, var20, var25, -16777216);
            }
            var1.outline(var17, var18, var19, var20, this.d(4.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.14f));
            float var42 = var17 + this.hsv[1] * var19;
            float var43 = var18 + (1.0f - this.hsv[2]) * var20;
            var1.circle(var42, var43, this.d(8.0f), UiRenderer.argb(0.6f, 0, 0, 0));
            var1.circle(var42, var43, this.d(7.0f), UiRenderer.white(1.0f));
            var1.circle(var42, var43, this.d(5.0f), 0xFF000000 | Color.HSBtoRGB(this.hsv[0], this.hsv[1], this.hsv[2]) & 0xFFFFFF);
            if (var3 != null) {
                this.hit(var17, var18, var19, var20, var1x -> {
                    this.colorDrag = 1;
                    this.applyColorDrag();
                });
            }
            float var44 = var17 + var19 + this.d(16.0f);
            float var26 = this.d(18.0f);
            this.hueRect[0] = var44;
            this.hueRect[1] = var18;
            this.hueRect[2] = var26;
            this.hueRect[3] = var20;
            for (int var27 = 0; var27 < 6; ++var27) {
                int var28 = 0xFF000000 | Color.HSBtoRGB((float)var27 / 6.0f, 1.0f, 1.0f) & 0xFFFFFF;
                int var29 = 0xFF000000 | Color.HSBtoRGB((float)(var27 + 1) / 6.0f, 1.0f, 1.0f) & 0xFFFFFF;
                var1.gradient(var44, var18 + var20 * (float)var27 / 6.0f, var26, var20 / 6.0f + 1.0f, var28, var29);
            }
            var1.outline(var44, var18, var26, var20, this.d(4.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.14f));
            float var45 = var18 + this.hsv[0] * var20;
            var1.round(var44 - this.d(3.0f), var45 - this.d(3.0f), var26 + this.d(6.0f), this.d(6.0f), this.d(3.0f), UiRenderer.white(1.0f));
            if (var3 != null) {
                this.hit(var44 - this.d(4.0f), var18, var26 + this.d(8.0f), var20, var1x -> {
                    this.colorDrag = 2;
                    this.applyColorDrag();
                });
            }
            this.alphaRect[0] = var46 = var44 + var26 + this.d(14.0f);
            this.alphaRect[1] = var18;
            this.alphaRect[2] = var26;
            this.alphaRect[3] = var20;
            this.checker(var1, var46, var18, var26, var20, this.d(4.0f));
            int var47 = 0xFF000000 | Color.HSBtoRGB(this.hsv[0], this.hsv[1], this.hsv[2]) & 0xFFFFFF;
            var1.gradient(var46, var18, var26, var20, var47, var47 & 0xFFFFFF);
            var1.outline(var46, var18, var26, var20, this.d(4.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.14f));
            float var30 = var18 + (1.0f - this.alphaPick) * var20;
            var1.round(var46 - this.d(3.0f), var30 - this.d(3.0f), var26 + this.d(6.0f), this.d(6.0f), this.d(3.0f), UiRenderer.white(1.0f));
            if (var3 != null) {
                this.hit(var46 - this.d(4.0f), var18, var26 + this.d(8.0f), var20, var1x -> {
                    this.colorDrag = 3;
                    this.applyColorDrag();
                });
            }
            float var31 = var46 + var26 + this.d(20.0f);
            float var32 = var7 + var5 - this.d(22.0f) - var31;
            int var33 = var3 != null ? var3.getArgb() : var47;
            this.checker(var1, var31, var18, var32, this.d(54.0f), this.d(12.0f));
            var1.round(var31, var18, var32, this.d(54.0f), this.d(12.0f), var33);
            var1.outline(var31, var18, var32, this.d(54.0f), this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.16f));
            var1.textCentered(HookUiController.hex(var33), var31 + var32 / 2.0f, var18 + this.d(66.0f), this.d(12.5f), 1, TEXT);
            var1.textCentered(Math.round(this.alphaPick * 100.0f) + "% opacity", var31 + var32 / 2.0f, var18 + this.d(86.0f), this.d(11.0f), 0, SUB);
            int[] var34 = new int[]{0xFF000000 | UiPalette.accent(1.0f) & 0xFFFFFF, -1, -42406, -20898, -8090, -9508952, -10636545, -4809473};
            float var35 = (var32 - this.d(12.0f)) / 4.0f;
            float var36 = this.d(4.0f);
            for (int var37 = 0; var37 < var34.length; ++var37) {
                float var38 = var31 + (float)(var37 % 4) * (var35 + var36);
                float var39 = var18 + this.d(112.0f) + (float)(var37 / 4) * (var35 + var36);
                boolean var40 = var3 != null && this.hot(var38, var39, var35, var35);
                var1.round(var38, var39, var35, var35, this.d(8.0f), var34[var37]);
                var1.outline(var38, var39, var35, var35, this.d(8.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(var40 ? 0.6f : 0.15f));
                if (var3 == null) continue;
                int var41 = var34[var37];
                this.hit(var38, var39, var35, var35, var2x -> {
                    Color.RGBtoHSB(var41 >> 16 & 0xFF, var41 >> 8 & 0xFF, var41 & 0xFF, this.hsv);
                    this.writeColor();
                    UiAudio.play(UiAudio.Cue.TICK);
                });
            }
            var1.popTransform();
            var1.popAlpha();
        }
    }

    private void openPicker(ItemSetting var1) {
        this.picking = var1;
        this.pickQuery = "";
        this.pickItems = var1.blocksOnly() ? this.host.items().stream().filter(var1::accepts).toList() : this.host.items();
        this.pickScroll[0] = 0.0f;
        this.pickScroll[1] = 0.0f;
        UiAudio.play(UiAudio.Cue.OPEN);
    }

    private void closePicker() {
        this.picking = null;
        UiAudio.play(UiAudio.Cue.BACK);
    }

    private void drawPicker(UiRenderer var1) {
        float var2 = this.an("picker", this.picking != null ? 1.0f : 0.0f, 13.0f);
        if (!(var2 < 0.005f)) {
            var1.pushAlpha(var2);
            float var3 = this.mainX();
            var1.round(var3, this.wy, this.mainW(), this.mainH(), this.d(22.0f), UiRenderer.argb(0.45f, 4, 4, 6));
            float var4 = this.d(560.0f);
            float var5 = this.d(440.0f);
            float var6 = var3 + (this.mainW() - var4) / 2.0f;
            float var7 = this.wy + (this.mainH() - var5) / 2.0f;
            float var8 = 0.94f + 0.06f * UiEasing.out(var2);
            var1.pushTransform((var6 + var4 / 2.0f) * (1.0f - var8), (var7 + var5 / 2.0f) * (1.0f - var8), var8);
            var1.shadow(var6, var7, var4, var5, this.d(20.0f), this.d(34.0f), UiRenderer.argb(0.55f, 0, 0, 0));
            var1.round(var6, var7, var4, var5, this.d(20.0f), UiRenderer.argb(0.94f, 24, 25, 29));
            var1.outline(var6, var7, var4, var5, this.d(20.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.1f));
            var1.text(this.picking != null && this.picking.blocksOnly() ? "Select block" : "Select item", var6 + this.d(22.0f), var7 + this.d(20.0f), this.d(17.0f), 1, TEXT);
            float var9 = this.d(30.0f);
            float var10 = var6 + var4 - this.d(18.0f) - var9;
            float var11 = var7 + this.d(16.0f);
            boolean var12 = this.picking != null && this.hot(var10, var11, var9, var9);
            float var13 = this.an("picker:x", var12 ? 1.0f : 0.0f, 15.0f);
            var1.round(var10, var11, var9, var9, var9 / 2.0f, UiRenderer.white(0.05f + 0.08f * var13));
            float var14 = this.d(14.0f);
            var1.icon(UiIcon.CLOSE, var10 + (var9 - var14) / 2.0f, var11 + (var9 - var14) / 2.0f, var14, UiRenderer.white(0.6f + 0.4f * var13));
            if (this.picking != null) {
                this.hit(var6, var7, var4, var5, var0 -> {});
                this.hit(var10, var11, var9, var9, var1x -> this.closePicker());
            }
            float var15 = var7 + this.d(60.0f);
            this.field(var1, var6 + this.d(20.0f), var15, var4 - this.d(40.0f), this.d(38.0f), this.pickQuery, "Search items\u2026", true, 1.0f, UiIcon.SEARCH);
            float var16 = var6 + this.d(20.0f);
            float var17 = var15 + this.d(52.0f);
            float var18 = var4 - this.d(40.0f);
            float var19 = var7 + var5 - this.d(18.0f) - var17;
            this.pickRect[0] = var16;
            this.pickRect[1] = var17;
            this.pickRect[2] = var18;
            this.pickRect[3] = var19;
            ArrayList var20 = new ArrayList();
            String var21 = this.pickQuery.toLowerCase(Locale.ROOT);
            if (this.pickItems != null) {
                for (Object var23 : this.pickItems) {
                    if (!var21.isEmpty() && !this.host.itemName(var23).toLowerCase(Locale.ROOT).contains(var21)) continue;
                    var20.add(var23);
                }
            }
            float var40 = this.d(48.0f);
            float var41 = this.d(8.0f);
            int var24 = Math.max(1, (int)((var18 + var41) / (var40 + var41)));
            float var25 = (float)var24 * var40 + (float)(var24 - 1) * var41;
            float var26 = var16 + (var18 - var25) / 2.0f;
            int var27 = (var20.size() + var24 - 1) / var24;
            float var28 = (float)var27 * (var40 + var41) - var41;
            this.pickMax = Math.max(0.0f, var28 - var19);
            float var29 = this.scroll(this.pickScroll, this.pickMax);
            Object var30 = this.picking != null ? this.host.itemOf(this.picking) : null;
            var1.pushClip(var16, var17, var18, var19);
            this.hitClip = new float[]{var16, var17, var18, var19};
            String var31 = null;
            for (int var32 = 0; var32 < var20.size(); ++var32) {
                float var33 = var26 + (float)(var32 % var24) * (var40 + var41);
                float var34 = var17 + (float)(var32 / var24) * (var40 + var41) - var29;
                if (var34 + var40 < var17 || var34 > var17 + var19) continue;
                Object var35 = var20.get(var32);
                boolean var36 = this.picking != null && this.hot(var33, var34, var40, var40);
                boolean var37 = var35 == var30;
                var1.round(var33, var34, var40, var40, this.d(12.0f), UiRenderer.white(0.04f + (var36 ? 0.08f : 0.0f) + (var37 ? 0.1f : 0.0f)));
                if (var37) {
                    var1.outline(var33, var34, var40, var40, this.d(12.0f), Math.max(1.0f, this.u * 0.9f), UiRenderer.white(0.5f));
                }
                float var38 = this.d(30.0f);
                var1.item(var35, var33 + (var40 - var38) / 2.0f, var34 + (var40 - var38) / 2.0f, var38);
                if (var36) {
                    var31 = this.host.itemName(var35);
                }
                if (this.picking == null) continue;
                ItemSetting var39 = this.picking;
                this.hit(var33, var34, var40, var40, var3x -> {
                    this.host.setItem(var39, var35);
                    this.picking = null;
                    UiAudio.play(UiAudio.Cue.CONFIRM);
                });
            }
            this.hitClip = null;
            var1.popClip();
            if (var31 != null) {
                var1.textRight(var31, var6 + var4 - this.d(22.0f), var7 + this.d(24.0f), this.d(12.0f), 0, SUB);
            }
            var1.popTransform();
            var1.popAlpha();
        }
    }

    private void scrollbar(UiRenderer var1, float var2, float var3, float var4, float var5, float var6, float var7) {
        if (var6 > 0.5f) {
            float var8 = var4 / var7;
            float var9 = Math.max(this.d(30.0f), var4 * var8);
            float var10 = var3 + (var4 - var9) * (var5 / var6);
            var1.round(var2, var10, this.d(4.0f), var9, this.d(2.0f), UiRenderer.white(0.18f));
        }
    }

    private float scroll(float[] var1, float var2) {
        var1[1] = UiEasing.clamp(var1[1], 0.0f, var2);
        var1[0] = var1[0] + (var1[1] - var1[0]) * (1.0f - (float)Math.exp(-16.0f * this.dt));
        if (Math.abs(var1[1] - var1[0]) < 0.2f) {
            var1[0] = var1[1];
        }
        return var1[0];
    }

    private List<String> wrap(UiRenderer var1, String var2, float var3, float var4, int var5) {
        ArrayList<String> var6 = new ArrayList<String>();
        if (var2 != null && !var2.isBlank()) {
            String[] var7 = var2.trim().split("\\s+");
            StringBuilder var8 = new StringBuilder();
            int var9 = 0;
            while (var9 < var7.length) {
                String var10;
                String string = var10 = var8.length() == 0 ? var7[var9] : String.valueOf(var8) + " " + var7[var9];
                if (!(var1.textWidth(var10, var4, 0) <= var3) && var8.length() != 0) {
                    var6.add(var8.toString());
                    var8.setLength(0);
                    if (var6.size() != var5 - 1) continue;
                    StringBuilder var11 = new StringBuilder();
                    for (int var12 = var9; var12 < var7.length; ++var12) {
                        var11.append(var12 == var9 ? "" : " ").append(var7[var12]);
                    }
                    var6.add(var1.ellipsize(var11.toString(), var3, var4, 0));
                    return var6;
                }
                var8.setLength(0);
                var8.append(var10);
                ++var9;
            }
            if (var8.length() > 0) {
                var6.add(var1.ellipsize(var8.toString(), var3, var4, 0));
            }
            return var6;
        }
        return var6;
    }

    private List<Module> modules(Category var1) {
        return ModuleCatalog.category(this, var1);
    }

    private List<Module> allModules() {
        try {
            return Hook.INSTANCE.getModuleManager().c().stream()
                .filter(module -> !PanicButtonModule.shouldHide(module))
                .toList();
        }
        catch (Throwable var2) {
            return List.of();
        }
    }

    private List<Module> searchResults() {
        return ModuleCatalog.search(this, this.query);
    }

    private Module clientModule() {
        for (Module var2 : this.allModules()) {
            if (!(var2 instanceof hook.module.modules.client.Hook)) continue;
            return var2;
        }
        return null;
    }

    private void hit(float var1, float var2, float var3, float var4, ClickAction var5) {
        if (this.hitClip != null) {
            float var6 = Math.min(var1 + var3, this.hitClip[0] + this.hitClip[2]);
            float var7 = Math.min(var2 + var4, this.hitClip[1] + this.hitClip[3]);
            var1 = Math.max(var1, this.hitClip[0]);
            var2 = Math.max(var2, this.hitClip[1]);
            var3 = var6 - var1;
            var4 = var7 - var2;
            if (var3 <= 0.0f || var4 <= 0.0f) {
                return;
            }
        }
        this.hits.add(new ClickRegion(var1, var2, var3, var4, this.layerNow, var5));
    }

    private int activeLayer() {
        return this.configs.active() ? 2 : (this.picking == null && this.coloring == null ? 0 : 1);
    }

    private boolean hot(float var1, float var2, float var3, float var4) {
        if (!(this.layerNow < this.activeLayer() || this.dragging || this.closing || this.vis < 0.6f)) {
            return this.hitClip == null || !(this.mx < (double)this.hitClip[0]) && !(this.my < (double)this.hitClip[1]) && !(this.mx >= (double)(this.hitClip[0] + this.hitClip[2])) && !(this.my >= (double)(this.hitClip[1] + this.hitClip[3])) ? this.mx >= (double)var1 && this.mx < (double)(var1 + var3) && this.my >= (double)var2 && this.my < (double)(var2 + var4) : false;
        }
        return false;
    }

    public boolean mouseClicked(double var1, double var3, int var5) {
        if (this.configs.active()) {
            return this.configs.mouseClicked(var1, var3, var5);
        }
        this.mx = var1;
        this.my = var3;
        if (!this.closing && !(this.vis < 0.4f)) {
            if (this.listening != null) {
                if (var5 >= 2) {
                    this.assignBind(this.listening, this.listeningModule, var5);
                } else {
                    this.listening = null;
                }
                return true;
            }
            this.searchFocus = false;
            this.editing = null;
            int var6 = this.activeLayer();
            for (int var7 = this.hits.size() - 1; var7 >= 0; --var7) {
                ClickRegion var8 = this.hits.get(var7);
                if (var8.layer != var6 || !(var1 >= (double)var8.x) || !(var1 < (double)(var8.x + var8.w)) || !(var3 >= (double)var8.y) || !(var3 < (double)(var8.y + var8.h))) continue;
                var8.act.on(var5);
                return true;
            }
            if (var6 == 1) {
                if (this.coloring != null) {
                    this.closeColor();
                } else {
                    this.closePicker();
                }
            } else if (var5 == 3 && this.page != null) {
                this.closePage();
            }
            return true;
        }
        return true;
    }

    public void mouseReleased(double var1, double var3, int var5) {
        this.mx = var1;
        this.my = var3;
        if (this.dragging) {
            this.dragging = false;
            this.savePosition();
        }
        if (this.sliding != null) {
            this.applySlide();
            this.sliding = null;
        }
        this.colorDrag = 0;
    }

    public void mouseDragged(double var1, double var3) {
        this.mx = var1;
        this.my = var3;
        this.applySlide();
        this.applyColorDrag();
    }

    public void mouseScrolled(double var1, double var3, double var5) {
        if (!this.configs.active()) {
            float var7 = this.d(70.0f) * (float)var5;
            if (this.picking != null) {
                if (HookUiController.inside(var1, var3, this.pickRect)) {
                    this.pickScroll[1] = UiEasing.clamp(this.pickScroll[1] - var7, 0.0f, this.pickMax);
                }
            } else if (this.page != null) {
                if (HookUiController.inside(var1, var3, this.pageRect)) {
                    this.pageScroll[1] = UiEasing.clamp(this.pageScroll[1] - var7, 0.0f, this.pageMax);
                }
            } else if (HookUiController.inside(var1, var3, this.gridRect)) {
                // Move the module grid one complete card row per wheel notch.
                float rowStep = this.d(102.0f) * (float)var5;
                this.gridScroll[1] = UiEasing.clamp(this.gridScroll[1] - rowStep, 0.0f, this.gridMax);
            }
        }
    }

    private static boolean inside(double var0, double var2, float[] var4) {
        return var0 >= (double)var4[0] && var2 >= (double)var4[1] && var0 < (double)(var4[0] + var4[2]) && var2 < (double)(var4[1] + var4[3]);
    }

    public boolean keyPressed(int var1, int var2) {
        boolean var3;
        if (this.closing) {
            return true;
        }
        if (this.configs.active()) {
            return this.configs.keyPressed(var1, var2);
        }
        boolean bl = var3 = (var2 & 2) != 0;
        if (this.coloring != null) {
            if (var1 == 256 || var1 == 257 || var1 == 335) {
                this.closeColor();
            }
            return true;
        }
        if (this.listening != null) {
            if (var1 == 256) {
                // Esc unbinds; the menu key can't be left unbound, so it goes back to Right Shift
                boolean menuKey = this.listening.isModuleKey() && this.listeningModule instanceof hook.module.modules.client.Hook;
                this.assignBind(this.listening, this.listeningModule, menuKey ? 344 : -1);
            } else if (var1 != 259 && var1 != 261) {
                this.assignBind(this.listening, this.listeningModule, var1);
            } else {
                boolean menuKey = this.listening.isModuleKey() && this.listeningModule instanceof hook.module.modules.client.Hook;
                this.assignBind(this.listening, this.listeningModule, menuKey ? 344 : -1);
            }
            return true;
        }
        if (this.picking != null) {
            if (var1 == 256) {
                this.closePicker();
            } else if (var1 == 259 && !this.pickQuery.isEmpty()) {
                this.pickQuery = var3 ? "" : this.pickQuery.substring(0, this.pickQuery.length() - 1);
                this.pickScroll[1] = 0.0f;
            }
            return true;
        }
        if (this.editing != null) {
            String var4;
            String string = var4 = this.editing.getValue() == null ? "" : this.editing.getValue();
            if (var1 == 256 || var1 == 257 || var1 == 335) {
                this.editing = null;
            } else if (var1 == 259 && !var4.isEmpty()) {
                this.editing.setValue(var3 ? "" : var4.substring(0, var4.length() - 1));
                this.caretT = 0.0f;
            }
            return true;
        }
        if (this.searchFocus) {
            if (var1 == 256) {
                if (this.query.isEmpty()) {
                    this.searchFocus = false;
                } else {
                    this.query = "";
                    this.tabT = 0.0f;
                }
            } else if (var1 == 257 || var1 == 335) {
                this.searchFocus = false;
            } else if (var1 == 259 && !this.query.isEmpty()) {
                this.query = var3 ? "" : this.query.substring(0, this.query.length() - 1);
                this.gridScroll[1] = 0.0f;
                if (this.query.isEmpty()) {
                    this.tabT = 0.0f;
                }
            }
            return true;
        }
        if (!(var3 && var1 == 70 || var1 == 47)) {
            if (var1 == 256) {
                if (this.page != null) {
                    this.closePage();
                } else if (!this.query.isEmpty()) {
                    this.query = "";
                    this.tabT = 0.0f;
                } else {
                    this.requestClose();
                }
                return true;
            }
            return false;
        }
        this.searchFocus = true;
        this.page = null;
        this.caretT = 0.0f;
        return true;
    }

    public boolean charTyped(char var1) {
        if (this.configs.active()) {
            return this.configs.charTyped(var1);
        }
        if (Character.isISOControl(var1) || this.closing) {
            return false;
        }
        if (this.picking != null) {
            if (this.pickQuery.length() < 32) {
                this.pickQuery = this.pickQuery + var1;
                this.pickScroll[1] = 0.0f;
            }
            return true;
        }
        if (this.editing != null) {
            String var3;
            String string = var3 = this.editing.getValue() == null ? "" : this.editing.getValue();
            if (var3.length() < 256) {
                this.editing.setValue(var3 + var1);
                this.caretT = 0.0f;
            }
            return true;
        }
        if (this.searchFocus) {
            if (this.query.length() < 40) {
                boolean var2 = this.query.isEmpty();
                this.query = this.query + var1;
                this.page = null;
                this.gridScroll[1] = 0.0f;
                this.caretT = 0.0f;
                if (var2) {
                    this.tabT = 0.0f;
                }
            }
            return true;
        }
        return false;
    }

    private void startDrag() {
        this.dragging = true;
        this.grabX = this.mx - (double)(this.offX * this.u);
        this.grabY = this.my - (double)(this.offY * this.u);
    }

    private void clampPosition() {
        float var1 = Math.max(0.0f, ((float)this.sw / this.u - WIN_W) / 2.0f - 8.0f);
        float var2 = Math.max(0.0f, ((float)this.sh / this.u - WIN_H) / 2.0f - 8.0f);
        this.offX = UiEasing.clamp(this.offX, -var1, var1);
        this.offY = UiEasing.clamp(this.offY, -var2, var2);
    }

    private void loadPosition() {
        if (!this.posLoaded) {
            this.posLoaded = true;
            try {
                Path var1 = this.host.stateFile();
                if (var1 != null && Files.isRegularFile(var1, new LinkOption[0])) {
                    String[] var2 = Files.readString(var1, StandardCharsets.UTF_8).trim().split(",");
                    this.offX = Float.parseFloat(var2[0]);
                    this.offY = Float.parseFloat(var2[1]);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    private void savePosition() {
        try {
            Path var1 = this.host.stateFile();
            if (var1 != null) {
                Files.createDirectories(var1.getParent(), new FileAttribute[0]);
                Files.writeString(var1, (CharSequence)String.format(Locale.ROOT, "%.2f,%.2f", Float.valueOf(this.offX), Float.valueOf(this.offY)), StandardCharsets.UTF_8, new OpenOption[0]);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static interface UiDataSource {
        public String playerName();

        public String serverLine();

        public boolean inWorld();

        public String keyName(int var1);

        public void openStorage(StorageBlocksSetting var1);

        public void openHudEditor();

        public void closeNow();

        public List<?> items();

        public String itemName(Object var1);

        public Object itemOf(ItemSetting var1);

        public void setItem(ItemSetting var1, Object var2);

        public Path stateFile();

        default public float beat() {
            return 0.0f;
        }

        default public float shakeStrength() {
            return 0.0f;
        }
    }

    private static final class UiParticleState {
        float x;
        float y;
        float speed;
        float size;
        float phase;
        float fade;
        float sway;
        float base;

        private UiParticleState() {
        }
    }

    @FunctionalInterface
    private static interface ClickAction {
        public void on(int var1);
    }

    private record ClickRegion(float x, float y, float w, float h, int layer, ClickAction act) {
    }
}

