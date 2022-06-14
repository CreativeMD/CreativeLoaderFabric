package team.creative.creativecore;

import java.util.OptionalLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import team.creative.creativecore.common.config.event.ConfigEventHandler;
import team.creative.creativecore.common.config.gui.ClientSyncGuiLayer;
import team.creative.creativecore.common.config.gui.ConfigGuiLayer;
import team.creative.creativecore.common.config.gui.GuiInfoStackButton;
import team.creative.creativecore.common.config.gui.GuiPlayerSelectorButton;
import team.creative.creativecore.common.config.holder.CreativeConfigRegistry;
import team.creative.creativecore.common.config.sync.ConfigurationChangePacket;
import team.creative.creativecore.common.config.sync.ConfigurationClientPacket;
import team.creative.creativecore.common.config.sync.ConfigurationPacket;
import team.creative.creativecore.common.gui.dialog.GuiDialogHandler;
import team.creative.creativecore.common.gui.handler.GuiCreator;
import team.creative.creativecore.common.gui.handler.GuiCreator.GuiCreatorBasic;
import team.creative.creativecore.common.gui.handler.GuiLayerHandler;
import team.creative.creativecore.common.gui.integration.ContainerIntegration;
import team.creative.creativecore.common.gui.packet.ControlSyncPacket;
import team.creative.creativecore.common.gui.packet.LayerClosePacket;
import team.creative.creativecore.common.gui.packet.LayerOpenPacket;
import team.creative.creativecore.common.gui.packet.OpenGuiPacket;
import team.creative.creativecore.common.network.CreativeNetwork;
import team.creative.creativecore.common.util.argument.StringArrayArgumentType;
import team.creative.creativecore.mixin.ArgumentTypeInfosAccessor;

public class CreativeCore implements ModInitializer {
    
    private static final ICreativeLoader LOADER = new CreativeFabricLoader();
    public static final String MODID = "creativecore";
    public static final Logger LOGGER = LogManager.getLogger(CreativeCore.MODID);
    public static final CreativeNetwork NETWORK = new CreativeNetwork("1.0", LOGGER, new ResourceLocation(CreativeCore.MODID, "main"));
    public static final CreativeCoreConfig CONFIG = new CreativeCoreConfig();
    public static final ResourceLocation FAKE_WORLD_LOCATION = new ResourceLocation(MODID, "fake");
    public static ResourceKey<Level> FAKE_DIMENSION_NAME = ResourceKey.create(Registry.DIMENSION_REGISTRY, FAKE_WORLD_LOCATION);
    public static final GuiCreatorBasic CONFIG_OPEN = GuiCreator
            .register("config", new GuiCreatorBasic((player, nbt) -> new ConfigGuiLayer(CreativeConfigRegistry.ROOT, Side.SERVER)));
    
    public static final GuiCreatorBasic CONFIG_CLIENT_OPEN = GuiCreator
            .register("clientconfig", new GuiCreatorBasic((player, nbt) -> new ConfigGuiLayer(CreativeConfigRegistry.ROOT, Side.CLIENT)));
    public static final GuiCreatorBasic CONFIG_CLIENT_SYNC_OPEN = GuiCreator
            .register("clientsyncconfig", new GuiCreatorBasic((player, nbt) -> new ClientSyncGuiLayer(CreativeConfigRegistry.ROOT)));
    public static ConfigEventHandler CONFIG_HANDLER;
    public static Holder<DimensionType> FAKE_DIMENSION;
    public static MenuType<ContainerIntegration> GUI_CONTAINER;
    
    public CreativeCore() {
        ServerLifecycleEvents.SERVER_STARTING.register(this::server);
    }
    
    private void server(MinecraftServer server) {
        server.getCommands().getDispatcher().register(Commands.literal("cmdconfig").executes((CommandContext<CommandSourceStack> x) -> {
            CONFIG_OPEN.open(new CompoundTag(), x.getSource().getPlayerOrException());
            return 0;
        }));
    }
    
    @Override
    public void onInitialize() {
        GUI_CONTAINER = ScreenHandlerRegistry
                .registerSimple(new ResourceLocation(CreativeCore.MODID, "container"), new ScreenHandlerRegistry.SimpleClientHandlerFactory<ContainerIntegration>() {
                    @Override
                    public ContainerIntegration create(int syncId, Inventory inventory) {
                        return new ContainerIntegration(GUI_CONTAINER, syncId, inventory.player);
                    }
                });
        NETWORK.registerType(ConfigurationChangePacket.class, ConfigurationChangePacket::new);
        NETWORK.registerType(ConfigurationClientPacket.class, ConfigurationClientPacket::new);
        NETWORK.registerType(ConfigurationPacket.class, ConfigurationPacket::new);
        NETWORK.registerType(LayerClosePacket.class, LayerClosePacket::new);
        NETWORK.registerType(LayerOpenPacket.class, LayerOpenPacket::new);
        NETWORK.registerType(OpenGuiPacket.class, OpenGuiPacket::new);
        NETWORK.registerType(ControlSyncPacket.class, ControlSyncPacket::new);
        CONFIG_HANDLER = new ConfigEventHandler(FabricLoader.getInstance().getConfigDir().toFile(), LOGGER);
        FAKE_DIMENSION = Holder.direct(new DimensionType(OptionalLong
                .empty(), true, false, false, false, 1, false, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, BuiltinDimensionTypes.OVERWORLD_EFFECTS, 0.0F, new DimensionType.MonsterSettings(false, false, UniformInt
                        .of(0, 0), 0)));
        
        ArgumentTypeInfosAccessor.getByClass().put(StringArrayArgumentType.class, SingletonArgumentInfo.contextFree(() -> StringArrayArgumentType.stringArray()));
        
        GuiLayerHandler.REGISTRY.register("info", GuiInfoStackButton.INFO_LAYER);
        GuiLayerHandler.REGISTRY.register("player", GuiPlayerSelectorButton.PLAYER_LAYER);
        GuiLayerHandler.REGISTRY.register("dialog", GuiDialogHandler.DIALOG_HANDLER);
    }
    
    public static ICreativeLoader loader() {
        return LOADER;
    }
}
