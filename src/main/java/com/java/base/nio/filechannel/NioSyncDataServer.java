package com.java.base.nio.filechannel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by tangkun.tk on 2015/12/3.
 * nio 同步数据server
 */
public class NioSyncDataServer {
    private int port = 9026;
    ServerSocketChannel listener = null;
    protected void mySetup(){
        InetSocketAddress listenAddr =  new InetSocketAddress(port);
        try {
            listener = ServerSocketChannel.open();
            ServerSocket ss = listener.socket();
            ss.setReuseAddress(true);
            ss.bind(listenAddr);
            System.out.println("Listening on port : "+ listenAddr.toString());
        } catch (IOException e) {
            System.out.println("Failed to bind, is port : "+ listenAddr.toString()
                    + " already in use ? Error Msg : "+e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendData(final String fileName){
        try {
            while(true) {
                final SocketChannel conn = listener.accept();
                System.out.println("Accepted : "+conn);
                conn.configureBlocking(true);
                new Thread(new Runnable() {
                    public void run() {
                        send(conn,fileName);
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(SocketChannel conn,String directory) {
        try {
            List<File> filePaths = findAllFile(directory);
            ByteBuffer dirMetaBuffer  = genByteBuffer(directory);
            ByteBuffer fileNumsBuffer = ByteBuffer.allocate(8);
            fileNumsBuffer.putInt(filePaths.size());
            fileNumsBuffer.putInt(dirMetaBuffer.capacity());
            fileNumsBuffer.flip();
            conn.write(fileNumsBuffer);
            conn.write(dirMetaBuffer);
            for (File subFile : filePaths) {
                FileChannel fc = new FileInputStream(subFile).getChannel();
                long curnset = fc.transferTo(0, fc.size(), conn);
            }
            conn.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查找某目录下所有的文件
     * @param directory 目录的绝对路径
     * @return 文件列表
     */
    public static List<File> findAllFile(String directory){
        File file = new File(directory);
        if(file.isFile()){
            return Collections.singletonList(file);
        }

        File[] subFiles = file.listFiles();
        if( subFiles == null ){
            return Collections.emptyList();
        }

        List<File> list = new ArrayList<File>();
        for(File subFile : subFiles){
            if(subFile.isDirectory()){
                list.addAll(findAllFile(subFile.getAbsolutePath()));
            }else{
                list.add(subFile);
            }
        }
        return list;
    }

    public static ByteBuffer genByteBuffer(String directoryPath){
        File dir = new File(directoryPath);
        if ( !dir.isDirectory() ) {
            System.out.println("fileName must be a directory");
            throw new IllegalArgumentException("必须是目录,不能是文件"+directoryPath);
        }
        List<File> filePaths = findAllFile(directoryPath);
        if(filePaths.isEmpty()){
            throw new IllegalArgumentException("空目录"+directoryPath);
        }
        long total = 0;
        Map<String,Long> fileMetadataMap = new HashMap<String,Long>();
        for (File subFile : filePaths) {
            String path = subFile.getAbsolutePath();
            long length = subFile.length();
            total += ( 8 + 8 + path.length() );
            fileMetadataMap.put(path,length);
        }
        ByteBuffer buffer = ByteBuffer.allocate((int)total);
        for(Map.Entry<String,Long> entry : fileMetadataMap.entrySet()){
            /*
             fileNameLength，fileLength，fileName
              */
            buffer.putLong(entry.getKey().length());
            buffer.putLong(entry.getValue());
            buffer.put(entry.getKey().getBytes());
        }
        buffer.flip();
        return buffer;
    }


    public static void main(String[] args)
    {
        NioSyncDataServer dns = new NioSyncDataServer();
        dns.mySetup();
        dns.sendData( args[0] );
    }

}
