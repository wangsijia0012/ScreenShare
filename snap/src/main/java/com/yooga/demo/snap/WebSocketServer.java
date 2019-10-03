package com.yooga.demo.snap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileCacheImageOutputStream;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.FileSystem;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by yooga on 2018/10/11.
 * ServerEndpoint自定义访问路径
 */
@ServerEndpoint("/websocket")
@Component
public class WebSocketServer {
    //静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。可选
    private static int onlineCount = 0;
    private static Robot robot;
    private static int trueScreen = 0;

    static {
        try {
            Scanner scanner = new Scanner(System.in);
            System.err.println("请输入清晰度在0-5之间：");
            trueScreen=scanner.nextInt(6);
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private static Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    //获取主屏幕的大小
    private static Rectangle rect = new Rectangle(screen);


    public WebSocketServer() throws AWTException {

    }

    //concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。必须
    private static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();

    //与某个客户端的连接会话，需要通过它来给客户端发送数据。必须
    private Session session;



    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        webSocketSet.add(this);     //加入set中
        addOnlineCount();           //在线数加1
        System.out.println("有新窗口开始监听,当前在线人数为" + getOnlineCount());
        try {
//           sendMessage("连接成功");
        } catch (Exception e) {
            System.out.println("WebSocket IO异常");
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);  //从set中删除
        subOnlineCount();           //在线数减1
        System.out.println("有连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        while (true) {
            ByteArrayOutputStream baos = null;
            //群发消息
            for (WebSocketServer item : webSocketSet) {
                try {

                    BufferedImage img = robot.createScreenCapture(rect);
                    InputStream inputStream = this.getClass().getResourceAsStream("/static/point.png");
                    BufferedImage cursor = ImageIO.read(inputStream);  //把鼠标加载到缓存中


                    Point p = MouseInfo.getPointerInfo().getLocation();               //获取鼠标坐标
                    img.createGraphics().drawImage(cursor, p.x, p.y, null);          //在图片画上鼠标

                    BufferedImage bufferedImage = zoomOutImage(img, 1);

                    baos = new ByteArrayOutputStream();//io流
                    ImageIO.write(bufferedImage, "png", baos);//写入流中
                    byte[] bytes = baos.toByteArray();//转换成字节
                    BASE64Encoder encoder = new BASE64Encoder();
                    String png_base64 = encoder.encodeBuffer(bytes).trim();//转换成base64串
                    png_base64 = png_base64.replaceAll("\n", "").replaceAll("\r", "");//删除 \r\n

                    item.sendMessage(png_base64);


                } catch (IOException ioe) {
                    ioe.printStackTrace();
                    System.out.println("连接断开");
                } finally {
                    if (baos != null) {
                        try {
                            baos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }


            }
        }
    }

    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        System.out.println("发生错误");
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }
    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message) throws IOException {
        System.out.println("推送消息内容:" + message);
        for (WebSocketServer item : webSocketSet) {
            try {
                item.sendMessage(message);
            } catch (IOException e) {
                continue;
            }
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }



    private static void ImageToBase64(String imgPath) {
        byte[] data = null;
        // 读取图片字节数组
        try {
            InputStream in = new FileInputStream(imgPath);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 对字节数组Base64编码
        BASE64Encoder encoder = new BASE64Encoder();
        // 返回Base64编码过的字节数组字符串
        System.out.println("本地图片转换Base64:" + encoder.encode(Objects.requireNonNull(data)));
    }




    /**
     * 对图片进行缩小
     *
     * @param originalImage 原始图片
     * @param timeshieght 缩小倍数
     * @return 缩小后的Image
     */
    public static BufferedImage zoomOutImage(BufferedImage originalImage, int timeshieght)
    {
        int width = originalImage.getWidth() / timeshieght;
        int height = originalImage.getHeight() / timeshieght;
        BufferedImage newImage = new BufferedImage(width, height, originalImage.getType());
        Graphics g = newImage.getGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        return newImage;
    }
}