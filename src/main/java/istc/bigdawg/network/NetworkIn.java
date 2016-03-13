/**
 * 
 */
package istc.bigdawg.network;

import static istc.bigdawg.network.NetworkUtils.THIS_HOST_ADDRESS;
import static istc.bigdawg.network.NetworkUtils.deserialize;
import static istc.bigdawg.network.NetworkUtils.serialize;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import istc.bigdawg.LoggerSetup;
import istc.bigdawg.exceptions.NetworkException;
import istc.bigdawg.properties.BigDawgConfigProperties;

/**
 * Receive a message from the network and execute the received object.
 * 
 * @author Adam Dziedzic
 */
public class NetworkIn implements Runnable {

	/* log */
	private static Logger log = Logger.getLogger(NetworkIn.class);

	public void receive() {
		log.debug("network in: start listening for requests");
		ZMQ.Context context = ZMQ.context(1);

		// Socket to talk to clients
		ZMQ.Socket responder = context.socket(ZMQ.REP);
		try {
			String fullAddress = "tcp://" + BigDawgConfigProperties.INSTANCE.getGrizzlyIpAddress() + ":"
					+ BigDawgConfigProperties.INSTANCE.getNetworkMessagePort();
			log.debug(fullAddress);
			responder.bind(fullAddress);

			while (!Thread.currentThread().isInterrupted()) {
				// Wait for next request from the client
				byte[] requestBytes = responder.recv(0);
				if (requestBytes == null) {
					log.error("ZeroMQ: The message was not received properly!");
				}
				Object requestObject = null;
				try {
					requestObject = deserialize(requestBytes);
					NetworkObject requestCommand = (NetworkObject) requestObject;
					Object result = requestCommand.execute();
					// Send reply back
					boolean isSuccess = responder.send(serialize(result), 0);
					if (!isSuccess) {
						log.error("ZeroMQ: The response was not sent properly!");
					}
				} catch (NetworkException ex) {
					String message = "The request could not be processed properly! " + ex.getMessage();
					handleException(message, ex, responder);
				} catch (Exception ex) {
					ex.printStackTrace();
					String message = "The request command could not be executed on the remote server (host: "
							+ THIS_HOST_ADDRESS + "). " + ex.getMessage();
					handleException(message, ex, responder);
				}
			}
		} finally {
			responder.close();
			context.term();
		}
	}

	/**
	 * Handler the exception from the network.
	 * 
	 * @param message
	 * @param ex
	 * @param responder
	 */
	private void handleException(String message, Exception ex, ZMQ.Socket responder) {
		log.error(message);
		byte[] exBytes = message.getBytes();
		/* try to send the exception message */
		try {
			exBytes = serialize(ex);
		} catch (NetworkException exSerialize) {
			exSerialize.printStackTrace();
			log.error(exSerialize.getMessage());
		}
		boolean isSuccess = responder.send(exBytes, 0);
		if (!isSuccess) {
			log.error("ZeroMQ: The response (from NetworkException) was not sent properly!");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		new NetworkIn().receive();
	}

	public static void main(String[] args) throws Exception {
		LoggerSetup.setLogging();
		System.out.println("network in: start listening for requests");
		new NetworkIn().receive();
		// ZMQ.Context context = ZMQ.context(1);
		//
		// // Socket to talk to clients
		// ZMQ.Socket responder = context.socket(ZMQ.REP);
		// responder.bind("tcp://*:5555");
		//
		// while (!Thread.currentThread().isInterrupted()) {
		// // Wait for next request from the client
		// byte[] request = responder.recv(0);
		// System.out.println("Received Hello: " + new String(request));
		//
		// // Do some 'work'
		// Thread.sleep(1000);
		//
		// // Send reply back to client
		// String reply = "World";
		// responder.send(reply.getBytes(), 0);
		// }
		// responder.close();
		// context.term();
	}

}