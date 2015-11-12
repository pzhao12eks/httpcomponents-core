/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.protocol;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWithTrailers;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Test;

public class TestTrailerNameFormatter {

    @Test
    public void testTrailerFormatting() throws Exception {
        final HttpEntity entity = new HttpEntityWithTrailers(
                new StringEntity("some stuff with trailers", Consts.ASCII),
                new BasicHeader("z", "this"), new BasicHeader("b", "that"), new BasicHeader("a", "this and that"));
        final Header header = TrailerNameFormatter.format(entity);
        Assert.assertNotNull(header);
        Assert.assertEquals("a, b, z", header.getValue());
    }

    @Test
    public void testTrailerFormattingSameName() throws Exception {
        final HttpEntity entity = new HttpEntityWithTrailers(
                new StringEntity("some stuff with trailers", Consts.ASCII),
                new BasicHeader("a", "this"), new BasicHeader("a", "that"), new BasicHeader("a", "this and that"));
        final Header header = TrailerNameFormatter.format(entity);
        Assert.assertNotNull(header);
        Assert.assertEquals("a", header.getValue());
    }

    @Test
    public void testTrailerNoTrailers() throws Exception {
        final HttpEntity entity = new HttpEntityWithTrailers(
                new StringEntity("some stuff and no trailers", Consts.ASCII));
        final Header header = TrailerNameFormatter.format(entity);
        Assert.assertNull(header);
    }

    @Test
    public void testTrailerNullTrailers() throws Exception {
        final HttpEntity entity = new StringEntity("some stuff and no trailers", Consts.ASCII);
        final Header header = TrailerNameFormatter.format(entity);
        Assert.assertNull(header);
    }

}
