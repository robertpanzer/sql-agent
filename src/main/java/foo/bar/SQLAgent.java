package foo.bar;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.BadBytecode;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * The "main" class of the javaagent.
 */
public class SQLAgent {

    /*
     * To avoid repeated compilation of regexps etc the information of the annotations is
     * resolved and cached in this list.
     */
    private static List<ProcessMethodCall.ResolvedProcessMethodCall> resolvedProcessMethods = new ArrayList<>();


    public static void premain(String agentArgs, Instrumentation instrumentation) throws Exception {
        // Resolve the methods of SQLAgentMethodProcessor and its methods
        // and cache them in the list.
        Class processorClass = SQLAgentMethodProcessor.class;
        for (Method m: processorClass.getDeclaredMethods()) {
            if (m.getAnnotation(ProcessMethodCall.class) != null) {
                try {
                    resolvedProcessMethods.add(new ProcessMethodCall.ResolvedProcessMethodCall(m.getAnnotation(ProcessMethodCall.class), processorClass.newInstance(), m));
                } catch (InstantiationException  | IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }

        // Register the class transformer that processes all JDBC method calls.
        instrumentation.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

                ClassPool cp = ClassPool.getDefault();
                CtClass ctClass = null;
                try {
                    ctClass = cp.makeClass(new ByteArrayInputStream(classfileBuffer));

                    ctClass.defrost();
                    processClass(ctClass);

                    byte[] ret = ctClass.toBytecode();

                    ctClass.detach();

                    return ret;
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        });

    }

    private static void processClass(final CtClass ctClass) throws BadBytecode, CannotCompileException {

        for (CtMethod method: ctClass.getDeclaredMethods()) {

            method.instrument(new ExprEditor() {
                public void edit(MethodCall methodCall) throws CannotCompileException {

                    try {
                        methodCall.getMethodName();
                    } catch (Exception e) {
                        // Sometimes this seems to fail and I don't know why yet.
                        // Handle it gracefully.
                        e.printStackTrace();
                        return;
                    }

                    for (ProcessMethodCall.ResolvedProcessMethodCall processMethodCall: resolvedProcessMethods) {

                        try {
                            processMethodCall.process(methodCall);
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }

                    }
                }
            });
        }
    }

}
