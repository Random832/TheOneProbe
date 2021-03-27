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

    // Thanks to King Lemming for the basic idea of how to do this
    public static int getTextureColor(ResourceLocation location) {
        if (cache.containsKey(location))
            return cache.getInt(location);
        AtlasTexture textureMap = Minecraft.getInstance().getModelManager().getAtlasTexture(AtlasTexture.LOCATION_BLOCKS_TEXTURE);
        TextureAtlasSprite sprite = textureMap.getSprite(location);
        if (sprite.getFrameCount() == 0) return -1;
        int total = 0, totalR = 0, totalB = 0, totalG = 0;
        for (int x = 0; x < sprite.getWidth(); x++) {
            for (int y = 0; y < sprite.getHeight(); y++) {
                int pixel = sprite.getPixelRGBA(0, x, y);
                // this is in 0xAABBGGRR format, not the usual 0xAARRGGBB.
                int pixelB = pixel >> 16 & 255;
                int pixelG = pixel >> 8 & 255;
                int pixelR = pixel & 255;
                ++total;
                totalR += pixelR;
                totalG += pixelG;
                totalB += pixelB;
            }
        }
        int color;
        if (total <= 0)
            color = 0xFFFFFFFF;
        else
            color = ColorHelper.PackedColor.packColor(255,
                    totalR / total,
                    totalG / total,
                    totalB / total);
        cache.put(location, color);
        return color;
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