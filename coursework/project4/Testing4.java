package uk.ncl.CSC8016.jackbergus.coursework.project4;

import au.com.dius.pact.provider.org.fusesource.jansi.HtmlAnsiOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Testing4 {

    public static class Message implements Comparable<Message> {
        public final boolean isOK;
        public final String message;

        public Message(boolean isOK, String message) {
            this.isOK = isOK;
            this.message = message;
        }

        public String toString() {
            return (this.isOK ? ANSI_GREEN : ANSI_RED) + message + (ANSI_RESET);
        }

        @Override
        public int compareTo(@NotNull Message o) {
            return message.compareTo(o.message);
        }
    }
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static boolean isGlobal = true;
    public static double total_score = 0.0;
    public static double total_max_score = 0.0;

    public static @NotNull List<Message> testI_1() {
        var users = new HashMap<String, Double>() {{
                put("one", 1.0);
                put("two", 2.0);
        }};
        var b = new Solution(users);
        List<Message> messages = new ArrayList<>();
        var test = testBlockingEvent(() -> b.openTransaction("three").isEmpty(), 2000);
        if (!isPresentTest(test))  {
            messages.add(new Message(false, "ERROR (1): this should not have timed-out!"));
            messages.add(new Message(false, "ERROR (2): So, no value can be checked"));
        } else {
            messages.add(new Message(true, "GOOD (1): the test returned successfully"));
            if (test.get()) {
                messages.add(new Message(true, "GOOD (1): the test returned the corrct value"));
            } else {
                messages.add(new Message(false, "ERROR (2): the returned value was wrong!"));
            }
        }

        return messages;
    }

    public static @NotNull List<Message> testI_2() {
        var users = new HashMap<String, Double>() {{
            put("one", 1.0);
            put("two", 2.0);
        }};
        var b = new Solution(users);
        List<Message> messages = new ArrayList<>();
        Optional<TransactionCommands> test = testBlockingEvent(() -> {
            var obj = b.openTransaction("one");
            if  (!obj.isPresent())
                return null;
            return (TransactionCommands)obj.get();
        }, 2000);
        if (!isPresentTest(test))  {
            messages.add(new Message(false, "ERROR (1): this should not have timed-out!"));
            messages.add(new Message(false, "ERROR (2): So, no value can be checked [1]"));
            messages.add(new Message(false, "ERROR (3): So, cannot check for commit [2]"));
            messages.add(new Message(false, "ERROR (4): So, cannot check for commit value [3]"));
        } else {
            messages.add(new Message(true, "GOOD (1): the test returned successfully"));
            if (test.get() != null) {
                messages.add(new Message(true, "GOOD (2): the test returned the corrct value"));
                var noBlock = testBlockingEvent(() -> test.get().commit(), 2000);
                if (!isPresentTest(noBlock)) {
                    messages.add(new Message(false, "ERROR (3): Cannot commit"));
                    messages.add(new Message(false, "ERROR (4): So, I cannot check for the initial value"));
                } else {
                    messages.add(new Message(true, "GOOD (3): Can abort"));
                    messages.add(new Message(noBlock.get().totalAmount == 1.0, "Result for testing the amount (4)"));
                }
            } else {
                messages.add(new Message(false, "ERROR (2): the returned value was wrong!"));
                messages.add(new Message(false, "ERROR (3): Cannot test this!"));
                messages.add(new Message(false, "ERROR (4): Cannot test this!"));
            }
        }
        return messages;
    }

    public static @NotNull List<Message> testI_3() {
        var users = new HashMap<String, Double>() {{
            put("one", 1.0);
            put("two", 2.0);
        }};
        var b = new Solution(users);
        List<Message> messages = new ArrayList<>();
        Optional<TransactionCommands> test = testBlockingEvent(() -> {
            var obj = b.openTransaction("one");
            if  (!obj.isPresent())
                return null;
            return (TransactionCommands)obj.get();
        }, 2000);
        if (!isPresentTest(test))  {
            messages.add(new Message(false, "ERROR (1): this should not have timed-out!"));
            messages.add(new Message(false, "ERROR (2): So, no value can be checked [1]"));
            messages.add(new Message(false, "ERROR (3): So, cannot check for commit [2]"));
            messages.add(new Message(false, "ERROR (4): So, cannot check for commit value [3]"));
            messages.add(new Message(false, "ERROR (5)"));
            messages.add(new Message(false, "ERROR (6)"));
        } else {
            messages.add(new Message(true, "GOOD (1): the test returned successfully"));
            if (test.get() != null) {
                messages.add(new Message(true, "GOOD (2): the test returned the corrct value"));

                var noBlock = testBlockingEvent(() -> test.get().payMoneyToAccount(100), 2000);
                if ((!noBlock.isPresent()) || (!noBlock.get())) {
                    messages.add(new Message(false, "ERROR (2): cannot pay money in my account"));
                } else {
                    messages.add(new Message(true, "GOOD (2): I can pay money in my account"));
                }

                noBlock = testBlockingEvent(() -> test.get().payMoneyToAccount(50), 2000);
                if ((!noBlock.isPresent()) || (!noBlock.get())) {
                    messages.add(new Message(false, "ERROR (3): cannot pay money in my account"));
                } else {
                    messages.add(new Message(true, "GOOD (3): I can pay money in my account"));
                }

                var noBlock2 = testBlockingEvent(() -> test.get().commit(), 2000);
                if (!isPresentTest(noBlock2)) {
                    messages.add(new Message(false, "ERROR (4): Cannot commit"));
                    messages.add(new Message(false, "ERROR (5): So, I cannot check for the initial value"));
                    messages.add(new Message(false, "ERROR (6): So, I cannot check for the number of committed operations"));
                } else {
                    messages.add(new Message(true, "GOOD (4): Can commit"));
                    messages.add(new Message(noBlock2.get().totalAmount == 151.0, "Result for testing the amount (5)"));
                    messages.add(new Message(noBlock2.get().successfulOperations.size() == 2, "Result of the number of overall expected operations (6)"));
                }

                noBlock2 = testBlockingEvent(() -> test.get().commit(), 2000);
                if (isPresentTest(noBlock2)) {
                    messages.add(new Message(false, "ERROR (7): Cannot re-commit"));
                } else {
                    messages.add(new Message(true, "GOOD (7): Re-committing was disabled!"));
                }
            } else {
                messages.add(new Message(false, "ERROR (2): the returned value was wrong!"));
                messages.add(new Message(false, "ERROR (3): Cannot test this!"));
                messages.add(new Message(false, "ERROR (4): Cannot test this!"));
                messages.add(new Message(false, "ERROR (5): Cannot test this!"));
                messages.add(new Message(false, "ERROR (6): Cannot test this!"));
                messages.add(new Message(false, "ERROR (7): Cannot test this!"));
            }
        }
        return messages;
    }


    public static @NotNull List<Message> testI_4() {
        var users = new HashMap<String, Double>() {{
            put("one", 1.0);
            put("two", 2.0);
        }};
        var b = new Solution(users);
        List<Message> messages = new ArrayList<>();
        Optional<TransactionCommands> test = testBlockingEvent(() -> {
            var obj = b.openTransaction("one");
            if  (!obj.isPresent())
                return null;
            return (TransactionCommands)obj.get();
        }, 2000);
        if (!isPresentTest(test))  {
            messages.add(new Message(false, "ERROR (1): this should not have timed-out!"));
            messages.add(new Message(false, "ERROR (2): So, no value can be checked [1]"));
            messages.add(new Message(false, "ERROR (3): So, cannot check for commit [2]"));
            messages.add(new Message(false, "ERROR (4): So, cannot check for commit value [3]"));
            messages.add(new Message(false, "ERROR (5)"));
            messages.add(new Message(false, "ERROR (6)"));
        } else {
            messages.add(new Message(true, "GOOD (1): the test returned successfully"));
            if (test.get() != null) {
                messages.add(new Message(true, "GOOD (2): the test returned the corrct value"));


                var noBlock = testBlockingEvent(() -> test.get().withdrawMoney(0.0), 2000);
                if ((!noBlock.isPresent()) || (!noBlock.get())) {
                    messages.add(new Message(false, "ERROR (3): I can always withdraw 0 money from my account"));
                } else {
                    messages.add(new Message(true, "GOOD (3): I can withdraw no money from my account"));
                }

                noBlock = testBlockingEvent(() -> test.get().withdrawMoney(100.0), 2000);
                if ((!noBlock.isPresent()) || (noBlock.get())) {
                    messages.add(new Message(false, "ERROR (4): I shall not be able to withdraw more money than the one in my bank account"));
                } else {
                    messages.add(new Message(true, "GOOD (4): You stopped the withdrawal of more money than the one in the bank account"));
                }

                noBlock = testBlockingEvent(() -> test.get().payMoneyToAccount(50), 2000);
                if ((!noBlock.isPresent()) || (!noBlock.get())) {
                    messages.add(new Message(false, "ERROR (5): cannot pay money in my account"));
                } else {
                    messages.add(new Message(true, "GOOD (5): I can pay money in my account"));
                }

                noBlock = testBlockingEvent(() -> test.get().withdrawMoney(51.0), 2000);
                if ((!noBlock.isPresent()) || (!noBlock.get())) {
                    messages.add(new Message(false, "ERROR (6): I can always withdraw 51 money from my account if I have it"));
                } else {
                    messages.add(new Message(true, "GOOD (6): I can withdraw no money from my account"));
                }

                var noBlock2 = testBlockingEvent(() -> test.get().commit(), 2000);
                if (!isPresentTest(noBlock2)) {
                    messages.add(new Message(false, "ERROR (7): Cannot commit"));
                    messages.add(new Message(false, "ERROR (8): So, I cannot check for the initial value"));
                    messages.add(new Message(false, "ERROR (9): So, I cannot check for the number of committed operations"));
                    messages.add(new Message(false, "ERROR (9): So, I cannot check for the number of ignored operations"));
                } else {
                    messages.add(new Message(true, "GOOD (4): Can commit"));
                    messages.add(new Message(noBlock2.get().totalAmount == 0.0, "Result for testing the amount (5)"));
                    messages.add(new Message(noBlock2.get().successfulOperations.size() == 3, "Result of the number of overall expected operations (6)"));
                    messages.add(new Message(noBlock2.get().unsuccessfulOperation.size() == 0, "Result of the number of overall expected operations (6)"));
                }

                noBlock2 = testBlockingEvent(() -> test.get().commit(), 2000);
                if (isPresentTest(noBlock2)) {
                    messages.add(new Message(false, "ERROR (7): Cannot re-commit"));
                } else {
                    messages.add(new Message(true, "GOOD (7): Re-committing was disabled!"));
                }
            } else {
                messages.add(new Message(false, "ERROR (2): the returned value was wrong!"));
                messages.add(new Message(false, "ERROR (3): Cannot test this!"));
                messages.add(new Message(false, "ERROR (4): Cannot test this!"));
                messages.add(new Message(false, "ERROR (5): Cannot test this!"));
                messages.add(new Message(false, "ERROR (6): Cannot test this!"));
                messages.add(new Message(false, "ERROR (7): Cannot test this!"));
            }
        }
        return messages;
    }

//  Multithreading test using 2 Threads
public static @NotNull List<Message> testII_1() {
    Random rand = new Random();
    ArrayList<Message> final_messages = new ArrayList<>();
    // Iterating 20 times to check the results are deterministic
    for (int i = 0; i < 20; i++) {
        // Initialize fresh state for users each iteration
        var users = new HashMap<String, Double>() {{
            put("one", 100.0);
        }};
        Solution transactionCommands = new Solution(users);
        ArrayList<Thread> tl = new ArrayList<>();

        // Thread 1: Pay 50, Withdraw 30 (remaining 20)
        tl.add(new Thread(() -> {
            Optional<TransactionCommands> opt = transactionCommands.openTransaction("one");
            if (opt.isPresent()) {
                TransactionCommands tc = opt.get();
                tc.payMoneyToAccount(50.0);
                tc.withdrawMoney(30.0);
                tc.commit();
            }
        }));

        // Thread 2: Pay 40, Withdraw 20 (remaining 20)
        tl.add(new Thread(() -> {
            Optional<TransactionCommands> opt = transactionCommands.openTransaction("one");
            if (opt.isPresent()) {
                TransactionCommands tc = opt.get();
                tc.payMoneyToAccount(40.0);
                tc.withdrawMoney(20.0);
                tc.commit();
            }
        }));

        // Start threads with random delays up to a maximum delay of 500ms
        for (var x : tl) {
            try {
                Thread.sleep(rand.nextInt(500));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            x.start();
        }

        // Wait for all threads to complete
        try {
            for (Thread t : tl) t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Check final balance is as expected
        Optional<TransactionCommands> optFinal = transactionCommands.openTransaction("one");
        if (optFinal.isPresent()) {
            double finalBalance = optFinal.get().getTentativeTotalAmount();
            double expectedBalance = 140.0; // 100 + (50 - 30) + (40 - 20)
            if (finalBalance == expectedBalance) {
                final_messages.add(new Message(true, "Iteration " + i + ": Final balance correct (" + finalBalance + ")"));
            } else {
                final_messages.add(new Message(false, "Iteration " + i + ": Final balance incorrect. Expected " + expectedBalance + ", got " + finalBalance));
            }
        } else {
            final_messages.add(new Message(false, "Iteration " + i + ": Failed opening final transaction"));
        }
    }
    return final_messages;
}






    static HtmlAnsiOutputStream html = null;

    public static void noline(String line) {
        var x = line+System.lineSeparator();
        try {
            html.write(x.getBytes(StandardCharsets.UTF_8));
            html.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void writeln(String line) {
        var x = "<p>"+line+"</p>"+System.lineSeparator();
        try {
            html.write(x.getBytes(StandardCharsets.UTF_8));
            html.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String args[]) throws IOException {
        String StudentId = BankFacade.StudentID();
        FileOutputStream fos = new FileOutputStream(new File(StudentId+".html"));
        html = new HtmlAnsiOutputStream(fos);
        Consumer<String> toFile = Testing4::writeln;
        Consumer<String> toConsole = System.out::println;
        Consumer<String> currentConsumer = toConsole; //toFile, toConsole;

        Function<Boolean, List<Message>> none = null;
        List<Test> scoring = new ArrayList<>();

        if (currentConsumer.hashCode() == toFile.hashCode())
            noline("<!DOCTYPE html><html><body>");
        currentConsumer.accept("StudentId: " + StudentId);
        currentConsumer.accept("I. Single Threaded Correctness");
        currentConsumer.accept("==============================");
        currentConsumer.accept("");
        scoring.add(new Test(Testing4::testI_1,
                "I cannot open a transaction if the user does not appear in the initialization map.",
                10.0));
        scoring.add(new Test(Testing4::testI_2,
                "I can always open a transaction if the user, on the other hand, appears on the initialization map.",
                10.0));
        scoring.add(new Test(Testing4::testI_3,
                "After committing a transaction, the results provides the total changes into the account.",
                15.0));
        scoring.add(new Test(Testing4::testI_4,
                "No overdraft is allowed.",
                15.0));

//        scoring.add(new Test(Testing4::testI_2,
//                "I can always interact with a topic thread that was previously created.",
//                16.0));
//        scoring.add(new Test(Testing4::testI_3
//                "I am correctly handling the thread closure.",
//                4.0));
//        scoring.add(new Test(Testing4::testI_4,
//                "I am correctly handling the pollForUpdate method where, if successful requests are always fired before polling for events, should always return the most recent event available.",
//                7.0));
//        scoring.add(new Test(Testing4::testI_5,
//                "I am handling the event update messages correctly.",
//                3.0));
//        scoring.add(new Test(Testing4::testI_6,
//                "I am correctly handling the set method from ReadWriteMonitorMultiRead.",
//                10.0));
//        scoring.add(new Test(Testing4::testI_7,
//                "I am correctly handling the get method from ReadWriteMonitorMultiRead.",
//                6.0));


        FunctionScoring(scoring, currentConsumer);
        scoring.clear();

        currentConsumer.accept("");
        currentConsumer.accept("II. Multi-Threaded Correctness");
        currentConsumer.accept("==============================");
        currentConsumer.accept("");
        scoring.add(new Test(Testing4::testII_1,
                "Correctly handling the concurrent creation of different topics.",
                7.0));


//        scoring.add(new Test(Testing4::testII_2,
//                "Correctly handling the concurrent creation of different messages/posts within the same topic.",
//                9.0));

//        //currentConsumer.accept("The following two are tests usually not working on studen's projects");
//        scoring.add(new Test(Testing4::testII_3,
//                "The moderator is able to wait to receive 10 messages, after which the main thread and their posts are deleted",
//                12.0));

//        scoring.add(new Test(Testing4::testII_4,
//                "While having only one single user running and one subscriber to receive the updates from the website, no interference occurs, and all the perceived events actually match the expected results",
//                12.0));

        FunctionScoring(scoring, currentConsumer);
        scoring.clear();
        executor.shutdown();
        currentConsumer.accept("");
        currentConsumer.accept("[" + StudentId + "] Total Score: " + total_score + "/" + total_max_score + " = " + (total_score/total_max_score));
        if (currentConsumer.hashCode() == toFile.hashCode())
            noline("</body></html>");
        fos.close();
    }



    public static double sumUpOk(Collection<Message> msg) {
        if ((msg == null) || msg.isEmpty()) return 0.0;
        else {
            double N = msg.size();
            double OK = 0.0;
            for (var x : msg) if (x.isOK) OK++;
            return OK / N;
        }
    }
//  This
    public static void FunctionScoring(List<Test> scoring, Consumer<String> c) {
        for (var x : scoring) {
            double score = 0;
            int nMsg = 0;
            List<Message> result = null;
            if (x != null && x.test != null) {
                result = x.test.get();
                if (result != null) {
                    nMsg = result.size();
                    score = sumUpOk(result) * x.max_score;
                }
                total_max_score += x.max_score;
            }
            c.accept(" * " + x.name + "[#"+nMsg+"]. Score = "+score);
            if (result != null)
                for (var res : result)
                    c.accept("   - " + res);
            total_score += score;
        }
    }


    private static ExecutorService executor = Executors.newFixedThreadPool(100);

    public static <T> Optional<T> testBlockingEvent(Callable<T> r, long time) {
        FutureTask<T> futureTask = new FutureTask<>(r);
        executor.submit(futureTask);
        return testFuture(futureTask, time);
    }

    public static <T> Optional<T> testBlockingEvent(Callable<T> r) {
        FutureTask<T> futureTask = new FutureTask<>(r);
        executor.submit(futureTask);
        return testFuture(futureTask, 3000);
    }

    public static <T> Optional<T> testFuture(FutureTask<T> futureTask, long time) {
        try {
            T s = futureTask.get(time, TimeUnit.MILLISECONDS);
            if (s != null)
                return Optional.of(s);
            else
                return Optional.empty();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            futureTask.cancel(true);
            return Optional.empty();
        }
    }

    public static<T> boolean testDoesNotTimeOut(Callable<T> r) {
        FutureTask<T> futureTask = new FutureTask<>(r);
        executor.submit(futureTask);
        return testDoesNotTimeOut(futureTask, 100);
    }

    public static<T> boolean testDoesNotTimeOut(Callable<T> r, long time) {
        FutureTask<T> futureTask = new FutureTask<>(r);
        executor.submit(futureTask);
        return testDoesNotTimeOut(futureTask, time);
    }

    public static <T> boolean testDoesNotTimeOut(FutureTask<T> futureTask, long time) {
        try {
            T s = futureTask.get(time, TimeUnit.MILLISECONDS);
            return s != null;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return false;
        }
    }

    public static <T>  boolean isPresentTest(Optional<T> result1) {
        return result1.isPresent() && result1.get() != null;
    }

    public static boolean testBooleanBlockingEvent(Callable<Boolean> r) {
        var result = testBlockingEvent(r);
        return result.isPresent() ? result.get() : false;
    }


    public static class Test {
        public final Supplier<List<Message>> test;
        public final String name;
        public final double max_score;

        public Test(Supplier<List<Message>> test, String name, double max_score) {
            this.test = test;
            this.name = name;
            this.max_score = max_score;
        }
    }



}
