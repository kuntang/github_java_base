package com.java.base.nio.filechannel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tangkun.tk on 2015/12/3.
 * nio 同步数据client
 */
public class NioSyncDataClient {
    private int port = 9026;
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        NioSyncDataClient sfc = new NioSyncDataClient();
        sfc.receiveData(args[0]);
        long end = System.currentTimeMillis();
        System.out.println("耗时" + (end - start) + "毫秒" );
    }
    public void receiveData(String ip) throws IOException {
        SocketAddress sad = new InetSocketAddress(ip, port);
        SocketChannel sc = SocketChannel.open();
        System.out.println("Accepted : "+sc);
        sc.connect(sad);
        sc.configureBlocking(true);

        ByteBuffer fileNumBuff = ByteBuffer.allocate(8);
        sc.read(fileNumBuff);
        fileNumBuff.flip();
        int fileNum = fileNumBuff.getInt();
        int fileMetaBufferSize = fileNumBuff.getInt();

        ByteBuffer fileMetaBuffer  = ByteBuffer.allocate(fileMetaBufferSize);
        sc.read(fileMetaBuffer);
        fileMetaBuffer.flip();
        long remain = 0;
        Map<String,Long> fileMetaMap = new HashMap<String,Long>();
        for (int i=0;i<fileNum;i++){
            long fileNameLength = fileMetaBuffer.getLong();
            long fileLength = fileMetaBuffer.getLong();
            byte[] fileNameBytes = new byte[(int)fileNameLength];
            fileMetaBuffer.get(fileNameBytes);
            String fileName2 = new String(fileNameBytes);
            fileMetaMap.put(fileName2,fileLength);
        }

        for(Map.Entry<String,Long> entry: fileMetaMap.entrySet()){
            remain = entry.getValue();
            transferFrom(entry.getKey(),sc,remain);
        }
        System.out.println("ok!");
    }

//    public static void readBytes(FileChannel fc,ByteBuffer buf,SocketChannel sc,long remain) throws IOException {
//        long size = 0;
//        while( (size = sc.read(buf)) >= 0){
//            buf.flip();
//            fc.write(buf);
//            buf.clear();
//            if(remain - buf.capacity() >= buf.capacity() ){
//                remain -= buf.capacity();
//            }else if(remain-buf.capacity() > 0){
//                remain -= buf.capacity();
//                buf = ByteBuffer.allocate((int)remain);
//            }else{
//                break;
//            }
//        }
//    }

    public static void transferFrom(String fileName,SocketChannel sc,long count) throws IOException {
        ensureDirExist(fileName);
        FileChannel fc = new FileOutputStream(fileName).getChannel();
        fc.transferFrom(sc,0,count);
    }

    public static boolean ensureDirExist(String fileName) {
        String dirPath = fileName.substring(0, fileName.lastIndexOf(File.separator));
        File file = new File(dirPath);
        if (file.isFile()) {
            throw new IllegalArgumentException("文件名解析出错,请关注");
        }
        return file.exists() || file.mkdirs();
    }

}
