package mcp.mobius.waila.plugin.vanilla.provider;

import java.awt.Rectangle;
import java.util.Objects;

import com.mojang.blaze3d.vertex.PoseStack;
import mcp.mobius.waila.api.ICommonAccessor;
import mcp.mobius.waila.api.IEventListener;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.plugin.vanilla.config.Options;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;

public enum BreakingProgressProvider implements IEventListener {

    INSTANCE;

    @Override
    public void onAfterTooltipRender(PoseStack matrices, Rectangle rect, ICommonAccessor accessor, IPluginConfig config) {
        MultiPlayerGameMode gameMode = Objects.requireNonNull(Minecraft.getInstance().gameMode);
        if (config.getBoolean(Options.BREAKING_PROGRESS) && gameMode.isDestroying()) {
            int color = config.getInt(Options.BREAKING_PROGRESS_COLOR);
            int lineLenght;

            if (config.getBoolean(Options.BREAKING_PROGRESS_BOTTOM_ONLY)) {
                lineLenght = (int) ((rect.width - 2) * gameMode.destroyProgress);
            } else {
                lineLenght = (int) (((rect.width + rect.height - 4) * 2) * gameMode.destroyProgress);
            }

            int innerBoxWidth = rect.width - 2;
            int innerBoxHeight = rect.height - 2;

            int x = rect.x + 1;
            int y = rect.y + rect.height - 1;
            GuiComponent.fill(matrices, x, y, x + Math.min(lineLenght, innerBoxWidth), y + 1, color);
            lineLenght -= innerBoxWidth;

            if (lineLenght <= 0) {
                return;
            }

            x = rect.x + rect.width - 1;
            y = rect.y + rect.height;
            GuiComponent.fill(matrices, x, y, x + 1, y - Math.min(lineLenght, innerBoxHeight), color);
            lineLenght -= innerBoxHeight;

            if (lineLenght <= 0) {
                return;
            }

            x = rect.x + rect.width;
            y = rect.y + 1;
            GuiComponent.fill(matrices, x, y, x - Math.min(lineLenght, innerBoxWidth), y + 1, color);
            lineLenght -= innerBoxWidth;

            if (lineLenght <= 0) {
                return;
            }

            x = rect.x + 1;
            y = rect.y + 1;
            GuiComponent.fill(matrices, x, y, x + 1, y + Math.min(lineLenght, innerBoxHeight), color);
        }
    }

}