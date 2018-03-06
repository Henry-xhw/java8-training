package com.sg.java8.training.completable.future;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A few {@link java.util.concurrent.CompletableFuture} usage samples
 */
public class CompletableFutureMain {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    private static final Executor EXECUTOR = Executors.newFixedThreadPool(AVAILABLE_PROCESSORS / 4);

    public static void main(String[] args) {
        helloSimpleCompletableFutures();

        simpleCompletableFutures();

        chainedCompletionStages();

        simpleProductsOperations();

        moreComplexProductsOperations();

        shutdownExecutor();
    }

    private static void helloSimpleCompletableFutures() {
        final CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            displayCurrentThread();
            return "I will run on Saturday";
        });
        System.out.println(completableFuture.join());

        completableFuture.thenAcceptAsync(value -> {
            displayCurrentThread();
            System.out.println("The received value is " + value);
        });

        completableFuture.exceptionally(ex -> "Some exception occurred");

        String processingResult = completableFuture.join();
        System.out.println("The processing returned " + processingResult);
    }

    private static void simpleCompletableFutures() {
        final CompletableFuture<String> completableFuture =
                CompletableFuture.supplyAsync(() -> "a very simple text");

        final Consumer<String> stringConsumer = displayValueAndRunningThread();
        completableFuture.thenAcceptAsync(stringConsumer);

        final CompletableFuture<String> anotherFuture =
                CompletableFuture.supplyAsync(() -> "another text");

        completableFuture.thenCompose(value -> anotherFuture);
        System.out.println(anotherFuture.thenApplyAsync(value -> value)
                                        .join());

        completableFuture.exceptionally(throwable -> "Thrown: " + throwable.getMessage());
        completableFuture.thenApplyAsync(String::toUpperCase, Executors.newCachedThreadPool());
        completableFuture.acceptEither(anotherFuture, stringConsumer);
    }

    private static void chainedCompletionStages() {
        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> {
            displayCurrentThread();
            return "first";
        }, EXECUTOR);

        CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> {
            displayCurrentThread();
            return "second";
        }, EXECUTOR);

        CompletableFuture<Integer> third = CompletableFuture.supplyAsync(() -> {
            displayCurrentThread();
            return 7;
        }, EXECUTOR);

        final CompletableFuture<Integer> future =
                first.thenComposeAsync(value -> second)
                     .thenComposeAsync(value -> third);

        System.out.println(future.join());

        multipleCallsProcessing(first, second, third);
    }

    private static void multipleCallsProcessing(final CompletableFuture<String> first,
                                                final CompletableFuture<String> second,
                                                final CompletableFuture<Integer> third) {

        // will return a CompletableFuture<Void> when all the tasks will be finished --> no further processing can be made
        final CompletableFuture<Void> allOfFuture = CompletableFuture.allOf(first, second, third);
        allOfFuture.thenAccept(value -> notifyFinishedTasks());

        // will return a CompletableFuture<Object> when any of the tasks will be finished --> can chain further processing(s)
        final CompletableFuture<Object> anyOfFuture = CompletableFuture.anyOf(first, second, third);
        anyOfFuture.thenAccept(returnValue -> System.out.println("Processing the value '" + returnValue + "'..."));

        final Object result = anyOfFuture.join(); // the first finished processing
        System.out.println("The first finished processing is '" + result + "'");
    }

    private static void simpleProductsOperations() {
        final ProductProcessor productProcessor = new ProductProcessor();

        final CompletableFuture<Long> getProductsStock = productProcessor.getProductsStock("iPad");
        final Function<Long, CompletableFuture<Double>> getProductsPrice = productProcessor.getProductsPrice();
        final Function<Double, CompletableFuture<String>> getProductsDisplayText = productProcessor.getDisplayedText();

        /*
            The three processing stages:
                - 1) get products stock
                - 2) get products price, for the resulted stock
                - 3) get the displayed text, for the products price and stock
        */

        final String displayedText = getProductsStock.thenComposeAsync(getProductsPrice, EXECUTOR)
                                                     .thenComposeAsync(getProductsDisplayText)
                                                     .exceptionally(Throwable::getMessage)
                                                     .join();
        System.out.println("Got the text '" + displayedText + "'");

        shutdownExecutor();
    }

    private static void moreComplexProductsOperations() {
        final ProductProcessor productProcessor = new ProductProcessor();

        final CompletableFuture<Long> getProductsStock = productProcessor.getProductsStock("iPad");
        final CompletableFuture<Long> getReserveStock = productProcessor.getReserveStock("iPad");
        final Function<Long, CompletableFuture<Double>> getProductsPrice = productProcessor.getProductsPrice();
        final Function<Double, CompletableFuture<String>> getProductsDisplayText = productProcessor.getDisplayedText();

        /*
            The five processing stages:
                - 1) get products stock OR get the reserve stock (whichever finishes first)
                - 2) get products price, for the resulted stock
                - 3) get the displayed text, for the products price and stock
                - 4) when either the displayed text or an exception is returned, complete the stage asynchronously
        */

        // --> use the final call as a sync / async calls orchestration
        final String displayedText = getProductsStock.applyToEitherAsync(getReserveStock, Function.identity(), EXECUTOR)
                                                     .thenComposeAsync(getProductsPrice, EXECUTOR)
                                                     .thenComposeAsync(getProductsDisplayText, EXECUTOR)
                                                     .whenCompleteAsync(CompletableFutureMain::processResult, EXECUTOR)
                                                     .join();
        System.out.println("Got the text '" + displayedText + "'");

        shutdownExecutor();
    }

    private static void processResult(final String result, final Throwable exception) {
        if (exception != null) {
            throw new RuntimeException(exception.getMessage());
        } else {
            CompletableFuture.supplyAsync(() -> result, EXECUTOR);
        }
    }

    private static void displayCurrentThread() {
        System.out.println(Thread.currentThread().getName());
    }

    private static void notifyFinishedTasks() {
        System.out.println(Thread.currentThread().getName() + " - All good");
    }

    private static Consumer<String> displayValueAndRunningThread() {
        return value -> System.out.println(Thread.currentThread().getName() + " - " + value);
    }

    private static void shutdownExecutor() {
        ((ExecutorService) EXECUTOR).shutdown();
        System.out.println("The executor was properly shutdown");
    }
}