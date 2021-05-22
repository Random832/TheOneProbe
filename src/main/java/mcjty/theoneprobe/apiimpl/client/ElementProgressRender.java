package mcjty.theoneprobe.apiimpl.client;

import java.util.function.Function;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import mcjty.theoneprobe.api.IProgressStyle;
import mcjty.theoneprobe.api.TankReference;
import mcjty.theoneprobe.apiimpl.elements.ElementProgress;
import mcjty.theoneprobe.rendering.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;

public class ElementProgressRender {

    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");

    public static void render(IProgressStyle style, long current, long max, MatrixStack matrixStack, int x, int y, int w, int h) {
        if (style.isLifeBar()) {
            renderLifeBar(current, matrixStack, x, y, w, h);
        } else if (style.isArmorBar()) {
            renderArmorBar(current, matrixStack, x, y, w, h);
        } else {
            RenderHelper.drawThickBeveledBox(matrixStack, x, y, x + w, y + h, 1, style.getBorderColor(), style.getBorderColor(), style.getBackgroundColor());
            if (current > 0 && max > 0) {
                // Determine the progress bar width, but limit it to the size of the element (minus 2).
                int dx = (int) Math.min((current * (w - 2) / max), w - 2);

                if (style.getFilledColor() == style.getAlternatefilledColor()) {
                    if (dx > 0) {
                        RenderHelper.drawThickBeveledBox(matrixStack, x + 1, y + 1, x + dx + 1, y + h - 1, 1, style.getFilledColor(), style.getFilledColor(), style.getFilledColor());
                    }
                } else {
                    for (int xx = x + 1; xx < x + dx + 1; xx++) {
                        int color = (xx & 1) == 0 ? style.getFilledColor() : style.getAlternatefilledColor();
                        RenderHelper.drawVerticalLine(matrixStack, xx, y + 1, y + h - 1, color);
                    }
                }
            }
        }
        renderText(matrixStack, x, y, w, current, style);
    }

    private static void renderText(MatrixStack matrixStack, int x, int y, int w, long current, IProgressStyle style) {
        if (style.isShowText()) {
            Minecraft mc = Minecraft.getInstance();
            FontRenderer render = mc.fontRenderer;
            ITextComponent s = style.getPrefixComp().deepCopy().appendSibling(ElementProgress.format(current, style.getNumberFormat(), style.getSuffixComp()));
            int textWidth = render.func_243245_a(s.func_241878_f());
            int height = render.FONT_HEIGHT;
            int bg = style.getBackgroundColor() & 0xFFFFFF | 0x55000000;
            int textX;
            switch (style.getAlignment()) {
                case ALIGN_BOTTOMRIGHT:
                    textX = (x + w - 3) - textWidth;
                    break;
                case ALIGN_CENTER:
                    textX = (x + (w / 2)) - (textWidth / 2);
                    break;
                case ALIGN_TOPLEFT:
                default:
                    textX = x + 3;
                    break;
            }
            int textY = y + 2;
            RenderHelper.renderText(mc, matrixStack, textX, textY, s);
            RenderHelper.drawThickBeveledBox(matrixStack, textX - 1, textY - 1, textX + textWidth, textY + height, 1, bg, bg, bg);
        }
    }

    private static void renderLifeBar(long current, MatrixStack matrixStack, int x, int y, int w, int h) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bindTexture(ICONS);
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        if (current * 4 >= w) {
            // Shortened view
            RenderHelper.drawTexturedModalRect(matrix, x, y, 52, 0, 9, 9);
            RenderHelper.renderText(Minecraft.getInstance(), matrixStack, x + 12, y, TextFormatting.WHITE + String.valueOf((current / 2)));
        } else {
            for (int i = 0; i < current / 2; i++) {
                RenderHelper.drawTexturedModalRect(matrix, x, y, 52, 0, 9, 9);
                x += 8;
            }
            if (current % 2 != 0) {
                RenderHelper.drawTexturedModalRect(matrix, x, y, 61, 0, 9, 9);
            }
        }
    }

    private static void renderArmorBar(long current, MatrixStack matrixStack, int x, int y, int w, int h) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getInstance().getTextureManager().bindTexture(ICONS);
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        if (current * 4 >= w) {
            // Shortened view
            RenderHelper.drawTexturedModalRect(matrix, x, y, 43, 9, 9, 9);
            RenderHelper.renderText(Minecraft.getInstance(), matrixStack, x + 12, y, TextFormatting.WHITE + String.valueOf((current / 2)));
        } else {
            for (int i = 0; i < current / 2; i++) {
                RenderHelper.drawTexturedModalRect(matrix, x, y, 43, 9, 9, 9);
                x += 8;
            }
            if (current % 2 != 0) {
                RenderHelper.drawTexturedModalRect(matrix, x, y, 25, 9, 9, 9);
            }
        }
    }

    public static void renderTank(MatrixStack matrixStack, int x, int y, int width, int height, IProgressStyle style, TankReference tank) {
        RenderHelper.drawThickBeveledBox(matrixStack, x, y, x + width, y + height, 1, style.getBorderColor(), style.getBorderColor(), style.getBackgroundColor());
        if (tank.getStored() <= 0) {
            if (style.isShowText()) {
                renderText(matrixStack, x, y, width, 0, style);
            }
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bindTexture(PlayerContainer.LOCATION_BLOCKS_TEXTURE);
        Function<ResourceLocation, TextureAtlasSprite> map = mc.getAtlasSpriteGetter(PlayerContainer.LOCATION_BLOCKS_TEXTURE);
        width -= 2;
        FluidStack[] fluids = tank.getFluids();
        int start = 1;
        int tanks = fluids.length;
        int max = tank.getCapacity();
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        for (int i = 0; i < tanks; i++) {
            FluidStack stack = fluids[i];
            int lvl = (int) (stack == null ? 0 : (((double) stack.getAmount() / max) * width));
            if (lvl <= 0) continue;
            FluidAttributes attr = stack.getFluid().getAttributes();
            TextureAtlasSprite liquidIcon = map.apply(attr.getStillTexture(stack));
            if (liquidIcon == map.apply(MissingTextureSprite.getLocation())) continue;
            int color = attr.getColor(stack);
            RenderSystem.color4f(((color >> 16) & 255) / 255F, ((color >> 8) & 255) / 255F, (color & 255) / 255F, ((color >> 24) & 255) / 255F);
            while (lvl > 0) {
                int maxX = Math.min(16, lvl);
                lvl -= maxX;
                RenderHelper.drawTexturedModalRect(matrix, x + start, y + 1, liquidIcon, maxX, height - 2);
                start += maxX;
            }
        }
        RenderSystem.color4f(1F, 1F, 1F, 1F);
        if(style.isShowText())
        {
            renderText(matrixStack, x, y, width + 2, tank.getStored(), style);
        }
    }
}
