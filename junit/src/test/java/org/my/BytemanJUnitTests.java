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

import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.my.pipeline.impl.CharSequenceSource;
import org.my.pipeline.impl.CharSequenceSink;
import org.my.pipeline.impl.PatternReplacer;

/**
 * Test class showing how to use Byteman BMUnit package with
 * JUnit to do simple tracing
 * 
 * The RunWith annotation on the class tells JUnit to use the BMUnit
 * code when runnign this test. BMUnit ensures that the Byteman agent
 * is loaded into the test JVM and checks the test class and test
 * methods for annotations specifying Byteman rules which should be
 * used to moidfy the behaviour of test code, application code or JVM
 * runtime code while the tests are running.
 * 
 * The BMScript annotation on the class BytemanJUnitTests selects a
 * script containing Byteman rules which tarce execution of some of
 * the pipeline classes, displaying data going into the pipeline,
 * being transformed and being collected at the end of the pipeline.
 */
@RunWith(BMUnitRunner.class)
@BMScript(value="trace", dir="target/test-classes")
public class BytemanJUnitTests
{
    /**
     * a simple test of the pattern replacer functionality. we feed a String into the pipeline via a
     * CharSequenceReader, transform it via a PatternReplacer and then retrieve the transformed String
     * using a CharSequenceWriter. 
     * @throws Exception
     */
    @Test
    public void testPipeline() throws Exception
    {
        System.out.println("testPipeLine:");
        String input = "hello world!\ngoodbye cruel world!\ngoodbye!\n";
        CharSequenceSource reader = new CharSequenceSource(input);
        PatternReplacer replacer = new PatternReplacer("world", "mum",reader);
        CharSequenceSink writer = new CharSequenceSink(replacer);
        reader.start();
        replacer.start();
        writer.start();
        reader.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals("hello mum!\ngoodbye cruel mum!\ngoodbye!\n"));
    }

}
