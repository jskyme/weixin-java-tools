package me.chanjar.weixin.cp.demo;

import me.chanjar.weixin.cp.api.*;
import me.chanjar.weixin.cp.bean.WxCpXmlMessage;
import me.chanjar.weixin.cp.bean.WxCpXmlOutMessage;
import me.chanjar.weixin.cp.bean.WxCpXmlOutTextMessage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.Map;

public class WxCpDemoServer {

  private static WxCpConfigStorage wxCpConfigStorage;
  private static WxCpService wxCpService;
  private static WxCpMessageRouter wxCpMessageRouter;

  public static void main(String[] args) throws Exception {
    initWeixin();

    Server server = new Server(8080);

    ServletHandler servletHandler = new ServletHandler();
    server.setHandler(servletHandler);

    ServletHolder endpointServletHolder = new ServletHolder(new WxCpEndpointServlet(wxCpConfigStorage, wxCpService, wxCpMessageRouter));
    servletHandler.addServletWithMapping(endpointServletHolder, "/*");

    ServletHolder oauthServletHolder = new ServletHolder(new WxCpOAuth2Servlet(wxCpService));
    servletHandler.addServletWithMapping(oauthServletHolder, "/oauth2/*");

    server.start();
    server.join();
  }

  private static void initWeixin() {
    try {
      InputStream is1 = ClassLoader.getSystemResourceAsStream("test-config.xml");
      WxCpDemoInMemoryConfigStorage config = WxCpDemoInMemoryConfigStorage.fromXml(is1);

      wxCpConfigStorage = config;
      wxCpService = new WxCpServiceImpl();
      wxCpService.setWxCpConfigStorage(config);

      WxCpMessageHandler handler = new WxCpMessageHandler() {
        @Override
        public WxCpXmlOutMessage handle(WxCpXmlMessage wxMessage, Map<String, Object> context,
            WxCpService wxCpService) {
          WxCpXmlOutTextMessage m = WxCpXmlOutMessage
              .TEXT()
              .content("测试加密消息")
              .fromUser(wxMessage.getToUserName())
              .toUser(wxMessage.getFromUserName())
              .build();
          return m;
        }
      };

      WxCpMessageHandler oauth2handler = new WxCpMessageHandler() {
        @Override
        public WxCpXmlOutMessage handle(WxCpXmlMessage wxMessage, Map<String, Object> context,
            WxCpService wxCpService) {
          String href = "<a href=\"" + wxCpService.oauth2buildAuthorizationUrl(null)
              + "\">测试oauth2</a>";
          return WxCpXmlOutMessage
              .TEXT()
              .content(href)
              .fromUser(wxMessage.getToUserName())
              .toUser(wxMessage.getFromUserName()).build();
        }
      };

      wxCpMessageRouter = new WxCpMessageRouter(wxCpService);
      wxCpMessageRouter
          .rule()
          .async(false)
          .content("哈哈") // 拦截内容为“哈哈”的消息
          .handler(handler)
          .end()
          .rule()
          .async(false)
          .content("oauth")
          .handler(oauth2handler)
          .end()
      ;

    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }
}
