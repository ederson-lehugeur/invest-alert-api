package com.invest.application.ports.in;

import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;
import com.invest.application.responses.AssetResponse;

public interface ListAssetsUseCase {

    PageResult<AssetResponse> execute(PageRequest pageRequest);
}
