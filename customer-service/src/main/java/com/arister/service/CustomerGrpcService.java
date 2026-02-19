package com.arister.service;

import com.arister.enums.CustomerStatus;
import com.arister.enums.IdType;
import com.arister.model.Customer;
import com.arister.proto.CreateCustomerRequest;
import com.arister.proto.CustomerResponse;
import com.arister.proto.CustomerServiceGrpc;
import com.arister.proto.DeleteCustomerRequest;
import com.arister.proto.DeleteCustomerResponse;
import com.arister.proto.GetCustomerByCodeRequest;
import com.arister.proto.GetCustomerRequest;
import com.arister.proto.ListCustomersRequest;
import com.arister.proto.ListCustomersResponse;
import com.arister.proto.UpdateCustomerRequest;
import com.arister.repository.CustomerRepository;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@GrpcService
public class CustomerGrpcService extends CustomerServiceGrpc.CustomerServiceImplBase {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final CustomerRepository customerRepository;

    public CustomerGrpcService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public void createCustomer(CreateCustomerRequest request, StreamObserver<CustomerResponse> responseObserver) {
        if (request.getFirstName().isBlank()
                || request.getLastName().isBlank()
                || request.getEmail().isBlank()) {
            respond(
                    responseObserver,
                    CustomerResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("first_name, last_name, and email are required")
                            .build());
            return;
        }

        if (customerRepository.existsByEmail(request.getEmail())) {
            respond(
                    responseObserver,
                    CustomerResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Customer with that email already exists")
                            .build());
            return;
        }

        String idNumber = request.getIdNumber().isBlank() ? null : request.getIdNumber();
        if (idNumber != null && customerRepository.existsByIdNumber(idNumber)) {
            respond(
                    responseObserver,
                    CustomerResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Customer with that id_number already exists")
                            .build());
            return;
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone().isBlank() ? null : request.getPhone())
                .dateOfBirth(
                        request.getDateOfBirth().isBlank() ? null : LocalDate.parse(request.getDateOfBirth(), DATE_FMT))
                .address(request.getAddress().isBlank() ? null : request.getAddress())
                .idType(idNumber != null ? IdType.valueOf(request.getIdType().name()) : null)
                .idNumber(idNumber)
                .kycVerified(false)
                .status(CustomerStatus.ACTIVE)
                .branchId(request.getBranchId() == 0 ? null : request.getBranchId())
                .build();

        customer = customerRepository.save(customer);

        respond(
                responseObserver,
                CustomerResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Customer created successfully")
                        .setCustomer(toProto(customer))
                        .build());
    }

    @Override
    public void getCustomer(GetCustomerRequest request, StreamObserver<CustomerResponse> responseObserver) {
        customerRepository
                .findById(request.getId())
                .ifPresentOrElse(
                        customer -> respond(
                                responseObserver,
                                CustomerResponse.newBuilder()
                                        .setSuccess(true)
                                        .setMessage("Customer found")
                                        .setCustomer(toProto(customer))
                                        .build()),
                        () -> respond(
                                responseObserver,
                                CustomerResponse.newBuilder()
                                        .setSuccess(false)
                                        .setMessage("Customer not found with id: " + request.getId())
                                        .build()));
    }

    @Override
    public void getCustomerByCode(GetCustomerByCodeRequest request, StreamObserver<CustomerResponse> responseObserver) {
        customerRepository
                .findByCustomerCode(request.getCustomerCode())
                .ifPresentOrElse(
                        customer -> respond(
                                responseObserver,
                                CustomerResponse.newBuilder()
                                        .setSuccess(true)
                                        .setMessage("Customer found")
                                        .setCustomer(toProto(customer))
                                        .build()),
                        () -> respond(
                                responseObserver,
                                CustomerResponse.newBuilder()
                                        .setSuccess(false)
                                        .setMessage("Customer not found with code: " + request.getCustomerCode())
                                        .build()));
    }

