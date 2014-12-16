/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @authors Andrew Dinn
 */

package org.my.pipeline.core;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

/**
 * A SourceProcessor sits at the front of a processor pipeline feeding
 * its downstream sink with a data stream derived from data located
 * in memory or on some persistent storage medium.
 */

public abstract class SourceProcessor extends Thread implements Source {
	/**
	 * the stream of data produced by this source
	 */
	protected PipedWriter output;

	protected SourceProcessor() {
		output = null;
	}

	
	public void feed(Sink sink) throws IOException {
		if (output != null) {
			throw new IOException("output already connected");
		}
		output = new PipedWriter();
		sink.setInput(new PipedReader(output));
	}

    /**
     * method implemented by subclasses which produces the data
     * needed by a downstream source
     * 
     * @throws IOException
     */
	public abstract void produce() throws IOException;

	public void run() {
		if (output==null) {
			//nothing to do
			return;
		}
		try {
			produce();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			try {
				output.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
	}
}
