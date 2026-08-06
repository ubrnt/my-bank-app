package ru.yandex.practicum.mybank.notifications.outbox;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.persistence.autoconfigure.EntityScanPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

class NotificationsOutboxEntityRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		List<String> packages = new ArrayList<>();

		if (registry instanceof BeanFactory beanFactory && AutoConfigurationPackages.has(beanFactory)) {
			packages.addAll(AutoConfigurationPackages.get(beanFactory));
		}
		packages.add(NotificationsOutboxEvent.class.getPackageName());

		EntityScanPackages.register(registry, packages.toArray(new String[0]));
	}
}
