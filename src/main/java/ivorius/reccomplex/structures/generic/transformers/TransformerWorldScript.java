/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.structures.generic.transformers;

import com.google.gson.*;
import ivorius.ivtoolkit.tools.MCRegistry;
import ivorius.ivtoolkit.tools.NBTCompoundObjects;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.gui.editstructure.transformers.TableDataSourceBTWorldScript;
import ivorius.reccomplex.gui.table.TableDataSource;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.json.NbtToJson;
import ivorius.reccomplex.scripts.world.WorldScriptMulti;
import ivorius.reccomplex.structures.StructureLoadContext;
import ivorius.reccomplex.structures.StructurePrepareContext;
import ivorius.reccomplex.structures.StructureSpawnContext;
import ivorius.reccomplex.structures.generic.WeightedBlockState;
import ivorius.reccomplex.structures.generic.matchers.BlockMatcher;
import ivorius.reccomplex.utils.NBTStorable;
import ivorius.reccomplex.worldgen.StructureGenerator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Type;

/**
 * Created by lukas on 25.05.14.
 */
public class TransformerWorldScript extends TransformerSingleBlock<TransformerWorldScript.InstanceData>
{
    public WorldScriptMulti script;
    public BlockMatcher sourceMatcher;

    public TransformerWorldScript()
    {
        this(randomID(TransformerWorldScript.class), new WorldScriptMulti(), BlockMatcher.of(RecurrentComplex.specialRegistry, Blocks.WOOL));
    }

    public TransformerWorldScript(String id, WorldScriptMulti script, String sourceExpression)
    {
        super(id);
        this.script = script;
        this.sourceMatcher = new BlockMatcher(RecurrentComplex.specialRegistry, sourceExpression);
    }

    @Override
    public boolean matches(InstanceData instanceData, IBlockState state)
    {
        return sourceMatcher.test(state);
    }

    @Override
    public void transformBlock(InstanceData instanceData, Phase phase, StructureSpawnContext context, BlockPos coord, IBlockState sourceState)
    {
        WorldScriptMulti.InstanceData scriptInstanceData = script.prepareInstanceData(new StructurePrepareContext(context.random, context.world, context.biome, context.transform, context.boundingBox, context.generateAsSource), coord);
        script.generate(context, scriptInstanceData, coord);
    }

    @Override
    public InstanceData prepareInstanceData(StructurePrepareContext context)
    {
        return new InstanceData();
    }

    @Override
    public InstanceData loadInstanceData(StructureLoadContext context, NBTBase nbt)
    {
        InstanceData instanceData = new InstanceData();
        instanceData.readFromNBT(this, context, nbt);
        return instanceData;
    }

    @Override
    public String getDisplayString()
    {
        return script.getDisplayString();
    }

    @Override
    public TableDataSource tableDataSource(TableNavigator navigator, TableDelegate delegate)
    {
        return new TableDataSourceBTWorldScript(this, navigator, delegate);
    }

    @Override
    public boolean generatesInPhase(InstanceData instanceData, Phase phase)
    {
        return phase == Phase.BEFORE;
    }

    public static class InstanceData implements NBTStorable
    {
        @Override
        public NBTBase writeToNBT()
        {
            NBTTagCompound compound = new NBTTagCompound();
            return compound;
        }

        public void readFromNBT(TransformerWorldScript transformer, StructureLoadContext context, NBTBase nbt)
        {
        }
    }

    public static class Serializer implements JsonDeserializer<TransformerWorldScript>, JsonSerializer<TransformerWorldScript>
    {
        private MCRegistry registry;
        private Gson gson;

        public Serializer(MCRegistry registry)
        {
            this.registry = registry;
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(WeightedBlockState.class, new WeightedBlockState.Serializer(registry));
            NbtToJson.registerSafeNBTSerializer(builder);
            gson = builder.create();
        }

        @Override
        public TransformerWorldScript deserialize(JsonElement jsonElement, Type par2Type, JsonDeserializationContext context)
        {
            JsonObject jsonObject = JsonUtils.getJsonElementAsJsonObject(jsonElement, "transformerReplace");

            String id = JsonUtils.getJsonObjectStringFieldValueOrDefault(jsonObject, "id", randomID(TransformerWorldScript.class));

            String expression = JsonUtils.getJsonObjectStringFieldValueOrDefault(jsonObject, "sourceExpression", "");
            WorldScriptMulti script = NBTCompoundObjects.read(gson.fromJson(JsonUtils.getJsonObjectFieldOrDefault(jsonObject, "script", new JsonObject()), NBTTagCompound.class), WorldScriptMulti.class);

            return new TransformerWorldScript(id, script, expression);
        }

        @Override
        public JsonElement serialize(TransformerWorldScript transformer, Type par2Type, JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("id", transformer.id());
            jsonObject.addProperty("sourceExpression", transformer.sourceMatcher.getExpression());

            jsonObject.add("script", gson.toJsonTree(NBTCompoundObjects.write(transformer.script)));

            return jsonObject;
        }
    }
}
