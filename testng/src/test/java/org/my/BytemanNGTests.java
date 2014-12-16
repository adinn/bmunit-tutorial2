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

import org.jboss.byteman.contrib.bmunit.BMNGRunner;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMScripts;
import org.my.pipeline.core.PipelineProcessor;
import org.my.pipeline.impl.Binder;
import org.my.pipeline.impl.BindingReplacer;
import org.my.pipeline.impl.CharSequenceSource;
import org.my.pipeline.impl.CharSequenceSink;
import org.my.pipeline.util.BindingMap;
import org.testng.annotations.Test;

/**
 * These tests show how the BindingMap shared by a Binder
 * and a BindingReplacer provides a communication channel
 * which invalidates their independence and introduces a
 * race condition that can affect the output.
 *
 * The test uses the following input
 * <ul>
 *     <li>the boy threw the stick for the dog to catch</li>
 *     <li>a ${X1} broke a ${X4} with a ${X2}</li>
 *     <li>the boy threw the stick at the window</li>
 * </ul>
 * The binder is configured to match pattern "the ([A-Za-z]+)"
 * binding X1, X2 etc. So, it will generate the bindings
 * [X1 -> "boy", X2 -> "stick", X3 -> "dog"] from line 1
 * and [X4 -> "window"] from line 3. n.b. the Binder does
 * not transform the corresponding lines, it merely inserts
 * the required bindings into the binding map.
 *
 * The BindingReplacer uses the bindings in the map to
 * transform line 2. The BindingReplacer cannot see this
 * line until the Binder has processed and forwarded it
 * and this means line 1 will also have been processed
 * and forwarded. So, the BindingMap is guaranteed to
 * contain entries for X1, X2 and X3. However, there is no
 * guarantee that the Binder thread will have processed line
 * 3 by the time line 2 reaches the BindingReplacer thread.
 * The JVM runtime could potentially suspend the Binder just
 * after it has forwarded line 2. This is very unlikely to
 * happen so in most runs the map will also contain the
 * binding for X4 and the BindingReplacer will outpu the
 * transformed line "a boy broke a window with a stick".
 * However, if the timing is right (or, rather, wrong) then
 * the binding for X4 will be missing and the output will be
 * "a boy broke a ${X4} with a stick"
 *
 * We can use Byteman to display this timing dependency by
 * injecting synchronization operations into the pipeline
 * threads which make sure they process at the relevant lines
 * in the desired order. The rules are in script timing.btm
 * and they use Byteman built-in operations to inject a
 * rendezvous into the pipeline processes. A rendezvous
 * allows threads to meet up with other threads before
 * any of them can proceed to perform some order-dependent
 * action (another name for it is barrier). An N-way
 * rendezvous stalls the first N-1 threads which call the
 * rendezvous entry method, only allowing them to exit the
 * call once the Nth thread arrives. The Byteman rules use
 * a restartable rendezvous which means that once the Nth
 * thread has entered and left the rendezvous is reset and
 * can be re-entered by N more threads.
 * 
 * The test employs a pair of 2-way rendezvous, one of which
 * isentered by the Binder and the other by the
 * BindingReplacer.The rules injected into the Binder cause
 * it to enter its rendezvous just before it processes the
 * 3rd line and just after it finishes processing that line.
 * The rules injected into the BindingReplacer cause it to
 * enter its rendezvous just before it processes the 2nd line
 * and just after it finishes processing that line. The other
 * partner in each rendezvous is the test thread.
 *
 * Neither of the pipeline threads can process their line
 * until they have passed their first rendezvous. Once they
 * have passed the second rendezvous then the line has definitely
 * been processed. So, the test thread can choose to rendezvous
 * with either the Binder or the BindingReplacer thread and in
 * doing so control which line gets processed first and which
 * one second. There are two tests: the first one ensures that
 * the Binder processes its line before the BindingReplacer gets
 * to process its line; the second one ensures the lines are
 * processes in the reverse order.
 *
 * Of course, the test thread cannot directly call the Byteman
 * built-in operations which execute rendezvous operations. These
 * are only accessible from Byteman rules injected b Byteman. Of
 * course, that presents no problem. Byteman can inject code into
 * methods of any class, including the test class itself! When it
 * ants to perform a rendezvous the test code calls a dummy method
 * triggerRendezvous. A Byteman rule injects a call to rendezvous
 * into this method. The argument provided in the call is either
 * the Binder or the BindingReplacer which are used as the keys
 * to identify the corresponding rendezvous.
 */

/*
 * This script annotation installs some tracing rules which
 * write information to System.out. We want these trace rules
 * to be used for both test runs so we attach the annotation
 * to the class. The annotation reuses the junit test script
 * which displays the pipeline input and output. We also add
 * a second script which provides trace rules for the binder
 * and binding replacer.
 */
@BMScripts(scripts = {@BMScript(value="trace", dir="target/test-classes"),
        @BMScript(value="trace2", dir="target/test-classes")})
