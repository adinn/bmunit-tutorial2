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

package org.my;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.my.pipeline.impl.CharSequenceSource;
import org.my.pipeline.impl.CharSequenceSink;
import org.my.pipeline.impl.PatternReplacer;

/**
 * Test class showing how to use Byteman BMUnit package with JUnit to
 * do tracing and fault injection
 */
@RunWith(BMUnitRunner.class)
@BMScript(value="trace", dir="target/test-classes")
public class BytemanJUnitTests2
{
    /**
     * this is the same code as {@link org.my.BytemanJUnitTests#testPipeline}
     * but it uses a Byteman rule to throw an IOException in the pattern replacer
     * before it can processes any text lines. The pattern replacer thread
     * should catch the exception, close its input and output streams and exit,
     * causing the writer to finish with an empty output. All the pipeline
     * threads should exit cleanly.
     * 
     * The rule is presented using a BMRule annotation attached to this
     * test method. This annotation allows the rule text to be specified
     * inline in the test program rather than in a script on disk. Notice
     * that the class still retains the BMScript annotation. Annotations on
     * the class inject rules for the duration of all test methods belonging
     * to the class. Annotatiojns on a test method inject rules just during
     * execution of the test method and the code is uninjected before the
     * next test is started. 
     * 
     * There is a race here between the pattern replacer and the file source
     * to close their respective input and output streams. The file source
     * will normally write all its data and close its output stream before the
     * pattern replacer starts. The next test creates a situation where the
     * file source will lose this race.
     * 
     * @throws Exception
     */
    @Test
    @BMRule(name="throw IOException at 1st transform",
            targetClass = "TextLineProcessor",
            targetMethod = "processPipeline",
            targetLocation="AT ENTRY",
            action = "throw new java.io.IOException()")
    public void testErrorInPipeline() throws Exception
    {
        System.out.println("testErrorInPipeline:");
        StringBuffer buffer = new StringBuffer("hello world!");
        buffer.append(" goodbye cruel world!\n");
        CharSequenceSource reader = new CharSequenceSource(buffer);
        PatternReplacer replacer = new PatternReplacer("world", "mum",reader);
        CharSequenceSink writer = new CharSequenceSink(replacer);
        reader.start();
        replacer.start();
        writer.start();
        reader.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals(""));
    }

    /**
     * This is a similar test to the previous one but it differs in
     * two small respects. Firstly  the reader passes in many lines
     * of text, enough to fill the input pipeline to the pattern
     * replacer. Secondly, there are two rules, the first of which
     * creates a countDown used to control firing of the second.
     * 
     * The first rule is injected into the constructor for
     * TextLineProcessor. It creates a countDown with count 2 using
     * the newly constructed instance as an identifying key.
     *
     * The second rule throws an IOException in method processPipeline
     * as in the previous test. However, this time it specifies a
     * specific target location and a condition (note that the condition
     * defaulted to "TRUE" in the previous example). The target location
     * is inside the loop body, just before a call is made to method
     * transform(String). This means the rule is triggered each time a
     * line of text is about to be processed and written to the output.
     *
     * The condition calls countDown passing $0 as the identifying key.
     * This ensures that the countdown used to perform the test is the
     * one with initial counter 2 created when the text processor was
     * constructed and that this same countDown is used on each
     * successive firing. At the first triggering method countDown
     * decrements the counter from 2 to 1 and returns false. At the
     * second triggering it decrements it from 1 to 0 and again returns
     * false. At the third firing it finds that the counter is zero so
     * it deletes the countDown instance returning true. So, at this
     * triggering the rule fires and throws an exception.
     *
     * The pattern replacer should process two lines of text before the
     * exception is thrown. It should catch and print the exception,
     * close its input and output streams and exit, causing the writer to
     * finish with two lines of transformed output. The upstream thread
     * should also see an exception. It will either still be writing
     * text to its output or, more likely, it will be sitting on a full
     * pipeline waiting for the pattern replacer to clear the pipe. In
     * either case a close on the stream it is feeding will cause it
     * to suffer a Pipe closed IOException. A Byteman rule is used to
     * print a stack trace when the IOException is initialised.
     *
     * Notice that the BMRules annotation is used to group multiple
     * BMRule annotations. There is also a BMScripts annotation
     * which allows more than one script to be loaded.
     * 
     * @throws Exception
     */
    @Test
    @BMRules(rules={@BMRule(name="create countDown for TextLineProcessor",
                    targetClass = "TextLineProcessor",
                    targetMethod = "<init>",
                    targetLocation = "AT EXIT",
                    action = "createCountDown($0, 2)"),
                    @BMRule(name="throw IOException at 3rd transform",
                    targetClass = "TextLineProcessor",
                    targetMethod = "processPipeline",
                    targetLocation = "AT CALL transform(String)",
                    condition = "countDown($0)",
                    action = "throw new java.io.IOException()")})
    public void testErrorInFullPipeline() throws Exception
    {
        System.out.println("testErrorInFullPipeline:");
        StringBuffer buffer = new StringBuffer("hello world!\n");
        buffer.append("goodbye cruel world!\n");
        for (int i = 0; i < 40; i++) {
            buffer.append("goodbye! goodbye! goodbye!\n");
        }
        CharSequenceSource reader = new CharSequenceSource(buffer);
        PatternReplacer replacer = new PatternReplacer("world", "mum",reader);
        CharSequenceSink writer = new CharSequenceSink(replacer);
        reader.start();
        replacer.start();
        writer.start();
        reader.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals("hello mum!\ngoodbye cruel mum!\n"));
    }
}
