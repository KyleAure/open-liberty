/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

package web.metadatacompletetruewebxml;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//Because metadata-complete=true in web.xml, static annotations here are ignored
@WebServlet(name = "MetadataCompleteTrueWebXML1", urlPatterns = { "/MetadataCompleteTrueWebXML1" })
@ServletSecurity(@HttpConstraint(EmptyRoleSemantic.DENY))
public class MetadataCompleteTrueWebXML1 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public MetadataCompleteTrueWebXML1() {
        super("MetadataCompleteTrueWebXML1");
    }

}
