package com.arister.service;

import com.arister.model.Branch;
import com.arister.proto.BranchResponse;
import com.arister.proto.BranchServiceGrpc;
import com.arister.proto.CreateBranchRequest;
import com.arister.proto.DeleteBranchRequest;
import com.arister.proto.DeleteBranchResponse;
import com.arister.proto.GetBranchByCodeRequest;
import com.arister.proto.GetBranchRequest;
import com.arister.proto.ListBranchesRequest;
import com.arister.proto.ListBranchesResponse;
import com.arister.proto.UpdateBranchRequest;
import com.arister.repository.BranchRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@GrpcService
@RequiredArgsConstructor
public class BranchGrpcService extends BranchServiceGrpc.BranchServiceImplBase {

    private final BranchRepository branchRepository;

    @Override
    public void createBranch(CreateBranchRequest request, StreamObserver<BranchResponse> observer) {
        if (request.getBranchCode().isBlank() || request.getBranchName().isBlank() || request.getCity().isBlank()) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription("branchCode, branchName and city are required").asRuntimeException());
            return;
        }
        if (branchRepository.existsByBranchCode(request.getBranchCode())) {
            observer.onError(Status.ALREADY_EXISTS.withDescription("Branch code already exists: " + request.getBranchCode()).asRuntimeException());
            return;
        }
        Branch branch = Branch.builder()
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                .city(request.getCity())
                .state(request.getState())
                .phone(request.getPhone())
                .active(true)
                .build();
        respond(observer, "Branch created successfully", branchRepository.save(branch));
    }

    @Override
    public void getBranch(GetBranchRequest request, StreamObserver<BranchResponse> observer) {
        branchRepository.findById(request.getId())
                .ifPresentOrElse(
                        b -> respond(observer, "Branch found", b),
                        () -> notFound(observer, "Branch not found: " + request.getId()));
    }

    @Override
    public void getBranchByCode(GetBranchByCodeRequest request, StreamObserver<BranchResponse> observer) {
        branchRepository.findByBranchCode(request.getBranchCode())
                .ifPresentOrElse(
                        b -> respond(observer, "Branch found", b),
                        () -> notFound(observer, "Branch not found: " + request.getBranchCode()));
    }

    @Override
    public void updateBranch(UpdateBranchRequest request, StreamObserver<BranchResponse> observer) {
        branchRepository.findById(request.getId())
                .ifPresentOrElse(branch -> {
                    if (!request.getBranchName().isBlank()) branch.setBranchName(request.getBranchName());
                    if (!request.getCity().isBlank()) branch.setCity(request.getCity());
                    if (!request.getState().isBlank()) branch.setState(request.getState());
                    if (!request.getPhone().isBlank()) branch.setPhone(request.getPhone());
                    branch.setActive(request.getIsActive());
                    respond(observer, "Branch updated successfully", branchRepository.save(branch));
                }, () -> notFound(observer, "Branch not found: " + request.getId()));
    }

    @Override
    public void deleteBranch(DeleteBranchRequest request, StreamObserver<DeleteBranchResponse> observer) {
        if (!branchRepository.existsById(request.getId())) {
            observer.onNext(DeleteBranchResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Branch not found: " + request.getId())
                    .build());
            observer.onCompleted();
            return;
        }
        branchRepository.deleteById(request.getId());
        observer.onNext(DeleteBranchResponse.newBuilder().setSuccess(true).setMessage("Branch deleted successfully").build());
        observer.onCompleted();
    }

    @Override
    public void listBranches(ListBranchesRequest request, StreamObserver<ListBranchesResponse> observer) {
        int page = request.getPage();
        int size = request.getSize() > 0 ? request.getSize() : 20;
        Page<Branch> result = branchRepository.findAll(PageRequest.of(page, size));
        observer.onNext(ListBranchesResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Branches fetched")
                .addAllBranches(result.getContent().stream().map(this::toProto).toList())
                .setTotalCount((int) result.getTotalElements())
                .setPage(page)
                .setSize(size)
                .build());
        observer.onCompleted();
    }

    private void respond(StreamObserver<BranchResponse> observer, String message, Branch branch) {
        observer.onNext(BranchResponse.newBuilder().setSuccess(true).setMessage(message).setBranch(toProto(branch)).build());
        observer.onCompleted();
    }

    private void notFound(StreamObserver<BranchResponse> observer, String message) {
        observer.onNext(BranchResponse.newBuilder().setSuccess(false).setMessage(message).build());
        observer.onCompleted();
    }

    private com.arister.proto.Branch toProto(Branch b) {
        return com.arister.proto.Branch.newBuilder()
                .setId(b.getId())
                .setBranchCode(b.getBranchCode())
                .setBranchName(b.getBranchName())
                .setCity(b.getCity())
                .setState(b.getState() != null ? b.getState() : "")
                .setPhone(b.getPhone() != null ? b.getPhone() : "")
                .setIsActive(b.isActive())
                .setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : "")
                .build();
    }
}
