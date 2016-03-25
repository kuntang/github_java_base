package com.java.base.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import sun.misc.Unsafe;

/**
 * 计算java对象占内存的大小
 */
public class ComputeObjSize {

    private static final Unsafe unsafe;

    /** Size of any Object reference */
    private static final int objectRefSize;

    /** Sizes of all primitive values */
    private static final Map<Class, Integer> primitiveSizes;

    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
            objectRefSize = unsafe.arrayIndexScale(Object[].class);
        } catch (Exception e) {
           throw new RuntimeException(e);
        }

        primitiveSizes = new HashMap<Class, Integer>(10);
        primitiveSizes.put(byte.class, 1);
        primitiveSizes.put(char.class, 2);
        primitiveSizes.put(int.class, 4);
        primitiveSizes.put(long.class, 8);
        primitiveSizes.put(float.class, 4);
        primitiveSizes.put(double.class, 8);
        primitiveSizes.put(boolean.class, 1);
    }


            /**
     ?? ? * Get object information for any Java object. Do not pass primitives to
     ?? ? * this method because they will boxed and the information you will get will
     ?? ? * be related to a boxed version of your value.
     ?? ? *
     ?? ? * @param obj
     ?? ? *??????????? Object to introspect
     ?? ? * @return Object info
     ?? ? * @throws IllegalAccessException
     ?? ? */
            public ObjectInfo introspect(final Object obj) throws IllegalAccessException {
            try {
                    return introspect(obj, null);
            } finally { // clean visited cache before returning in order to make
                // this object reusable
                m_visited.clear();
            }
        }

    // we need to keep track of already visited objects in order to support
    // cycles in the object graphs
    private IdentityHashMap<Object, Boolean> m_visited = new IdentityHashMap<Object, Boolean>(100);


    private ObjectInfo introspect(final Object obj, final Field fld)
            throws IllegalAccessException {
        // use Field type only if the field contains null. In this case we will
        // at least know what's expected to be
        // stored in this field. Otherwise, if a field has interface type, we
        // won't see what's really stored in it.
        // Besides, we should be careful about primitives, because they are
        // passed as boxed values in this method
        // (first arg is object) - for them we should still rely on the field
        // type.
        boolean isPrimitive = fld != null && fld.getType().isPrimitive();
        boolean isRecursive = false; // will be set to true if we have already
        // seen this object
        if (!isPrimitive) {
            if (m_visited.containsKey(obj))
                isRecursive = true;
            m_visited.put(obj, true);
        }

        final Class type = (fld == null || (obj != null && !isPrimitive)) ? obj
                .getClass() : fld.getType();
        int arraySize = 0;
        int baseOffset = 0;
        int indexScale = 0;
        if (type.isArray() && obj != null) {
            baseOffset = unsafe.arrayBaseOffset(type);
            indexScale = unsafe.arrayIndexScale(type);
            arraySize = baseOffset + indexScale * Array.getLength(obj);
        }

        final ObjectInfo root;
        if (fld == null) {
            root = new ObjectInfo("", type.getCanonicalName(), getContents(obj,
                    type), 0, getShallowSize(type), arraySize, baseOffset,
                    indexScale);
        } else {
            final int offset = (int) unsafe.objectFieldOffset(fld);
            root = new ObjectInfo(fld.getName(), type.getCanonicalName(),
                    getContents(obj, type), offset, getShallowSize(type),
                    arraySize, baseOffset, indexScale);
        }

        if (!isRecursive && obj != null) {
            if (isObjectArray(type)) {
                // introspect object arrays
                final Object[] ar = (Object[]) obj;
                for (final Object item : ar)
                    if (item != null)
                        root.addChild(introspect(item, null));
            } else {
                for (final Field field : getAllFields(type)) {
                    if ((field.getModifiers() & Modifier.STATIC) != 0) {
                        continue;
                    }
                    field.setAccessible(true);
                    root.addChild(introspect(field.get(obj), field));
                }
            }
        }

        root.sort(); // sort by offset
        return root;
    }

    // get all fields for this class, including all superclasses fields
    private static List<Field> getAllFields(final Class type) {
    if (type.isPrimitive())
        return Collections.emptyList();
        Class cur = type;
        final List<Field> res = new ArrayList<Field>();
        while (true) {
            Collections.addAll(res, cur.getDeclaredFields());
            if (cur == Object.class)
            break;
            cur = cur.getSuperclass();
        }
        return res;
    }

    // check if it is an array of objects. I suspect there must be a more
    // API-friendly way to make this check.
    private static boolean isObjectArray(final Class type) {
    if (!type.isArray())
       return false;
     if (type == byte[].class || type == boolean[].class
        || type == char[].class || type == short[].class
        || type == int[].class || type == long[].class
        || type == float[].class || type == double[].class)
        return false;
       return true;
     }

    // advanced toString logic
    private static String getContents(final Object val, final Class type) {
        if (val == null)
            return "null";
        if (type.isArray()) {
            if (type == byte[].class)
                return Arrays.toString((byte[]) val);
            else if (type == boolean[].class)
                return Arrays.toString((boolean[]) val);
            else if (type == char[].class)
                return Arrays.toString((char[]) val);
            else if (type == short[].class)
                return Arrays.toString((short[]) val);
            else if (type == int[].class)
                return Arrays.toString((int[]) val);
            else if (type == long[].class)
                return Arrays.toString((long[]) val);
            else if (type == float[].class)
                return Arrays.toString((float[]) val);
            else if (type == double[].class)
                return Arrays.toString((double[]) val);
            else
                return Arrays.toString((Object[]) val);
        }
        return val.toString();
    }

    // obtain a shallow size of a field of given class (primitive or object
    // reference size)
    private static int getShallowSize(final Class type) {
        if (type.isPrimitive()) {
            final Integer res = primitiveSizes.get(type);
            return res != null ? res : 0;
        } else
            return objectRefSize;
    }


    public class ObjectInfo {
        /** Field name */
        public final String name;
        /** Field type name */
        public final String type;
        /** Field data formatted as string */
        public final String contents;
        /** Field offset from the start of parent object */
        public final int offset;
        /** Memory occupied by this field */
        public final int length;
        /** Offset of the first cell in the array */
        public final int arrayBase;
        /** Size of a cell in the array */
        public final int arrayElementSize;
        /** Memory occupied by underlying array (shallow), if this is array type */
        public final int arraySize;
        /** This object fields */
        public final List<ObjectInfo> children;

        public ObjectInfo(String name, String type, String contents, int offset, int length, int arraySize,
                          int arrayBase, int arrayElementSize)
        {
            this.name = name;
            this.type = type;
            this.contents = contents;
            this.offset = offset;
            this.length = length;
            this.arraySize = arraySize;
            this.arrayBase = arrayBase;
            this.arrayElementSize = arrayElementSize;
            children = new ArrayList<ObjectInfo>( 1 );
        }

        public void addChild( final ObjectInfo info )
        {
            if ( info != null )
                children.add( info );
        }

        /**
         * Get the full amount of memory occupied by a given object. This value may be slightly less than
         * an actual value because we don't worry about memory alignment - possible padding after the last object field.
         *
         * The result is equal to the last field offset + last field length + all array sizes + all child objects deep sizes
         * @return Deep object size
         */
        public long getDeepSize()
        {
            //return length + arraySize + getUnderlyingSize( arraySize != 0 );
            return addPaddingSize(arraySize + getUnderlyingSize( arraySize != 0 ));
        }

        long size = 0;

        private long getUnderlyingSize( final boolean isArray )
        {
            //long size = 0;
            for ( final ObjectInfo child : children )
                size += child.arraySize + child.getUnderlyingSize( child.arraySize != 0 );
            if ( !isArray && !children.isEmpty() ){
                int tempSize = children.get( children.size() - 1 ).offset + children.get( children.size() - 1 ).length;
                size += addPaddingSize(tempSize);
            }

            return size;
        }

        private final class OffsetComparator implements Comparator<ObjectInfo>
        {

            public int compare( final ObjectInfo o1, final ObjectInfo o2 )
            {
                return o1.offset - o2.offset; //safe because offsets are small non-negative numbers
            }
        }

        //sort all children by their offset
        public void sort()
        {
            Collections.sort( children, new OffsetComparator() );
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            toStringHelper( sb, 0 );
            return sb.toString();
        }

        private void toStringHelper( final StringBuilder sb, final int depth )
        {
            depth( sb, depth ).append("name=").append( name ).append(", type=").append( type )
                    .append( ", contents=").append( contents ).append(", offset=").append( offset )
                    .append(", length=").append( length );
            if ( arraySize > 0 )
            {
                sb.append(", arrayBase=").append( arrayBase );
                sb.append(", arrayElemSize=").append( arrayElementSize );
                sb.append( ", arraySize=").append( arraySize );
            }
            for ( final ObjectInfo child : children )
            {
                sb.append( '\n' );
                child.toStringHelper(sb, depth + 1);
            }
        }

        private StringBuilder depth( final StringBuilder sb, final int depth )
        {
            for ( int i = 0; i < depth; ++i )
                sb.append( "\t");
            return sb;
        }

        private long addPaddingSize(long size){
            if(size % 8 != 0){
                return (size / 8 + 1) * 8;
            }
            return size;
        }
    }

    public long getObjectSize(Object o) throws IllegalAccessException {
        final ComputeObjSize ci = new ComputeObjSize();
        ObjectInfo res;
        res = ci.introspect(o);
        return  res.getDeepSize();
    }

    // http://blog.csdn.net/xieyuooo/article/details/7068216
}


