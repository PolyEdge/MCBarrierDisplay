package cc.dyspore.barrier;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockPart;
import net.minecraft.client.renderer.block.model.BlockPartFace;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelRotation;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ITransformation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

@Mod(modid = "barrier-render", name = "Barrier Renderer", version = "1.0", acceptedMinecraftVersions = "[1.8.8,1.8.9]", clientSideOnly = true)
public class BarrierDisplayMod extends CommandBase {
    public static BarrierDisplayMod instance;

    private static final Pattern FORMAT_CODE = Pattern.compile("&([0-9a-fA-Fk-oK-OrR])");
    private static final String BARRIER_MODEL = "{\n" +
            "    \"elements\": [\n" +
            "        {   \"from\": [ 0, 0, 0 ],\n" +
            "            \"to\": [ 16, 16, 16 ],\n" +
            "            \"faces\": {\n" +
            "                \"down\":  { \"uv\": [ 0, 0, 16, 16 ], \"texture\": \"#all\", \"cullface\": \"down\" },\n" +
            "                \"up\":    { \"uv\": [ 0, 0, 16, 16 ], \"texture\": \"#all\", \"cullface\": \"up\" },\n" +
            "                \"north\": { \"uv\": [ 0, 0, 16, 16 ], \"texture\": \"#all\", \"cullface\": \"north\" },\n" +
            "                \"south\": { \"uv\": [ 0, 0, 16, 16 ], \"texture\": \"#all\", \"cullface\": \"south\" },\n" +
            "                \"west\":  { \"uv\": [ 0, 0, 16, 16 ], \"texture\": \"#all\", \"cullface\": \"west\" },\n" +
            "                \"east\":  { \"uv\": [ 0, 0, 16, 16 ], \"texture\": \"#all\", \"cullface\": \"east\" }\n" +
            "            }\n" +
            "        }\n" +
            "    ],\n" +
            "    \"textures\": {\n" +
            "        \"all\": \"items/barrier\",\n" +
            "        \"particle\": \"items/barrier\"\n" +
            "    }\n" +
            "}";

    private static final ModelBlock BARRIER_BLOCK_MODEL = ModelBlock.deserialize(BARRIER_MODEL);
    private static final MakeBakedQuad METHOD_BAKE_QUAD = getMethodBake();

    private Configuration configuration;
    private ModelBakery bakery;
    private boolean enabled;

    @SuppressWarnings("SameParameterValue")
    private static Method getMethod(Class<?> clazz, MethodGetter getter, String... names) {
        for (String string : names) {
            try {
                Method method = getter.get(clazz, string);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    private static MakeBakedQuad getMethodBake() {
        Method method = getMethod(ModelBakery.class, (clazz, name) -> clazz.getDeclaredMethod(name, BlockPart.class, BlockPartFace.class, TextureAtlasSprite.class, EnumFacing.class, ITransformation.class, boolean.class),
                "makeBakedQuad", "func_177589_a", "a");
        return (bakery, part, face, texture, facing, transform, uvLock) -> {
            try {
                return (BakedQuad) method.invoke(bakery, part, face, texture, facing, transform, uvLock);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                return null;
            }
        };
    }

    @SuppressWarnings("SameParameterValue")
    private static IBakedModel bakeModel(ModelBlock modelBlockIn, ITransformation modelRotationIn, boolean uvLocked, ModelBakery bakery, MakeBakedQuad make, TextureMap map) {
        TextureAtlasSprite sprite = map.getAtlasSprite(new ResourceLocation(modelBlockIn.resolveTextureName("particle")).toString());
        SimpleBakedModel.Builder builder = (new SimpleBakedModel.Builder(modelBlockIn)).setTexture(sprite);

        for (BlockPart blockpart : modelBlockIn.getElements()) {
            for (EnumFacing enumfacing : blockpart.mapFaces.keySet()) {
                BlockPartFace face = blockpart.mapFaces.get(enumfacing);
                TextureAtlasSprite sprite1 = map.getAtlasSprite(new ResourceLocation(modelBlockIn.resolveTextureName(face.texture)).toString());

                if (face.cullFace == null || !net.minecraftforge.client.model.TRSRTransformation.isInteger(modelRotationIn.getMatrix())) {
                    builder.addGeneralQuad(make.create(bakery, blockpart, face, sprite1, enumfacing, modelRotationIn, uvLocked));
                } else {
                    builder.addFaceQuad(modelRotationIn.rotate(face.cullFace), make.create(bakery, blockpart, face, sprite1, enumfacing, modelRotationIn, uvLocked));
                }
            }
        }
        return builder.makeBakedModel();
    }

    public static void bake(Map<IBlockState, IBakedModel> map) {
        if (instance.bakery == null) {
            return;
        }
        IBakedModel model = bakeModel(BARRIER_BLOCK_MODEL, ModelRotation.X0_Y0, true, instance.bakery, METHOD_BAKE_QUAD, Minecraft.getMinecraft().getTextureMapBlocks());
        map.put(Blocks.barrier.getDefaultState(), model);
        instance.bakery = null;
    }

    public static boolean cull(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        IBlockState iblockstate = worldIn.getBlockState(pos);
        Block block = iblockstate.getBlock();
        boolean barrier = block == Blocks.barrier;

        if (worldIn.getBlockState(pos.offset(side.getOpposite())) != iblockstate) {
            return false;
        }

        return barrier;
    }

    public static int getType() {
        return instance.enabled ? 3 : -1;
    }

    @Mod.EventHandler
    public void preInit(final FMLPreInitializationEvent event) {
        instance = this;
        this.configuration = new Configuration(event.getSuggestedConfigurationFile());
        this.enabled = this.configuration.getBoolean("enabled", "display", false, null);
    }

    @Mod.EventHandler
    public void init(final FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void preBake(final ModelBakeEvent event) {
        this.bakery = event.modelLoader;
    }

    public void save() {
        this.configuration.get("display", "enabled", false).set(this.enabled);
        this.configuration.save();
    }

    @Override
    public String getCommandName() {
        return "barrier";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/barrier";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        boolean enabled = !BarrierDisplayMod.this.enabled;
        BarrierDisplayMod.this.enabled = enabled;
        String message = FORMAT_CODE.matcher("&9Barrier> &7Barrier display &e" + (enabled ? "enabled" : "disabled") + "&7.").replaceAll(((char) 167) + "$1");
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.ingameGUI.getChatGUI().printChatMessage(new ChatComponentText(message));
        minecraft.renderGlobal.loadRenderers();
        BarrierDisplayMod.this.save();
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    public interface MethodGetter {
        Method get(Class<?> clazz, String name) throws NoSuchMethodException;
    }

    public interface MakeBakedQuad {
        BakedQuad create(ModelBakery bakery, BlockPart part, BlockPartFace face, TextureAtlasSprite texture, EnumFacing facing, ITransformation transform, boolean uvLock);
    }
}
