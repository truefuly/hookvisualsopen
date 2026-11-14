/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.JsonPrimitive
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_1792
 *  net.minecraft.class_2960
 *  net.minecraft.class_6880$class_6883
 *  net.minecraft.class_7923
 */
package hook.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import hook.Hook;
import hook.manager.ConfigLayoutSupport;
import hook.manager.ConfigLibrary;
import hook.module.Module;
import hook.module.setting.BindSetting;
import hook.module.setting.BooleanSetting;
import hook.module.setting.ChoiceSetting;
import hook.module.setting.ItemSetting;
import hook.module.setting.MinMaxSetting;
import hook.module.setting.ModeSetting;
import hook.module.setting.NumberSetting;
import hook.module.setting.RgbSetting;
import hook.module.setting.Setting;
import hook.module.setting.StorageBlocksSetting;
import hook.module.setting.StringSetting;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1792;
import net.minecraft.class_2960;
import net.minecraft.class_6880;
import net.minecraft.class_7923;

public final class SettingsManager {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path settingsPath;
    private final JsonObject defaults;
    private final ConfigLibrary configs;

    public SettingsManager() {
        Path var1 = FabricLoader.getInstance().getConfigDir();
        this.settingsPath = var1.resolve("hook.json");
        this.defaults = this.captureModules();
        for (Map.Entry var3 : this.defaults.entrySet()) {
            ((JsonElement)var3.getValue()).getAsJsonObject().addProperty("enabled", Boolean.valueOf(false));
        }
        this.defaults.add("_layout", (JsonElement)ConfigLayoutSupport.defaults());
        this.configs = new ConfigLibrary(var1.resolve("hook-configs.json"), this::capture, this::apply, this::validate, this::resetAll);
    }

    public ConfigLibrary configs() {
        return this.configs;
    }

    private List<Module> modules() {
        return Hook.INSTANCE.getModuleManager().c();
    }

    private static boolean client(Module var0) {
        return var0 instanceof hook.module.modules.client.Hook;
    }

    public synchronized JsonObject capture() {
        JsonObject var1 = this.captureModules();
        var1.add("_layout", (JsonElement)ConfigLayoutSupport.capture());
        return var1;
    }

    private JsonObject captureModules() {
        JsonObject var1 = new JsonObject();
        for (Module var3 : this.modules()) {
            JsonObject var4 = new JsonObject();
            var4.addProperty("enabled", Boolean.valueOf(!SettingsManager.client(var3) && var3.isEnabled()));
            var4.addProperty("$keybind", (Number)var3.getKeybind());
            for (Setting var6 : var3.getSettings()) {
                JsonElement var7 = SettingsManager.encode(var6);
                if (var7 == null) continue;
                var4.add(var6.getName().toString(), var7);
            }
            var1.add(var3.getName().toString(), (JsonElement)var4);
        }
        return var1;
    }

    private static JsonElement encode(Setting var0) {
        if (var0 instanceof BooleanSetting) {
            BooleanSetting var14 = (BooleanSetting)var0;
            return new JsonPrimitive(Boolean.valueOf(var14.getValue()));
        }
        if (var0 instanceof ModeSetting) {
            ModeSetting var13 = (ModeSetting)var0;
            return new JsonPrimitive((Number)var13.getModeIndex());
        }
        if (var0 instanceof NumberSetting) {
            NumberSetting var12 = (NumberSetting)var0;
            return new JsonPrimitive((Number)var12.getValue());
        }
        if (var0 instanceof BindSetting) {
            BindSetting var11 = (BindSetting)var0;
            return new JsonPrimitive((Number)var11.getValue());
        }
        if (var0 instanceof StringSetting) {
            StringSetting var10 = (StringSetting)var0;
            return new JsonPrimitive(var10.getValue() == null ? "" : var10.getValue());
        }
        if (var0 instanceof MinMaxSetting) {
            MinMaxSetting var9 = (MinMaxSetting)var0;
            JsonObject var15 = new JsonObject();
            var15.addProperty("min", (Number)var9.getCurrentMin());
            var15.addProperty("max", (Number)var9.getCurrentMax());
            return var15;
        }
        if (var0 instanceof ItemSetting) {
            ItemSetting var8 = (ItemSetting)var0;
            return new JsonPrimitive(class_7923.field_41178.method_10221(var8.getItem()).toString());
        }
        if (var0 instanceof ChoiceSetting) {
            ChoiceSetting var7 = (ChoiceSetting)var0;
            return new JsonPrimitive(var7.getMode());
        }
        if (var0 instanceof RgbSetting) {
            RgbSetting var6 = (RgbSetting)var0;
            return new JsonPrimitive(String.format(Locale.ROOT, "%08X", var6.getArgb()));
        }
        if (!(var0 instanceof StorageBlocksSetting)) {
            return null;
        }
        StorageBlocksSetting var1 = (StorageBlocksSetting)var0;
        JsonObject var2 = new JsonObject();
        for (StorageBlocksSetting.Entry var4 : var1.entries()) {
            JsonObject var5 = new JsonObject();
            var5.addProperty("enabled", Boolean.valueOf(var4.enabled()));
            var5.addProperty("color", (Number)(var4.color().getRGB() & 0xFFFFFF));
            var2.add(var4.id(), (JsonElement)var5);
        }
        return var2;
    }

