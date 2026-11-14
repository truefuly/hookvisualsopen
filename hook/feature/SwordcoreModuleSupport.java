package hook.feature;

import hook.module.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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
    private static class_304 smartSwordKey;
    private static boolean smartSwordKeyWasDown;
    private static boolean smartSwordSwitchActive;
    private static String previousSmartSwordActivation = "Hold";

    private SwordcoreModuleSupport() {}

    public static void initialize() {
        SwordcoreClient.swordConfig = SwordcoreConfig.load();
        SwordcoreClient.swordConfig.enabled = true;
        SwordcoreClient.swordConfig.chatLogs = false;
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
        class_304 key = KeyBindingHelper.registerKeyBinding(new class_304(
                "key.hookvisuals.smart_sword", class_3675.class_307.field_1668, 82, class_304.class_11900.field_62556));
        smartSwordKey = key;
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
            updateSmartSwordKey(manager);
        }
        SwordcoreClient.processAttackInput(client);
    }

    private static void updateSmartSwordKey(ModuleManager manager) {
        if (smartSwordKey == null) return;
        SmartSwordModule module = (SmartSwordModule)manager.getModuleByClass(SmartSwordModule.class);
        boolean enabled = module != null && module.isEnabled();
        String activation = module == null ? "Hold" : module.activation.getMode();
        if (!enabled) {
            smartSwordSwitchActive = false;
            smartSwordKeyWasDown = false;
            smartSwordKey.method_23481(false);
        } else if (!activation.equals(previousSmartSwordActivation)) {
            smartSwordSwitchActive = false;
            smartSwordKeyWasDown = false;
        }
        previousSmartSwordActivation = activation;
        if (enabled && "Switch".equals(activation)) {
            boolean down = smartSwordKey.method_1434();
            if (down && !smartSwordKeyWasDown) smartSwordSwitchActive = !smartSwordSwitchActive;
            smartSwordKeyWasDown = down;
            smartSwordKey.method_23481(smartSwordSwitchActive);
        } else {
            smartSwordKeyWasDown = smartSwordKey.method_1434();
        }
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

        SmartSwordModule smartSword = (SmartSwordModule)manager.getModuleByClass(SmartSwordModule.class);
        config.smartSword = smartSword != null && smartSword.isEnabled();
        if (smartSword != null) config.targetRange = smartSword.targetRange.getFloatValue();

        ShieldDisableModule shield = (ShieldDisableModule)manager.getModuleByClass(ShieldDisableModule.class);
        config.shieldDisable = shield != null && shield.isEnabled();
        if (shield != null) {
            config.shieldDisableMode = switch (shield.mode.getMode()) {
                case "Stun" -> SwordcoreConfig.ShieldDisableMode.STUN;
                case "Stun Sneak" -> SwordcoreConfig.ShieldDisableMode.STUN_SNEAK;
                default -> SwordcoreConfig.ShieldDisableMode.NORMAL;
            };
        }

        MovementResistModule resist = (MovementResistModule)manager.getModuleByClass(MovementResistModule.class);
        config.movementResisting = resist != null && resist.isEnabled();
        if (resist != null) {
            config.movementResistingRange = resist.range.getFloatValue();
            config.movementResistEscape = resist.escape.getValue();
        }

        STappingModule sTap = (STappingModule)manager.getModuleByClass(STappingModule.class);
        config.sTapping = sTap != null && sTap.isEnabled();
        if (sTap != null) config.sTapLength = sTap.duration.getIntValue();

        JumpFollowingModule jump = (JumpFollowingModule)manager.getModuleByClass(JumpFollowingModule.class);
        config.jumpFollowingMode = jump == null || !jump.isEnabled()
                ? SwordcoreConfig.JumpFollowingMode.OFF
                : switch (jump.mode.getMode()) {
                    case "Neutral Based" -> SwordcoreConfig.JumpFollowingMode.NEUTRAL_BASED;
                    case "Off" -> SwordcoreConfig.JumpFollowingMode.OFF;
                    default -> SwordcoreConfig.JumpFollowingMode.ON;
                };
        if (jump != null) {
            config.jumpFollowingRespectsResist = jump.respectResist.getValue();
            config.preventBackDuringAggressiveJump = jump.preventBackDuringJump.getValue();
        }

        SprintHitFollowModule sprintFollow = (SprintHitFollowModule)manager.getModuleByClass(SprintHitFollowModule.class);
        config.sprintHitFollowThrough = sprintFollow != null && sprintFollow.isEnabled();
        if (sprintFollow != null) config.sprintHitJumpDelay = sprintFollow.jumpDelay.getIntValue();

        TrueJumpResetModule jumpReset = (TrueJumpResetModule)manager.getModuleByClass(TrueJumpResetModule.class);
        config.jumpResetting = jumpReset != null && jumpReset.isEnabled();
    }
}
