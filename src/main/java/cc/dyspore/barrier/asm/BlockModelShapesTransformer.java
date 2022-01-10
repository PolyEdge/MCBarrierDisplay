package cc.dyspore.barrier.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

public class BlockModelShapesTransformer implements IClassTransformer {
    private static final String className = "cc/dyspore/barrier/BarrierDisplayMod";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!transformedName.equals("net.minecraft.client.renderer.BlockModelShapes")) {
            return bytes;
        }

        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        for (MethodNode method : classNode.methods) {
            String mappedMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, method.name, method.desc);
            String fieldName = null;
            String fieldDesc = null;

            if ((mappedMethodName.equals("reloadModels") || mappedMethodName.equals("func_178124_c"))) {
                Iterator<AbstractInsnNode> iterator = method.instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode insn = iterator.next();
                    if (insn.getOpcode() == Opcodes.GETFIELD && fieldName == null && insn instanceof FieldInsnNode) {
                        FieldInsnNode node = (FieldInsnNode) insn;
                        fieldName = node.name;
                        fieldDesc = node.desc;
                    }
                    if (insn.getOpcode() == Opcodes.INVOKEINTERFACE && insn instanceof MethodInsnNode) {
                        MethodInsnNode methodNode = (MethodInsnNode) insn;
                        if (methodNode.owner.equals("java/util/Map") && methodNode.name.equals("clear")) {
                            InsnList insnList = new InsnList();
                            insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            insnList.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, fieldName, fieldDesc));
                            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, className, "bake", "(Ljava/util/Map;)V", false));
                            method.instructions.insert(insn, insnList);
                            break;
                        }
                    }
                }
                break;
            }
        }
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}
