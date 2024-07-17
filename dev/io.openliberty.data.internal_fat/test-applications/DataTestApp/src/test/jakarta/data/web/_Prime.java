/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package test.jakarta.data.web;

import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.AttributeRecord;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Metamodel for the Prime entity.
 * Using class instead of interface with public static final fields
 */
@StaticMetamodel(Prime.class)
public class _Prime {
    public static final String BINARYDIGITS = "binaryDigits";
    public static final String EVEN = "even";
    public static final String HEX = "hex";
    public static final String NAME = "name";
    public static final String NUMBERID = "numberId";
    public static final String ROMANNUMERAL = "romanNumeral";
    public static final String ROMANNUMERALSYMBOLS = "romanNumeralSymbols";
    public static final String SUMOFBITS = "sumOfBits";

    public static final TextAttribute<Prime> binaryDigits = new TextAttributeRecord<>(BINARYDIGITS);

    public static final SortableAttribute<Prime> even = new SortableAttributeRecord<>(EVEN);

    public static final TextAttribute<Prime> hex = new TextAttributeRecord<>(HEX);

    public static final TextAttribute<Prime> name = new TextAttributeRecord<>(NAME);

    public static final SortableAttribute<Prime> numberId = new SortableAttributeRecord<>(NUMBERID);

    public static final TextAttribute<Prime> romanNumeral = new TextAttributeRecord<>(ROMANNUMERAL);

    public static final Attribute<Prime> romanNumeralSymbols = new AttributeRecord<>(ROMANNUMERALSYMBOLS);

    public static final SortableAttribute<Prime> sumOfBits = new SortableAttributeRecord<>(SUMOFBITS);
}
