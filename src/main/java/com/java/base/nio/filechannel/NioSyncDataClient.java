import com.taobao.ateye.annotation.AteyeInvoker;
import com.taobao.trip.atw.util.CommonSwitchManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * Created by tangkun.tk on 2015/12/3.
 * nio 同步数据client
 */
@Service
public class NioSyncDataClient {

    @Resource
    protected CommonSwitchManager commonSwitchManager;

    private static Set<Integer> groupList = new HashSet<Integer>();

    private static final Logger log = LoggerFactory.getLogger("atw-init");

    static {
        groupList.add(12);
        groupList.add(23);
        groupList.add(34);
        groupList.add(5);
    }

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        NioSyncDataClient sfc = new NioSyncDataClient();
        sfc.receiveData(args[0],groupList);
        long end = System.currentTimeMillis();
        System.out.println("耗时" + (end - start) + "毫秒" );
    }
    public boolean receiveData(String ip,Set<Integer> groupList){
        try{
            int port = commonSwitchManager == null ? 9026 : commonSwitchManager.getShardingPort();
            SocketAddress sad = new InetSocketAddress(ip,port);
            SocketChannel sc = SocketChannel.open();
            log.error("Accepted : "+sc);
            sc.connect(sad);
            sc.configureBlocking(true);

        /* 写入到server 端的group metadata 数组 */
            ByteBuffer groupBufferLength = ByteBuffer.allocate(8);
            groupBufferLength.putLong(groupList.size() * 4);
            groupBufferLength.flip();
            sc.write(groupBufferLength);
            // write buffer(group buffer)
            ByteBuffer groupBuffer = ByteBuffer.allocate(groupList.size() * 4);
            for(int group : groupList){
                groupBuffer.putInt(group);
            }
            groupBuffer.flip();
            sc.write(groupBuffer);

        /* 读取服务端的反馈metadata数据 */
            ByteBuffer fileNumBuff = ByteBuffer.allocate(8);
            sc.read(fileNumBuff);
            fileNumBuff.flip();
            int fileNum = fileNumBuff.getInt();
            int fileMetaBufferSize = fileNumBuff.getInt();
            ByteBuffer fileMetaBuffer  = ByteBuffer.allocate(fileMetaBufferSize);
            while(fileMetaBuffer.remaining() != 0){
                sc.read(fileMetaBuffer);
                log.error("读取file metaData,remaining=[{}],total=[{}]",new Object[]{fileMetaBuffer.remaining(),fileMetaBufferSize});
            }
            fileMetaBuffer.flip();
            long remain = 0;
            // 此处必须用linkedHashMap,防止文件顺序被错乱
            LinkedHashMap<String,Long> fileMetaMap = new LinkedHashMap<String,Long>();
            for (int i=0;i<fileNum;i++){
                long fileNameLength = fileMetaBuffer.getLong();
                long fileLength = fileMetaBuffer.getLong();
                byte[] fileNameBytes = new byte[(int)fileNameLength];
                fileMetaBuffer.get(fileNameBytes);
                String fileName2 = new String(fileNameBytes);
                fileMetaMap.put(fileName2,fileLength);
            }

            log.error("共需要接收[{}]个文件,分别如下：",fileMetaMap.size());
            for(Map.Entry<String,Long> entry : fileMetaMap.entrySet()){
                log.error("文件名=[{}],长度=[{}]",new Object[]{entry.getKey(),entry.getValue()});
            }
            log.error("文件metadata解析结束,开始接收文件内容..");

            for(Map.Entry<String,Long> entry: fileMetaMap.entrySet()){
                log.error("开始接收文件=[{}]",new Object[]{entry.getKey()});
                remain = entry.getValue();
                transferFrom(entry.getKey(),sc,remain);
            }
        }catch (Exception e){
            log.error("",e);
        }
        log.error("ok!");
        return true;
    }

    public  void transferFrom(String fileName,SocketChannel sc,long count) throws IOException {
        ensureDirExist(fileName);
        FileChannel fc = new FileOutputStream(commonSwitchManager.getShardingBaseDirPath()+fileName).getChannel();
        fc.transferFrom(sc,0,count);
    }

    public  boolean ensureDirExist(String fileName) {
        String abPath = commonSwitchManager.getShardingBaseDirPath() + fileName;
        String dirPath = abPath.substring(0, abPath.lastIndexOf(File.separator));
        File file = new File(dirPath);
        if (file.isFile()) {
            throw new IllegalArgumentException("文件名解析出错,请关注");
        }
        return file.exists() || file.mkdirs();
    }

    @AteyeInvoker(description = "启动动态扩容数据传输client",paraDesc = "机器ip")
    public String startNioClient(String ip) throws IOException {
        long start = System.currentTimeMillis();
        NioSyncDataClient sfc = new NioSyncDataClient();
        sfc.receiveData(ip,groupList);
        long end = System.currentTimeMillis();
        log.error("从[{}]拷贝扩容数据,耗时[{}]毫秒",new Object[]{ip,(end - start)} );
        return "ok";
    }

}
