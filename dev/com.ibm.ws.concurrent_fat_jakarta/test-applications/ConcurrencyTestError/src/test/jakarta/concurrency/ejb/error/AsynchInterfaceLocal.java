/**
 *
 */
package test.jakarta.concurrency.ejb.error;

import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.concurrent.Asynchronous;

/**
 * This generic interface is using the Jakarta Concurrency @Asynchronous annotations.
 * Since users should not expect annotations on methods to be inherited from interfaces
 * (only super-classes) these annotations should be ignored.
 * The application should be installed, and a warning given to the user.
 */
public interface AsynchInterfaceLocal {

    @Asynchronous
    public void getThreadName();

    public void getThreadNameNonAsyc();

    @Asynchronous
    public CompletableFuture<String> getState(String city);

    @Asynchronous(executor = "java:comp/DefaultManagedExecutorService")
    public CompletableFuture<String> getStateFromService(String city);

}
