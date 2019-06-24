package com.edu.uestc.RelationTag;


import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TagServer {
    private Map<SocketChannel, String> map = new HashMap<SocketChannel, String>();


    public void initsocket(){
        ServerSocketChannel ssc=null;
        Selector selector=null;
        Iterator<SelectionKey> iter=null;
        try {
            ssc = ServerSocketChannel.open();
            ssc.socket().setReuseAddress(true);
            ssc.socket().bind(new InetSocketAddress(59996));
            selector = Selector.open();
            ssc.configureBlocking(false);
            ssc.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("...................服务端已启动.......................");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        while (true) {
            try {
                int keys = selector.select(1000);
                if (keys <= 0)
                    continue;
                iter=null;
                Set<SelectionKey> set_key = selector.selectedKeys();
                iter= set_key.iterator();
            } catch (Exception e) {
                // TODO: handle exception
            }
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();
                if (key.isAcceptable()) {
                    try {
                        SocketChannel client = ssc.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                    } catch (ClosedChannelException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                } else if (key.isReadable()) {
                    SocketChannel client = (SocketChannel) key.channel();
                    ByteBuffer dst = ByteBuffer.allocate(4094);
                    StringBuffer sb = new StringBuffer();
                    int readsize = -1;
                    try {
                        while ((readsize = client.read(dst)) > 0) {
                            dst.flip();
                            sb.append(new String(dst.array(), 0, readsize));
                            dst.clear();
                            if (readsize < 0) {//对端关闭，read 返回-1，没有数据返回0，如果客户端一直没有发送数据则isReadable事件激发不了。
                                key.cancel();//取消select 监听并关闭服务端socket.
                                client.close();
                                continue;
                            }
                            System.out.println("server read : "+sb.toString());

                            //TODO 胡某人做实体组合
                            //TODO 组合完毕生成AimData类
                            //TODO AimData生成json字符串
//                            Client clienttmp = new Client(sb.toString());
//                            clienttmp.run();
//                            AimData data = clienttmp.getResult();
                            AimData data = Tag.zwCreateAimData();
                            String datajson = JSON.toJSONString(data);
                            map.put(client, datajson);
                            client.register(selector, SelectionKey.OP_WRITE);
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                } else if (key.isWritable()) {
                    System.out.println("coming write\n");
                    SocketChannel client = (SocketChannel) key.channel();
                    String req = map.get(client);
                    if(req==null) {
                        continue;
                    }
                    System.out.println("ready write len"+req.length());
//                    int wcode=client.write(ByteBuffer.wrap(httpResponse.getBytes()));
                    try {//捕捉异常，客户端端可能关闭导致写异常。

                        int wcode=client.write(ByteBuffer.wrap(req.getBytes()));
                        System.out.println("write code "+wcode);
                        client.register(selector, SelectionKey.OP_READ);
                    } catch (Exception e) {
                        // TODO: handle exception
                        map.remove(client);
                        key.cancel();
                        System.out.println("client is closed!");
                        try {
                            client.close();
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}