    public synchronized void validate(JsonObject var1) {
        if (var1 != null && var1.size() <= 512) {
            for (Module var3 : this.modules()) {
                JsonElement var4 = var1.get(var3.getName().toString());
                if (var4 == null) continue;
                if (!var4.isJsonObject()) {
                    throw new IllegalArgumentException("Invalid module settings.");
                }
                JsonObject var5 = var4.getAsJsonObject();
                if (var5.has("enabled")) {
                    SettingsManager.bool(var5.get("enabled"));
                }
                if (var5.has("$keybind")) {
                    SettingsManager.integer(var5.get("$keybind"), -1, 512);
                }
                for (Setting var7 : var3.getSettings()) {
                    JsonElement var8 = var5.get(var7.getName().toString());
                    if (var8 == null) continue;
                    try {
                        SettingsManager.validateSetting(var7, var8);
                    }
                    catch (RuntimeException var10) {
                        throw new IllegalArgumentException("Invalid value: " + String.valueOf(var3.getName()) + " / " + String.valueOf(var7.getName()));
                    }
                }
            }
            if (var1.has("_layout")) {
                ConfigLayoutSupport.validate(var1.getAsJsonObject("_layout"));
            }
        } else {
            throw new IllegalArgumentException("Invalid config.");
        }
    }

    private static void validateSetting(Setting var0, JsonElement var1) {
        if (var0 instanceof BooleanSetting) {
            SettingsManager.bool(var1);
        } else if (var0 instanceof ModeSetting) {
            ModeSetting var2 = (ModeSetting)var0;
            SettingsManager.integer(var1, -1, var2.getPossibleValues().size() - 1);
        } else if (var0 instanceof NumberSetting) {
            NumberSetting var3 = (NumberSetting)var0;
            SettingsManager.number(var1, var3.getMin(), var3.getMax());
        } else if (var0 instanceof BindSetting) {
            SettingsManager.integer(var1, -1, 512);
        } else if (var0 instanceof StringSetting) {
            if (SettingsManager.string(var1).length() > 4096) {
                throw new IllegalArgumentException();
            }
        } else if (var0 instanceof MinMaxSetting) {
            double var10;
            MinMaxSetting var4 = (MinMaxSetting)var0;
            JsonObject var7 = var1.getAsJsonObject();
            double var8 = SettingsManager.number(var7.get("min"), var4.getMinValue(), var4.getMaxValue());
            if (var8 > (var10 = SettingsManager.number(var7.get("max"), var4.getMinValue(), var4.getMaxValue()))) {
                throw new IllegalArgumentException();
            }
        } else if (var0 instanceof ItemSetting) {
            class_2960 var12 = class_2960.method_12829((String)SettingsManager.string(var1));
            if (var12 == null || class_7923.field_41178.method_10223(var12).isEmpty()) {
                throw new IllegalArgumentException();
            }
        } else if (var0 instanceof ChoiceSetting) {
            ChoiceSetting var5 = (ChoiceSetting)var0;
            if (!var5.getModes().contains(SettingsManager.string(var1))) {
                throw new IllegalArgumentException();
            }
        } else if (var0 instanceof RgbSetting) {
            if (!SettingsManager.string(var1).matches("[0-9a-fA-F]{6,8}")) {
                throw new IllegalArgumentException();
            }
        } else if (var0 instanceof StorageBlocksSetting) {
            StorageBlocksSetting var6 = (StorageBlocksSetting)var0;
            JsonObject var13 = var1.getAsJsonObject();
            for (StorageBlocksSetting.Entry var9 : var6.entries()) {
                if (!var13.has(var9.id())) continue;
                JsonObject var15 = var13.getAsJsonObject(var9.id());
                if (var15.has("enabled")) {
                    SettingsManager.bool(var15.get("enabled"));
                }
                if (!var15.has("color")) continue;
                SettingsManager.integer(var15.get("color"), 0, 0xFFFFFF);
            }
        }
    }

