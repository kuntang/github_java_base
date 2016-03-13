import com.taobao.ateye.annotation.AteyeInvoker;
import com.taobao.ateye.annotation.Switch;
import com.taobao.trip.atw.util.CommonSwitchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
@Service
public class NioSyncDataServer {
    private static final Logger log = LoggerFactory.getLogger("atw-init");
    @Resource
    protected CommonSwitchManager commonSwitchManager;

    @Switch(description = "动态扩容数据传输server开关")
    private boolean startNioServer = true;

    @Switch(description = "是否已经启动数据传输server,防止重复启动")
    private boolean yetStarted = false;

    @Switch(description = "动态扩容数据传输server开关")
    private String dirPath = "/home/admin/atw/sharding/";

    ServerSocketChannel listener = null;
    private void init(){
        int port = commonSwitchManager == null ? 9026 : commonSwitchManager.getShardingPort();
        InetSocketAddress listenAddr =  new InetSocketAddress(port);
        try {
            listener = ServerSocketChannel.open();
            ServerSocket ss = listener.socket();
            ss.setReuseAddress(true);
            ss.bind(listenAddr);
            log.error("Listening on port : "+ listenAddr.toString());
        } catch (IOException e) {
            System.out.println("Failed to bind, is port : "+ listenAddr.toString()
                    + "Error Msg : "+e.getMessage());
            log.error("Failed to bind, is port : "+ listenAddr.toString()
                    + "Error Msg : "+e.getMessage());
        }
    }

    private void prepareSendData(){
        try {
            while(startNioServer) {
                final SocketChannel conn = listener.accept();
                log.error("Accepted : "+conn);
                conn.configureBlocking(true);
                Set<Integer> groupList = readGroupMetadataBuffer(conn);
                send(conn,groupList);
            }
        } catch (IOException e) {
            log.error("监听失败",e);
        }
    }

    private Set<Integer> readGroupMetadataBuffer(SocketChannel conn){
        ByteBuffer metadataBufferLength = ByteBuffer.allocate(8);
        try {
            conn.read(metadataBufferLength);
            metadataBufferLength.flip();
            long metadataLength = metadataBufferLength.getLong();
            ByteBuffer metadataBuffer = ByteBuffer.allocate((int)metadataLength);
            if(metadataLength % 4 != 0){
                log.error("group metadata 长度不满足N个int的长度,metadataLength=[{}]",new Object[]{metadataLength});
                return Collections.emptySet();
            }
            while(metadataBuffer.remaining() != 0){
                conn.read(metadataBuffer);
                log.error("metadata remaining=[{}],total=[{}]",new Object[]{metadataBuffer.remaining(),metadataLength});
            }
            metadataBuffer.flip();
            Set<Integer> groupSet = new HashSet<Integer>();
            while(metadataBuffer.remaining() != 0){
                groupSet.add(metadataBuffer.getInt());
            }
            return groupSet;
        } catch (IOException e) {
            log.error("读取group metadata buffer 失败",e);
        }
        return Collections.emptySet();
    }

    private void send(SocketChannel conn,Set<Integer> groupList) {
        String directory = findNewestDir().getAbsolutePath();
        try {
            List<File> filePaths = findAllFile(directory,groupList);
            log.error("共需要传输[{}]个文件,分别是：",filePaths.size());
            for(File file : filePaths){
                log.error("文件名=[{}],文件长度=[{}]",new Object[]{file.getAbsolutePath(),file.length()});
            }
            ByteBuffer dirMetaBuffer  = genByteBuffer(filePaths);
            ByteBuffer fileNumsBuffer = ByteBuffer.allocate(8);
            fileNumsBuffer.putInt(filePaths.size());
            fileNumsBuffer.putInt(dirMetaBuffer.capacity());
            fileNumsBuffer.flip();
            conn.write(fileNumsBuffer);
            conn.write(dirMetaBuffer);
            for (File subFile : filePaths) {
                FileChannel fc = new FileInputStream(subFile).getChannel();
                long transLength = fc.transferTo(0, fc.size(), conn);
                log.error("开始传输文件 file=[{}],fileLength=[{}],transport length=[{}]",new Object[]{subFile.getAbsolutePath(),subFile.length(),transLength});
            }
        } catch (FileNotFoundException e) {
            log.error("FileNotFoundException",e);
        } catch (IOException e) {
            log.error("IOException",e);
        }finally {
            try {
                conn.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 查找某目录下所有的文件
     * @param directory 目录的绝对路径
     * @return 文件列表
     */
    private List<File> findAllFile(String directory,Set<Integer> groupSet){
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
                String groupName = subFile.getName();
                if(groupSet.contains(Integer.parseInt(groupName))){
                    list.addAll(findAllFile(subFile.getAbsolutePath(),groupSet));
                }
            }else{
                list.add(subFile);
            }
        }
        return list;
    }

    private File findNewestDir(){
        File dir = new File(dirPath);
        if(!dir.isDirectory()){
            return null;
        }
        File[] files = dir.listFiles();
        if(files == null || files.length == 0){
            return null;
        }
        long lastModified = 0;
        File lastModifiedFile = null;
        for(File file : files){
            if(file.lastModified() > lastModified){
                lastModified = file.lastModified();
                lastModifiedFile = file;
            }
        }
        return lastModifiedFile;
    }


    private ByteBuffer genByteBuffer(List<File> filePaths){
        if(filePaths.isEmpty()){
            throw new IllegalArgumentException("没有需要传输的文件");
        }
        long total = 0;
        // 此处必须用linkedHashMap,防止文件顺序被错乱
        LinkedHashMap<String,Long> fileMetadataMap = new LinkedHashMap<String,Long>();
        for (File subFile : filePaths) {
            String path = subFile.getAbsolutePath();
            String arr[] = path.split(File.separator);
            if(arr.length >= 2){
                String tempPath = arr[arr.length - 2] + File.separator +arr[arr.length - 1];
                long length = subFile.length();
                total += ( 8 + 8 + tempPath.length() );
                fileMetadataMap.put(tempPath,length);
            }
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
        dns.init();
        if(args != null && args.length>0){
            dns.dirPath = args[0];
        }
        dns.prepareSendData();
    }

    @AteyeInvoker(description = "启动动态扩容数据传输server",paraDesc = "待传输的文件目录路径，绝对路径")
    public void startShardingNioServer()
    {
        if(yetStarted){
            return;
        }
        yetStarted = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                NioSyncDataServer dns = new NioSyncDataServer();
                dns.init();
                dns.prepareSendData();
            }
        }).start();
    }

    @AteyeInvoker(description = "关闭动态扩容数据传输server")
    private void closeShardingNioServer() throws IOException {
        listener.close();
    }

}
