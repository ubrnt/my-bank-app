package ru.yandex.practicum.mybank.chassis.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BatchProcessor<T> {

	private static final Logger log = LoggerFactory.getLogger(BatchProcessor.class);

	private final Supplier<List<T>> source;
	private final Consumer<T> action;
	private final Consumer<T> onSuccess;
	private final BiConsumer<T, RuntimeException> onFailure;

	public BatchProcessor(Supplier<List<T>> source, Consumer<T> action, Consumer<T> onSuccess,
			BiConsumer<T, RuntimeException> onFailure) {
		this.source = source;
		this.action = action;
		this.onSuccess = onSuccess;
		this.onFailure = onFailure;
	}

	public void run() {
		List<T> items = source.get();
		if (items.isEmpty()) {
			return;
		}

		log.debug("Processing {} items", items.size());

		for (T item : items) {
			try {
				action.accept(item);
				onSuccess.accept(item);
			} catch (RuntimeException e) {
				onFailure.accept(item, e);
			}
		}
	}
}
