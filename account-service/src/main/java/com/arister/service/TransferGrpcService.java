package com.arister.service;

import com.arister.enums.TransactionType;
import com.arister.enums.TransferStatus;
import com.arister.model.Account;
import com.arister.model.Transaction;
import com.arister.model.Transfer;
import com.arister.proto.CreateTransferRequest;
import com.arister.proto.GetTransferRequest;
import com.arister.proto.ListTransfersByAccountRequest;
import com.arister.proto.ListTransfersResponse;
import com.arister.proto.TransferResponse;
import com.arister.proto.TransferServiceGrpc;
import com.arister.repository.AccountRepository;
import com.arister.repository.TransactionRepository;
import com.arister.repository.TransferRepository;
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
public class TransferGrpcService extends TransferServiceGrpc.TransferServiceImplBase {

    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void createTransfer(CreateTransferRequest request, StreamObserver<TransferResponse> observer) {
        Account from = accountRepository.findById(request.getFromAccountId()).orElse(null);
        Account to = accountRepository.findById(request.getToAccountId()).orElse(null);

        if (from == null) {
            notFound(observer, "Source account not found: " + request.getFromAccountId());
            return;
        }
        if (to == null) {
            notFound(observer, "Destination account not found: " + request.getToAccountId());
            return;
        }

        BigDecimal amount = new BigDecimal(request.getAmount());
        BigDecimal fee = request.getFee().isBlank() ? BigDecimal.ZERO : new BigDecimal(request.getFee());
        BigDecimal totalDebit = amount.add(fee);

        if (from.getBalance().subtract(totalDebit).compareTo(from.getOverdraftLimit().negate()) < 0) {
            observer.onError(Status.FAILED_PRECONDITION
                    .withDescription("Insufficient funds. Available: " + from.getBalance().toPlainString())
                    .asRuntimeException());
            return;
        }

        BigDecimal fromBalanceBefore = from.getBalance();
        BigDecimal fromBalanceAfter = fromBalanceBefore.subtract(totalDebit);
        BigDecimal toBalanceBefore = to.getBalance();
        BigDecimal toBalanceAfter = toBalanceBefore.add(amount);

        from.setBalance(fromBalanceAfter);
        to.setBalance(toBalanceAfter);
        accountRepository.save(from);
        accountRepository.save(to);

        Transaction outTxn = Transaction.builder()
                .accountId(from.getId())
                .transactionType(TransactionType.TRANSFER_OUT)
                .amount(totalDebit)
                .balanceBefore(fromBalanceBefore)
                .balanceAfter(fromBalanceAfter)
                .description(request.getDescription())
                .build();
        Transaction savedOut = transactionRepository.save(outTxn);

        Transaction inTxn = Transaction.builder()
                .accountId(to.getId())
                .transactionType(TransactionType.TRANSFER_IN)
                .amount(amount)
                .balanceBefore(toBalanceBefore)
                .balanceAfter(toBalanceAfter)
                .description(request.getDescription())
                .relatedTxnId(savedOut.getId())
                .build();
        Transaction savedIn = transactionRepository.save(inTxn);

        savedOut.setRelatedTxnId(savedIn.getId());
        transactionRepository.save(savedOut);

        Transfer transfer = Transfer.builder()
                .fromAccountId(from.getId())
                .toAccountId(to.getId())
                .amount(amount)
                .fee(fee)
                .status(TransferStatus.COMPLETED)
                .description(request.getDescription())
                .build();
        respond(observer, "Transfer completed successfully", transferRepository.save(transfer));
    }

    @Override
    public void getTransfer(GetTransferRequest request, StreamObserver<TransferResponse> observer) {
        transferRepository.findById(request.getId())
                .ifPresentOrElse(
                        t -> respond(observer, "Transfer found", t),
                        () -> notFound(observer, "Transfer not found: " + request.getId()));
    }

    @Override
    public void listTransfersByAccount(ListTransfersByAccountRequest request, StreamObserver<ListTransfersResponse> observer) {
        int page = request.getPage();
        int size = request.getSize() > 0 ? request.getSize() : 20;
        Page<Transfer> result = transferRepository.findByAccountId(request.getAccountId(), PageRequest.of(page, size));
        observer.onNext(ListTransfersResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Transfers fetched")
                .addAllTransfers(result.getContent().stream().map(this::toProto).toList())
                .setTotalCount((int) result.getTotalElements())
                .setPage(page)
                .setSize(size)
                .build());
        observer.onCompleted();
    }

    private void respond(StreamObserver<TransferResponse> observer, String message, Transfer transfer) {
        observer.onNext(TransferResponse.newBuilder().setSuccess(true).setMessage(message).setTransfer(toProto(transfer)).build());
        observer.onCompleted();
    }

    private void notFound(StreamObserver<TransferResponse> observer, String message) {
        observer.onNext(TransferResponse.newBuilder().setSuccess(false).setMessage(message).build());
        observer.onCompleted();
    }

    private com.arister.proto.Transfer toProto(Transfer t) {
        return com.arister.proto.Transfer.newBuilder()
                .setId(t.getId())
                .setTransferRef(t.getTransferRef() != null ? t.getTransferRef() : "")
                .setFromAccountId(t.getFromAccountId())
                .setToAccountId(t.getToAccountId())
                .setAmount(t.getAmount().toPlainString())
                .setFee(t.getFee() != null ? t.getFee().toPlainString() : "0")
                .setStatus(com.arister.proto.TransferStatus.valueOf(t.getStatus().name()))
                .setDescription(t.getDescription() != null ? t.getDescription() : "")
                .setCreatedAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : "")
                .build();
    }
}
