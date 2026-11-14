package hook.mixin;

import hook.feature.NoFogModule;
import net.minecraft.class_4184;
import net.minecraft.class_638;
import net.minecraft.class_758;
import net.minecraft.class_9779;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Lets No Fog rewrite the fog distances and colour right before they are uploaded. */
@Mixin(value = {class_758.class}, priority = 1100)
public abstract class NoFogMixin {
    @ModifyArgs(method = {"method_3211(Lnet/minecraft/class_4184;ILnet/minecraft/class_9779;FLnet/minecraft/class_638;)Lorg/joml/Vector4f;"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_758;method_71110(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V", remap = false), remap = false)
    private void hook$noFog(Args args, class_4184 camera, int viewDistance, class_9779 tick, float skyDarkness, class_638 world) {
        float[] fog = new float[6];
        for (int i = 0; i < 6; i++) fog[i] = (Float) args.get(3 + i);
        Vector4f color = new Vector4f((Vector4fc) args.get(2));
        NoFogModule.apply(camera, fog, color);
        args.set(2, color);
        for (int i = 0; i < 6; i++) args.set(3 + i, fog[i]);
    }
}
