package foo.bar;

import java.lang.reflect.Field;
import java.sql.Statement;

public class ReflectionUtil {

    static Object getField(Statement stmt, String fieldName) throws IllegalAccessException {

        Class<?> clazz = stmt.getClass();

        do {
            try {
                Field f = clazz.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(stmt);
            } catch (NoSuchFieldException e) {
                // No problem atm
            }
        } while (clazz != Object.class);

        return null;
    }

}
