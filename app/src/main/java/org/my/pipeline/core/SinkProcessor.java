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

/**
 * A SinkProcessor sits at the end of a processor pipeline collecting the
 * transformed data stream either in memory or on persistent storage.
 */
public abstract class SinkProcessor extends Thread implements Sink {
	/**
	 * the stream of data consumed by this sink
	 */
    protected PipedReader input;

    protected SinkProcessor(Source source) throws IOException
    {
    	input = null;
        source.feed(this);
    }

    public void setInput(PipedReader input) throws IOException {
        if (this.input != null) {
            throw new IOException("input already connected");
        }
        this.input = input;
    }
    
    /**
     * method implemented by subclasses which consumes the data
     * coming from an upstream source
     * 
     * @throws IOException
     */
    public abstract void consume() throws IOException;

    public void run()
    {
        if (input==null) {
            //nothing to do
            return;
        }

        try {
        	consume();
        } catch (IOException ioe) {
            ioe.printStackTrace();
    	} finally {
    		try {
    			input.close();
    		} catch (IOException ioe) {
    			ioe.printStackTrace();
    		}
    	}
    }
}
