package net.diva.browser.common;

import java.math.BigDecimal;

public class Representation {
	protected Representation() {}

	public static Representation getInstance() {
		return new Representation();
	}

	public String difficulty(int value) {
		return BigDecimal.valueOf(value, 1).stripTrailingZeros().toPlainString();
	}
}
