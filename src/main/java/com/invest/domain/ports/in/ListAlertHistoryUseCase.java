package com.invest.domain.ports.in;

import com.invest.application.commands.AlertFilterCommand;
import com.invest.application.responses.AlertResponse;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;

public interface ListAlertHistoryUseCase {

    PageResult<AlertResponse> execute(Long userId, AlertFilterCommand filter, PageRequest pageRequest);
}
