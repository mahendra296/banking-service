package com.arister.service;

import com.arister.enums.KycVerificationStatus;
import com.arister.model.CustomerKyc;
import com.arister.proto.AddCustomerKycRequest;
import com.arister.proto.CustomerKycResponse;
import com.arister.proto.CustomerKycServiceGrpc;
import com.arister.proto.GetCustomerKycRequest;
import com.arister.proto.ListCustomerKycRequest;
import com.arister.proto.ListCustomerKycResponse;
import com.arister.proto.UpdateKycVerificationRequest;
import com.arister.repository.CustomerKycRepository;
import com.arister.repository.CustomerRepository;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class CustomerKycGrpcService extends CustomerKycServiceGrpc.CustomerKycServiceImplBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CustomerRepository customerRepository;
    private final CustomerKycRepository customerKycRepository;

    public CustomerKycGrpcService(CustomerRepository customerRepository, CustomerKycRepository customerKycRepository) {
        this.customerRepository = customerRepository;
        this.customerKycRepository = customerKycRepository;
    }

    @Override
    public void addCustomerKyc(AddCustomerKycRequest request, StreamObserver<CustomerKycResponse> responseObserver) {
        if (!customerRepository.existsById(request.getCustomerId())) {
            respond(
                    responseObserver,
                    CustomerKycResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Customer not found with id: " + request.getCustomerId())
                            .build());
            return;
        }

        CustomerKyc kyc = CustomerKyc.builder()
                .customerId(request.getCustomerId())
                .documentType(request.getDocumentType().isBlank() ? null : request.getDocumentType())
                .documentNumber(request.getDocumentNumber().isBlank() ? null : request.getDocumentNumber())
                .issueDate(request.getIssueDate().isBlank() ? null : LocalDate.parse(request.getIssueDate(), DATE_FMT))
                .expiryDate(
                        request.getExpiryDate().isBlank() ? null : LocalDate.parse(request.getExpiryDate(), DATE_FMT))
                .issuingCountry(request.getIssuingCountry().isBlank() ? null : request.getIssuingCountry())
                .issuingAuthority(request.getIssuingAuthority().isBlank() ? null : request.getIssuingAuthority())
                .verificationStatus(KycVerificationStatus.PENDING)
                .notes(request.getNotes().isBlank() ? null : request.getNotes())
                .build();

        kyc = customerKycRepository.save(kyc);

        respond(
                responseObserver,
                CustomerKycResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("KYC record added successfully")
                        .setKyc(toProto(kyc))
                        .build());
    }

    @Override
    public void updateKycVerification(
            UpdateKycVerificationRequest request, StreamObserver<CustomerKycResponse> responseObserver) {
        customerKycRepository
                .findById(request.getKycId())
                .ifPresentOrElse(
                        kyc -> {
                            KycVerificationStatus status = KycVerificationStatus.valueOf(
                                    request.getVerificationStatus().name());
                            kyc.setVerificationStatus(status);
                            if (!request.getVerifiedBy().isBlank()) kyc.setVerifiedBy(request.getVerifiedBy());
                            if (!request.getRejectionReason().isBlank())
                                kyc.setRejectionReason(request.getRejectionReason());
                            if (status == KycVerificationStatus.VERIFIED) {
                                kyc.setVerifiedAt(LocalDateTime.now());
                                customerRepository.findById(kyc.getCustomerId()).ifPresent(customer -> {
                                    customer.setKycVerified(true);
                                    customerRepository.save(customer);
                                });
                            }
                            CustomerKyc saved = customerKycRepository.save(kyc);
                            respond(
                                    responseObserver,
                                    CustomerKycResponse.newBuilder()
                                            .setSuccess(true)
                                            .setMessage("KYC verification updated successfully")
                                            .setKyc(toProto(saved))
                                            .build());
                        },
                        () -> respond(
                                responseObserver,
                                CustomerKycResponse.newBuilder()
                                        .setSuccess(false)
                                        .setMessage("KYC record not found with id: " + request.getKycId())
                                        .build()));
    }

    @Override
    public void getCustomerKyc(GetCustomerKycRequest request, StreamObserver<CustomerKycResponse> responseObserver) {
        customerKycRepository
                .findById(request.getKycId())
                .ifPresentOrElse(
                        kyc -> respond(
                                responseObserver,
                                CustomerKycResponse.newBuilder()
                                        .setSuccess(true)
                                        .setMessage("KYC record found")
                                        .setKyc(toProto(kyc))
                                        .build()),
                        () -> respond(
                                responseObserver,
                                CustomerKycResponse.newBuilder()
                                        .setSuccess(false)
                                        .setMessage("KYC record not found with id: " + request.getKycId())
                                        .build()));
    }

    @Override
    public void listCustomerKyc(
            ListCustomerKycRequest request, StreamObserver<ListCustomerKycResponse> responseObserver) {
        List<CustomerKyc> records = customerKycRepository.findByCustomerId(request.getCustomerId());

        ListCustomerKycResponse.Builder builder =
                ListCustomerKycResponse.newBuilder().setSuccess(true).setMessage("KYC records retrieved successfully");

        records.forEach(kyc -> builder.addKycRecords(toProto(kyc)));
        respond(responseObserver, builder.build());
    }

    private com.arister.proto.CustomerKyc toProto(CustomerKyc kyc) {
        com.arister.proto.CustomerKyc.Builder builder = com.arister.proto.CustomerKyc.newBuilder()
                .setId(kyc.getId())
                .setCustomerId(kyc.getCustomerId())
                .setDocumentType(kyc.getDocumentType() != null ? kyc.getDocumentType() : "")
                .setDocumentNumber(kyc.getDocumentNumber() != null ? kyc.getDocumentNumber() : "")
                .setIssuingCountry(kyc.getIssuingCountry() != null ? kyc.getIssuingCountry() : "")
                .setIssuingAuthority(kyc.getIssuingAuthority() != null ? kyc.getIssuingAuthority() : "")
                .setVerifiedBy(kyc.getVerifiedBy() != null ? kyc.getVerifiedBy() : "")
                .setRejectionReason(kyc.getRejectionReason() != null ? kyc.getRejectionReason() : "")
                .setNotes(kyc.getNotes() != null ? kyc.getNotes() : "")
                .setCreatedAt(kyc.getCreatedAt() != null ? kyc.getCreatedAt().format(DT_FMT) : "");

        if (kyc.getIssueDate() != null) {
            builder.setIssueDate(kyc.getIssueDate().format(DATE_FMT));
        }
        if (kyc.getExpiryDate() != null) {
            builder.setExpiryDate(kyc.getExpiryDate().format(DATE_FMT));
        }
        if (kyc.getVerificationStatus() != null) {
            builder.setVerificationStatus(com.arister.proto.KycVerificationStatus.valueOf(
                    kyc.getVerificationStatus().name()));
        }
        if (kyc.getVerifiedAt() != null) {
            builder.setVerifiedAt(kyc.getVerifiedAt().format(DT_FMT));
        }
        return builder.build();
    }

    private <T> void respond(StreamObserver<T> observer, T response) {
        observer.onNext(response);
        observer.onCompleted();
    }
}
