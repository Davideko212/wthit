package mcp.mobius.waila.plugin.core;

import mcp.mobius.waila.api.IDrawableText;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IModInfo;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.IWailaConfig;
import mcp.mobius.waila.api.WailaConstants;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public enum EntityComponent implements IEntityComponentProvider {

    INSTANCE;

    @Override
    public void appendHead(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
        Entity entity = accessor.getEntity();
        IWailaConfig.Formatting formatting = IWailaConfig.getInstance().getFormatting();
        tooltip.set(WailaConstants.OBJECT_NAME_TAG, new TextComponent(String.format(formatting.getEntityName(), entity.getDisplayName().getString())));
        if (config.get(WailaConstants.CONFIG_SHOW_REGISTRY))
            tooltip.set(WailaConstants.REGISTRY_NAME_TAG, new TextComponent(String.format(formatting.getRegistryName(), Registry.ENTITY_TYPE.getKey(entity.getType()))));
    }

    @Override
    public void appendBody(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
        if (config.get(WailaCore.CONFIG_SHOW_ENTITY_HEALTH) && accessor.getEntity() instanceof LivingEntity living) {
            float health = living.getHealth();
            float maxHealth = living.getMaxHealth();

            if (living.getMaxHealth() > IWailaConfig.getInstance().getGeneral().getMaxHealthForRender())
                tooltip.add(new TranslatableComponent("tooltip.waila.health", String.format("%.2f", health), String.format("%.2f", maxHealth)));
            else {
                CompoundTag healthData = new CompoundTag();
                healthData.putFloat("health", health / 2.0F);
                healthData.putFloat("max", maxHealth / 2.0F);
                tooltip.add(IDrawableText.of(WailaCore.RENDER_ENTITY_HEALTH, healthData));
            }
        }
    }

    @Override
    public void appendTail(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
        if (config.get(WailaConstants.CONFIG_SHOW_MOD_NAME))
            tooltip.set(WailaConstants.MOD_NAME_TAG, new TextComponent(String.format(IWailaConfig.getInstance().getFormatting().getModName(), IModInfo.get(accessor.getEntity()).getName())));
    }

}
