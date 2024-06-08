package ru.mts.depositservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.Request;
import ru.mts.depositservice.model.*;
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

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.OK)
    public void requestForOpeningDeposit(@RequestBody OpenDepositRequest openDepositRequest) {
        requestService.openRequest(openDepositRequest);
    }

    @PostMapping("/open/confirm")
    public ResponseEntity<RequestResponse> confirmRequestOpenDeposit(@RequestBody OpenDepositRequest openDepositRequest) {
        RequestResponse response = confirmationService.confirmOpenRequest(openDepositRequest);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<DepositAccountResponse> showDepositsAndRejectedRequests(@PathVariable Integer customerId) {
        DepositAccountResponse depositAccountResponse = new DepositAccountResponse();

        List<Request> requests = requestService.findRejectedRequests(customerId);
        List<Deposit> deposits = depositService.findOpenedDeposits(customerId);

        depositAccountResponse.setRejectedRequests(requests);
        depositAccountResponse.setDepositList(deposits);

        return new ResponseEntity<>(depositAccountResponse, HttpStatus.FOUND);
    }

    @GetMapping("/refill")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestRefillDepositAccount() {
        requestService.refillRequest();
    }

    @PostMapping("/refill/confirm")
    public ResponseEntity<RequestResponse> refillDepositAccount(@RequestBody RefillDepositRequest request) {
        RequestResponse response = confirmationService.confirmRefillDeposit(request);

        return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
    }

    @GetMapping("/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void applicationForClosingDepositAccount() {
        requestService.closeRequest();
    }

    @PostMapping("/close/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmCloseDeposit(@RequestBody CloseDepositRequest request) {
        confirmationService.confirmCloseDeposit(request);
    }
}
