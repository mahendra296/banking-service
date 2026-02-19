package com.arister.service;

import com.arister.enums.TransactionType;
import com.arister.model.Account;
import com.arister.model.Transaction;
import com.arister.proto.DepositRequest;
import com.arister.proto.GetTransactionRequest;
import com.arister.proto.ListTransactionsByAccountRequest;
import com.arister.proto.ListTransactionsResponse;
import com.arister.proto.TransactionResponse;
import com.arister.proto.TransactionServiceGrpc;
import com.arister.proto.WithdrawRequest;
import com.arister.repository.AccountRepository;
import com.arister.repository.TransactionRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@GrpcService
@RequiredArgsConstructor
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void deposit(DepositRequest request, StreamObserver<TransactionResponse> observer) {
        Account account = accountRepository.findById(request.getAccountId()).orElse(null);
        if (account == null) {
            notFound(observer, "Account not found: " + request.getAccountId());
            return;
        }
        BigDecimal amount = new BigDecimal(request.getAmount());
        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        account.setBalance(balanceAfter);
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .accountId(account.getId())
                .transactionType(TransactionType.DEPOSIT)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(request.getDescription())
                .performedBy(request.getPerformedBy() > 0 ? request.getPerformedBy() : null)
                .build();
        respond(observer, "Deposit successful", transactionRepository.save(txn));
    }

    @Override
    @Transactional
    public void withdraw(WithdrawRequest request, StreamObserver<TransactionResponse> observer) {
        Account account = accountRepository.findById(request.getAccountId()).orElse(null);
        if (account == null) {
            notFound(observer, "Account not found: " + request.getAccountId());
            return;
        }
        BigDecimal amount = new BigDecimal(request.getAmount());
        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.subtract(amount);
        BigDecimal effectiveMinimum = account.getOverdraftLimit().negate();

        if (balanceAfter.compareTo(effectiveMinimum) < 0) {
            observer.onError(Status.FAILED_PRECONDITION
                    .withDescription("Insufficient funds. Available: " + balanceBefore.toPlainString())
                    .asRuntimeException());
            return;
        }
        account.setBalance(balanceAfter);
        accountRepository.save(account);

        Transaction txn = Transaction.builder()
                .accountId(account.getId())
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(request.getDescription())
                .performedBy(request.getPerformedBy() > 0 ? request.getPerformedBy() : null)
                .build();
        respond(observer, "Withdrawal successful", transactionRepository.save(txn));
    }

    @Override
    public void getTransaction(GetTransactionRequest request, StreamObserver<TransactionResponse> observer) {
        transactionRepository.findById(request.getId())
                .ifPresentOrElse(
                        t -> respond(observer, "Transaction found", t),
                        () -> notFound(observer, "Transaction not found: " + request.getId()));
    }

    @Override
    public void listTransactionsByAccount(ListTransactionsByAccountRequest request, StreamObserver<ListTransactionsResponse> observer) {
        int page = request.getPage();
        int size = request.getSize() > 0 ? request.getSize() : 20;
        Page<Transaction> result = transactionRepository.findByAccountId(request.getAccountId(), PageRequest.of(page, size));
        observer.onNext(ListTransactionsResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Transactions fetched")
                .addAllTransactions(result.getContent().stream().map(this::toProto).toList())
                .setTotalCount((int) result.getTotalElements())
                .setPage(page)
                .setSize(size)
                .build());
        observer.onCompleted();
    }

    private void respond(StreamObserver<TransactionResponse> observer, String message, Transaction txn) {
        observer.onNext(TransactionResponse.newBuilder().setSuccess(true).setMessage(message).setTransaction(toProto(txn)).build());
        observer.onCompleted();
    }

    private void notFound(StreamObserver<TransactionResponse> observer, String message) {
        observer.onNext(TransactionResponse.newBuilder().setSuccess(false).setMessage(message).build());
        observer.onCompleted();
    }

    private com.arister.proto.Transaction toProto(Transaction t) {
        com.arister.proto.Transaction.Builder b = com.arister.proto.Transaction.newBuilder()
                .setId(t.getId())
                .setTransactionRef(t.getTransactionRef() != null ? t.getTransactionRef() : "")
                .setAccountId(t.getAccountId())
                .setTransactionType(com.arister.proto.TransactionType.valueOf(t.getTransactionType().name()))
                .setAmount(t.getAmount().toPlainString())
                .setBalanceBefore(t.getBalanceBefore().toPlainString())
                .setBalanceAfter(t.getBalanceAfter().toPlainString())
                .setDescription(t.getDescription() != null ? t.getDescription() : "")
                .setCreatedAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "");
        if (t.getRelatedTxnId() != null) b.setRelatedTxnId(t.getRelatedTxnId());
        if (t.getPerformedBy() != null) b.setPerformedBy(t.getPerformedBy());
        return b.build();
    }
}
