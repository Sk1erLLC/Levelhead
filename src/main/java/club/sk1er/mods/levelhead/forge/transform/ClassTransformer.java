package club.sk1er.mods.levelhead.forge.transform;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

public final class ClassTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"net.minecraft.client.gui.GuiPlayerTabOverlay".equals(transformedName)) {
            return basicClass;
        }

        ClassReader classReader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            String methodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, method.name, method.desc);
            if (methodName.equals("drawPing") || methodName.equals("func_175245_a")) {
                method.instructions.insertBefore(method.instructions.getFirst(), getHookCall(method, "drawPing"));
                System.out.println("Hooked drawPing method");
                break;
            }
        }

        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private static InsnList getHookCall(MethodNode node, String name) {
        InsnList insnList = new InsnList();

        List<Integer> opcodes = new ArrayList<>();

        StringBuilder classNameBuilder = new StringBuilder();
        StringBuilder mappedSignature = new StringBuilder("(");

        boolean className = false;

        String desc = node.desc;
        for (char c : desc.toCharArray()) {
            if (c == '(') {
                continue;
            } else if (c == ')') {
                break;
            }

            if (className) {
                if (c == ';') {
                    mappedSignature.append(FMLDeobfuscatingRemapper.INSTANCE.map(classNameBuilder.toString())).append(";");
                    className = false;
                } else {
                    classNameBuilder.append(c);
                }

                continue;
            } else if (c == 'I') {
                opcodes.add(Opcodes.ILOAD);
            } else if (c == 'D') {
                opcodes.add(Opcodes.DLOAD);
            } else if (c == 'F') {
                opcodes.add(Opcodes.FLOAD);
            } else if (c == 'L') {
                opcodes.add(Opcodes.ALOAD);

                classNameBuilder = new StringBuilder();
                className = true;
            }

            mappedSignature.append(c);
        }

        mappedSignature.append(")V"); // All hooks must be of 'void' type

        for (int i = 0; i < opcodes.size(); i++) {
            insnList.add(new VarInsnNode(opcodes.get(i), i + 1));
        }

        String signature = mappedSignature.toString();

        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "club/sk1er/mods/levelhead/forge/transform/Hooks",
                name + "Hook", signature, false));

        return insnList;
    }

}
