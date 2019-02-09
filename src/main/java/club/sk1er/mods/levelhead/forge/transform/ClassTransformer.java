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
        classReader.accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            String methodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, method.name, method.desc);
            if (methodName.equals("func_175245_a")) {
                method.instructions.insertBefore(method.instructions.getFirst(), getHookCall(method, "drawPing"));
            } else if (methodName.equals("func_175249_a")) {
                int found = 0;

                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode abstractInsnNode = iterator.next();

                    if (abstractInsnNode.getOpcode() == Opcodes.ALOAD) {
                        VarInsnNode node = (VarInsnNode) abstractInsnNode;
                        if (node.var == 0) found = 1;
                        else if (node.var == (vanillaEnhancements ? 8 : 9) && found == 1)  found = 2;
                        else found = 0;
                    } else if (abstractInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        if (vanillaEnhancements) { // vanilla enhancements uses their own GuiPlayerTabOverlay impl
                            MethodInsnNode node = (MethodInsnNode) abstractInsnNode;
                            if (node.name.equals("func_175243_a") && found == 2 && // getPlayerName
                                    node.owner.equals("com/orangemarshall/enhancements/modules/tab/CustomGuiPlayerTabOverlay") &&
                                    node.desc.equals("(Lnet/minecraft/client/network/NetworkPlayerInfo;)Ljava/lang/String;")) {
                                found = 3;
                            } else if (node.name.equals("func_78256_a") && found == 3 && // getStringWidth
                                    node.owner.equals("net/minecraft/client/gui/FontRenderer") &&
                                    node.desc.equals("(Ljava/lang/String;)I")) {

                                method.instructions.insert(node, this.getLevelheadWidthCall(8));
                                break;
                            }
                        } else { // regular forge
                            MethodInsnNode node = (MethodInsnNode) abstractInsnNode;
                            if (node.name.equals("a") && found == 2 && // getPlayerName
                                    node.owner.equals("awh") && // net/minecraft/client/gui/GuiPlayerTabOverlay
                                    node.desc.equals("(Lbdc;)Ljava/lang/String;")) { // net/minecraft/client/network/NetworkPlayerInfo
                                found = 3;
                            } else if (node.name.equals("a") && found == 3 && // getStringWidth
                                    node.owner.equals("avn") &&  // net/minecraft/client/gui/FontRenderer
                                    node.desc.equals("(Ljava/lang/String;)I")) {

                                method.instructions.insert(node, this.getLevelheadWidthCall(9));
                                break;
                            }
                        }
                    }
                }
            }
        }

        ClassWriter classWriter = new ClassWriter(0);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private InsnList getLevelheadWidthCall(int index) {
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(Opcodes.ALOAD, index));

        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "club/sk1er/mods/levelhead/forge/transform/Hooks",
                "getLevelheadWith", "(Lbdc;)I", false));

        insnList.add(new InsnNode(Opcodes.IADD));
        return insnList;
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
