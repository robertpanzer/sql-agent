package foo.bar;

import javassist.NotFoundException;
import javassist.expr.MethodCall;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This annotation defines the method call to be replaced.
 * To replace method calls to {@code String.equals()} add the following annotation to a method that takes a
 * {@link MethodCall} as parameter:
 * <p>
 * <code>
 *     <pre>
 * &#64;ProcessMethodCall(targetClass = String.class, methodName = "equals")
 * public void processStringEquals(MethodCall methodCall) {
 *     methodCall.replace(...);
 * }
 *     </pre>
 * </code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ProcessMethodCall {


    /**
     * @return The target class of the method call to process.
     */
    Class<?> targetClass();

    /**
     * @return A string with regular expression over the method name to match.
     */
    String methodName();

    static class ResolvedProcessMethodCall {

        private final Object processor;
        private final Method processMethod;
        private Class<?> targetClass;
        
        private final Pattern methodName;

        ResolvedProcessMethodCall(ProcessMethodCall annotation, Object processor, Method processMethod) {
            this.methodName = Pattern.compile(annotation.methodName());
            this.targetClass = annotation.targetClass();
            
            this.processMethod = processMethod;
            this.processor = processor;
        }

        public boolean process(MethodCall methodCall) throws InvocationTargetException, IllegalAccessException, NotFoundException {

            try {
                if (!methodCall.getClassName().equals(targetClass.getName())) {
                    return false;
                }
            } catch (Exception e) {
                // Don't know yet why an NPE sometimes appears here
                return false;
            }
            if (methodName == null || !methodName.matcher(methodCall.getMethod().getName()).matches()) {
                return false;
            }

            processMethod.invoke(processor, methodCall);
            return true;
        }


    }
    
}
