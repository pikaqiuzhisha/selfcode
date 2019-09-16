/**
 * 
 */
package com.chargedot.refund.handler;

import com.chargedot.refund.handler.request.Request;
import com.chargedot.refund.util.SpringBeanUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;

/**
 * @author gmm
 *
 */
@Slf4j
public class RequestProcessor implements Runnable {

	/**
	 * should stop
	 */
	private volatile boolean shouldStop = false;
	/**
	 * queue to accept data packet
	 */
	private BlockingQueue<Request> queue;
	/**
	 * thread 
	 */
	private volatile Thread currentThread;
	
	/**
	 * 
	 */
	public RequestProcessor(BlockingQueue<Request> queue) {
		this.queue = queue;
	}
	
	/**
	 * try to stop
	 */
	public void stop() {
		shouldStop = true;
	}

	@Override
	public void run() {
		currentThread = Thread.currentThread();
		long id = currentThread.getId();
		String name = currentThread.getName();
		log.info("[thread(" + id + "," + name + ")]" + "request handler start");

		while (!shouldStop && !Thread.currentThread().isInterrupted()) {
			try {
				Request event = queue.take();
				SpringBeanUtil.getBean(RequestHandler.class).handle(event);
			} catch (InterruptedException e) {
				log.warn("[thread(" + id + "," + name + ")]" + "request handler interrupt", e);
				break;
			} catch (Exception e) {
				log.warn("[thread(" + id + "," + name + ")]" + "request handler exception", e);
			}
		}
		log.info("[thread(" + id + "," + name + ")]" + "request handler stop");
	}

}