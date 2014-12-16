/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, 2014 Red Hat and individual contributors as identified
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
import org.my.pipeline.core.TeeProcessor;
import org.my.pipeline.impl.FileSource;
import org.my.pipeline.impl.FileSink;
import org.my.pipeline.impl.PatternReplacer;

import java.io.IOException;

/**
 * A more complicated pipeline application which transforms input
 * file foo.txt in 3 stages collecting the transformed text in
 * output file bar.txt
 * 
 * The transformation processes use class PatternReplacer which is
 * a subclass of TextLineProcessor. It's transform method looks for
 * occurrences of a pattern in each input line and substitutes each
 * match with some replacement text.
 * 
 * In the simplest case the pattern and replacement are just a plain
 * String -- the first PatternReplacer, pipeline[0], provides an
 * example of this, replacing every occurrence of the text "adinn" in
 * an input line with the replacement text "msmith".
 * 
 *  The pattern String is actually treated as a standard Java regular
 *  expression. The second PatternReplacer, pipeline[2], provides an
 *  example of this. Pattern "[Aa]ndrew" will match any occurrence of
 *  "Andrew" or "andrew" in an input line. In either case, the matched
 *  text will be replaced with "Michael".
 *  
 *  Patterns can also include bracketed terms called match groups to
 *  identify elements of the matched text which are to be included in
 *  the replacement text. An example of this is provided by the third
 *  PatternReplacer, pipeline[4], which uses two wildcard patterns to 
 *  match text preceding and following the pattern "[Dd]inn". If an
 *  input line contains the text "Dinn" or "dinn" then this pattern
 *  will match the whole line. The text matched by the pattern groups
 *  is referenced in the replacement String using a numerical index.
 *  So, "\\2" refers to the second match group, the text which follows
 *  the "Dinn" and "\\1" refers to the first match group. The replacement
 *  text replaces Dinn with Smith and adds the preceding and following
 *  text in reverse order.
 *  
 *  n.b. the replacement uses need two '\' characters to ensure that
 *  the '\' actually appears as a character in the resulting String
 *  "\2" would be parsed as the unicode character literal for Ctrl-B.
 *  
 *  The pipeline includes two extra TeeProcessor elements. TeeProcessor
 *  is another class used to aid tracing/debug. TeeProcessor accepts a
 *  connection to two downstream Sinks, effectively branching the
 *  pipeline and all it does is copy bytes from its input stream to each
 *  of the two outputs. It does not care about how the data is organised
 *  in lines of text so it can subclass PipelineProcess directly,
 *  implementing abstract method processPipeline.
 *  
 *  The extra TeeProcessor branch from pipeline[1] is fed into a
 *  FileSink, writer, which saves its  input to file bar1.txt. Similarly,
 *  extra TeeProcessor branch from pipeline[3] feeds writer2 which writes
 *  file bar2.txt. So, when this program is run the 3 output files show
 *  the data in the pipeline at each stage of the transformation process.
 *  If you execute a diff between each successive pair of files, foo.txt
 *  and bar1.txt, bar1.txt and bar2.txt, bar2.txt and bar.txt you will
 *  see the changes intorduced by each of the PatternReplacer processes.  
 */
public class PipelineAppMain1
{
    public static void main(String[] args)
    {
        try {
            // pipeline source reads file foo.txt
            SourceProcessor reader = new FileSource("foo.txt");
            PipelineProcessor[] pipeline = new PipelineProcessor[5];

            // pipeline stage 0 replaces login name
            pipeline[0] = new PatternReplacer("adinn", "msmith", reader);
            // pipeline stage 1 tees intermediate output to a trace filewriter
            pipeline[1] = new TeeProcessor(pipeline[0]);
            // pipeline stage 2 replaces first name
            pipeline[2] = new PatternReplacer("[Aa]ndrew", "Michael", pipeline[1]);
            // pipeline stage 3 tees intermediate output to a trace filewriter
            pipeline[3] = new TeeProcessor(pipeline[2]);
            // pipeline stage 4 replaces surname
            pipeline[4] = new PatternReplacer("(.*)[Dd]inn(.*)", "\\2Smith\\1", pipeline[3]);

            // the tees feed file wwriters so we can sanity check the intermediate results
            SinkProcessor writer = new FileSink("bar1.txt", pipeline[1]);
            SinkProcessor writer2 = new FileSink("bar2.txt", pipeline[3]);

            // pipeline stage 4 writes the final output to filebar.txt
            SinkProcessor writer3 = new FileSink("bar.txt", pipeline[4]);
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
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
