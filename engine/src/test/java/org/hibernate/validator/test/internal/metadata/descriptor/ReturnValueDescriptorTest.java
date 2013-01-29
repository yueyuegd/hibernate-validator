/*
* JBoss, Home of Professional Open Source
* Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.validator.test.internal.metadata.descriptor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.metadata.ElementDescriptor;
import javax.validation.metadata.MethodDescriptor;
import javax.validation.metadata.ReturnValueDescriptor;
import javax.validation.metadata.Scope;

import org.testng.annotations.Test;

import org.hibernate.validator.internal.metadata.descriptor.ReturnValueDescriptorImpl;
import org.hibernate.validator.test.internal.metadata.CustomerRepository;
import org.hibernate.validator.test.internal.metadata.CustomerRepository.ValidationGroup;
import org.hibernate.validator.test.internal.metadata.CustomerRepositoryExt;
import org.hibernate.validator.testutil.TestForIssue;

import static org.hibernate.validator.testutil.ConstraintViolationAssert.assertConstraintTypes;
import static org.hibernate.validator.testutil.ValidatorUtil.getMethodReturnValueDescriptor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@Test
public class ReturnValueDescriptorTest {

	@Test
	public void testElementDescriptorType() {
		ElementDescriptor elementDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"foo"
		);
		assertEquals( elementDescriptor.getKind(), ElementDescriptor.Kind.RETURN_VALUE );
	}

	@Test
	public void testIsCascaded() {
		ElementDescriptor elementDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"foo"
		);
		assertTrue( elementDescriptor.as( ReturnValueDescriptor.class ).isCascaded() );
	}

	@Test
	public void testIsNotCascaded() {
		ElementDescriptor elementDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"bar"
		);
		assertFalse( elementDescriptor.as( ReturnValueDescriptor.class ).isCascaded() );
	}

	@Test
	public void testNarrowDescriptor() {
		ElementDescriptor elementDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"bar"
		);

		ReturnValueDescriptor returnValueDescriptor = elementDescriptor.as( ReturnValueDescriptor.class );
		assertTrue( returnValueDescriptor != null );

		returnValueDescriptor = elementDescriptor.as( ReturnValueDescriptorImpl.class );
		assertTrue( returnValueDescriptor != null );
	}

	@Test(expectedExceptions = ClassCastException.class,
			expectedExceptionsMessageRegExp = "HV000118.*")
	public void testUnableToNarrowDescriptor() {
		ElementDescriptor elementDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"bar"
		);
		elementDescriptor.as( MethodDescriptor.class );
	}

	@Test
	public void testHasConstraints() {
		ReturnValueDescriptor unconstrainedReturnValueDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"foo"
		);
		assertFalse( unconstrainedReturnValueDescriptor.hasConstraints() );

		ReturnValueDescriptor constrainedReturnDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"bar"
		);
		assertTrue( constrainedReturnDescriptor.hasConstraints() );
	}

	@Test
	public void testHasConstraintsConsidersConstraintsFromSuperType() {
		ReturnValueDescriptor constrainedReturnDescriptor = getMethodReturnValueDescriptor(
				CustomerRepositoryExt.class,
				"bar"
		);
		assertTrue( constrainedReturnDescriptor.hasConstraints() );
	}

	@Test
	public void testGetConstraintDescriptors() {
		ReturnValueDescriptor unconstrainedReturnValueDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"foo"
		);
		assertTrue( unconstrainedReturnValueDescriptor.getConstraintDescriptors().isEmpty() );

		ReturnValueDescriptor constrainedReturnValueDescriptor = getMethodReturnValueDescriptor(
				CustomerRepository.class,
				"bar"
		);
		assertConstraintTypes(
				constrainedReturnValueDescriptor.getConstraintDescriptors(),
				NotNull.class
		);
	}

	@Test
	public void testGetConstraintDescriptorsConsidersConstraintsFromSuperType() {
		ReturnValueDescriptor returnValueDescriptor = getMethodReturnValueDescriptor(
				CustomerRepositoryExt.class,
				"baz"
		);
		assertConstraintTypes(
				returnValueDescriptor.getConstraintDescriptors(),
				Min.class,
				NotNull.class
		);
	}

	@TestForIssue(jiraKey = "HV-443")
	@Test
	public void testConstraintsLookingAt() {
		ReturnValueDescriptor returnValueDescriptor = getMethodReturnValueDescriptor(
				CustomerRepositoryExt.class,
				"baz"
		);

		assertConstraintTypes(
				returnValueDescriptor.findConstraints()
						.lookingAt( Scope.LOCAL_ELEMENT )
						.getConstraintDescriptors(), Min.class
		);
		assertConstraintTypes(
				returnValueDescriptor.findConstraints()
						.lookingAt( Scope.HIERARCHY )
						.getConstraintDescriptors(), Min.class, NotNull.class
		);
	}

	@Test
	public void testFindConstraintMatchingGroups() {
		ReturnValueDescriptor returnValueDescriptor = getMethodReturnValueDescriptor(
				CustomerRepositoryExt.class,
				"baz"
		);
		assertConstraintTypes(
				returnValueDescriptor.findConstraints()
						.unorderedAndMatchingGroups( ValidationGroup.class )
						.getConstraintDescriptors(), NotNull.class
		);
	}
}