    private static String string(JsonElement var0) {
        if (var0 != null && var0.isJsonPrimitive() && var0.getAsJsonPrimitive().isString()) {
            return var0.getAsString();
        }
        throw new IllegalArgumentException();
    }

    private static boolean bool(JsonElement var0) {
        if (var0 != null && var0.isJsonPrimitive() && var0.getAsJsonPrimitive().isBoolean()) {
            return var0.getAsBoolean();
        }
        throw new IllegalArgumentException();
    }

    private static double number(JsonElement var0, double var1, double var3) {
        if (var0 != null && var0.isJsonPrimitive() && var0.getAsJsonPrimitive().isNumber()) {
            double var5 = var0.getAsDouble();
            if (Double.isFinite(var5) && !(var5 < var1) && !(var5 > var3)) {
                return var5;
            }
            throw new IllegalArgumentException();
        }
        throw new IllegalArgumentException();
    }

    private static int integer(JsonElement var0, int var1, int var2) {
        double var3 = SettingsManager.number(var0, var1, var2);
        if (var3 != Math.rint(var3)) {
            throw new IllegalArgumentException();
        }
        return (int)var3;
    }

    private static void set(Setting var0, JsonElement var1, Module var2) {
        if (var0 instanceof BooleanSetting) {
            BooleanSetting var3 = (BooleanSetting)var0;
            var3.setValue(var1.getAsBoolean());
        } else if (var0 instanceof ModeSetting) {
            ModeSetting var4 = (ModeSetting)var0;
            int var13 = var1.getAsInt();
            var4.setModeIndex(var13 == -1 ? var4.getOriginalValue() : var13);
        } else if (var0 instanceof NumberSetting) {
            NumberSetting var5 = (NumberSetting)var0;
            var5.getValue(var1.getAsDouble());
        } else if (var0 instanceof BindSetting) {
            BindSetting var6 = (BindSetting)var0;
            var6.setValue(var1.getAsInt());
            var6.setListening(false);
            if (var6.isModuleKey()) {
                var2.setKeybind(var6.getValue());
            }
        } else if (var0 instanceof StringSetting) {
            StringSetting var7 = (StringSetting)var0;
            var7.setValue(var1.getAsString());
        } else if (var0 instanceof MinMaxSetting) {
            MinMaxSetting var8 = (MinMaxSetting)var0;
            JsonObject var17 = var1.getAsJsonObject();
            var8.setCurrentMin(var17.get("min").getAsDouble());
            var8.setCurrentMax(var17.get("max").getAsDouble());
        } else if (var0 instanceof ItemSetting) {
            ItemSetting var9 = (ItemSetting)var0;
            var9.setItem((class_1792)((class_6880.class_6883)class_7923.field_41178.method_10223(class_2960.method_60654((String)var1.getAsString())).orElseThrow()).comp_349());
        } else if (var0 instanceof ChoiceSetting) {
            ChoiceSetting var10 = (ChoiceSetting)var0;
            var10.setMode(var1.getAsString());
        } else if (var0 instanceof RgbSetting) {
            RgbSetting var11 = (RgbSetting)var0;
            var11.setArgb((int)Long.parseLong(var1.getAsString(), 16));
        } else if (var0 instanceof StorageBlocksSetting) {
            StorageBlocksSetting var12 = (StorageBlocksSetting)var0;
            for (StorageBlocksSetting.Entry var14 : var12.entries()) {
                JsonElement var15 = var1.getAsJsonObject().get(var14.id());
                if (var15 == null) continue;
                JsonObject var16 = var15.getAsJsonObject();
                if (var16.has("enabled")) {
                    var14.setEnabled(var16.get("enabled").getAsBoolean());
                }
                if (!var16.has("color")) continue;
                var14.setColor(new Color(var16.get("color").getAsInt()));
            }
        }
    }

