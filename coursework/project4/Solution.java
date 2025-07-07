package uk.ncl.CSC8016.jackbergus.coursework.project4;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Solution extends BankFacade {
/**    ConcurrentHashMap allows multiple threads
        to read and write data simultaneously
 */
    private final ConcurrentHashMap<String, Double> accounts;
/**    ReentrantReadWriteLock can distinguish locks
        between read and write operations boosting performance
 */
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> accountLocks;
    private final ConcurrentHashMap<String, Map<BigInteger, TransactionState>> activeTransactions;
//    Data type chosen to perform operations with larger numbers
    private final AtomicBigInteger transactionIdGenerator;

    private static class TransactionState {
        double initialBalance;
        double tentativeBalance;
        List<Operation> operations;
        boolean isCommitted;
        boolean isAborted;
        boolean isClosed;
//  Initialize variables for constructor
        TransactionState(double initialBalance) {
            this.initialBalance = initialBalance;
            this.tentativeBalance = initialBalance;
            this.operations = new ArrayList<>();
            this.isCommitted = false;
            this.isAborted = false;
            this.isClosed = false;
        }
    }
/**  Constructor takes HashMap of keys corresponding to userId and double being the
    initial amount of money in the account
 */
    public Solution(HashMap<String, Double> userIdToTotalInitialAmount) {
        super(userIdToTotalInitialAmount);
        this.accounts = new ConcurrentHashMap<>(userIdToTotalInitialAmount);
        this.accountLocks = new ConcurrentHashMap<>();
        this.activeTransactions = new ConcurrentHashMap<>();
        this.transactionIdGenerator = new AtomicBigInteger(BigInteger.ZERO);

        /** Initialize locks for each account and populate user transactions along with read
         and write operations for each user account
         */
        for (String userId : userIdToTotalInitialAmount.keySet()) {
            accountLocks.put(userId, new ReentrantReadWriteLock(true));
            activeTransactions.put(userId, new ConcurrentHashMap<>());
        }
    }

    /**
     * Method to get transactional commands of a user given their userId
     * @param userId  For an oversimplification, the user is only required to access its account thorugh their id.
     *
     * @return
     */
    @Override
    public Optional<TransactionCommands> openTransaction(String userId) {
        if (!accounts.containsKey(userId)) {
            return Optional.empty();
        }

        final BigInteger transactionId = transactionIdGenerator.incrementAndGet();
        final ReentrantReadWriteLock accountLock = accountLocks.get(userId);

        double currentBalance;
        accountLock.readLock().lock();
        try {
            currentBalance = accounts.get(userId);
        } finally {
            accountLock.readLock().unlock();
        }

        final TransactionState state = new TransactionState(currentBalance);
        activeTransactions.get(userId).put(transactionId, state);

        return Optional.of(new TransactionCommands() {
            @Override
            public BigInteger getTransactionId() {
                return transactionId;
            }

            /**
             * Method to obtain total amount of money present in user account
             * @return
             */
            @Override
            public double getTentativeTotalAmount() {
                TransactionState state = activeTransactions.get(userId).get(transactionId);
                if (state == null || state.isClosed) {
                    accountLock.readLock().lock();
                    try {
                        return accounts.get(userId);
                    } finally {
                        accountLock.readLock().unlock();
                    }
                }
                return state.tentativeBalance;
            }

            /**
             * Method to withdraw money from user account
             * @param amount
             * @return boolean state
             */
            @Override
            public boolean withdrawMoney(double amount) {
                if (amount < 0) return false;

                TransactionState state = activeTransactions.get(userId).get(transactionId);
                if (state == null || state.isClosed) {
                    return false;
                }

                synchronized (state) {
                    if (state.isClosed) {
                        return false;
                    }

                    if (state.tentativeBalance >= amount) {
                        state.tentativeBalance -= amount;
                        state.operations.add(Operation.Withdraw(amount, state.operations.size()));
                        return true;
                    } else {
                        // Record the operation but mark it as unsuccessful (will be ignored later)
                        state.operations.add(Operation.Withdraw(amount, state.operations.size()));
                        return false;
                    }
                }
            }

            /**
             * Method to pay money to account
             * @param amount double
             * @return boolean state
             */
            @Override
            public boolean payMoneyToAccount(double amount) {
                if (amount < 0) return false;

                TransactionState state = activeTransactions.get(userId).get(transactionId);
                if (state == null || state.isClosed) {
                    return false;
                }

                synchronized (state) {
                    if (state.isClosed) {
                        return false;
                    }

                    state.tentativeBalance += amount;
                    state.operations.add(Operation.Pay(amount, state.operations.size()));
                    return true;
                }
            }

            /**
             * Method to abort transaction
             */
            @Override
            public void abort() {
                TransactionState state = activeTransactions.get(userId).get(transactionId);
                if (state != null && !state.isClosed) {
                    synchronized (state) {
                        if (!state.isClosed) {
                            state.isAborted = true;
                            state.isClosed = true;
                        }
                    }
                }
            }

            /**
             * Method to commit with the transactions using read and write lock to ensure
             * data integrity during mathematical operations
             * @return
             */
            @Override
            public CommitResult commit() {
                TransactionState state = activeTransactions.get(userId).get(transactionId);
                if (state == null || state.isClosed) {
                    return null;
                }

                synchronized (state) {
                    if (state.isClosed) {
                        return null;
                    }

                    state.isClosed = true;

                    if (state.isAborted) {
                        // If aborted, return with no successful operations
                        accountLock.readLock().lock();
                        try {
                            double currentAccountBalance = accounts.get(userId);
                            return new CommitResult(new ArrayList<>(), state.operations, currentAccountBalance);
                        } finally {
                            accountLock.readLock().unlock();
                            activeTransactions.get(userId).remove(transactionId);
                        }
                    }
                }

                // Acquire write lock for the actual commit
                accountLock.writeLock().lock();
                try {
                    double currentBalance = accounts.get(userId);
                    double runningBalance = currentBalance;
                    List<Operation> successfulOps = new ArrayList<>();
                    List<Operation> ignoredOps = new ArrayList<>();

                    // Validate and apply operations
                    for (Operation op : state.operations) {
                        if (op.getT() == OperationType.Pay) {
                            runningBalance += op.getAmount();
                            successfulOps.add(op);
                        } else if (op.getT() == OperationType.Withdraw) {
                            if (runningBalance >= op.getAmount()) {
                                runningBalance -= op.getAmount();
                                successfulOps.add(op);
                            } else {
                                ignoredOps.add(op);
                            }
                        }
                    }

                    // Update the account balance
                    accounts.put(userId, runningBalance);
                    state.isCommitted = true;

                    return new CommitResult(successfulOps, ignoredOps, runningBalance);
                } finally {
                    accountLock.writeLock().unlock();
                    // Remove the transaction from active transactions
                    activeTransactions.get(userId).remove(transactionId);
                }
            }

            /**
             * Method to close transaction
             */
            @Override
            public void close() {
                TransactionState state = activeTransactions.get(userId).get(transactionId);
                if (state != null && !state.isClosed) {
                    commit();
                }
            }
        });
    }
}