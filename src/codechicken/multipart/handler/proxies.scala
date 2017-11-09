package codechicken.multipart.handler

import java.io.File

import codechicken.lib.config.ConfigFile
import codechicken.lib.packet.PacketCustom
import codechicken.lib.world.{TileChunkLoadHook, WorldExtensionManager}
import codechicken.multipart._
import codechicken.multipart.handler.MultipartSaveLoad.TileNBTContainer
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.{BlockPos, ChunkPos}
import net.minecraftforge.client.event.ModelBakeEvent
import net.minecraftforge.client.model.ModelLoader
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.apache.logging.log4j.{LogManager, Logger}

class MultipartProxy_serverImpl
{
    var block:BlockMultipart = _
    var config:ConfigFile = _
    var logger:Logger = LogManager.getLogger("ForgeMultiPartCBE")

    def preInit(cfgdir: File)
    {
        config = new ConfigFile(new File(cfgdir, "multipart.cfg"))
            .setComment("Multipart API config file")

        block = new BlockMultipart()
        GameRegistry.register(block.setRegistryName(new ResourceLocation(MultipartMod.modID, "multipart_block")))

        MultipartGenerator.registerTrait("net.minecraft.util.ITickable", "codechicken.multipart.scalatraits.JTickableTile")
        MultipartGenerator.registerTrait("codechicken.multipart.TSlottedPart", "codechicken.multipart.scalatraits.TSlottedTile")
        MultipartGenerator.registerTrait("net.minecraftforge.common.capabilities.ICapabilityProvider", "codechicken.multipart.scalatraits.JCapabilityProvider")
        //MultipartGenerator.registerTrait("net.minecraftforge.fluids.IFluidHandler", "codechicken.multipart.scalatraits.TFluidHandlerTile")
        MultipartGenerator.registerTrait("net.minecraft.inventory.IInventory", "codechicken.multipart.scalatraits.JInventoryTile")
        MultipartGenerator.registerTrait("net.minecraft.inventory.ISidedInventory", "codechicken.multipart.scalatraits.JInventoryTile")
        MultipartGenerator.registerTrait("codechicken.multipart.TPartialOcclusionPart", "codechicken.multipart.scalatraits.TPartialOcclusionTile")
        MultipartGenerator.registerTrait("codechicken.multipart.IRedstonePart", "codechicken.multipart.scalatraits.TRedstoneTile")
        MultipartGenerator.registerTrait("codechicken.multipart.IRandomDisplayTickPart", "codechicken.multipart.scalatraits.TRandomDisplayTickTile", null)
        MultipartGenerator.registerTrait("codechicken.multipart.INeighborTileChangePart", null, "codechicken.multipart.scalatraits.TTileChangeTile")
        MultipartGenerator.registerTrait("codechicken.multipart.IModelRenderPart", "codechicken.multipart.scalatraits.TModelRenderTile", null)

        MultipartSaveLoad.load()
    }

    def init(){}

    def postInit()
    {
        MinecraftForge.EVENT_BUS.register(MultipartEventHandler)
        PacketCustom.assignHandler(MultipartSPH.channel, MultipartSPH)
        PacketCustom.assignHandshakeHandler(MultipartSPH.registryChannel, MultipartSPH)

        WorldExtensionManager.registerWorldExtension(TickScheduler)
        TileChunkLoadHook.init()

        MultipartCompatiblity.load()
    }

    def onTileClassBuilt(t: Class[_ <: TileEntity])
    {
        MultipartSaveLoad.registerTileClass(t)
    }
}

class MultipartProxy_clientImpl extends MultipartProxy_serverImpl
{
    @SideOnly(Side.CLIENT)
    override def preInit(cfgdir:File)
    {
        super.preInit(cfgdir)

        ModelLoader.setCustomStateMapper(block, MultipartStateMapper)
    }

    @SideOnly(Side.CLIENT)
    override def init()
    {
        super.init()

        MinecraftForge.EVENT_BUS.register(this)
    }

    @SideOnly(Side.CLIENT)
    override def postInit()
    {
        super.postInit()

        PacketCustom.assignHandler(MultipartCPH.channel, MultipartCPH)
        PacketCustom.assignHandler(MultipartCPH.registryChannel, MultipartCPH)

        MinecraftForge.EVENT_BUS.register(ControlKeyHandler)
        ClientRegistry.registerKeyBinding(ControlKeyHandler)

        MultipartRenderer.register()
    }

    @SideOnly(Side.CLIENT)
    override def onTileClassBuilt(t:Class[_ <: TileEntity])
    {
        super.onTileClassBuilt(t)
        ClientRegistry.bindTileEntitySpecialRenderer(t.asInstanceOf[Class[TileEntity]],
            MultipartRenderer.asInstanceOf[TileEntitySpecialRenderer[TileEntity]])
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    def onModelBakeEvent(event:ModelBakeEvent)
    {
//        event.getModelRegistry.putObject(
//            new ModelResourceLocation(block.getRegistryName.toString),
//            MultipartTileModel
//        )
    }
}

object MultipartProxy extends MultipartProxy_clientImpl
{
    def indexInChunk(cc:ChunkPos, i: Int) = new BlockPos(cc.chunkXPos << 4 | i & 0xF, (i >> 8) & 0xFF, cc.chunkZPos << 4 | (i & 0xF0) >> 4)
    def indexInChunk(pos:BlockPos) = pos.getX & 0xF | pos.getY << 8 | (pos.getZ & 0xF) << 4
}
