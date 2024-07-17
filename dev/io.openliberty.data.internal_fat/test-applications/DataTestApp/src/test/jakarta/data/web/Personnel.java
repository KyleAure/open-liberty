/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

import static jakarta.data.repository.By.ID;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;
import jakarta.enterprise.concurrent.Asynchronous;

/**
 * This is a second repository interface for the Person entity,
 * where the focus is on returning CompletableFuture/CompletionStage
 * and experimenting with how generated repository method implementations
 * fit with asynchronous methods.
 */
@Repository
public interface Personnel {
    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Query("UPDATE Person o SET o.lastName=?3 WHERE o.lastName=?1 AND o.ssn_id IN ?2")
    CompletionStage<Integer> changeSurnames(String oldSurname,
                                            List<Long> ssnList,
                                            String newSurname);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    CompletableFuture<Long> countByFirstNameStartsWith(String beginningOfFirstName);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    void deleteByFirstName(String firstName);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Delete
    CompletableFuture<Void> deleteById(@By(ID) long ssn);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Delete
    CompletableFuture<Void> deleteMultiple(Person... people);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Delete
    CompletableFuture<Integer> deleteSeveral(Stream<Person> people);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    CompletionStage<List<Person>> findByLastNameOrderByFirstName(String lastName);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    CompletableFuture<Person> findBySSN_Id(long ssn);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Query("SELECT o.firstName FROM Person o WHERE o.lastName=?1 ORDER BY o.firstName")
    CompletableFuture<Stream<String>> firstNames(String lastName);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Insert
    CompletableFuture<Void> insertAll(Person... people);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Query("SELECT DISTINCT o.lastName FROM Person o ORDER BY o.lastName")
    CompletionStage<String[]> lastNames();

    @Query("select firstName where firstName like concat(:beginningOfFirstName, '%')")
    List<String> namesThatStartWith(String beginningOfFirstName);

    // An alternative to the above would be to make the Collector class a parameter
    // of the Paginated annotation, although this would rule out easily accessing the
    // various built-in collectors that are provided by Java's Collectors interface.

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Delete
    CompletableFuture<Long> removeAll();

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Save
    CompletableFuture<List<Person>> save(Person... p);

    @Query("UPDATE Person o SET o.lastName=:lastName WHERE o.ssn_id=:ssn")
    long setSurname(@Param("ssn") long ssn, @Param("lastName") String newSurname);

    @Asynchronous //TODO 28636 not spec compliant (Asynchronous not in 1.0)
    @Query("UPDATE Person SET lastName=?2 WHERE ssn_id=?1")
    CompletableFuture<Boolean> setSurnameAsync(long ssn_id, String lastName);
}
