# github_java_base
java 世界里一些比较有意思的代码记录下来。
分为几个大类:

一, juc 并发工具包
java.util.concurrent.tools 并发包里的一些有工具类,如：

1) countDownLath.
2) Semaphore.
3) CyclicBarrier.

java.util.tools 里面有意思的集合:

1) ConcurrentSkipListMap 跳表等.

二： 死锁
列举了死锁的几种场景,并编码实现解决

三： jvm的calssLoader
实现了一个自己的ClassLoader，可以实现动态加载类的功能.

四： nio的一些特性探索
java nio的一些特性实践，如FileChannel的transTo 和 transFrom 特性.
实现了Server 和 client的数据传输代码,利用linux 底层的sendFile特性,实现零拷贝传输数据,非常cool.
