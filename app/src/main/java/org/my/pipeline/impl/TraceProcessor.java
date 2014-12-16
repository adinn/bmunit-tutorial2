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

package org.my.pipeline.impl;

import org.my.pipeline.core.Source;
import org.my.pipeline.core.TextLineProcessor;

import java.io.IOException;

/**
 * A TraceProcessor is a text line processor used to trace flow of data
 * through a pipeline. It dumps its input to System.out a line at a time,
 * labelling each output line with a prefix supplied at create time.
 */

public class TraceProcessor extends TextLineProcessor {
    private String prefix;

    /**
     * create a trace processor with a specific prefix
     * @param prefix
     * @param source
     * @throws IOException
     */
    public TraceProcessor(String prefix, Source source) throws IOException {
        super(source);
        this.prefix = prefix;
    }

    /**
     * dump the line of text to System.out pefixed with prefix
     * @param line a line of text from the file omitting any line terminator
     * @return
     */
    @Override
    public String transform(String line) {
        System.out.print(prefix);
        System.out.println(line);
        return line;
    }
}
