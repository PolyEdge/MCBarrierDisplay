package cc.dyspore.barrier.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;


@SuppressWarnings("SameParameterValue")
public class BlockBarrierTransformer implements IClassTransformer {
    private static final String className = "cc/dyspore/barrier/BarrierDisplayMod";

    private final String blockLayer = "net/minecraft/util/EnumWorldBlockLayer";
    private final String blockLayerObf = FMLDeobfuscatingRemapper.INSTANCE.unmap(this.blockLayer);
    private final String block = FMLDeobfuscatingRemapper.INSTANCE.unmap("net/minecraft/block/Block");
    private final String blockAccess = FMLDeobfuscatingRemapper.INSTANCE.unmap("net/minecraft/world/IBlockAccess");
    private final String blockPos = FMLDeobfuscatingRemapper.INSTANCE.unmap("net/minecraft/util/BlockPos");
    private final String enumFacing = FMLDeobfuscatingRemapper.INSTANCE.unmap("net/minecraft/util/EnumFacing");
    private final String descriptor = "(L" + blockAccess + ";L" + blockPos + ";L" + enumFacing + ";)Z";

    public BlockBarrierTransformer() {
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!transformedName.equals("net.minecraft.block.BlockBarrier")) {
            return bytes;
        }

        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        for (MethodNode method : classNode.methods) {
            String mappedMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, method.name, method.desc);

            if (mappedMethodName.equals("getRenderType") || mappedMethodName.equals("func_149645_b")) {
                //System.out.println("" + method.name + " " + method.desc + " " + method.signature);

                Iterator<AbstractInsnNode> iterator = method.instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode node = iterator.next();
                    if (node.getOpcode() == Opcodes.ICONST_M1) {
                        method.instructions.insertBefore(node, new MethodInsnNode(Opcodes.INVOKESTATIC, className, "getType", "()I"));
                        method.instructions.remove(node);
                        break;
                    }
                }
                break;
            }
        }
        //classNode.methods.add(this.createRenderLayerMethod("m"));
        classNode.methods.add(this.createRenderLayerMethod("func_180664_k"));
        //classNode.methods.add(this.createRenderLayerMethod("getBlockLayer"));

        //classNode.methods.add(this.createFullCubeMethod("d"));
        classNode.methods.add(this.createFullCubeMethod("func_149686_d"));
        //classNode.methods.add(this.createFullCubeMethod("isFullCube"));

        //classNode.methods.add(this.createShouldSideRender("a"));
        classNode.methods.add(this.createShouldSideRender("func_176225_a"));
        //classNode.methods.add(this.createShouldSideRender("shouldSideBeRendered"));

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private MethodNode createRenderLayerMethod(String name) {
        String desc = "L" + this.blockLayerObf + ";";
        MethodNode node = new MethodNode(Opcodes.ACC_PUBLIC, name, "()" + desc, null, null);
        node.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, this.blockLayerObf, EnumWorldBlockLayer.CUTOUT.name(), desc));
        node.instructions.add(new InsnNode(Opcodes.ARETURN));
        return node;
    }

    private MethodNode createFullCubeMethod(String name) {
        MethodNode node = new MethodNode(Opcodes.ACC_PUBLIC, name, "()Z", null, null);
        node.instructions.add(new InsnNode(Opcodes.ICONST_0));
        node.instructions.add(new InsnNode(Opcodes.IRETURN));
        return node;
    }

    private MethodNode createShouldSideRender(String name) {
        MethodNode node = new MethodNode(Opcodes.ACC_PUBLIC, name, this.descriptor, null, null);
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        node.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, className, "cull", this.descriptor, false));
        LabelNode label = new LabelNode();
        node.instructions.add(new JumpInsnNode(Opcodes.IFEQ, label));
        node.instructions.add(new InsnNode(Opcodes.ICONST_0));
        node.instructions.add(new InsnNode(Opcodes.IRETURN));
        node.instructions.add(label);
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        node.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
        node.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, this.block, name, this.descriptor, false));
        node.instructions.add(new InsnNode(Opcodes.IRETURN));
        return node;
    }
}
