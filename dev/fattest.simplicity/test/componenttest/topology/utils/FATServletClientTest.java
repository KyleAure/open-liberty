/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.topology.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;
import org.junit.internal.AssumptionViolatedException;

import componenttest.app.AssertionErrorSerializer;
import componenttest.app.AssumptionViolatedExceptionSerializer;
import componenttest.app.FATServlet;

/**
 * Test assertion and assumption error parsing
 */
public class FATServletClientTest {

    @SuppressWarnings("serial")
    private static class TestClass extends FATServlet {
        // NO_OP class to associate the error serialization too.
    }

    //Mimics the behavior of the FATServlet writing a response
    private static String generateResponse(AssertionError e) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);

        writer.write(AssertionErrorSerializer.START_TAG);
        AssertionError simple = AssertionErrorSerializer.simplify(TestClass.class, "generateResponse", e);
        AssertionErrorSerializer.serialize(simple, writer);
        writer.write(AssertionErrorSerializer.END_TAG);

        return out.toString();
    }

    @Test
    public void testAssertionError() {
        AssertionError expected = null, actual = null;
        String message = "expected:<[abc]> but was:<[123]>";

        try {
            assertEquals("abc", "123");
        } catch (AssertionError e) {
            expected = e;
        }

        assertNotNull(expected);
        assertEquals(message, expected.getMessage());

        try {
            FATServletClient.assertTestResponse(generateResponse(expected));
        } catch (AssertionError e) {
            actual = e;
        }

        assertNotNull(actual);
        assertTrue(actual.getMessage().contains(message));
    }

    //Mimics the behavior of the FATServlet writing a response
    private static String generateResponse(AssumptionViolatedException e) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);

        writer.write(AssumptionViolatedExceptionSerializer.START_TAG);
        AssumptionViolatedException simple = AssumptionViolatedExceptionSerializer.simplify(TestClass.class, "generateResponse", e);
        AssumptionViolatedExceptionSerializer.serialize(simple, writer);
        writer.write(AssumptionViolatedExceptionSerializer.END_TAG);

        writer.println(FATServlet.SUCCESS);

        return out.toString();
    }

    @Test
    public void testAssumptionException() {
        AssumptionViolatedException expected = null, actual = null;
        String message = "got: <false>, expected: is <true>";

        try {
            assumeTrue(false);
        } catch (AssumptionViolatedException e) {
            expected = e;
        }

        assertNotNull(expected);
        assertEquals(message, expected.getMessage());

        try {
            FATServletClient.assertTestResponse(generateResponse(expected));
        } catch (AssumptionViolatedException e) {
            actual = e;
        }

        assertNotNull(actual);
        assertTrue(actual.getMessage().contains(message));
    }

    //Mimics the behavior of the FATServlet writing a response
    private static String generateResponse(Throwable t) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);

        writer.println("ERROR: Caught exception attempting to call test method generateResponse on servlet " + TestClass.class.getName());
        t.printStackTrace(writer);

        return out.toString();
    }

    @Test
    public void testUnexpectedException() {
        AssertionError actual = null;
        String message = "test failure occured";
        Throwable expected = new Throwable(message);

        try {
            FATServletClient.assertTestResponse(generateResponse(expected));
        } catch (AssertionError e) {
            actual = e;
        }

        assertNotNull(actual);
        assertTrue(actual.getMessage().contains(message));
    }
}
