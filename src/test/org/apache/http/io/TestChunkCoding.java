/*
 * $HeadURL$
 * $Revision$
 * $Date$
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.Header;
import org.apache.http.util.EncodingUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestChunkCoding extends TestCase {

    private static final String CONTENT_CHARSET = "ISO-8859-1";
    
    public TestChunkCoding(String testName) {
        super(testName);
    }

    // ------------------------------------------------------- TestCase Methods

    public static Test suite() {
        return new TestSuite(TestChunkCoding.class);
    }

    // ------------------------------------------------------------------- Main
    public static void main(String args[]) {
        String[] testCaseName = { TestChunkCoding.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testConstructors() throws Exception {
        try {
            new ChunkedInputStream((HttpDataReceiver)null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            new ChunkedInputStream((InputStream)null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        new MalformedChunkCodingException();
        new MalformedChunkCodingException("");
    }

    private final static String CHUNKED_INPUT 
        = "10;key=\"value\\\r\nnewline\"\r\n1234567890123456\r\n5\r\n12345\r\n0\r\nFooter1: abcde\r\nFooter2: fghij\r\n";
    
    private final static String CHUNKED_RESULT 
        = "123456789012345612345";
    
    // Test for when buffer is larger than chunk size
    public void testChunkedInputStreamLargeBuffer() throws IOException {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        EncodingUtil.getBytes(CHUNKED_INPUT, CONTENT_CHARSET)));
        byte[] buffer = new byte[300];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        assertEquals(-1, in.read(buffer));
        assertEquals(-1, in.read(buffer));
        
        in.close();
        
        String result = EncodingUtil.getString(out.toByteArray(), CONTENT_CHARSET);
        assertEquals(result, CHUNKED_RESULT);
        
        Header[] footers = in.getFooters();
        assertNotNull(footers);
        assertEquals(2, footers.length);
        assertEquals("Footer1", footers[0].getName());
        assertEquals("abcde", footers[0].getValue());
        assertEquals("Footer2", footers[1].getName());
        assertEquals("fghij", footers[1].getValue());
    }        

    //Test for when buffer is smaller than chunk size.
    public void testChunkedInputStreamSmallBuffer() throws IOException {
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                            EncodingUtil.getBytes(CHUNKED_INPUT, CONTENT_CHARSET)));

        byte[] buffer = new byte[7];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        assertEquals(-1, in.read(buffer));
        assertEquals(-1, in.read(buffer));

        in.close();
                
        String result = EncodingUtil.getString(out.toByteArray(), CONTENT_CHARSET);
        Header[] footers = in.getFooters();
        assertNotNull(footers);
        assertEquals(2, footers.length);
        assertEquals("Footer1", footers[0].getName());
        assertEquals("abcde", footers[0].getValue());
        assertEquals("Footer2", footers[1].getName());
        assertEquals("fghij", footers[1].getValue());
    }
        
    // One byte read
    public void testChunkedInputStreamOneByteRead() throws IOException {
        String s = "5\r\n01234\r\n5\r\n56789\r\n0\r\n";
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        EncodingUtil.getBytes(s, CONTENT_CHARSET)));
        int ch;
        int i = '0';
        while ((ch = in.read()) != -1) {
            assertEquals(i, ch);
            i++;
        }
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());        

        in.close();        
    }

    public void testChunkedInputStreamClose() throws IOException {
        String s = "5\r\n01234\r\n5\r\n56789\r\n0\r\n";
        ChunkedInputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        EncodingUtil.getBytes(s, CONTENT_CHARSET)));
        in.close();
        in.close();
        try {
            in.read();
            fail("IOException should have been thrown");
        } catch (IOException ex) {
            // expected
        }
        byte[] tmp = new byte[10]; 
        try {
            in.read(tmp);
            fail("IOException should have been thrown");
        } catch (IOException ex) {
            // expected
        }
        try {
            in.read(tmp, 0, tmp.length);
            fail("IOException should have been thrown");
        } catch (IOException ex) {
            // expected
        }
    }

    // Missing \r\n at the end of the first chunk
    public void testCorruptChunkedInputStreamMissingCRLF() throws IOException {
        String s = "5\r\n012345\r\n56789\r\n0\r\n";
        InputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        EncodingUtil.getBytes(s, CONTENT_CHARSET)));
        byte[] buffer = new byte[300];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;
        try {
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            fail("MalformedChunkCodingException should have been thrown");
        } catch(MalformedChunkCodingException e) {
            /* expected exception */
        }
    }

    // Missing LF
    public void testCorruptChunkedInputStreamMissingLF() throws IOException {
        String s = "5\r01234\r\n5\r\n56789\r\n0\r\n";
        InputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        EncodingUtil.getBytes(s, CONTENT_CHARSET)));
        try {
            in.read();
            fail("MalformedChunkCodingException should have been thrown");
        } catch(MalformedChunkCodingException e) {
            /* expected exception */
        }
    }

    // Missing closing chunk
    public void testCorruptChunkedInputStreamNoClosingChunk() throws IOException {
        InputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(new byte[] {}));
        try {
            in.read();
            fail("MalformedChunkCodingException should have been thrown");
        } catch(MalformedChunkCodingException e) {
            /* expected exception */
        }
    }

    // Invalid chunk size
    public void testCorruptChunkedInputStreamInvalidSize() throws IOException {
        String s = "whatever\r\n01234\r\n5\r\n56789\r\n0\r\n";
        InputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        EncodingUtil.getBytes(s, CONTENT_CHARSET)));
        try {
            in.read();
            fail("MalformedChunkCodingException should have been thrown");
        } catch(MalformedChunkCodingException e) {
            /* expected exception */
        }
    }

    // Invalid footer
    public void testCorruptChunkedInputStreamInvalidFooter() throws IOException {
        String s = "1\r\n0\r\n0\r\nstuff\r\n";
        InputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        EncodingUtil.getBytes(s, CONTENT_CHARSET)));
        try {
            in.read();
            in.read();
            fail("MalformedChunkCodingException should have been thrown");
        } catch(MalformedChunkCodingException e) {
            /* expected exception */
        }
    }

    public void testEmptyChunkedInputStream() throws IOException {
        String input = "0\r\n";
        InputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        EncodingUtil.getBytes(input, CONTENT_CHARSET)));
        byte[] buffer = new byte[300];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
        assertEquals(0, out.size());
    }
    
    public void testContentLengthInputStream() throws IOException {
        String correct = "1234567890123456";
        InputStream in = new ContentLengthInputStream(new ByteArrayInputStream(
            EncodingUtil.getBytes(correct, CONTENT_CHARSET)), 10L);
        byte[] buffer = new byte[50];
        int len = in.read(buffer);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(buffer, 0, len);
        String result = EncodingUtil.getString(out.toByteArray(), CONTENT_CHARSET);
        assertEquals(result, "1234567890");
    }

    public void testContentLengthInputStreamSkip() throws IOException {
        InputStream in = new ContentLengthInputStream(new ByteArrayInputStream(new byte[20]), 10L);
        assertEquals(10, in.skip(10));
        assertTrue(in.read() == -1);

        in = new ContentLengthInputStream(new ByteArrayInputStream(new byte[20]), 10L);
        in.read();
        assertEquals(9, in.skip(10));
        assertTrue(in.read() == -1);

        in = new ContentLengthInputStream(new ByteArrayInputStream(new byte[20]), 2L);
        in.read();
        in.read();
        assertTrue(in.skip(10) <= 0);
        assertTrue(in.read() == -1);
    }

    public void testChunkedConsitance() throws IOException {
        String input = "76126;27823abcd;:q38a-\nkjc\rk%1ad\tkh/asdui\r\njkh+?\\suweb";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        OutputStream out = new ChunkedOutputStream(buffer);
        out.write(EncodingUtil.getBytes(input, CONTENT_CHARSET));
        out.flush();
        out.close();
        out.close();
        buffer.close();
        InputStream in = new ChunkedInputStream(
                new ByteArrayInputStream(
                        buffer.toByteArray()));

        byte[] d = new byte[10];
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int len = 0;
        while ((len = in.read(d)) > 0) {
            result.write(d, 0, len);
        }

        String output = EncodingUtil.getString(result.toByteArray(), CONTENT_CHARSET);
        assertEquals(input, output);
    }

    public void testChunkedOutputStream() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ChunkedOutputStream out = new ChunkedOutputStream(buffer, 2);
        out.write('1');  
        out.write('2');  
        out.write('3');  
        out.write('4');  
        out.finish();
        out.close();
        
        byte [] rawdata =  buffer.toByteArray();
        
        assertEquals(19, rawdata.length);
        assertEquals('2', rawdata[0]);
        assertEquals('\r', rawdata[1]);
        assertEquals('\n', rawdata[2]);
        assertEquals('1', rawdata[3]);
        assertEquals('2', rawdata[4]);
        assertEquals('\r', rawdata[5]);
        assertEquals('\n', rawdata[6]);
        assertEquals('2', rawdata[7]);
        assertEquals('\r', rawdata[8]);
        assertEquals('\n', rawdata[9]);
        assertEquals('3', rawdata[10]);
        assertEquals('4', rawdata[11]);
        assertEquals('\r', rawdata[12]);
        assertEquals('\n', rawdata[13]);
        assertEquals('0', rawdata[14]);
        assertEquals('\r', rawdata[15]);
        assertEquals('\n', rawdata[16]);
        assertEquals('\r', rawdata[17]);
        assertEquals('\n', rawdata[18]);
    }

    public void testChunkedOutputStreamLargeChunk() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ChunkedOutputStream out = new ChunkedOutputStream(buffer, 2);
        out.write(new byte[] {'1', '2', '3', '4'});
        out.finish();
        out.close();
        
        byte [] rawdata =  buffer.toByteArray();
        
        assertEquals(14, rawdata.length);
        assertEquals('4', rawdata[0]);
        assertEquals('\r', rawdata[1]);
        assertEquals('\n', rawdata[2]);
        assertEquals('1', rawdata[3]);
        assertEquals('2', rawdata[4]);
        assertEquals('3', rawdata[5]);
        assertEquals('4', rawdata[6]);
        assertEquals('\r', rawdata[7]);
        assertEquals('\n', rawdata[8]);
        assertEquals('0', rawdata[9]);
        assertEquals('\r', rawdata[10]);
        assertEquals('\n', rawdata[11]);
        assertEquals('\r', rawdata[12]);
        assertEquals('\n', rawdata[13]);
    }

    public void testChunkedOutputStreamSmallChunk() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        ChunkedOutputStream out = new ChunkedOutputStream(buffer, 2);
        out.write('1');  
        out.finish();
        out.close();
        
        byte [] rawdata =  buffer.toByteArray();
        
        assertEquals(11, rawdata.length);
        assertEquals('1', rawdata[0]);
        assertEquals('\r', rawdata[1]);
        assertEquals('\n', rawdata[2]);
        assertEquals('1', rawdata[3]);
        assertEquals('\r', rawdata[4]);
        assertEquals('\n', rawdata[5]);
        assertEquals('0', rawdata[6]);
        assertEquals('\r', rawdata[7]);
        assertEquals('\n', rawdata[8]);
        assertEquals('\r', rawdata[9]);
        assertEquals('\n', rawdata[10]);
    }

}

