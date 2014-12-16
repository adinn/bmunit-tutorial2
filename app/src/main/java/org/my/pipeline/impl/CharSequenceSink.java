/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat and individual contributors as identified
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

package org.my.pipeline.impl;

import org.my.pipeline.core.SinkProcessor;
import org.my.pipeline.core.Source;

import java.io.IOException;

/**
 * CharSequenceSink is a data Sink which collects the characters from its input stream
 * making them available as a CharSequence.
 */
public class CharSequenceSink extends SinkProcessor implements CharSequence {
    private StringBuffer buffer;

    public CharSequenceSink(Source source) throws IOException
    {
        super(source);
        this.buffer = new StringBuffer();
    }

    public void consume() throws IOException
    {
        int next = input.read();
        while  (next >= 0) {
            buffer.append((char) next);
            next = input.read();
        }
    }

    public int length() {
        return buffer.length();
    }

    public char charAt(int index) {
        return buffer.charAt(index);
    }

    public CharSequence subSequence(int start, int end) {
        return buffer.subSequence(start, end);
    }

    public String toString()
    {
        return buffer.toString();
    }
}
