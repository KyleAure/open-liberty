/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb;

/**
 * Remote interface for Session bean used for testing EJB Injection.
 **/
public interface EJBInjectionRemote {
    public void verifyEJBFieldInjection();

    public void verifyEJBMethodInjection();

    public void verifyNoEJBFieldInjection();

    public void verifyNoEJBMethodInjection();
}