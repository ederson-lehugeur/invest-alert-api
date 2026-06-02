package com.invest.application.ports.in;

import java.util.List;

public interface GetSupportedIndicatorsUseCase {

    List<String> execute(String assetType);
}
