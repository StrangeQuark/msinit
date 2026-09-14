package com.example.gatewayservice.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/*
    This file is for redirecting traffic from http (port 8080) to https (port 443)
    It is commented out for development but should be made active when doing a prod deployment
 */
@Configuration
public class HttpRedirectConfig {
//    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRedirectConfig.class);
//
//    @Autowired
//    private HostsConfig hostsConfig;
//
//    @PostConstruct
//    public void startRedirectServer() {
//        new Thread(() -> {
//            LOGGER.info("Start a new thread for https redirect server");
//
//            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
//            EventLoopGroup workerGroup = new NioEventLoopGroup();
//            try {
//                ServerBootstrap b = new ServerBootstrap();
//                b.group(bossGroup, workerGroup)
//                        .channel(NioServerSocketChannel.class)
//                        .childHandler(new ChannelInitializer<>() {
//                            @Override
//                            protected void initChannel(Channel ch) {
//                                LOGGER.info("Begin channel initialization");
//                                ChannelPipeline p = ch.pipeline();
//                                p.addLast(new HttpServerCodec());
//                                p.addLast(new HttpObjectAggregator(65536));
//                                p.addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
//                                    @Override
//                                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
//                                        LOGGER.info("Being channel read");
//                                        String host = req.headers().get(HttpHeaderNames.HOST);
//
//                                        // If the host is not null and contains a port, remove the port
//                                        if (host != null && host.contains(":")) {
//                                            host = host.substring(0, host.indexOf(":"));
//                                        }
//
//                                        // If the host in the request header is null or not in the valid hosts list
//                                        // then we redirect to the main host
//                                        if(host == null || !hostsConfig.getHosts().values().contains(host.toLowerCase())) {
//                                            LOGGER.error("Host " + host + " is not a valid host, redirecting to main host");
//                                            host = hostsConfig.getHosts().get("main").toLowerCase();
//                                        }
//
//                                        String redirectUrl = "https://" + host + req.uri();
//
//                                        FullHttpResponse response = new DefaultFullHttpResponse(
//                                                HttpVersion.HTTP_1_1,
//                                                HttpResponseStatus.MOVED_PERMANENTLY,
//                                                Unpooled.EMPTY_BUFFER
//                                        );
//                                        response.headers().set(HttpHeaderNames.LOCATION, redirectUrl);
//                                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
//                                    }
//                                });
//                            }
//                        });
//
//                LOGGER.info("Bind to port 8080");
//                b.bind(8080).sync().channel().closeFuture().sync();
//            } catch (InterruptedException e) {
//                LOGGER.error("Thread interrupted");
//                LOGGER.error(e.getMessage());
//                Thread.currentThread().interrupt();
//            } finally {
//                LOGGER.info("Shutdown after completion");
//                bossGroup.shutdownGracefully();
//                workerGroup.shutdownGracefully();
//            }
//        }).start();
//    }
}