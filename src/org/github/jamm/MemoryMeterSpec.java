package org.github.jamm;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Inaccurate guessing. Does not consider any {@code @Contended} or proper, VM dependent field reordering, etc.
 * TL;DR the object sizes are likely incorrect, but hopefully rather too big (better safe than sorry).
 */
final class MemoryMeterSpec extends MemoryMeterRef
{
    private static boolean warned;

    MemoryMeterSpec(Builder builder)
    {
        super(builder, MemoryMeterSpec::sizeOf);
        maybeWarn();
    }

    private static void maybeWarn()
    {
        if (warned)
            return;
        warned = true;
        System.err.println("***********************************************************************************");
        System.err.println("** jamm will GUESS the size of objects on heap. This is wrong and");
        System.err.println("** results in wrong assumptions of the free/occupied Java heap and");
        System.err.println("** potentially in OOMs. The implementation does not always consider");
        System.err.println("** Java object layouts under all circumstances for all JVMs.");
        System.err.println("**");
        System.err.println("** Solutions:");
        System.err.println("** - Use a JDK/JVM with JEP-8249196");
        System.err.println("** - Load jamm as an agent into the JVM");
        System.err.println("***********************************************************************************");
    }

    /*private static long sizeOf(Class<?> type)
    {
        long size = sizeOf(SPEC.getObjectHeaderSize(), type);

        size = roundTo(size, SPEC.getObjectAlignment());

        return size;
    }*/
    // sizeOfInstance from 0.3.1
    private static long sizeOf(Class<?> type) {
        long size = SPEC.getObjectHeaderSize() + sizeOfDeclaredFields(type);
        while ((type = type.getSuperclass()) != Object.class && type != null)
            size += roundTo(sizeOfDeclaredFields(type), SPEC.getSuperclassFieldPadding());
        return roundTo(size, SPEC.getObjectAlignment());
    }


    /*private static long sizeOf(long size, Class<?> type)
    {
        Class<?> superclass = type.getSuperclass();
        if (superclass != Object.class && superclass != null)
            size = sizeOf(size, superclass);

        size = sizeOfDeclaredFields(size, type);

        return size;
    }*/


    /*private static long sizeOfDeclaredFields(long size, Class<?> type)
    {
        boolean any = false;
        for (Field f : type.getDeclaredFields())
        {
            if (!Modifier.isStatic(f.getModifiers()))
            {
                Class<?> t = f.getType();
                if (!any && (t == long.class || !t.isPrimitive() || t == double.class))
                {
                    any = true;
                    size = roundTo(size, SPEC.getObjectAlignment());
                }
                size += sizeOfField(t);
            }
        }
        return size;
    }*/
    private static long sizeOfDeclaredFields(Class<?> type) {
        long size = 0;
        for (Field f : declaredFieldsOf(type))
            size += sizeOf(f);
        return size;
    }

    private static Iterable<Field> declaredFieldsOf(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        for (Field f : type.getDeclaredFields())
        {
            if (!Modifier.isStatic(f.getModifiers()))
                fields.add(f);
        }
        return fields;
    }

    public static int sizeOf(Field field) {
        return sizeOfField(field.getType());
    }


}
