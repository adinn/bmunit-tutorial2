/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat and individual contributors as identified
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
import org.my.pipeline.core.TeeProcessor;
import org.my.pipeline.impl.BindingInserter;
import org.my.pipeline.impl.CharSequenceSource;
import org.my.pipeline.impl.CharSequenceSink;
import org.my.pipeline.util.BindingMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This application employs BindingInserter, another subclass of
 * TextLineProcessor, to locate terms in the input text, bind them
 * to unique named variables in a BindingMap and then replace the
 * term with a reference to the variable. So, for example, the
 * first BindingInserter looks for patterns like "the boy", inserts
 * a binding [X1 --> boy] into the binding map and then replaces the
 * bound text with a reference to the variable, "the ${X1}".
 * 
 * If a term is already bound then the existing binding is re-used.
 * Each time a new binding is needed the counter used to generate a
 * unique variable name is incremented. So, the first input line
 * "the boy threw the stick at the boy\n" is transformed by the first
 * BindingInserter to "the ${X1} threw the ${X2} at the ${X1}\n" and
 * the binding map is updated to contain [X1 --> boy, X2 --> stick].
 * 
 * The pipeline is fed by a CharSequenceSource and its final stage
 * is a CharSequenceSink. The former produces its input from a
 * String or some other CharSequence, passed as input when it is
 * created. The second collects its input into a StringBuffer which
 * can be queried after the pipeline has finished execution to find
 * the text length and the characters it contains (CharSequenceSink
 * implements interface CharSequence).
 * 
 * Note that BindingMap object used by the 3 BindingInserter instances
 * breaks the assumption that the 3 processes are independent. It is
 * a shared state which could potentially communicate information between
 * the processes out of order (unlike the pipeline itself which
 * communicates changes to the data stream in order). Given certain
 * data in original data source this may make the outcome of the program
 * depend in the scheduling order of the BindingInserter processes.
 * 
 * For example, if the first and second input lines are reversed then
 * there is a race condition between the first and second BindingInserter
 * to insert a binding for "boy" (convince yourself this is possible even
 * though you are unlikely to ever see it except, maybe , on a very
 * heavily loaded machine).
 */

public class PipelineAppMain2
{
    public static void main(String[] args)
    {
        try {
            BindingMap  bindings = new BindingMap();
            StringBuilder input = new StringBuilder();
            input.append("the boy threw the stick at the boy\n");
            input.append("a boy threw a stick at a window\n");

            // pipeline source is the input char sequence
            CharSequenceSource reader = new CharSequenceSource(input);
            PipelineProcessor[] pipeline = new PipelineProcessor[5];

            // pipeline stage 0 matches "the Aann" replacing it with
            // "the ${Xn}" and binding Xn --> Aann
            pipeline[0] = new BindingInserter("the ([A-Za-z0-9]+)", "X", bindings, reader);
            // pipeline stage 1 tees intermediate output to char sequence sink writer
            pipeline[1] = new TeeProcessor(pipeline[0]);
            // pipeline stage 2 matches "a Aann" replacing it with
            // "a ${Yn}" and binding Yn --> Aann
            pipeline[2] = new BindingInserter("a ([A-Za-z0-9]+)", "Y", bindings, pipeline[1]);
            // pipeline stage 3 tees intermediate output to char sequence sink writer2
            pipeline[3] = new TeeProcessor(pipeline[2]);
            // pipeline stage 4 matches "a ${Xn}" replacing it with
            // "${Zn}" and binding Zn --> a ${Xn}

            pipeline[4] = new BindingInserter("a \\$\\{[X0-9]+\\}", "Z", bindings, pipeline[3]);

            // connect the tees to their char sequence sinks so we can sanity check the intermediate results
            CharSequenceSink writer = new CharSequenceSink(pipeline[1]);
            CharSequenceSink writer2 = new CharSequenceSink(pipeline[3]);

            // the final output is also a char sequence sink
            CharSequenceSink writer3 = new CharSequenceSink(pipeline[4]);

            // start all the stream processors
            reader.start();
            for(int i = 0; i <pipeline.length ;i++) {
                pipeline[i].start();
            }
            writer.start();
            writer2.start();
            writer3.start();
            //now wait for all the processors to finish
            reader.join();
            for(int i = 0; i <pipeline.length; i++) {
                pipeline[i].join();
            }
            writer.join();
            writer2.join();
            writer3.join();

            // here is what happened at each stage
            System.out.println("input:");
            System.out.println(input);
            System.out.println("1st intermediate:");
            System.out.println(writer.toString());
            System.out.println("2nd intermediate:");
            System.out.println(writer2.toString());
            System.out.println("output:");
            System.out.println(writer3.toString());

            Iterator<String> iterator = bindings.iterator();
            List<String> list = new ArrayList<String>();
            while (iterator.hasNext()) {
                String id = iterator.next();
                list.add(id);
            }
            Collections.sort(list);
            iterator = list.iterator();
            System.out.println("bindings[");
            while (iterator.hasNext()) {
                String id = iterator.next();
                System.out.println(id + " -> " + bindings.get(id));
            }
            System.out.println("]");
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
