package codechicken.multipart.minecraft;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import codechicken.core.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry.IPartConverter;
import codechicken.multipart.MultiPartRegistry.IPartFactory;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.TMultiPart;

public class Content implements IPartFactory, IPartConverter
{
    @Override
    public TMultiPart createPart(String name, boolean client)
    {
        if(name.equals("mc_torch")) return new TorchPart();
        if(name.equals("mc_lever")) return new LeverPart();
        
        return null;
    }
    
    public void init()
    {
        MultiPartRegistry.registerConverter(this);
        MultiPartRegistry.registerParts(this, new String[]{
                "mc_torch",
                "mc_lever"
            });
    }

    @Override
    public boolean canConvert(int blockID)
    {
        return blockID == Block.torchWood.blockID || 
                blockID == Block.lever.blockID;
    }

    @Override
    public TMultiPart convert(World world, BlockCoord pos)
    {
        int id = world.getBlockId(pos.x, pos.y, pos.z);
        int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
        if(id == Block.torchWood.blockID)
            return new TorchPart(meta);
        if(id == Block.lever.blockID)
            return new LeverPart(meta);
        
        return null;
    }
}