    @Override
    public void updateCustomer(UpdateCustomerRequest request, StreamObserver<CustomerResponse> responseObserver) {
        customerRepository
                .findById(request.getId())
                .ifPresentOrElse(
                        customer -> {
                            if (!request.getFirstName().isBlank()) customer.setFirstName(request.getFirstName());
                            if (!request.getLastName().isBlank()) customer.setLastName(request.getLastName());
                            if (!request.getEmail().isBlank()) customer.setEmail(request.getEmail());
                            if (!request.getPhone().isBlank()) customer.setPhone(request.getPhone());
                            if (!request.getAddress().isBlank()) customer.setAddress(request.getAddress());
                            customer.setStatus(
                                    CustomerStatus.valueOf(request.getStatus().name()));
                            Customer saved = customerRepository.save(customer);
                            respond(
                                    responseObserver,
                                    CustomerResponse.newBuilder()
                                            .setSuccess(true)
                                            .setMessage("Customer updated successfully")
                                            .setCustomer(toProto(saved))
                                            .build());
                        },
                        () -> respond(
                                responseObserver,
                                CustomerResponse.newBuilder()
                                        .setSuccess(false)
                                        .setMessage("Customer not found with id: " + request.getId())
                                        .build()));
    }

    @Override
    public void deleteCustomer(DeleteCustomerRequest request, StreamObserver<DeleteCustomerResponse> responseObserver) {
        if (!customerRepository.existsById(request.getId())) {
            respond(
                    responseObserver,
                    DeleteCustomerResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Customer not found with id: " + request.getId())
                            .build());
            return;
        }
        customerRepository.deleteById(request.getId());
        respond(
                responseObserver,
                DeleteCustomerResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Customer deleted successfully")
                        .build());
    }

    @Override
    public void listCustomers(ListCustomersRequest request, StreamObserver<ListCustomersResponse> responseObserver) {
        int page = request.getPage();
        int size = request.getSize() == 0 ? 20 : request.getSize();
        Page<Customer> customerPage = customerRepository.findAll(PageRequest.of(page, size));

        ListCustomersResponse.Builder builder = ListCustomersResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Customers retrieved successfully")
                .setTotalCount((int) customerPage.getTotalElements())
                .setPage(page)
                .setSize(size);

        customerPage.getContent().forEach(c -> builder.addCustomers(toProto(c)));
        respond(responseObserver, builder.build());
    }

    private com.arister.proto.Customer toProto(Customer customer) {
        com.arister.proto.Customer.Builder builder = com.arister.proto.Customer.newBuilder()
                .setId(customer.getId())
                .setCustomerCode(customer.getCustomerCode() != null ? customer.getCustomerCode() : "")
                .setFirstName(customer.getFirstName())
                .setLastName(customer.getLastName())
                .setEmail(customer.getEmail())
                .setPhone(customer.getPhone() != null ? customer.getPhone() : "")
                .setAddress(customer.getAddress() != null ? customer.getAddress() : "")
                .setIdNumber(customer.getIdNumber() != null ? customer.getIdNumber() : "")
                .setKycVerified(customer.isKycVerified())
                .setCreatedAt(
                        customer.getCreatedAt() != null
                                ? customer.getCreatedAt().format(DT_FMT)
                                : "");

        if (customer.getDateOfBirth() != null) {
            builder.setDateOfBirth(customer.getDateOfBirth().format(DATE_FMT));
        }
        if (customer.getIdType() != null) {
            builder.setIdType(
                    com.arister.proto.IdType.valueOf(customer.getIdType().name()));
        }
        if (customer.getStatus() != null) {
            builder.setStatus(com.arister.proto.CustomerStatus.valueOf(
                    customer.getStatus().name()));
        }
        if (customer.getBranchId() != null) {
            builder.setBranchId(customer.getBranchId());
        }
        return builder.build();
    }

    private <T> void respond(StreamObserver<T> observer, T response) {
        observer.onNext(response);
        observer.onCompleted();
    }
}
