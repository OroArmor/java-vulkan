package com.oroarmor.profiler;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;

public class ProfileTransformer implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new ProfileTransformer());
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        return new ProfileClassWriter(classfileBuffer).addProfiles();
    }

    public static class ProfileClassWriter {
        public static final String PROFILER_CLASS_NAME = "com/oroarmor/profiler/Profiler";
        ClassReader reader;
        ClassWriter writer;

        public ProfileClassWriter(byte[] contents) {
            reader = new ClassReader(contents);
            writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        }

        public byte[] addProfiles() {
            reader.accept(new ProfileClassVisitor(writer), 0);
            return writer.toByteArray();
        }
    }

    public static class ProfileClassVisitor extends ClassVisitor {
        private String className = "";

        public ProfileClassVisitor(ClassVisitor cv) {
            super(ASM9, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            className = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            return new ProfileMethodVisitor(cv.visitMethod(access, name, desc, signature, exceptions), name + desc, className, (access & ACC_STATIC) != 0);
        }
    }

    public static class ProfileMethodVisitor extends MethodVisitor {
        private final String name;
        private final boolean isStatic;
        private final String className;
        private boolean annotationExists = false;

        private String annotationValue = "";

        public ProfileMethodVisitor(MethodVisitor visitor, String name, String desc, boolean isStatic) {
            super(Opcodes.ASM9, visitor);
            this.name = name;
            this.isStatic = isStatic;
            this.className = desc.replaceAll("/", ".");
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            annotationExists = descriptor.equals("Lcom/oroarmor/profiler/Profile;");
            return new ProfileAnnotationVisitor(super.visitAnnotation(descriptor, visible), this);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (annotationExists) {
                String name1 = annotationValue.isEmpty() ? className + (isStatic ? "." : "#") + name : annotationValue;
                this.mv.visitCode();
                this.mv.visitFieldInsn(Opcodes.GETSTATIC, ProfileClassWriter.PROFILER_CLASS_NAME, "PROFILER", "L" + ProfileClassWriter.PROFILER_CLASS_NAME + ";");
                this.mv.visitLdcInsn(name1);
                this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ProfileClassWriter.PROFILER_CLASS_NAME, "push", "(Ljava/lang/String;)V", false);
            }
        }

        @Override
        public void visitInsn(int opcode) {
            if (annotationExists && (opcode <= RETURN && opcode >= IRETURN)) {
                this.mv.visitFieldInsn(Opcodes.GETSTATIC, ProfileClassWriter.PROFILER_CLASS_NAME, "PROFILER", "L" + ProfileClassWriter.PROFILER_CLASS_NAME + ";");
                this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, ProfileClassWriter.PROFILER_CLASS_NAME, "pop", "()V", false);
                this.mv.visitMaxs(0, 0);
                this.mv.visitEnd();
            }
            super.visitInsn(opcode);
        }
    }

    public static class ProfileAnnotationVisitor extends AnnotationVisitor {
        private final ProfileMethodVisitor nameHolder;

        public ProfileAnnotationVisitor(AnnotationVisitor visitor, ProfileMethodVisitor nameHolder) {
            super(Opcodes.ASM9, visitor);
            this.nameHolder = nameHolder;
        }

        @Override
        public void visit(String name, Object value) {
            if (name.equals("value")) {
                nameHolder.annotationValue = (String) value;
            }
            super.visit(name, value);
        }
    }
}
