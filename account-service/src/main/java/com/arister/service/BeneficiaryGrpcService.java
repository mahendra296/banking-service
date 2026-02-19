package com.arister.service;

import com.arister.model.Beneficiary;
import com.arister.proto.AddBeneficiaryRequest;
import com.arister.proto.BeneficiaryResponse;
import com.arister.proto.BeneficiaryServiceGrpc;
import com.arister.proto.GetBeneficiaryRequest;
import com.arister.proto.ListBeneficiariesRequest;
import com.arister.proto.ListBeneficiariesResponse;
import com.arister.proto.RemoveBeneficiaryRequest;
import com.arister.proto.RemoveBeneficiaryResponse;
import com.arister.proto.UpdateBeneficiaryRequest;
import com.arister.repository.BeneficiaryRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class BeneficiaryGrpcService extends BeneficiaryServiceGrpc.BeneficiaryServiceImplBase {

    private final BeneficiaryRepository beneficiaryRepository;

    @Override
    public void addBeneficiary(AddBeneficiaryRequest request, StreamObserver<BeneficiaryResponse> observer) {
        if (beneficiaryRepository.existsByCustomerIdAndAccountNumber(request.getCustomerId(), request.getAccountNumber())) {
            observer.onError(Status.ALREADY_EXISTS.withDescription("Beneficiary already exists for this account number").asRuntimeException());
            return;
        }
        Beneficiary b = Beneficiary.builder()
                .customerId(request.getCustomerId())
                .beneficiaryName(request.getBeneficiaryName())
                .accountNumber(request.getAccountNumber())
                .bankName(request.getBankName().isBlank() ? "SAME_BANK" : request.getBankName())
                .ifscCode(request.getIfscCode())
                .build();
        respond(observer, "Beneficiary added successfully", beneficiaryRepository.save(b));
    }

    @Override
    public void getBeneficiary(GetBeneficiaryRequest request, StreamObserver<BeneficiaryResponse> observer) {
        beneficiaryRepository.findById(request.getId())
                .ifPresentOrElse(
                        b -> respond(observer, "Beneficiary found", b),
                        () -> notFound(observer, "Beneficiary not found: " + request.getId()));
    }

    @Override
    public void updateBeneficiary(UpdateBeneficiaryRequest request, StreamObserver<BeneficiaryResponse> observer) {
        beneficiaryRepository.findById(request.getId())
                .ifPresentOrElse(b -> {
                    if (!request.getBeneficiaryName().isBlank()) b.setBeneficiaryName(request.getBeneficiaryName());
                    if (!request.getBankName().isBlank()) b.setBankName(request.getBankName());
                    if (!request.getIfscCode().isBlank()) b.setIfscCode(request.getIfscCode());
                    b.setVerified(request.getIsVerified());
                    respond(observer, "Beneficiary updated successfully", beneficiaryRepository.save(b));
                }, () -> notFound(observer, "Beneficiary not found: " + request.getId()));
    }

    @Override
    public void removeBeneficiary(RemoveBeneficiaryRequest request, StreamObserver<RemoveBeneficiaryResponse> observer) {
        if (!beneficiaryRepository.existsById(request.getId())) {
            observer.onNext(RemoveBeneficiaryResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Beneficiary not found: " + request.getId())
                    .build());
            observer.onCompleted();
            return;
        }
        beneficiaryRepository.deleteById(request.getId());
        observer.onNext(RemoveBeneficiaryResponse.newBuilder().setSuccess(true).setMessage("Beneficiary removed successfully").build());
        observer.onCompleted();
    }

    @Override
    public void listBeneficiaries(ListBeneficiariesRequest request, StreamObserver<ListBeneficiariesResponse> observer) {
        List<Beneficiary> list = beneficiaryRepository.findByCustomerId(request.getCustomerId());
        observer.onNext(ListBeneficiariesResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Beneficiaries fetched")
                .addAllBeneficiaries(list.stream().map(this::toProto).toList())
                .build());
        observer.onCompleted();
    }

    private void respond(StreamObserver<BeneficiaryResponse> observer, String message, Beneficiary b) {
        observer.onNext(BeneficiaryResponse.newBuilder().setSuccess(true).setMessage(message).setBeneficiary(toProto(b)).build());
        observer.onCompleted();
    }

    private void notFound(StreamObserver<BeneficiaryResponse> observer, String message) {
        observer.onNext(BeneficiaryResponse.newBuilder().setSuccess(false).setMessage(message).build());
        observer.onCompleted();
    }

    private com.arister.proto.Beneficiary toProto(Beneficiary b) {
        return com.arister.proto.Beneficiary.newBuilder()
                .setId(b.getId())
                .setCustomerId(b.getCustomerId())
                .setBeneficiaryName(b.getBeneficiaryName())
                .setAccountNumber(b.getAccountNumber())
                .setBankName(b.getBankName() != null ? b.getBankName() : "SAME_BANK")
                .setIfscCode(b.getIfscCode() != null ? b.getIfscCode() : "")
                .setIsVerified(b.isVerified())
                .setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "")
                .build();
    }
}
