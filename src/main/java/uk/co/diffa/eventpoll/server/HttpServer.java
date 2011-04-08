package uk.co.diffa.eventpoll.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.event.EventUtil;
import org.apache.sling.event.JobProcessor;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @scr.component immediate="true"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.property name="event.topics" valueRef="org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_ADDED"
 */
public class HttpServer implements JobProcessor, EventHandler {
    private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

    /** Server Channel */
	private Channel channel;
    /** Active client channel set */
	private ChannelGroup clientChannels;


    /**
     * Called on OSGi service startup
     * @param ctx
     */
	protected void activate(ComponentContext ctx) {
        start();
	}

    private void start() {
        log.info("START");
        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        clientChannels = new DefaultChannelGroup();
        // Set up the event pipeline factory.
        bootstrap.setPipelineFactory(new HttpServerPipelineFactory(clientChannels));

        // Bind and start to accept incoming connections.
        channel = bootstrap.bind(new InetSocketAddress(8080));
    }

    /**
     * Called on OSGi service shutdown
     * @param ctx
     */
	protected void deactivate(ComponentContext ctx) {
		log.info("STOP");
		clientChannels.close();
		channel.close();
	}

    /**
     * Send the data chunk to all of the connected clients
     *
     * @param propPath
     */
    private void writeToAllClients(String propPath) {
        for (Channel channel : clientChannels ) {
            if (channel.isBound() && channel.isConnected() && channel.isOpen() && channel.isWritable()) {
                writeChunk(channel, propPath);
            }
        }
    }

    /**
     * Write a chunk to a channel
     * @param channel
     * @param chunk
     * @return
     */
    private void writeChunk(Channel channel, String chunk) {
        String msg = new StringBuilder(chunk).append("\n").toString();
        ChannelBuffer buf = ChannelBuffers.copiedBuffer(msg.getBytes());
        channel.write(new DefaultHttpChunk(buf));
    }

    /**
     * Handle the OSGi event
     * @param event
     */
    public void handleEvent(Event event) {
		log.debug("HANDLING EVENT");

		if (EventUtil.isLocal(event)) {
	        EventUtil.processJob(event, this);
	    }
	}

    /**
     * Process the Sling Job Event
     * @param event
     * @return
     */
    public boolean process(Event event) {
        log.debug("PROCESSING JOB : RESOURCE_ADDED");
        String propPath = (String) event.getProperty(SlingConstants.PROPERTY_PATH);

        writeToAllClients(propPath);

        return false;
    }

}