package mcjty.theoneprobe.rendering;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ColorUtil {
    public static int getFluidColor(FluidStack fluid) {
        ResourceLocation location = fluid.getFluid().getAttributes().getStillTexture(fluid);
        int tint = fluid.getFluid().getAttributes().getColor(fluid);
        return ColorHelper.PackedColor.blendColors(getTextureColor(location), tint);
    }

    private static final Object2IntMap<ResourceLocation> cache = new Object2IntOpenHashMap<>();

    public static void addResourceListener(IReloadableResourceManager manager) {
        manager.addReloadListener(new ReloadListener<Void>() {
            @Override
            protected Void prepare(IResourceManager resourceManagerIn, IProfiler profilerIn) {
                return null;
            }

            @Override
            protected void apply(Void objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
                cache.clear();
            }
        });
    }

    public static int getTextureColor(ResourceLocation location) {
        return cache.computeIntIfAbsent(location, ColorUtil::getTextureColorInner);
    }

    // Thanks to King Lemming for the basic idea of how to do this
    public static int getTextureColorInner(ResourceLocation location) {
        AtlasTexture textureMap = Minecraft.getInstance().getModelManager().getAtlasTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite sprite = textureMap.getSprite(location);
        if (sprite.getFrameCount() == 0) return -1;
        float total = 0, red = 0, green = 0, blue = 0;
        for (int x = 0; x < sprite.getWidth(); x++) {
            for (int y = 0; y < sprite.getHeight(); y++) {
                int pixel = sprite.getPixelRGBA(0, x, y);
                // this is in 0xAABBGGRR format, not the usual 0xAARRGGBB.
                int alpha = pixel >> 24 & 255;
                total += alpha;
                red += (pixel & 255) * alpha;
                blue += (pixel >> 8 & 255) * alpha;
                green += (pixel >> 16 & 255) * alpha;
            }
        }
        if (total > 0)
            return ColorHelper.PackedColor.packColor(255, (int) (red / total), (int) (blue / total), (int) (green / total));
        return 0xFFFFFFFF;
    }

    public static int darker(int color) {
        // based on java.awt.Color
        return ColorHelper.PackedColor.packColor(color >> 24 & 255,
                (color >> 16 & 255) * 7 / 10,
                (color >> 8 & 255) * 7 / 10,
                (color & 255) * 7 / 10);
    }

    public static int brighter(int color) {
        // loosely based on java.awt.Color, with different treatment of too-dark colors
        int alpha = color >> 24 & 255;
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        int MINIMUM = 17;
        int v = Math.max(Math.max(r, g), b);
        if (v == 0)
            return ColorHelper.PackedColor.packColor(alpha, MINIMUM, MINIMUM, MINIMUM);
        if (v < MINIMUM)
            return ColorHelper.PackedColor.packColor(alpha,
                    r * MINIMUM / v,
                    g * MINIMUM / v,
                    b * MINIMUM / v);
        else
            return ColorHelper.PackedColor.packColor(alpha,
                    Math.min(r * 10 / 7, 255),
                    Math.min(g * 10 / 7, 255),
                    Math.min(b * 10 / 7, 255));
    }
}