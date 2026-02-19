package com.arister.service;

import com.arister.enums.AccountStatus;
import com.arister.enums.AccountType;
import com.arister.model.Account;
import com.arister.proto.AccountResponse;
import com.arister.proto.AccountServiceGrpc;
import com.arister.proto.CreateAccountRequest;
import com.arister.proto.DeleteAccountRequest;
import com.arister.proto.DeleteAccountResponse;
import com.arister.proto.GetAccountByNumberRequest;
import com.arister.proto.GetAccountRequest;
import com.arister.proto.ListAccountsByCustomerRequest;
import com.arister.proto.ListAccountsRequest;
import com.arister.proto.ListAccountsResponse;
import com.arister.proto.UpdateAccountRequest;
import com.arister.repository.AccountRepository;
import io.grpc.stub.StreamObserver;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@GrpcService
@RequiredArgsConstructor
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final AccountRepository accountRepository;

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<AccountResponse> observer) {
        Account account = Account.builder()
                .customerId(request.getCustomerId())
                .branchId(request.getBranchId())
                .accountType(AccountType.valueOf(request.getAccountType().name()))
                .currency(request.getCurrency().isBlank() ? "USD" : request.getCurrency())
                .interestRate(request.getInterestRate().isBlank() ? BigDecimal.ZERO : new BigDecimal(request.getInterestRate()))
                .minBalance(request.getMinBalance().isBlank() ? new BigDecimal("500.00") : new BigDecimal(request.getMinBalance()))
                .overdraftLimit(request.getOverdraftLimit().isBlank() ? BigDecimal.ZERO : new BigDecimal(request.getOverdraftLimit()))
                .build();
        respond(observer, "Account created successfully", accountRepository.save(account));
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<AccountResponse> observer) {
        accountRepository.findById(request.getId())
                .ifPresentOrElse(
                        a -> respond(observer, "Account found", a),
                        () -> notFound(observer, "Account not found: " + request.getId()));
    }

    @Override
    public void getAccountByNumber(GetAccountByNumberRequest request, StreamObserver<AccountResponse> observer) {
        accountRepository.findByAccountNumber(request.getAccountNumber())
                .ifPresentOrElse(
                        a -> respond(observer, "Account found", a),
                        () -> notFound(observer, "Account not found: " + request.getAccountNumber()));
    }

    @Override
    public void updateAccount(UpdateAccountRequest request, StreamObserver<AccountResponse> observer) {
        accountRepository.findById(request.getId())
                .ifPresentOrElse(account -> {
                    if (!request.getInterestRate().isBlank()) account.setInterestRate(new BigDecimal(request.getInterestRate()));
                    if (!request.getMinBalance().isBlank()) account.setMinBalance(new BigDecimal(request.getMinBalance()));
                    if (!request.getOverdraftLimit().isBlank()) account.setOverdraftLimit(new BigDecimal(request.getOverdraftLimit()));
                    account.setStatus(AccountStatus.valueOf(request.getStatus().name()));
                    respond(observer, "Account updated successfully", accountRepository.save(account));
                }, () -> notFound(observer, "Account not found: " + request.getId()));
    }

    @Override
    public void deleteAccount(DeleteAccountRequest request, StreamObserver<DeleteAccountResponse> observer) {
        if (!accountRepository.existsById(request.getId())) {
            observer.onNext(DeleteAccountResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Account not found: " + request.getId())
                    .build());
            observer.onCompleted();
            return;
        }
        accountRepository.deleteById(request.getId());
        observer.onNext(DeleteAccountResponse.newBuilder().setSuccess(true).setMessage("Account deleted successfully").build());
        observer.onCompleted();
    }

    @Override
    public void listAccounts(ListAccountsRequest request, StreamObserver<ListAccountsResponse> observer) {
        int page = request.getPage();
        int size = request.getSize() > 0 ? request.getSize() : 20;
        Page<Account> result = accountRepository.findAll(PageRequest.of(page, size));
        observer.onNext(ListAccountsResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Accounts fetched")
                .addAllAccounts(result.getContent().stream().map(this::toProto).toList())
                .setTotalCount((int) result.getTotalElements())
                .setPage(page)
                .setSize(size)
                .build());
        observer.onCompleted();
    }

    @Override
    public void listAccountsByCustomer(ListAccountsByCustomerRequest request, StreamObserver<ListAccountsResponse> observer) {
        List<Account> accounts = accountRepository.findByCustomerId(request.getCustomerId());
        observer.onNext(ListAccountsResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Accounts fetched")
                .addAllAccounts(accounts.stream().map(this::toProto).toList())
                .setTotalCount(accounts.size())
                .build());
        observer.onCompleted();
    }

    private void respond(StreamObserver<AccountResponse> observer, String message, Account account) {
        observer.onNext(AccountResponse.newBuilder().setSuccess(true).setMessage(message).setAccount(toProto(account)).build());
        observer.onCompleted();
    }

    private void notFound(StreamObserver<AccountResponse> observer, String message) {
        observer.onNext(AccountResponse.newBuilder().setSuccess(false).setMessage(message).build());
        observer.onCompleted();
    }

    com.arister.proto.Account toProto(Account a) {
        com.arister.proto.Account.Builder b = com.arister.proto.Account.newBuilder()
                .setId(a.getId())
                .setAccountNumber(a.getAccountNumber() != null ? a.getAccountNumber() : "")
                .setCustomerId(a.getCustomerId())
                .setBranchId(a.getBranchId())
                .setAccountType(com.arister.proto.AccountType.valueOf(a.getAccountType().name()))
                .setBalance(a.getBalance() != null ? a.getBalance().toPlainString() : "0")
                .setCurrency(a.getCurrency() != null ? a.getCurrency() : "USD")
                .setInterestRate(a.getInterestRate() != null ? a.getInterestRate().toPlainString() : "0")
                .setMinBalance(a.getMinBalance() != null ? a.getMinBalance().toPlainString() : "0")
                .setOverdraftLimit(a.getOverdraftLimit() != null ? a.getOverdraftLimit().toPlainString() : "0")
                .setStatus(com.arister.proto.AccountStatus.valueOf(a.getStatus().name()))
                .setCreatedAt(a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
        if (a.getClosedAt() != null) b.setClosedAt(a.getClosedAt().toString());
        return b.build();
    }
}
