/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 *  net.minecraft.class_4184
 *  org.joml.FrustumIntersection
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package hook.utils;

import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class RenderVisibility {
    private static final FrustumIntersection FRUSTUM = new FrustumIntersection();
    private static final Matrix4f CLIP = new Matrix4f();
    private static double originX;
    private static double originY;
    private static double originZ;
    private static boolean ready;
    private static float projectionScaleY;

    private RenderVisibility() {
    }

    public static void begin(Matrix4f var0, Matrix4f var1, double var2, double var4, double var6) {
        RenderVisibility.set((Matrix4fc)var0, (Matrix4fc)var1, var2, var4, var6);
    }

    public static void beginWorldFrame(Matrix4fc var0, Matrix4fc var1) {
        class_4184 var2 = class_310.method_1551().field_1773.method_19418();
        if (var2 != null && var2.method_19332()) {
            class_243 var3 = var2.method_71156();
            RenderVisibility.set(var1, var0, var3.field_1352, var3.field_1351, var3.field_1350);
        } else {
            RenderVisibility.disable();
        }
    }

    private static void set(Matrix4fc var0, Matrix4fc var1, double var2, double var4, double var6) {
        CLIP.set(var0).mul(var1);
        boolean bl = ready = CLIP.isFinite() && Double.isFinite(var2) && Double.isFinite(var4) && Double.isFinite(var6);
        if (ready) {
            FRUSTUM.set((Matrix4fc)CLIP);
            projectionScaleY = Math.abs(var0.m11());
            originX = var2;
            originY = var4;
            originZ = var6;
        }
    }

    public static void disable() {
        ready = false;
    }

    /**
     * Projects a world position to window pixels with this frame's camera.
     * @return false when the matrices aren't ready or the point is behind the camera
     */
    public static boolean project(double x, double y, double z, int windowWidth, int windowHeight, float[] out) {
        if (!ready) {
            return false;
        }
        org.joml.Vector4f clip = new org.joml.Vector4f((float)(x - originX), (float)(y - originY), (float)(z - originZ), 1.0f);
        CLIP.transform(clip);
        if (clip.w <= 0.05f) {
            return false;
        }
        out[0] = (clip.x / clip.w + 1.0f) * 0.5f * windowWidth;
        out[1] = (1.0f - clip.y / clip.w) * 0.5f * windowHeight;
        return true;
    }

    /** 1 / tan(vertical fov / 2) of this frame (changes with zoom), 0 when unknown. */
    public static float projectionScaleY() {
        return ready ? projectionScaleY : 0.0f;
    }

    public static double cameraDistance(double x, double y, double z) {
        double dx = x - originX, dy = y - originY, dz = z - originZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static boolean box(double var0, double var2, double var4, double var6, double var8, double var10) {
        return !ready ? true : FRUSTUM.testAab((float)(Math.min(var0, var6) - originX - 0.5), (float)(Math.min(var2, var8) - originY - 0.5), (float)(Math.min(var4, var10) - originZ - 0.5), (float)(Math.max(var0, var6) - originX + 0.5), (float)(Math.max(var2, var8) - originY + 0.5), (float)(Math.max(var4, var10) - originZ + 0.5));
    }
}

