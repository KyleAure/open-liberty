/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi12.observersinjars;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cdi12.observersinjars.manifestjar.ManifestAutostartObserver;
import cdi12.observersinjars.webinf.WebInfAutostartObserver;
import cdi12.observersinjarsbeforebean.WarBeforeBeansObserver;

@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    @Inject
    WebInfAutostartObserver webInfObserver;
    @Inject
    ManifestAutostartObserver manifestObserver;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter pw = response.getWriter();

        pw.println("web-inf jar saw initilization: " + webInfObserver.methodCalled());
        pw.println("manifest jar saw initilization: " + manifestObserver.methodCalled());

        pw.println("could load clases from TCCL in war: " + WarBeforeBeansObserver.correctClassLoader());

        pw.flush();
        pw.close();

    }

}
