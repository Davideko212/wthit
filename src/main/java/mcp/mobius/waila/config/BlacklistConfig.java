package mcp.mobius.waila.config;

import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import mcp.mobius.waila.api.IBlacklistConfig;
import mcp.mobius.waila.api.IRegistryFilter;
import mcp.mobius.waila.util.Log;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public class BlacklistConfig {

    private static final Log LOG = Log.create();

    public static final int VERSION = 0;

    public final LinkedHashSet<String> blocks = new LinkedHashSet<>();
    public final LinkedHashSet<String> blockEntityTypes = new LinkedHashSet<>();
    public final LinkedHashSet<String> entityTypes = new LinkedHashSet<>();

    private int configVersion = 0;
    public int[] pluginHash = {0, 0, 0};

    @Nullable
    private transient View view;

    public int getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }

    public View getView() {
        if (view == null) view = new View();
        return view;
    }

    public class View implements IBlacklistConfig {

        public final IRegistryFilter<Block> blockFilter;
        public final IRegistryFilter<BlockEntityType<?>> blockEntityFilter;
        public final IRegistryFilter<EntityType<?>> entityFilter;

        private Set<Block> syncedBlockFilter = Set.of();
        private Set<BlockEntityType<?>> syncedBlockEntityFilter = Set.of();
        private Set<EntityType<?>> syncedEntityFilter = Set.of();

        private View() {
            blockFilter = IRegistryFilter.of(BuiltInRegistries.BLOCK).parse(blocks).build();
            blockEntityFilter = IRegistryFilter.of(BuiltInRegistries.BLOCK_ENTITY_TYPE).parse(blockEntityTypes).build();
            entityFilter = IRegistryFilter.of(BuiltInRegistries.ENTITY_TYPE).parse(entityTypes).build();
        }

        public void sync(Set<String> blockRules, Set<String> blockEntityRules, Set<String> entityRules) {
            syncedBlockFilter = sync(BuiltInRegistries.BLOCK, blockFilter, blockRules);
            syncedBlockEntityFilter = sync(BuiltInRegistries.BLOCK_ENTITY_TYPE, blockEntityFilter, blockEntityRules);
            syncedEntityFilter = sync(BuiltInRegistries.ENTITY_TYPE, entityFilter, entityRules);
        }

        private static <T> Set<T> sync(Registry<T> registry, IRegistryFilter<T> filter, Set<String> rules) {
            LOG.debug("Syncing blacklist {}", registry.key().location());

            IRegistryFilter.Builder<T> builder = IRegistryFilter.of(registry);
            rules.forEach(builder::parse);

            Set<T> set = builder.build().getValues().stream()
                .filter(it -> !filter.contains(it))
                .collect(Collectors.toUnmodifiableSet());

            if (LOG.isDebugEnabled()) {
                LOG.debug("Finished syncing blacklist, total {} distinct entries", set.size());
                set.forEach(it -> LOG.debug("\t{}", registry.getKey(it)));
            }

            return set;
        }

        @Override
        public boolean contains(Block block) {
            return blockFilter.contains(block) || syncedBlockFilter.contains(block);
        }

        @Override
        public boolean contains(BlockEntity blockEntity) {
            BlockEntityType<?> type = blockEntity.getType();
            return blockEntityFilter.contains(type) || syncedBlockEntityFilter.contains(type);
        }

        @Override
        public boolean contains(Entity entity) {
            EntityType<?> type = entity.getType();
            return entityFilter.contains(type) || syncedEntityFilter.contains(type);
        }
    }

    public static class Adapter implements JsonSerializer<BlacklistConfig>, JsonDeserializer<BlacklistConfig> {

        @Override
        public JsonElement serialize(BlacklistConfig src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject object = new JsonObject();

            String[] comments = """
                On the SERVER, changes will be applied after the server is restarted
                On the CLIENT, changes will be applied after player quit and rejoin a world
                                
                Operators:
                @namespace - Filter objects based on their namespace location
                #tag       - Filter objects based on data pack tags
                /regex/    - Filter objects based on regular expression
                default    - Filter objects with specific ID"""
                .split("\n");

            JsonArray commentArray = new JsonArray();
            for (String line : comments) commentArray.add(line);
            object.add("_comment", commentArray);

            object.add("blocks", context.serialize(src.blocks));
            object.add("blockEntityTypes", context.serialize(src.blockEntityTypes));
            object.add("entityTypes", context.serialize(src.entityTypes));

            object.addProperty("configVersion", src.configVersion);
            object.add("pluginHash", context.serialize(src.pluginHash));

            return object;
        }

        @Override
        public BlacklistConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            BlacklistConfig res = new BlacklistConfig();

            deserializeEntries(res.blocks, object.getAsJsonArray("blocks"));
            deserializeEntries(res.blockEntityTypes, object.getAsJsonArray("blockEntityTypes"));
            deserializeEntries(res.entityTypes, object.getAsJsonArray("entityTypes"));

            res.configVersion = object.get("configVersion").getAsInt();
            res.pluginHash = context.deserialize(object.get("pluginHash"), int[].class);

            return res;
        }

        private void deserializeEntries(LinkedHashSet<String> set, JsonArray array) {
            for (JsonElement entry : array) {
                set.add(entry.getAsString());
            }
        }

    }

}
