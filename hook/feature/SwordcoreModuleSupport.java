package hook.feature;

import hook.module.ModuleManager;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.class_304;
import net.minecraft.class_3675;
import net.minecraft.class_310;
import thetruetrident.swordcore.SwordcoreClient;
import thetruetrident.swordcore.SwordcoreConfig;
import thetruetrident.swordcore.SwordcoreParticles;
import thetruetrident.swordcore.MovementResistParticle;
import thetruetrident.swordcore.MovementSyncInnerParticle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Connects the Swordcore client controller to Hook's individual combat modules. */
public final class SwordcoreModuleSupport {
    private static Method releaseForcedKeys;
    private static Method resetTransientState;

    private SwordcoreModuleSupport() {}

    public static void initialize() {
        SwordcoreClient.swordConfig = SwordcoreConfig.load();
        SwordcoreClient.swordConfig.enabled = true;
        SwordcoreClient.swordConfig.chatLogs = false;
        // Without "premium" Swordcore forces its sounds (the bell on every target swing) back on each tick
        SwordcoreClient.premiumFeaturesEnabled = true;
        bindSmartSwordKey();
        ParticleFactoryRegistry.getInstance().register(SwordcoreParticles.MOVEMENT_RESIST, MovementResistParticle.Factory::new);
        ParticleFactoryRegistry.getInstance().register(SwordcoreParticles.MOVEMENT_SYNC_INNER, MovementSyncInnerParticle.Factory::new);
        try {
            releaseForcedKeys = SwordcoreClient.class.getDeclaredMethod("releaseForcedKeys", class_310.class);
            resetTransientState = SwordcoreClient.class.getDeclaredMethod("resetTransientState");
            releaseForcedKeys.setAccessible(true);
            resetTransientState.setAccessible(true);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not initialize Swordcore input cleanup", error);
        }
        disableFeaturesUntilHookSync();
    }

    private static void bindSmartSwordKey() {
        // Smart Sword was removed; Swordcore still needs a key object, so give it one that is never shown or bound
        class_304 key = new class_304("key.hookvisuals.smart_sword", class_3675.class_307.field_1668, -1, class_304.class_11900.field_62556);
        try {
            Field field = SwordcoreClient.class.getDeclaredField("smartSwordKey");
            field.setAccessible(true);
            field.set(null, key);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not initialize Smart Sword key", error);
        }
    }

    public static void onClientTick(class_310 client) {
        if (client.field_1687 == null || client.field_1724 == null) {
            invoke(resetTransientState);
            return;
        }
        if (client.field_1755 != null || client.method_1493()) {
            invoke(releaseForcedKeys, client);
            return;
        }
        if (hook.Hook.INSTANCE != null && hook.Hook.INSTANCE.getModuleManager() != null) {
            ModuleManager manager = hook.Hook.INSTANCE.getModuleManager();
            sync(manager);
        }
        SwordcoreClient.processAttackInput(client);
    }

    private static void invoke(Method method, Object... args) {
        if (method == null) return;
        try {
            method.invoke(null, args);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Could not clean up Swordcore movement input", error);
        }
    }

    private static void disableFeaturesUntilHookSync() {
        SwordcoreConfig config = SwordcoreClient.swordConfig;
        if (config == null) return;
        config.enabled = true;
        config.chatLogs = false;
        config.freeHitting = true; // never block swings at the air
        config.soundEffects = false; // no bell/beep sounds in fights
        config.movementSyncing = false;
        config.smartSword = false;
        config.shieldDisable = false;
        config.movementResisting = false;
        config.sTapping = false;
        config.jumpFollowingMode = SwordcoreConfig.JumpFollowingMode.OFF;
        config.sprintHitFollowThrough = false;
        config.jumpResetting = false;
        config.autoWTapAutosprint = false;
    }

    public static void sync(ModuleManager manager) {
        SwordcoreConfig config = SwordcoreClient.swordConfig;
        if (config == null || manager == null) return;
        config.enabled = true;
        config.chatLogs = false;
        // Swordcore's "free hitting" off = swings at the air get cancelled; keep vanilla behaviour
        config.freeHitting = true;
        // Swordcore rings a bell every time the target swings; never play its feedback sounds
        config.soundEffects = false;
        SwordcoreClient.premiumFeaturesEnabled = true; // otherwise Swordcore turns its sounds back on every tick
        config.autoWTapAutosprint = false;

        MovementSyncModule sync = (MovementSyncModule)manager.getModuleByClass(MovementSyncModule.class);
        config.movementSyncing = sync != null && sync.isEnabled();
        if (sync != null) {
            config.movementSyncInnerRange = sync.innerRange.getFloatValue();
            config.airGate = sync.airGate.getValue();
            config.itemGate = sync.itemGate.getValue();
            config.neutralMode = switch (sync.neutralMode.getMode()) {
                case "Passive" -> SwordcoreConfig.NeutralMode.PASSIVE;
                case "Aggressive" -> SwordcoreConfig.NeutralMode.AGGRESSIVE;
                case "Adaptable Inverse" -> SwordcoreConfig.NeutralMode.ADAPTABLE_INVERSE;
                default -> SwordcoreConfig.NeutralMode.ADAPTABLE;
            };
            config.movementEqualizer = switch (sync.equalizer.getMode()) {
                case "Off" -> SwordcoreConfig.MovementEqualizerMode.OFF;
                case "Jump" -> SwordcoreConfig.MovementEqualizerMode.JUMP;
                case "Equalizer Jump" -> SwordcoreConfig.MovementEqualizerMode.EQUALIZER_JUMP;
                case "Equalizer" -> SwordcoreConfig.MovementEqualizerMode.EQUALIZER;
                default -> SwordcoreConfig.MovementEqualizerMode.ADAPTABLE_EXPERIMENTAL;
            };
            config.movementEqualizerRange = sync.equalizerRange.getFloatValue();
        }

        // the Smart Sword, Shield Disable, Movement Resist, S-Tapping, Jump Following and Sprint Hit Follow modules were removed
        config.smartSword = false;
        config.shieldDisable = false;
        config.movementResisting = false;
        config.sTapping = false;
        config.jumpFollowingMode = SwordcoreConfig.JumpFollowingMode.OFF;
        config.sprintHitFollowThrough = false;

        TrueJumpResetModule jumpReset = (TrueJumpResetModule)manager.getModuleByClass(TrueJumpResetModule.class);
        config.jumpResetting = jumpReset != null && jumpReset.isEnabled();
    }
}
