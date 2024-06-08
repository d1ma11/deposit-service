package ru.mts.depositservice.model;

import lombok.Data;
import ru.mts.depositservice.entity.Deposit;
import ru.mts.depositservice.entity.Request;

import java.util.List;

@Data
public class DepositAccountResponse {
    private List<Deposit> depositList;
    private List<Request> rejectedRequests;
}
