package net.diva.browser;


public enum SortOrder {
	by_name,
	by_difficulty,
	by_score,
	by_achivement,
	by_clear_status,
	by_trial_status,
	by_difference_to_saturation,
	by_original,
	by_publish_order,
	;

	public static SortOrder fromOrdinal(int ordinal) {
		for (SortOrder order: values()) {
			if (order.ordinal() == ordinal)
				return order;
		}
		return by_name;
	}
}
