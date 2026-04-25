package com.invest.domain.ports.in;

import com.invest.application.responses.AssetResponse;
import com.invest.domain.ports.out.PageRequest;
import com.invest.domain.ports.out.PageResult;

public interface ListAssetsUseCase {

    PageResult<AssetResponse> execute(PageRequest pageRequest);
}
