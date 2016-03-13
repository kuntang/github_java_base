# github_java_base
java 世界里一些比较有意思的代码记录下来。
分为几个大类:

一, juc 并发工具包
java.util.concurrent.tools 并发包里的一些有工具类,如：

1) countDownLath

2) Semaphore

3) CyclicBarrier

java.util.tools 里面有意思的集合:

1) ConcurrentSkipListMap 跳表等

二： 死锁

列举了死锁的几种场景,并编码实现解决,如：

1）静态死锁

2）协作间的死锁 

3）饥饿死锁


三： jvm的calssLoader
实现了一个自己的ClassLoader，可以实现动态加载类的功能.

四： nio的一些特性探索

  1) nio FileChannel.transTo 和 transFrom特性实践

  实现了Server 和 client的数据传输代码,利用linux 底层的sendFile特性,实现零拷贝传输数据,非常cool.
  
  遇到的问题：多文件和大文件的数据传输过程中，偶尔会引发传输中断。
  
  解决方案： FileChannel.read(ByteBuffer)时，可能会读取不到完整的ByteBuffer内容，通过fileMetaBuffer.remaining() 

  判断并循环读，直到读满整个buffer为止才返回即可解决上述问题。
  
五：java泛型特性的探索研究

    1) java泛型的实现原理 
  
    2）java泛型的编译和运行过程中的原理。 
  
