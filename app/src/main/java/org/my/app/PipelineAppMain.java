/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat and individual contributors
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

package org.my.app;

import org.my.pipeline.core.PipelineProcessor;
import org.my.pipeline.core.SinkProcessor;
import org.my.pipeline.core.SourceProcessor;
import org.my.pipeline.impl.FileSource;
import org.my.pipeline.impl.FileSink;
import org.my.pipeline.impl.PatternReplacer;
import org.my.pipeline.impl.TraceProcessor;

import java.io.IOException;

/**
 * A simple application which uses a FileSource and FileSink process
 * to copy a file, reading and writing the contents in parallel.
 *
 * Each element of the pipeline is a Thread which runs independently
 * of the other elements. Pipeline elements are linked in a dataflow
 * by means of a PipedWriter written by the upstream SourceProecssor
 * connected to a PipedReader read by the downstream SinkProcessor.
 * 
 * FileSource is a SourceProcessor which streams data from a disk
 * file into the head of a pipeline.
 * 
 * FileSink is a SinkProcessor which sits at the end of a pipeline
 * and collects upstream data into a disk file.
 * 
 * Between the FileSource and FileSink is a TraceProcessor. It is a
 * PipelineProcessor which acts both as a Sink, receiving file data
 * from upstream, and as a Source, feeding data downstream.
 * 
 * TraceProcessor actually subclasses TextLineProcessor which specialises
 * PipelineProcessor, reading input text a line at a time, transforming it,
 * and writing the modified text to the output stream, repeating until
 * the input stream is exhausted.
 * 
 * Subclasses of TextLineProcessor simply implement abstract method
 * transform which takes a line of text as (a String) input and returns
 * a transformed (String) version of the line to be passed downstream
 * as the output. The implementation in TraceLineProcessor always returns
 * the original line unmodified. However, as a side effect it also prints
 * the line to System.out, enabling you to see what data is passing through
 * the pipeline.
 */
public class PipelineAppMain
{
    public static void main(String[] args)
    {
        try {
        	// create all the elements of the pipeline in order
        	// from start to finish
            // a file source streams file foo.txt into the pipeline
            SourceProcessor fileSource = new FileSource("foo.txt");
            // a trace processor traces the data streamed through
            // the pipeline by the file source.
            PipelineProcessor tracer = new TraceProcessor("*** ", fileSource);
            //  a file sink streams the output to file bar.txt
            SinkProcessor fileSink = new FileSink("bar.txt", tracer);
            // start all the processors
            fileSource.start();
            tracer.start();
            fileSink.start();
            // wait for all the processors to finish
            fileSource.join();
            tracer.join();
            fileSink.join();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
