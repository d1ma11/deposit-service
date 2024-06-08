package ru.mts.depositservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.model.DepositAccountResponse;
import ru.mts.depositservice.model.OpenDepositRequest;
import ru.mts.depositservice.model.RefillDepositRequest;
import ru.mts.depositservice.model.RequestResponse;
import ru.mts.depositservice.service.DepositService;
import ru.mts.depositservice.service.RequestConfirmationService;
import ru.mts.depositservice.service.RequestService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/deposit")
public class DepositController {

    private final DepositService depositService;
    private final RequestService requestService;
    private final RequestConfirmationService confirmationService;

    @GetMapping("/check")
    public ResponseEntity<BigDecimal> checkDepositTerms(@RequestBody OpenDepositRequest request) {

        BigDecimal rate = depositService.calculateInterestRate(request);

        return ResponseEntity.ok(rate);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void applicationForOpeningDeposit(@RequestBody OpenDepositRequest openDepositRequest) {
        requestService.makeRequest(openDepositRequest);
    }

    @PostMapping("/confirm")
    public ResponseEntity<RequestResponse> confirmDepositRequest(@RequestBody OpenDepositRequest openDepositRequest) {
        RequestResponse response = confirmationService.confirmRequest(openDepositRequest);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<DepositAccountResponse> checkDepositsAndRequests(@PathVariable Integer customerId) {
        DepositAccountResponse depositAccountResponse = new DepositAccountResponse();

        List<Request> requests = requestService.findRejectedRequests(customerId);
        List<Deposit> deposits = depositService.findOpenedDeposits(customerId);

        depositAccountResponse.setRejectedRequests(requests);
        depositAccountResponse.setDepositList(deposits);

        return new ResponseEntity<>(depositAccountResponse, HttpStatus.FOUND);
    }

    @PostMapping("/refill")
    @ResponseStatus(HttpStatus.OK)
    public void refillDepositAccount(@RequestBody RefillDepositRequest request) {
        depositService.refillAccount(request);
    }
}
