package com.invest.application.ports.in;

import com.invest.application.responses.AssetResponse;

public interface GetAssetUseCase {

    AssetResponse execute(String ticker);
}
