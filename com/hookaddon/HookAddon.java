package com.hookaddon;

import com.mojang.logging.LogUtils;
import hook.Hook;
import org.slf4j.Logger;

/** Loads the saved Hook profile once the client has started. */
public class HookAddon {
    public static final Logger LOG = LogUtils.getLogger();

    public void onInitialize() {
        removeOldAddonFolder();
        Hook.INSTANCE.getSettingsManager().loadProfile();
        LOG.info("Hook Addon ready");
    }

    /** The removed automation modules kept their data in config/hook-addon; delete it if nothing but empty folders is left. */
    private static void removeOldAddonFolder() {
        java.nio.file.Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("hook-addon");
        if (!java.nio.file.Files.isDirectory(dir)) return;
        try (java.util.stream.Stream<java.nio.file.Path> walk = java.nio.file.Files.walk(dir)) {
            java.util.List<java.nio.file.Path> all = walk.sorted(java.util.Comparator.reverseOrder()).toList();
            if (all.stream().anyMatch(java.nio.file.Files::isRegularFile)) return;
            for (java.nio.file.Path p : all) java.nio.file.Files.deleteIfExists(p);
        } catch (Exception e) {
            LOG.warn("Could not remove the old hook-addon folder: {}", e.getMessage());
        }
    }
}
