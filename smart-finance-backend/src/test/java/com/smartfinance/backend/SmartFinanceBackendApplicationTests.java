package com.smartfinance.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

class SmartFinanceBackendApplicationTests {

	@Test
	void applicationClassIsInstantiable() throws Exception {
		Assertions.assertNotNull(SmartFinanceBackendApplication.class.getDeclaredConstructor().newInstance());
	}

}
