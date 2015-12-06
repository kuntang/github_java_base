package com.java.base.jvm.classloader;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by tangkun.tk on 2015/11/29.
 * 自定义 classLoader
 * 实现类的热替换.
 */
public class MyClassLoader extends URLClassLoader {

    public MyClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, null);
    }

    public MyClassLoader(URL[] urls) {
        super(urls);
    }

    public MyClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, null, factory);
    }

    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException{
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class c = findLoadedClass(name);
            if (c == null) {
                long t0 = System.nanoTime();
                try {
                      c = findClass(name);
                } catch (ClassNotFoundException e) {
                    // ClassNotFoundException thrown if class not found
                    // from the non-null parent class loader
                }

                if (c == null) {
                    // If still not found, then invoke findClass in order
                    // to find the class.
                    // getSystemClassLoader(); 返回的是appclassloader
                    ClassLoader systemClassLoader = getSystemClassLoader();
                    c = systemClassLoader.loadClass(name);
                    // this is the defining class loader; record the stats
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        URL[] urls = getURLs();
        String fileName = name.replace(".", "/") + ".class" ;
        for(URL url : urls){
            File classFile = new File(url.getPath(), fileName);
            if(!classFile.exists()){
                throw new ClassNotFoundException(classFile.getPath() + " 不存在") ;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
            ByteBuffer bf = ByteBuffer.allocate(1024) ;
            FileInputStream fis = null ;
            FileChannel fc = null ;
            try {
                fis = new FileInputStream(classFile) ;
                fc = fis.getChannel() ;
                while(fc.read(bf) > 0){
                    bf.flip() ;
                    bos.write(bf.array(),0 , bf.limit()) ;
                    bf.clear() ;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally{
                try {
                    fis.close() ;
                    fc.close() ;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return defineClass(name,bos.toByteArray() , 0 , bos.toByteArray().length) ;
        }
        return null;
    }



    public static void main(String[] args) throws ClassNotFoundException {
        URL[] urls = new URL[1];
        try {
            URL url = new URL("file:///D:/workspace/runTest/target/classes");
            urls[0] = url;
            MyClassLoader myClassLoader = new MyClassLoader(urls,null);
            System.out.println("MyClassLoader.classLoader="+myClassLoader.getClass().getClassLoader());
            Class c = myClassLoader.loadClass("com.hello.test.A");
            ClassLoader loader = c.getClassLoader();
            System.out.println("loader=myClassLoader ==>"+myClassLoader.equals(loader));
            System.out.println(loader);
            MyClassLoader myClassLoader2 = new MyClassLoader(urls,null);
            Method method = c.getMethod("test1",new Class[]{});
            method.invoke(c.newInstance());
            System.out.println("开始sleep,请替换文件.....");
            Thread.sleep(20 *1000L);

            Class c1 = myClassLoader2.loadClass("com.hello.test.A");
            method = c1.getMethod("test1",new Class[]{});
            method.invoke(c1.newInstance());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }  catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