    public synchronized void apply(JsonObject var1) {
        this.validate(var1);
        JsonObject var2 = this.capture();
        try {
            this.applyInternal(var1);
            this.writeCurrent();
        }
        catch (RuntimeException var6) {
            try {
                this.applyInternal(var2);
            }
            catch (RuntimeException var5) {
                var6.addSuppressed(var5);
                throw new IllegalStateException("Couldn't apply the config. Some module states could not be restored.", var6);
            }
            throw new IllegalStateException("Couldn't apply this config; previous settings were restored.", var6);
        }
    }

    private void applyInternal(JsonObject var1) {
        for (Module var3 : this.modules()) {
            if (SettingsManager.client(var3)) {
                var3.setEnabled(false);
                continue;
            }
            var3.toggle(false);
        }
        for (Module var12 : this.modules()) {
            String var4 = var12.getName().toString();
            JsonObject var5 = this.defaults.getAsJsonObject(var4).deepCopy();
            JsonElement var6 = var1.get(var4);
            if (var6 != null) {
                for (Map.Entry var8 : var6.getAsJsonObject().entrySet()) {
                    var5.add((String)var8.getKey(), (JsonElement)var8.getValue());
                }
            }
            var12.setKeybind(var5.get("$keybind").getAsInt());
            for (Setting var17 : var12.getSettings()) {
                JsonElement var9 = var5.get(var17.getName().toString());
                if (var9 == null) continue;
                SettingsManager.set(var17, var9, var12);
            }
        }
        if (var1.has("_layout")) {
            ConfigLayoutSupport.apply(var1.getAsJsonObject("_layout"));
        }
        for (Module var13 : this.modules()) {
            if (SettingsManager.client(var13)) continue;
            JsonElement var14 = var1.get(var13.getName().toString());
            boolean var15 = var14 == null
                ? this.defaults.getAsJsonObject(var13.getName().toString()).get("enabled").getAsBoolean()
                : var14.isJsonObject() && var14.getAsJsonObject().has("enabled") && var14.getAsJsonObject().get("enabled").getAsBoolean();
            var13.toggle(var15);
        }
    }

    public synchronized void resetAll() {
        this.apply(this.defaults.deepCopy());
    }

    public synchronized void loadProfile() {
        try {
            if (Files.isRegularFile(this.settingsPath, new LinkOption[0])) {
                if (Files.size(this.settingsPath) > 0x100000L) {
                    throw new IllegalArgumentException("Settings file is too large");
                }
                JsonObject var1 = JsonParser.parseString((String)Files.readString(this.settingsPath)).getAsJsonObject();
                this.validate(var1);
                this.applyInternal(var1);
                // drop saved data of modules that no longer exist, right away instead of on the next change
                java.util.Set<String> known = new java.util.HashSet<String>();
                for (Module m : this.modules()) known.add(m.getName().toString());
                boolean stale = false;
                for (String key : var1.keySet()) {
                    if (!key.startsWith("_") && !known.contains(key)) { stale = true; break; }
                }
                if (stale) this.writeCurrent();
            }
        }
        catch (Exception var2) {
            System.err.println("Hook: couldn't load settings: " + var2.getMessage());
        }
    }

    private void writeCurrent() {
        try {
            ConfigLibrary.writeAtomic(this.settingsPath, JSON.toJson((JsonElement)this.capture()));
        }
        catch (Exception var2) {
            throw new IllegalStateException("Couldn't save client settings.", var2);
        }
    }

    public synchronized void saveNow() {
        this.writeCurrent();
    }

    private int lastSavedHash;

    /** Autosave: writes the settings only when something changed since the last check. */
    public synchronized void saveIfChanged() {
        try {
            String json = JSON.toJson((JsonElement)this.capture());
            int hash = json.hashCode();
            if (hash == this.lastSavedHash) return;
            boolean first = this.lastSavedHash == 0;
            this.lastSavedHash = hash;
            if (first) return; // first call only remembers the loaded state
            ConfigLibrary.writeAtomic(this.settingsPath, json);
        }
        catch (Exception var2) {
            System.err.println("Hook: autosave failed: " + var2.getMessage());
        }
    }

    public synchronized void shutdown() {
        try {
            this.writeCurrent();
        }
        catch (Exception var2) {
            System.err.println("Hook: couldn't save settings: " + var2.getMessage());
        }
    }
}

