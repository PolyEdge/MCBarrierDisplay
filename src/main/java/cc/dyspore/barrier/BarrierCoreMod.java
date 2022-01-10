package cc.dyspore.barrier;

import cc.dyspore.barrier.asm.BlockBarrierTransformer;
import cc.dyspore.barrier.asm.BlockModelShapesTransformer;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class BarrierCoreMod implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                BlockBarrierTransformer.class.getName(),
                BlockModelShapesTransformer.class.getName()
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
