package club.sk1er.mods.levelhead.forge.transform;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public final class ClassTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        boolean vanillaEnhancements = false;
        if (!"net.minecraft.client.gui.GuiPlayerTabOverlay".equals(transformedName)) {
            if ("com.orangemarshall.enhancements.modules.tab.CustomGuiPlayerTabOverlay".equals(transformedName)) {
                vanillaEnhancements = true;
            } else {
                return basicClass;
            }
        }

        ClassReader classReader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

        for (MethodNode method : classNode.methods) {
            String methodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, method.name, method.desc);
            if (methodName.equals("func_175245_a") || methodName.equals("drawPing")) {
                method.instructions.insertBefore(method.instructions.getFirst(), getHookCall(method));
            } else if (methodName.equals("func_175249_a") || methodName.equals("renderPlayerlist")) {
                insertWidthCall(method, vanillaEnhancements);
            }
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private void insertWidthCall(MethodNode method, boolean vanillaEnhancements) {
        ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode node = iterator.next();

            if (node instanceof MethodInsnNode
                    && node.getPrevious() instanceof MethodInsnNode
                    && node.getNext() instanceof VarInsnNode) {

                MethodInsnNode widthCall = (MethodInsnNode) node;
                MethodInsnNode prevCall = (MethodInsnNode) node.getPrevious();
                VarInsnNode storeCall = (VarInsnNode) node.getNext();

                if (storeCall.getOpcode() == Opcodes.ISTORE
                        && (widthCall.name.equals("func_78256_a") || widthCall.name.equals("getStringWidth") || widthCall.name.equals("a"))
                        && (prevCall.name.equals("func_175243_a") || prevCall.name.equals("getPlayerName") || prevCall.name.equals("a"))) {
                    method.instructions.insert(node, getLevelheadWidthCall(vanillaEnhancements ? 8 : 9));
                }
            }
        }
    }

    private InsnList getLevelheadWidthCall(int index) {
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(Opcodes.ALOAD, index));

        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "club/sk1er/mods/levelhead/forge/transform/Hooks",
                "getLevelheadWidth", "(Lnet/minecraft/client/network/NetworkPlayerInfo;)I", false));

        insnList.add(new InsnNode(Opcodes.IADD));
        return insnList;
    }

    private static InsnList getHookCall(MethodNode node) {
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
                "drawPing" + "Hook", signature, false));

        return insnList;
    }

}
