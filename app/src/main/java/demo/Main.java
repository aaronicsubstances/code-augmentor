package demo;

import common.Descriptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
	private static final Logger LOG = LoggerFactory.getLogger(Main.class.getName());
	
	public static void main(String[] args) {
		if (LOG.isInfoEnabled()) {
			LOG.info("Hello, from java {} gradle build", Descriptor.description());
		}
	}
}