package com.invest.application.ports.in;

import com.invest.application.commands.AlertFilterCommand;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import com.invest.application.responses.AlertResponse;

public interface ListAlertHistoryUseCase {

    PageResult<AlertResponse> execute(Long userId, AlertFilterCommand filter, PageRequest pageRequest);
}