public class BytemanNGTests extends BMNGRunner
{
    /**
     * This test displays the case where the binder processes
     * line 3 before the binding replacer processes line 2.
     * That means that the variable X4 is bound before any
     * attempt is made to replace it. The timing script
     * injects rules which synchronize the test thread with
     * the pipeline threads to ensure they execute in this
     * order.
     */
    @BMScript(value="timing", dir="target/test-classes")
    @Test
    public void testForwardPublishingGood() throws Exception
    {
        System.out.println("testForwardPublishingGood:");
        BindingMap bindings = new BindingMap();
        // this first line binds [X1 ->"boy", X2 -> "stick", X3 -> "dog"]
        StringBuffer buffer = new StringBuffer("the boy threw the stick for the dog to catch\n");
        // this second line needs the bindings [X1 ->"boy", X2 -> "stick", X4 -> "dog"]
        buffer.append("a ${X1} broke a ${X4} with a ${X2}\n");
        // the third line reuses bindings [X1 ->"boy", X2 -> "stick"] and adds binding [X4 -> "window"]
        buffer.append("the boy threw the stick at the window\n");
        CharSequenceSource reader = new CharSequenceSource(buffer);
        Binder binder = new Binder("the ([A-Za-z]+)", "X", bindings, reader);
        BindingReplacer replacer = new BindingReplacer(bindings, binder);
        CharSequenceSink writer = new CharSequenceSink(replacer);
        reader.start();
        binder.start();
        replacer.start();
        writer.start();
        // first we rendezvous with the binder to allow it to pass the trigger point where it
        // is just about to process the third line
        triggerRendezvous(binder);
        // now we rendezvous again with the binder ensuring that it has completed processing
        // the third line. this means the binding for X4 should have been installed
        triggerRendezvous(binder);
        String value = bindings.get("X4");
        assert("window".equals(value));
        // next we rendezvous with the replacer to allow it to pass the trigger point where it
        // is just about to process the second line
        triggerRendezvous(replacer);
        // now we rendezvous again with the replacer ensuring that it has completed processing
        // the second line. this means the reference to X4 should have been replaced
        triggerRendezvous(replacer);
        reader.join();
        binder.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals("the boy threw the stick for the dog to catch\n" +
                            "a boy broke a window with a stick\n" +
                            "the boy threw the stick at the window\n"));

    }

    /**
     * This test displays the case where the binder processes
     * line 3 after the binding replacer processes line 2.
     * That means that the variable X4 is unbound when an
     * attempt is made to replace it. The same timing script
     * is used to synchronize with the pipeline threads but
     * this time the test code calls the rendezvous method
     * in the reverse order.
     */
    @BMScript(value="timing", dir="target/test-classes")
    @Test(dependsOnMethods = "testForwardPublishingGood") // maintain correct execution order
    public void testForwardPublishingBad() throws Exception
    {
        System.out.println("testForwardPublishingBad:");
        BindingMap bindings = new BindingMap();
        // this first line binds [X1 ->"boy", X2 -> "stick", X3 -> "dog"]
        StringBuffer buffer = new StringBuffer("the boy threw the stick for the dog to catch\n");
        // this second line needs the bindings [X1 ->"boy", X2 -> "stick", X4 -> "dog"]
        buffer.append("a ${X1} broke a ${X4} with a ${X2}\n");
        // this third line reuses bindings [X1 ->"boy", X2 -> "stick"] and add binding [X4 -> "window"]
        buffer.append("the boy threw the stick at the window\n");
        CharSequenceSource reader = new CharSequenceSource(buffer);
        Binder binder = new Binder("the ([A-Za-z]+)", "X", bindings, reader);
        BindingReplacer replacer = new BindingReplacer(bindings, binder);
        CharSequenceSink writer = new CharSequenceSink(replacer);
        reader.start();
        binder.start();
        replacer.start();
        writer.start();
        // first we rendezvous with the replacer to allow it to pass the trigger point where it
        // is just about to process the second line
        // note that the binder will still be wedged in its first rendezvous so it cannot
        // have processed the third line
        triggerRendezvous(replacer);
        // now we rendezvous again with the replacer ensuring that it has completed processing
        // the second line. this means the binding for X4 will not be found so the reference
        // will not get replaced
        triggerRendezvous(replacer);
        String value = bindings.get("X4");
        assert(value == null);
        // next we rendezvous with the binder to allow it to pass the trigger point where it
        // is just about to process the third line
        triggerRendezvous(binder);
        // now we rendezvous again with the binder ensuring that it has completed processing
        // the third line. this means the binding for X4 will have been installed too late
        triggerRendezvous(binder);
        reader.join();
        binder.join();
        replacer.join();
        writer.join();
        String output = writer.toString();
        assert(output.equals("the boy threw the stick for the dog to catch\n" +
                            "a boy broke a ${X4} with a stick\n" +
                            "the boy threw the stick at the window\n"));
    }

    /**
     * this method is called by the test thread when it wants
     * to rendezvous with a pipeline thread. One of the rules
     * in the timing script injects a call to rendezvous into
     * this method, using the String argument to identify the
     * rendezvous. This means the test threda can choose which
     * test thread it wants to synchronize with.
     * @param processor identifies which pipeline processor
     * thread to rendezvous with
     */
    private void triggerRendezvous(PipelineProcessor processor)
    {
        // nothing to do here as Byteman will inject the
    	// relevant code into this method!
    }
}
