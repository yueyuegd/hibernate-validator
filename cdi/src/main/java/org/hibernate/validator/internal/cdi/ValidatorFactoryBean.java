/*
 * Hibernate Validator, declare and validate application constraints
 *
 * License: Apache License, Version 2.0
 * See the license.txt file in the root directory or <http://www.apache.org/licenses/LICENSE-2.0>.
 */
package org.hibernate.validator.internal.cdi;

import static org.hibernate.validator.internal.util.CollectionHelper.newHashSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.validation.BootstrapConfiguration;
import javax.validation.ClockProvider;
import javax.validation.Configuration;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.ParameterNameProvider;
import javax.validation.TraversableResolver;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.internal.util.CollectionHelper;
import org.hibernate.validator.internal.util.classhierarchy.ClassHierarchyHelper;
import org.hibernate.validator.internal.util.privilegedactions.LoadClass;

/**
 * A {@link Bean} representing a {@link ValidatorFactory}. There is one instance of this type representing the default
 * validator factory and optionally another instance representing the HV validator factory in case the default provider
 * is not HV.
 *
 * @author Hardy Ferentschik
 * @author Gunnar Morling
 * @author Guillaume Smet
 */
public class ValidatorFactoryBean implements Bean<ValidatorFactory>, PassivationCapable {

	private final BeanManager beanManager;
	private final Set<DestructibleBeanInstance<?>> destructibleResources;
	private final ValidationProviderHelper validationProviderHelper;
	private final Set<Type> types;

	public ValidatorFactoryBean(BeanManager beanManager, ValidationProviderHelper validationProviderHelper) {
		this.beanManager = beanManager;
		this.destructibleResources = newHashSet( 5 );
		this.validationProviderHelper = validationProviderHelper;
		this.types = Collections.unmodifiableSet(
				CollectionHelper.<Type>newHashSet(
						ClassHierarchyHelper.getHierarchy( validationProviderHelper.getValidatorFactoryBeanClass() )
				)
		);
	}

	@Override
	public Class<?> getBeanClass() {
		return validationProviderHelper.getValidatorFactoryBeanClass();
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return Collections.emptySet();
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		return validationProviderHelper.getQualifiers();
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return ApplicationScoped.class;
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		return Collections.emptySet();
	}

	@Override
	public Set<Type> getTypes() {
		return types;
	}

	@Override
	public boolean isAlternative() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public ValidatorFactory create(CreationalContext<ValidatorFactory> ctx) {
		Configuration<?> config = getConfiguration();

		config.constraintValidatorFactory( createConstraintValidatorFactory( config ) );
		config.messageInterpolator( createMessageInterpolator( config ) );
		config.traversableResolver( createTraversableResolver( config ) );
		config.parameterNameProvider( createParameterNameProvider( config ) );
		config.clockProvider( createClockProvider( config ) );

		return config.buildValidatorFactory();
	}

	@Override
	public void destroy(ValidatorFactory instance, CreationalContext<ValidatorFactory> ctx) {
		for ( DestructibleBeanInstance<?> resource : destructibleResources ) {
			resource.destroy();
		}
		instance.close();
	}

	private MessageInterpolator createMessageInterpolator(Configuration<?> config) {
		BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
		String messageInterpolatorFqcn = bootstrapConfiguration.getMessageInterpolatorClassName();

		if ( messageInterpolatorFqcn == null ) {
			return config.getDefaultMessageInterpolator();
		}

		@SuppressWarnings("unchecked")
		Class<? extends MessageInterpolator> messageInterpolatorClass = (Class<? extends MessageInterpolator>) run(
				LoadClass.action(
						messageInterpolatorFqcn,
						null
				)
		);

		return createInstance( messageInterpolatorClass );
	}

	private TraversableResolver createTraversableResolver(Configuration<?> config) {
		BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
		String traversableResolverFqcn = bootstrapConfiguration.getTraversableResolverClassName();

		if ( traversableResolverFqcn == null ) {
			return config.getDefaultTraversableResolver();
		}

		@SuppressWarnings("unchecked")
		Class<? extends TraversableResolver> traversableResolverClass = (Class<? extends TraversableResolver>) run(
				LoadClass.action(
						traversableResolverFqcn,
						null
				)
		);

		return createInstance( traversableResolverClass );
	}

	private ParameterNameProvider createParameterNameProvider(Configuration<?> config) {
		BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
		String parameterNameProviderFqcn = bootstrapConfiguration.getParameterNameProviderClassName();

		if ( parameterNameProviderFqcn == null ) {
			return config.getDefaultParameterNameProvider();
		}

		@SuppressWarnings("unchecked")
		Class<? extends ParameterNameProvider> parameterNameProviderClass = (Class<? extends ParameterNameProvider>) run(
				LoadClass.action(
						parameterNameProviderFqcn,
						null
				)
		);

		return createInstance( parameterNameProviderClass );
	}

	private ClockProvider createClockProvider(Configuration<?> config) {
		BootstrapConfiguration bootstrapConfiguration = config.getBootstrapConfiguration();
		String clockProviderFqcn = bootstrapConfiguration.getClockProviderClassName();

		if ( clockProviderFqcn == null ) {
			return config.getDefaultClockProvider();
		}

		@SuppressWarnings("unchecked")
		Class<? extends ClockProvider> clockProviderClass = (Class<? extends ClockProvider>) run(
				LoadClass.action(
						clockProviderFqcn,
						null
				)
		);

		return createInstance( clockProviderClass );
	}

	private ConstraintValidatorFactory createConstraintValidatorFactory(Configuration<?> config) {
		BootstrapConfiguration configSource = config.getBootstrapConfiguration();
		String constraintValidatorFactoryFqcn = configSource.getConstraintValidatorFactoryClassName();

		if ( constraintValidatorFactoryFqcn == null ) {
			// use default
			return createInstance( InjectingConstraintValidatorFactory.class );
		}

		@SuppressWarnings("unchecked")
		Class<? extends ConstraintValidatorFactory> constraintValidatorFactoryClass = (Class<? extends ConstraintValidatorFactory>) run(
				LoadClass.action(
						constraintValidatorFactoryFqcn,
						null
				)
		);

		return createInstance( constraintValidatorFactoryClass );
	}

	private <T> T createInstance(Class<T> type) {
		DestructibleBeanInstance<T> destructibleInstance = new DestructibleBeanInstance<T>( beanManager, type );
		destructibleResources.add( destructibleInstance );
		return destructibleInstance.getInstance();
	}

	private Configuration<?> getConfiguration() {
		return validationProviderHelper.isDefaultProvider() ?
				Validation.byDefaultProvider().configure() :
				Validation.byProvider( org.hibernate.validator.HibernateValidator.class ).configure();
	}

	/**
	 * Runs the given privileged action, using a privileged block if required.
	 * <p>
	 * <b>NOTE:</b> This must never be changed into a publicly available method to avoid execution of arbitrary
	 * privileged actions within HV's protection domain.
	 */
	private <T> T run(PrivilegedAction<T> action) {
		return System.getSecurityManager() != null ? AccessController.doPrivileged( action ) : action.run();
	}

	@Override
	public String getId() {
		return ValidatorFactoryBean.class.getName() + "_" + ( validationProviderHelper.isDefaultProvider() ? "default" : "hv" );
	}

	@Override
	public String toString() {
		return "ValidatorFactoryBean [id=" + getId() + "]";
	}
}
